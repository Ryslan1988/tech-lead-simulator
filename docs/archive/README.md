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

A third file, `frontend-build-prompt.md`, was deleted rather than archived — a
one-shot prompt for bootstrapping the frontend build session, with no value once
that session ran. Its last line in `frontend-mvp-progress.md` notes it was
committed by accident. Recover it from history if ever needed:

```bash
git show 891c2e6:docs/archive/frontend-build-prompt.md
```
