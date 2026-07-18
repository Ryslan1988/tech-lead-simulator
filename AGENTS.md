# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Tech Lead Simulator — a game where the player interviews virtual candidates and
hires one. The MVP is **built end to end**: a Spring Boot monolith backed by
PostgreSQL, and a routed Vue SPA that can run standalone against MSW mocks.

## Layout

Two independently built projects in one repo, joined only by `openapi.yaml`:

- `backend/` — Spring Boot 4.1.0 on **Java 25**. Spring MVC + Data JPA + Flyway +
  PostgreSQL. Base package `com.techleadsim`, entry point `BackendApplication`.
  Serves on `:8080` under context path **`/api`**.
- `frontend/` — Vue 3 + Vite + TypeScript SPA with `vue-router` and Pinia.
  Entry `src/main.ts` → `src/App.vue`. Dev server on `:5173`.

## Commands

Backend (from `backend/`, uses the Maven wrapper — no local Maven needed):

```bash
docker compose up -d                        # start PostgreSQL (required to run the app)
./mvnw spring-boot:run                      # run the app on :8080/api
./mvnw package                              # compile, test, build runnable JAR into target/
./mvnw test                                 # run all tests
./mvnw test -Dtest=FullPlaythroughTest      # single test class
./mvnw test -Dtest=ScoringServiceTest#pointsFor   # single test method
```

**Tests need a running Docker daemon** — the web/repository tests extend
`AbstractPostgresIntegrationTest`, which starts a Testcontainers PostgreSQL.
Pure unit tests (`ScoringServiceTest`, `RuleBasedAiAnalyzerTest`) do not.

Frontend (from `frontend/`):

```bash
npm install        # first time
npm run dev        # Vite dev server with HMR (MSW mocks on by default)
npm run gen:api    # regenerate src/api/schema.d.ts from ../openapi.yaml
npm run build      # type-check (vue-tsc) + production build into dist/
npm run type-check # vue-tsc only
npm run lint       # oxlint + eslint, both with --fix
npm run format     # prettier over src/
```

There is **no frontend test runner** — no Vitest, no Playwright. `npm run build`
(type-check + build) is the only automated gate.

## Backend architecture

Classic layered monolith, one package per layer under `com.techleadsim`:

```
web/          InterviewController, HomeController — thin, DTOs only
web/dto/      request/response records mirroring openapi.yaml schemas
web/mapper/   DtoMapper — the single entity → DTO boundary
service/      InterviewService (game flow), ScoringService, StatisticService, PlayerStatsService
content/      QuestionProvider seam → SeedQuestionProvider (reads seeded questions)
ai/           AiAnalyzer seam → RuleBasedAiAnalyzer (stub, no LLM)
domain/       JPA entities + enums (Mode, Difficulty, InterviewStatus)
repository/   Spring Data JPA repositories
error/        ApiException hierarchy + GlobalExceptionHandler → ApiErrorResponse
config/       WebCorsConfig
```

Things worth knowing before editing:

- **Flyway owns the schema; Hibernate only validates it.**
  `spring.jpa.hibernate.ddl-auto=validate`. Any entity change needs a matching
  migration in `src/main/resources/db/migration/` (`V1__schema.sql`,
  `V2__seed.sql`). Never edit an applied migration — add a new `V3__…`.
- **Game content lives in `V2__seed.sql`**, not in Java. Questions, answers,
  candidates, and which candidate is right per question are all seed rows.
- **`spring.jpa.open-in-view=false`** — lazy associations must be resolved inside
  the `@Transactional` service method, not in the controller or mapper.
- **`AiAnalyzer` and `QuestionProvider` are deliberate seams** for later swapping
  in an LLM. Keep new content/AI logic behind them.
- Config is env-var driven with local defaults (`SPRING_DATASOURCE_URL`,
  `CORS_ALLOWED_ORIGIN`, `PORT`) so the same JAR runs locally and on Render.

## Frontend architecture

- **`src/api/schema.d.ts` is generated — never hand-edit it.** Run
  `npm run gen:api` after `openapi.yaml` changes. `src/api/client.ts` wraps it in
  an `openapi-fetch` client; every call returns `{ data, error }`.
- **`stores/interview.ts` is the game's spine** — holds the session, per-round
  state and all post-run payloads; each action maps to one spec endpoint.
  `stores/home.ts` is the home screen, `stores/ui.ts` is the pause overlay only.
- **Routes mirror the screens** (`/`, `/mode`, `/interview/:id/{lobby,question,result,offer,summary}`).
  Routes with `meta.requiresSession` are guarded by `interview.hasSessionFor(id)`,
  so deep-links without a live session bounce home. In-memory state only — a page
  reload mid-game sends the player back to `/`.
- **Two ways to run the frontend**, switched by `VITE_USE_MOCKS`:
  - `true` (the `.env.development` default) — MSW intercepts `/api` in the
    browser using `src/mocks/handlers.ts` + `fixtures.ts`. No backend needed.
  - `false` — the Vite dev proxy forwards `/api` to `http://localhost:8080`.
    The backend already serves under `/api`, so there is no path rewrite.
  It is a string env var: compare against `'true'`, never rely on truthiness.
- The UI is themed via CSS custom properties in `assets/styles/tokens.css`. The
  interview is staged as a **video call** (`VideoCallStage`, `VideoTile`,
  `CallControlBar`); candidate portraits are generated SVG (`AvatarArt` +
  `avatarPalettes.ts`), not image assets.

## Conventions that aren't obvious from the code

- **Spring Boot 4.x, not 3.x.** APIs and starter names differ. The web starter is
  `spring-boot-starter-webmvc` (not `-web`), and test starters are per-module
  (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`).
  Flyway comes in as `spring-boot-flyway` (not `-starter-`). Don't assume Boot 3
  artifact names — verify against the 4.1 docs (Context7 `/spring-projects/spring-boot/v4.1.0`).
- **The Spring Boot version in `pom.xml` is `4.1.0`, not `4.1.0.RELEASE`.** Spring
  Initializr emits the `.RELEASE` suffix but Maven Central has no such artifact
  (it 404s) — the suffix was dropped after Boot 2.4. Keep it plain.
- **`AbstractPostgresIntegrationTest` starts its container in a static block on
  purpose** — no `@Container`/`@Testcontainers`. The JUnit 5 extension would stop
  and restart the container per subclass while Spring's context cache still points
  at the dead container's port. See the comment in that file before "fixing" it.
- When fetching docs for any library/framework here (Spring Boot, Vue, Vite), use
  Context7 rather than training data — these are recent major versions.

## Not tracked in git

`.idea/`, `backend/target/`, `frontend/node_modules/` and `frontend/dist/`.

## API contract (contract-first)

`openapi.yaml` at the repo root is the **single source of truth** for the
BE ↔ FE contract, so both sides can build in parallel against an agreed shape.

**Workflow:** edit `openapi.yaml` first → agree the change → then implement on
both sides (backend controller/DTOs, frontend `npm run gen:api`). Do not add or
change an endpoint on one side without updating the spec.

Validate after editing:

```bash
npx @redocly/cli@latest lint openapi.yaml
```

The default Redocly ruleset also flags `security-defined` and
`operation-4xx-response` — advisory only. Auth is out of MVP scope, so those are
expected until a security scheme is added.

### MVP scope

The game: the player interviews virtual candidates. Each round is
question → candidates answer (one is correct) → player picks the correct answer.
Then per-candidate stats → hire one candidate → score result + AI roadmap.

Round count comes from `Mode`: **`CLASSIC` = 10, `HARDCORE` = 20.**

Client method → endpoint mapping (all under base path `/api`):

| Original call                  | Endpoint                                   |
|--------------------------------|--------------------------------------------|
| `getHomePage()`                | `GET  /home`                               |
| `start(context)`               | `POST /interviews`                         |
| `getQuestion()`                | `GET  /interviews/{interviewId}/question`  |
| `saveAnswer(question, answer)` | `POST /interviews/{interviewId}/answers`   |
| `getInterviewStatistic()`      | `GET  /interviews/{interviewId}/statistic` |
| `offer(person)`                | `POST /interviews/{interviewId}/offer`     |
| `getInterviewResult(id)`       | `GET  /interviews/{interviewId}/result`    |
| `getAiiInterviewResult(id)`    | `GET  /interviews/{interviewId}/ai-result` |

`getQuestion` and `getInterviewStatistic` take no args in the original sketch;
they are interview-scoped in the spec so the API stays stateless and can scale
horizontally (helps meet the availability NFR).

### Domain decisions (game mechanics)

Non-obvious rules agreed during design. Full rationale:
`docs/superpowers/specs/2026-07-17-backend-mvp-design.md`.

- **`/result` is the *player's* score; `/statistic` is about the *candidates*.**
  `getInterviewResult` returns the player's own correctness (score, points, streak,
  per-question breakdown). It is NOT about how candidates did. Candidate performance
  lives in `getInterviewStatistic`.
- **Two distinct per-candidate numbers in `/statistic`:** `timesChosen` = how often the
  **player** picked that candidate's answer; `correctAnswers` = how often that candidate
  was **objectively** right (independent of the player). `correctAnswers` is the intended
  basis for the hiring/offer decision; `timesChosen` shows who the player trusted. (Added
  to the contract in v0.2.0.)
- **Candidates must differ in competence.** Seed data assigns the correct answer per
  (question, candidate slot) so that stronger candidates are right more often, tied to
  their `strengths`. Without this, `correctAnswers` is uniform noise and the offer
  decision is meaningless. This also makes the AI `verdict` coherent (hiring the actually-
  strongest candidate = a good hire).
- **Read endpoints are not gated on the offer.** `GET /result`, `/statistic` and
  `/ai-result` return data as soon as all rounds are answered, regardless of whether an
  offer was made — the frontend fetches `/result` *before* the offer step. The interview
  status (`IN_PROGRESS → STATISTIC → OFFERED → FINISHED`) is advisory, not a hard gate on
  these GETs.
- **AI result is a synchronous rule-based stub (MVP)** behind an `AiAnalyzer` seam:
  always HTTP 200 `READY`, never emits the contract's `202`/`PENDING`. The FE's 202-poll
  path therefore never fires against the real backend.
- **Content is seed data** behind a `QuestionProvider` seam (no LLM-generated questions
  in MVP). Scoring: correct = `10 + 2×(streak−1)`, wrong = 0 and resets the streak.
  `timeLimitSeconds` is a client hint — the server does not enforce it.

### Non-functional

- **Availability: 98% / week** (≈ 3h 22m allowed downtime per week). Keep the API
  stateless where possible so instances are interchangeable behind a load balancer.

## Docs

All project docs live under `docs/` — nothing but `README.md`, `AGENTS.md`,
`CLAUDE.md` and `openapi.yaml` belongs at the repo root.

Current, worth reading:

- `docs/backend-mvp-summary.md` — endpoint table, error-code table, domain
  decisions as built, and a list of known follow-ups (N+1 read loops, AI verdict
  topic↔strength matching, seed covers `MEDIUM` difficulty only).
- `docs/frontend-handoff.md` — BE → FE integration notes (base URL, CORS, error
  codes, sequencing).
- `docs/superpowers/specs/2026-07-17-backend-mvp-design.md` + matching plan.
- `docs/superpowers/specs/2026-07-18-frontend-video-call-redesign-design.md` + matching plan.

`docs/archive/` holds superseded build-phase records (the original frontend plan,
its progress tracker, the build-kickoff prompt). They describe a state the repo
has moved past — see `docs/archive/README.md` before trusting anything in there.
