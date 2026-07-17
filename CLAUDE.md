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
