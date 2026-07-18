# Tech Lead Simulator — Backend MVP Design

**Date:** 2026-07-17
**Status:** Approved (design), pending implementation plan
**Contract:** `openapi.yaml` (repo root) is the single source of truth for the BE ↔ FE API.

## Goal

Implement the backend for the Tech Lead Simulator MVP: a game where the player
interviews 4 virtual candidates over N rounds (question → candidates answer, one
is correct → player picks), then sees per-candidate statistics, makes an offer,
and reads a score result plus an AI-style learning roadmap.

All 8 endpoints from `openapi.yaml` must be implemented with matching
`operationId`s, request/response schemas, and status codes.

## Locked decisions

These were confirmed with the product owner during brainstorming:

| Topic | Decision |
|-------|----------|
| **Persistence** | PostgreSQL + Spring Data JPA. State lives outside the instance so the API is stateless and horizontally scalable (satisfies the 98%/week availability NFR). |
| **Content source** | Seed data (questions, answers, candidates) behind a `QuestionProvider` interface, so an AI generator can replace it later without touching game logic. |
| **AI result** | Rule-based synchronous stub behind an `AiAnalyzer` interface. Always returns `READY` (HTTP 200). The 202/`PENDING` path stays in the contract but is unused in MVP. |
| **Player stats** | Global aggregates over ALL completed interviews (no auth / no player identity in the contract yet). |

## Non-goals (YAGNI for MVP)

- No authentication / no per-player identity.
- No real LLM integration (stubbed behind `AiAnalyzer`).
- No server-side answer timer enforcement (`timeLimitSeconds` is a client hint).
- No AI-generated questions (seed pool only).
- No async AI computation (synchronous, always `READY`).

## Architecture

Layered monolith. Package layout under `com.techleadsim`:

```
com.techleadsim
├── config/        CORS (:5173 → :8080), Jackson config
├── common/error/  Error DTO + @RestControllerAdvice (GlobalExceptionHandler)
├── home/          HomeController → HomeService, PlayerStatsService
├── interview/
│   ├── api/       Controllers (one method per operationId), request/response DTOs, mappers
│   ├── domain/    JPA entities + enums (Mode, Difficulty, InterviewStatus)
│   ├── repository/ Spring Data JPA repositories
│   └── service/   InterviewService, ScoringService, StatisticService
├── content/       QuestionProvider (interface) + SeedQuestionProvider
└── ai/            AiAnalyzer (interface) + RuleBasedAiAnalyzer
```

- Each OpenAPI endpoint maps to a controller method named after its `operationId`.
- `QuestionProvider` and `AiAnalyzer` are the seams where an LLM can be dropped in later.
- **DTOs are hand-written** to match the OpenAPI schemas (small surface — 8 endpoints;
  hand-writing avoids codegen dependencies and gives full control). Entities ≠ DTOs;
  mappers convert between them.
- **All endpoints are served under the `/api` base path** (`server.servlet.context-path=/api`
  or a shared mapping) — the frontend proxies `/api → :8080/api` and expects e.g.
  `GET /api/home`.

## Dependencies (Spring Boot 4.1.0, Java 25)

Add to `backend/pom.xml` (verified against Boot 4.1 docs via Context7):

- `spring-boot-starter-data-jpa`
- `org.postgresql:postgresql` (runtime)
- `org.flywaydb:flyway-core` + `org.flywaydb:flyway-database-postgresql`
- `spring-boot-starter-validation` (Bean Validation on request bodies)
- Test: `spring-boot-starter-data-jpa-test` (the JPA test slice, mirrors the existing
  `spring-boot-starter-webmvc-test`), Testcontainers (`org.testcontainers:postgresql`,
  `org.testcontainers:junit-jupiter`).

> Boot 4.x uses per-module test starters and dropped the `.RELEASE` version suffix —
> see `AGENTS.md`. Verify exact test-starter artifact name during implementation.

## Data model

Postgres schema created and seeded via Flyway migrations; JPA runs with
`ddl-auto=validate` (schema owned by migrations, not Hibernate).

### Seed (reference data, loaded by migration)

- **`candidate`** — 4 fixed candidates used in every game (MVP):
  `id, name, role, avatar_url, strengths` (strengths as a joined table or text array).
- **`question_template`** — `id, text, topic, difficulty, time_limit_seconds`.
  `topic` is an internal tag (NOT in the API contract) that links wrong answers to
  roadmap items.
- **`answer_template`** — `id, question_id, candidate_slot (0..3), text, is_correct`.
  Exactly one `is_correct = true` per question. Slot `i` maps to candidate `i`, so the
  "correct candidate" varies per question. **Seed is designed so candidates differ in
  competence** — a stronger candidate holds the correct answer on more questions
  (weighted toward their `strengths` topics). This makes per-candidate `correctAnswers`
  a meaningful hiring signal and keeps the AI `verdict` coherent.

### Game state

- **`interview`** — `id, mode, difficulty, player_name, status, hired_candidate_id,
  total_points, best_streak, created_at`.
  `status`: `IN_PROGRESS → STATISTIC → OFFERED → FINISHED`.
- **`interview_round`** — `id, interview_id, question_id, index, chosen_answer_id
  (nullable), correct (nullable), points_awarded`.
  At interview start, N rounds are created from a random selection of questions.

## Game rules (MVP defaults — approved)

- **Candidates:** 4, identical across all games.
- **Questions per run:** CLASSIC = 10, HARDCORE = 20 (from the `Mode` enum).
- **Scoring:** correct answer = `10 + 2 × (streak − 1)` points (base 10 + streak bonus).
  Wrong answer = 0 points and resets the current streak to 0.
- **`timeLimitSeconds`:** server returns a value (e.g. 45) but does NOT enforce it —
  it is a client-side hint (keeps the API stateless, no server timers).
- **Question selection by difficulty:** at start, pick questions whose `difficulty`
  matches the request; if the pool is short, top up from any difficulty. The frontend
  always sends `MEDIUM` (no difficulty UI in MVP), so the seed pool must hold **≥ 20
  MEDIUM questions** to cover HARDCORE without falling back.
- **Player stats (`/home`, global aggregates):**
  - `gamesPlayed` = count of completed interviews.
  - `winRate` = fraction of completed interviews with `correctCount ≥ 60%` of total.
  - `bestResult` = max `correctCount` across completed interviews.
  - `candidatesHired` = count of interviews with an offer made.

## Endpoint behavior

| operationId | Method / path | Notes |
|-------------|---------------|-------|
| `getHomePage` | `GET /home` | Title, modes (CLASSIC/HARDCORE), global `playerStats`. |
| `startInterview` | `POST /interviews` → 201 | Creates interview + N rounds; returns `interviewId` + candidate lineup. 400 on bad body. |
| `getQuestion` | `GET /interviews/{id}/question` | Next unanswered round as `Question` (index/total, answers, no correct flag). 404 no interview, 409 all answered. |
| `saveAnswer` | `POST /interviews/{id}/answers` | Records choice, returns feedback (correct, correctAnswerId, points, running score/streak, finished). 400 bad body, 404, 409 already answered. |
| `getInterviewStatistic` | `GET /interviews/{id}/statistic` | Per-candidate `timesChosen` (player's picks) **and `correctAnswers` (objective candidate competence)** + player `correctCount`. 404. |
| `offer` | `POST /interviews/{id}/offer` | Records hired candidate → status OFFERED. 400, 404. |
| `getInterviewResult` | `GET /interviews/{id}/result` | **Player's** score, points, best streak, per-question breakdown. Available once rounds are answered, **not gated on the offer**. 404. |
| `getAiInterviewResult` | `GET /interviews/{id}/ai-result` | Rule-based analysis, always 200 READY in MVP. 404. |

## AI analysis (RuleBasedAiAnalyzer)

Purely deterministic, derived from session data:

- **`summary`** — template from score/streak (e.g. "7/10 correct, best streak 4…").
- **`roadmap`** — wrong-answered rounds grouped by question `topic`; each group →
  a `RoadmapItem` with `priority = HIGH` when ≥ 2 misses in that topic, else MEDIUM/LOW,
  plus a `reason` and static `resources` from a `topic → resources` lookup table.
- **`verdict`** — compares the hired candidate's `strengths` against the player's weak
  topics ("Good hire — X covers your gaps in Y" / "Questionable — you're already strong there").
- `status` is always `READY`; response is HTTP 200.

Lives behind the `AiAnalyzer` interface so a real LLM implementation can replace it.

## Error handling

`@RestControllerAdvice` maps exceptions to the OpenAPI `Error` schema
`{ code, message, timestamp }`:

- `InterviewNotFoundException` → 404 `INTERVIEW_NOT_FOUND`
- Bean Validation failure → 400 `BAD_REQUEST`
- No unanswered round left → 409 `NO_QUESTION_AVAILABLE`
- Answering an already-answered round → 409 `QUESTION_ALREADY_ANSWERED`

## Testing (TDD)

- **Unit** (no Spring context): `ScoringService`, `RuleBasedAiAnalyzer`, `PlayerStatsService`.
- **Repository:** `@DataJpaTest` against Testcontainers Postgres.
- **Controller:** `@WebMvcTest` + MockMvc for status codes and JSON shape per endpoint.
- **Integration:** one `@SpringBootTest` that plays a full game end-to-end
  (start → 10 answers → statistic → offer → result → ai-result).
- **Test DB:** Testcontainers Postgres (matches production; higher fidelity than H2).

## Frontend alignment

Reconciled against `docs/archive/frontend-mvp-plan.md` (both sides share `openapi.yaml`):

- **Result before offer.** The FE fetches `/result` *before* the offer screen. Backend
  must NOT gate `/result`, `/statistic`, `/ai-result` on an offer — the status machine is
  advisory (see Error handling / Game rules).
- **Base path `/api`.** FE proxies `/api → :8080/api`; backend serves under `/api`.
- **Candidate competence in `/statistic`.** `correctAnswers` added to the contract
  (v0.2.0) so the FE offer screen can rank candidates objectively.
- **`avatarUrl` coordination.** Seed `candidate.avatar_url` values must match the static
  asset paths the FE serves (`/assets/candidates/*.png`, as in the OpenAPI examples).

Flagged for the **frontend** team (no backend action):

- **AI 202-poll is dead code against the real backend** — the stub always returns 200
  `READY`; the FE's PENDING/202 polling path never fires (harmless).
- **Mode labels** — FE maps "Быстрая игра" → `HARDCORE` (20 questions). A "quick" game
  with *more* questions looks like a mislabel worth double-checking (FE-side only).

## Open items for the implementation plan

- Confirm exact `spring-boot-starter-data-jpa-test` artifact name against Boot 4.1.
- Decide `strengths` storage (element-collection table vs Postgres text array).
- Flyway file layout: `V1__schema.sql`, `V2__seed_candidates.sql`,
  `V3__seed_questions.sql` (or a single seed migration).
