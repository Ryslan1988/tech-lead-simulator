# Frontend Build Prompt — paste into a new Claude Code session

> **How to use:** open a fresh Claude Code session at the repo root
> (`D:\JavaProjects\tech-lead-simulator`) and paste everything in the
> **PROMPT** block below. The `@`-file mentions will pull the referenced files
> into context automatically.

---

## PROMPT

Build the **Tech Lead Simulator frontend** (Vue 3.5 + Vite + TypeScript SPA in `frontend/`) to completion, following our existing plan and the contract-first workflow.

**No automated tests — this is a time-boxed UI MVP.** Do **not** write any test files (no Vitest, no component/unit/e2e tests), and do **not** use test-driven development. Verify by building and by a manual playthrough (see "Definition of done"). This matches the plan's own note: *"No unit-test suite is set up in the scaffold; MVP verification is the manual playthrough."* (A Vitest suite for the store's scoring logic is explicitly deferred as optional/later.)

**Workflow — execute the plan directly, no TDD loop.** Design decisions are already locked (see the plan), so do **not** re-brainstorm scope, and do **not** run the heavyweight per-task implementer+reviewer TDD loop. Instead:
1. Read the plan and contract first (below). If anything is genuinely blocking, ask me before starting — don't invent answers.
2. Work through the plan's **8 implementation phases in order**, committing once per phase (or per coherent sub-step). After each phase, run `npm run type-check` + `npm run lint` to keep the tree compiling; fix issues before moving on.
3. Optionally use **superpowers:writing-plans** first to expand the 8 phases into a short task checklist under `docs/superpowers/plans/` if that helps you track progress — but keep it lightweight; the existing plan is already detailed. Skip subagent-driven-development.

Work on a dedicated branch (e.g. `frontend`), not `main`. Commit as you go; don't push unless I ask.

### Authoritative inputs (read these before planning)
- @frontend-implementation-plan.md — the frontend plan (architecture, routes→screens→API, Pinia store, components, tokens, MSW mocks, phases, verification). **This is your primary spec.**
- @openapi.yaml — the BE↔FE contract and **single source of truth** (v0.2.0). Generate the TypeScript types from this (`openapi-typescript`) and consume via `openapi-fetch`. Never diverge the client from this file.
- @docs/frontend-handoff.md — backend runtime behavior now that the backend is **live**: base URL, CORS, the `Error` schema and error codes, per-endpoint behavior, sequencing, and coordination items.
- @AGENTS.md — repo conventions. Note especially: **fetch current docs via Context7** for every library here (Vue 3.5, Vite, vue-router, pinia, openapi-typescript, openapi-fetch, msw) — these are recent major versions and training data may be stale.
- Design reference: `design/third-variant.jpg` (or the `*-variant.jpg` file in the repo root) — the ~14 numbered screen mockups. Match the flat visual style and the design tokens in the plan.

### Important context that updates the plan
- **The backend is DONE and merged to `main`.** The plan's "backend has no REST layer yet" note is now outdated. The API runs at **`http://localhost:8080/api`** (start it with `docker compose -f backend/compose.yaml up -d` then `cd backend && ./mvnw spring-boot:run`), and it already allows CORS from `http://localhost:5173`. So build against **MSW mocks first** (`VITE_USE_MOCKS=true`), but you can — and should — verify the real-backend switch (`VITE_USE_MOCKS=false`) end-to-end against the running backend, not just as a 404 smoke test.
- **Errors:** every backend error returns `{ code, message, timestamp }`. Branch on `code` (`INTERVIEW_NOT_FOUND` 404, `BAD_REQUEST` 400, `NO_QUESTION_AVAILABLE` 409, `QUESTION_ALREADY_ANSWERED` 409, `INTERNAL_ERROR` 500), never on `message`.
- **`/result` and `/statistic` are available before the offer** — the backend does not gate reads on the offer. Fetch `/result` on the results screen, then go to the offer step.
- **`timesChosen` vs `correctAnswers`** in `/statistic`: rank candidates on the offer screen by **`correctAnswers`** (objective competence) — that's the intended hiring signal; `timesChosen` just shows who the player trusted.
- **AI result** always returns HTTP 200 `READY` from the real backend — the contract's `202`/`PENDING` polling path never fires against it. Keep the MSW mock exercising a one-off `202 → READY` (per the plan, to prove the polling code works), but know it's inert against the real backend.
- **`saveAnswer` now validates** that `answerId` belongs to the submitted `questionId` (400 otherwise) — send real ids from the current question.
- **Candidate avatars:** seed `avatarUrl` values point at `/assets/candidates/*.png`; make sure those static assets exist in `frontend/public/` (or mocks/real both 404).
- **Mode labels:** the plan maps "Быстрая игра" → `HARDCORE` (the 20-question mode). Double-check that's the intended label (a "quick" game with *more* questions looks odd) — flag to me if it seems wrong; it's a FE-only decision.

### Definition of done (verification)
- `npm run gen:api` produces `src/api/schema.d.ts`; `npm run type-check` and `npm run build` (vue-tsc + vite) and `npm run lint` all pass.
- **Full playthrough against mocks** (`npm run dev`, `VITE_USE_MOCKS=true`): Home → pick a mode → play all 10 rounds (feedback, streak, score, timer) → result + breakdown → statistic-driven offer → team → AI summary (roadmap renders; the 202→READY polling path is exercised). The route guard redirects deep-links like `/interview/999/question` (no active session) back home.
- **Real-backend end-to-end** (`VITE_USE_MOCKS=false`, backend running on :8080): the same playthrough works against the live API through the Vite proxy, with CORS succeeding from :5173.
- Keep everything **in `frontend/`**. Do NOT modify the backend or `openapi.yaml`; if you believe the contract needs a change, stop and ask me first (contract-first: spec changes are agreed before code).

Start by reading the inputs above and drafting the task plan.

---

*(End of prompt. The rest of this file is a quick reference for you, the human — not part of what you paste.)*

## Quick reference — what's already in the repo for the frontend

| File | What it gives the frontend session |
|---|---|
| `frontend-implementation-plan.md` | The full plan: layout, routes, store, components, phases, MSW design |
| `openapi.yaml` | Contract / typegen source (v0.2.0) |
| `docs/frontend-handoff.md` | Live-backend runtime notes, error codes, sequencing, gotchas |
| `summary.md` | Backend implementation summary (endpoints, domain decisions, how to run) |
| `AGENTS.md` | Repo conventions + Context7 mandate for library docs |
| `design/third-variant.jpg` | Screen mockups (untracked design reference) |

Backend is merged to `main` and runs at `http://localhost:8080/api` (CORS allows `http://localhost:5173`).
