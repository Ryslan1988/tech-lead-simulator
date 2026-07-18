# Archive — superseded build-phase documents

These files are **historical records of how the MVP was built**, kept for context.
They are *not* current guidance and are deliberately not maintained — several
statements in them were already false by the time the MVP landed.

Do not use them to answer "how does this work today". For that, read
`AGENTS.md`, `openapi.yaml`, and the code.

| File | What it was | Why it's stale |
|---|---|---|
| `frontend-mvp-plan.md` | The original frontend MVP plan (routes, store, components, MSW design, 8 phases) | Its "Context" section says the frontend is a *"bare `create-vue` scaffold"* and the backend has *"no REST layer yet"* — both were true when written, neither is now. The video-call restyling that followed is specced in `docs/superpowers/specs/2026-07-18-frontend-video-call-redesign-design.md`. |
| `frontend-mvp-progress.md` | Phase-by-phase progress tracker for the frontend build (Russian) | All 8 phases are done. Its "Definition of Done" cites Playwright drivers and screenshots in a scratchpad directory that no longer exists. Notes candidate avatars as PNG-with-fallback; they are now generated SVG (`AvatarArt`). |
| `frontend-build-prompt.md` | A one-shot prompt meant to be pasted into a fresh Claude Code session to kick off the frontend build | Its job is done. It `@`-references the files above at their old root paths, and instructs against writing tests — a scoping decision for that session, not a standing repo rule. Was committed by accident (see the last line of `frontend-mvp-progress.md`). |

`frontend-build-prompt.md` is a reasonable candidate for deletion — it has no
value beyond the session it bootstrapped. Kept for now only because it records
the constraints that build ran under.
