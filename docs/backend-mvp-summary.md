# Backend MVP — Implementation Summary

**Status:** ✅ Complete. All 15 planned tasks executed via TDD; full test suite **32/32 green**.
**Branch:** `backend` · **Final review:** *READY TO MERGE — yes-with-fixes* (fixes landed).
**Contract:** `openapi.yaml` (repo root) is the single source of truth; the implementation matches it.

## What was built

The complete backend for the Tech Lead Simulator game: the player interviews 4 virtual
candidates over N rounds (question → candidates answer, one is correct → player picks),
then sees per-candidate statistics, hires one candidate, and reads a score result plus a
rule-based "AI" learning roadmap. All **8 OpenAPI endpoints** are implemented under the
`/api` context path.

## Tech stack

| Concern | Choice |
|---|---|
| Framework | Spring Boot **4.1.0** (Spring MVC, `spring-boot-starter-webmvc`) on **Java 25** |
| Persistence | PostgreSQL + Spring Data JPA, `ddl-auto=validate` (schema owned by migrations) |
| Migrations / seed | Flyway (`V1__schema.sql`, `V2__seed.sql`) |
| Validation | Bean Validation (`spring-boot-starter-validation`) on request bodies |
| Tests | JUnit 5 + MockMvc against a **singleton Testcontainers Postgres** (prod-fidelity) |
| Base package | `com.techleadsim` · serves on `:8080`, context path `/api` |

## Architecture

Layered monolith — controllers → services → repositories, DTO-only responses (no entity
leakage). Two extension seams let an LLM replace the stubs later without touching game logic:

- **`QuestionProvider`** → `SeedQuestionProvider` (seed content; no LLM in MVP).
- **`AiAnalyzer`** → `RuleBasedAiAnalyzer` (deterministic, synchronous, always `READY`).

```
com.techleadsim
├── web/            controllers (InterviewController, HomeController) + web/dto/* records
├── service/        InterviewService, ScoringService, StatisticService, PlayerStatsService
├── domain/         JPA entities + enums (Mode, Difficulty, InterviewStatus)
├── repository/     Spring Data JPA repositories
├── content/        QuestionProvider + SeedQuestionProvider
├── ai/             AiAnalyzer + RuleBasedAiAnalyzer
├── error/          ApiException hierarchy + GlobalExceptionHandler (Error schema)
└── config/         WebCorsConfig
```

## Endpoints (all under `/api`)

| operationId | Method / path | Success | Errors |
|---|---|---|---|
| `getHomePage` | `GET /home` | 200 | — |
| `startInterview` | `POST /interviews` | 201 | 400 |
| `getQuestion` | `GET /interviews/{id}/question` | 200 | 404, 409 (all answered) |
| `saveAnswer` | `POST /interviews/{id}/answers` | 200 | 400, 404, 409 (already answered) |
| `getInterviewStatistic` | `GET /interviews/{id}/statistic` | 200 | 404 |
| `offer` | `POST /interviews/{id}/offer` | 200 | 400 (unknown candidate), 404 |
| `getInterviewResult` | `GET /interviews/{id}/result` | 200 | 404 |
| `getAiInterviewResult` | `GET /interviews/{id}/ai-result` | 200 `READY` (always) | 404 |

## Key domain decisions realized

- **`/result` = the player's score; `/statistic` = candidate performance.** Two independent
  per-candidate numbers: `timesChosen` (how often the player picked that candidate's answer)
  vs. `correctAnswers` (how often that candidate was objectively right). *Verified independent
  in code and tests.*
- **Candidates differ in competence (by design).** Seed distributes the correct answer so the
  correct-answer count per slot is **slot 0 = 10, slot 2 = 5, slot 1 = 4, slot 3 = 1** (of 20).
  So `correctAnswers` is a meaningful, non-uniform hiring signal — Alexey (slot 0) is
  unambiguously the strongest hire, keeping the AI verdict coherent.
- **Read endpoints are not gated on the offer.** `/result`, `/statistic`, `/ai-result` return
  as soon as rounds are answered — the FE fetches `/result` *before* the offer step.
- **Scoring:** correct = `10 + 2×(streak − 1)` (resulting streak); wrong = 0 and resets the
  streak. `timeLimitSeconds` is a client hint — the server does not enforce it.
- **AI result is a synchronous rule-based stub:** always HTTP 200 `READY`; the contract's
  `202`/`PENDING` path is never emitted in MVP.
- **Global player stats** (`/home`): aggregates over all completed interviews (no auth/identity).
  `winRate` = fraction with correctCount ≥ 60% of total; `bestResult` = max correctCount;
  `candidatesHired` = interviews with an offer.

## Error handling

`@RestControllerAdvice` maps to the contract `Error` schema `{code, message, timestamp}`:

| Code | HTTP | When |
|---|---|---|
| `INTERVIEW_NOT_FOUND` | 404 | Unknown interview id |
| `BAD_REQUEST` | 400 | Validation failure, malformed body, invalid enum, foreign `questionId`, or `answerId` not belonging to the question |
| `NO_QUESTION_AVAILABLE` | 409 | `GET /question` when all rounds are answered |
| `QUESTION_ALREADY_ANSWERED` | 409 | Re-answering an answered round |
| `INTERNAL_ERROR` | 500 | Catch-all (returns the Error shape, not Spring's default) |

## Testing

TDD throughout. Coverage: unit (`ScoringService`), repository/service integration, per-endpoint
MockMvc tests (status codes + JSON shape with exact-value assertions derived from independent
ground truth), and a **full end-to-end playthrough** (`FullPlaythroughTest`: start → 10 answers →
409 on the 11th → statistic/result before offer → offer → ai-result → home reflects the game).
A CORS test asserts `Access-Control-Allow-Origin` for the frontend origin.

## Running locally

```bash
docker compose -f backend/compose.yaml up -d      # Postgres (techleadsim/techleadsim)
cd backend && ./mvnw spring-boot:run              # app on :8080
curl -s http://localhost:8080/api/home | head     # smoke check
./mvnw test                                        # full suite (needs Docker for Testcontainers)
```

## Outstanding follow-ups (none block merge)

1. **AI verdict taxonomy** *(needs a product decision)* — the verdict does an exact string match
   between `Candidate.strengths` (e.g. "CI/CD", "Docker") and question `topic` (e.g. "DevOps"),
   so hiring the DevOps candidate after missing DevOps questions reads "Reasonable hire" instead
   of "Good hire". Cosmetic (no contract impact); only the DevOps candidate is affected — the
   strongest hire matches correctly. Fix options: add "DevOps" to that candidate's strengths,
   add a topic→strength synonym map, or defer.
2. **N+1 read loops** in `StatisticService`, `PlayerStatsService`, `RuleBasedAiAnalyzer`,
   `InterviewService.result`, `DtoMapper` — bounded to ≤20 rounds / 4 candidates; fine for MVP,
   collapse to grouped queries if they grow.
3. ~~**CORS origin is hardcoded** to `http://localhost:5173`~~ — **done.** Externalized to
   `app.cors.allowed-origin` (env `CORS_ALLOWED_ORIGIN`); `WebCorsConfig` also allows the
   Render frontend origin.
4. **Minor test-coverage gaps** — `/home` `bestResult`/`candidatesHired`, AI verdict branches, and
   the exactly-2-misses priority boundary are unasserted; a dead `finished` variable in
   `FullPlaythroughTest`.
5. **Seed content** — only `MEDIUM` questions are seeded (matches the FE, which always sends
   `MEDIUM`); `EASY`/`HARD` pools are future work. `InterviewStatus.FINISHED` is reserved but unused.

See `docs/frontend-handoff.md` for the frontend-facing integration notes.
