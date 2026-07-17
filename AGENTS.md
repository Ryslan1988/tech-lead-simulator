# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Tech Lead Simulator — a game where the player acts as a tech lead making decisions. Startup MVP, currently a bare skeleton: build tooling and entry points only, no game logic yet. Backend is a monolith (see `README.md`).

## Layout

Two independently built projects in one repo:

- `backend/` — Spring Boot 4.1.0 on **Java 25**, web layer only (`spring-boot-starter-webmvc`). Entry point `com.techleadsim.BackendApplication`. Base package `com.techleadsim`. Serves on `:8080`.
- `frontend/` — Vue 3 + Vite + TypeScript SPA. Entry `src/main.ts` → `src/App.vue`. Dev server on `:5173`.

There is no router, state store, database, or REST layer yet — those were deliberately left out until the domain settles. Add them per-feature.

## Commands

Backend (from `backend/`, uses the Maven wrapper — no local Maven needed):

```bash
./mvnw package                              # compile, test, build runnable JAR into target/
./mvnw test                                 # run all tests
./mvnw test -Dtest=BackendApplicationTests  # single test class
./mvnw test -Dtest=BackendApplicationTests#contextLoads   # single test method
./mvnw spring-boot:run                      # run the app
```

Frontend (from `frontend/`):

```bash
npm install        # first time
npm run dev        # Vite dev server with HMR
npm run build      # type-check (vue-tsc) + production build into dist/
npm run type-check # vue-tsc only
npm run lint       # oxlint + eslint, both with --fix
npm run format     # prettier over src/
```

## Conventions that aren't obvious from the code

- **Spring Boot 4.x, not 3.x.** APIs and starter names differ. The web starter is `spring-boot-starter-webmvc` (not `-web`), and test starters are per-module (e.g. `spring-boot-starter-webmvc-test`). Don't assume Boot 3 artifact names — verify against the 4.1 docs (Context7 `/spring-projects/spring-boot/v4.1.0`).
- **The Spring Boot version in `pom.xml` is `4.1.0`, not `4.1.0.RELEASE`.** Spring Initializr emits the `.RELEASE` suffix but Maven Central has no such artifact (it 404s) — the `.RELEASE` suffix was dropped after Boot 2.4. Keep it plain.
- When fetching docs for any library/framework here (Spring Boot, Vue, Vite), use Context7 rather than relying on training data — these are recent major versions.

## Not tracked in git

- `.idea/` (root `.gitignore`), `backend/target/`, `frontend/node_modules/` and `frontend/dist/`.
- Root `*-variant.jpg` files are design references, intentionally left untracked.

## API contract (contract-first)

`openapi.yaml` at the repo root is the **single source of truth** for the
BE ↔ FE contract, so both sides can build in parallel against an agreed shape.

**Workflow:** edit `openapi.yaml` first → agree the change → then implement. Do not
add or change an endpoint on one side without updating the spec.

Validate after editing:

```bash
npx @redocly/cli@latest lint openapi.yaml
```

The default Redocly ruleset also flags `security-defined` and
`operation-4xx-response` — advisory only. Auth is out of MVP scope, so those are
expected until a security scheme is added.

### MVP scope

The game: the player interviews virtual candidates. 10 rounds of
question → candidates answer (one is correct) → player picks the correct answer.
Then per-candidate stats → hire one candidate → score result + AI roadmap.

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

### Generating from the spec (parallel work)

- **Backend:** implement Spring MVC controllers matching the spec `operationId`s;
  optionally generate DTOs/interfaces (`openapi-generator`, `spring` generator).
- **Frontend:** generate a typed client (`openapi-typescript` + a fetch wrapper) and
  mock responses from the schema `examples` until the backend is live.
