# Frontend Handoff — Backend is Live

The backend MVP is complete and matches `openapi.yaml` (repo root), which stays the **single
source of truth** — generate the typed client from it (`openapi-typescript`). This file captures
only the **runtime behavior and non-obvious details** that the schema alone doesn't convey.

## Connection

- **Base URL:** `http://localhost:8080/api` (all endpoints live under the `/api` context path).
  Keep the Vite proxy `/api → http://localhost:8080/api`.
- **CORS:** the backend allows origin **`http://localhost:5173`** (methods GET/POST/PUT/DELETE/
  OPTIONS) for `/**`. If you run the dev server on a different port, the backend CORS config
  (`WebCorsConfig`) must be updated — it is currently hardcoded to `:5173`.
- **Local backend:** `docker compose -f backend/compose.yaml up -d` then
  `cd backend && ./mvnw spring-boot:run`.

## Error responses — one consistent shape

Every error returns the `Error` schema with these fields:

```json
{ "code": "BAD_REQUEST", "message": "…", "timestamp": "2026-07-18T07:30:02Z" }
```

| `code` | HTTP | Trigger |
|---|---|---|
| `INTERVIEW_NOT_FOUND` | 404 | Unknown interview id in the path |
| `BAD_REQUEST` | 400 | Invalid/missing body fields, malformed JSON, invalid enum value, a `questionId` not in the interview, or an `answerId` not belonging to the question |
| `NO_QUESTION_AVAILABLE` | 409 | `GET …/question` after all rounds are answered |
| `QUESTION_ALREADY_ANSWERED` | 409 | Posting an answer for an already-answered round |
| `INTERNAL_ERROR` | 500 | Unexpected server error (still returns this shape, not an HTML/Spring page) |

Branch on `code`, not on `message` (messages are human text and may change).

## Endpoint behavior notes (the non-obvious parts)

- **`POST /interviews`** → **201**. Body: `{ "mode": "CLASSIC" | "HARDCORE", "difficulty": "MEDIUM", "playerName"?: string }`.
  `difficulty` and `playerName` are optional; the FE always sends `MEDIUM`. Returns the interview
  id, mode, difficulty, `totalQuestions` (CLASSIC=10, HARDCORE=20), and the 4-candidate lineup.
  An invalid `mode` value → 400 `BAD_REQUEST`.
- **`GET …/question`** → the next unanswered round: `{ questionId, index, total, text, timeLimitSeconds, answers[] }`.
  Each answer is `{ answerId, candidateId, text }` — **no "correct" flag** is exposed. Returns
  **409 `NO_QUESTION_AVAILABLE`** once all rounds are answered (use this as your "interview over"
  signal). `timeLimitSeconds` is a **hint only** — the server never rejects a late answer.
- **`POST …/answers`** → **200** with rich running state:
  `{ correct, correctAnswerId, pointsAwarded, correctCount, currentStreak, totalPoints, answeredCount, totalQuestions, finished }`.
  `finished` becomes `true` on the last round. **Scoring:** correct = `10 + 2×(streak − 1)`; wrong
  = 0 and resets the streak. Submit `{ questionId, answerId }` — both must be real (`answerId` must
  belong to that question, else 400).
- **`GET …/statistic`** → candidate-focused. `perCandidate[]` has, per candidate,
  **`timesChosen`** (how often *you* picked their answer) and **`correctAnswers`** (how often they
  were *objectively* right). Use **`correctAnswers`** to rank candidates on the offer screen — it's
  the intended, objective hiring signal. `correctCount` at the top level is *your* score (same as in
  `/result`).
- **`GET …/result`** → **your** score: `{ correctCount, totalQuestions, totalPoints, bestStreak, breakdown[] }`
  where `breakdown[]` is per-question `{ questionId, text, correct }`.
- **`POST …/offer`** → **200** `{ interviewId, hiredCandidate, message }`. Body `{ personId }`
  (a candidate id). Unknown candidate → 400.
- **`GET …/ai-result`** → **always 200** with `status: "READY"`:
  `{ interviewId, status, summary, verdict, hiredCandidateId, roadmap[] }`. Each roadmap item is
  `{ topic, reason, priority: "HIGH"|"MEDIUM", resources[] }`; `resources[]` is `{ title, url }`.
  `verdict` is `null` until an offer has been made.
- **`GET /home`** → `{ title, subtitle, modes[], playerStats }`. `modes` = the two game modes with
  `questionCount` (10 / 20). `playerStats` are **global** aggregates across all completed games
  (there is no per-player identity in the MVP), so they change as anyone plays.

## Sequencing — important

- **`/result` and `/statistic` are available BEFORE the offer** (as your plan expects) — the backend
  does **not** gate reads on the offer. Fetch `/result` on the results screen, then move to `/offer`.
- **`/ai-result` works before or after the offer**; `verdict` is only populated once a candidate is
  hired. Fetch it after the offer if you want the verdict text.

## Coordination items for the FE side

1. **Candidate avatars.** Seed `candidate.avatarUrl` values point at static asset paths
   (`/assets/candidates/*.png`, per the OpenAPI examples). Make sure those image files exist in the
   frontend's static assets, or avatars 404.
2. **The AI 202 / `PENDING` polling path is dead code against this backend.** `/ai-result` always
   returns 200 `READY` synchronously in the MVP — your PENDING poll loop will never fire. Harmless,
   but you can skip building/keeping it until a real async analyzer lands.
3. **Mode labels — double-check on the FE.** The FE plan maps "Быстрая игра" → `HARDCORE`, which is
   the **20-question** (longer) mode. A "quick" game with *more* questions may be a mislabel — worth
   a sanity check (FE-side only; the backend is agnostic).
4. **Verdict wording (informational).** The AI `verdict` currently under-credits the DevOps candidate
   due to a strengths↔topic vocabulary mismatch on the backend (pending a decision). If a user hires
   the DevOps specialist, the verdict may read "Reasonable hire" rather than "Good hire". No shape
   impact — just be aware the wording isn't always maximally flattering yet.
