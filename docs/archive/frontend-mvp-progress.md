# Прогресс фронтенда — Tech Lead Simulator (MVP)

Ветка: `frontend` (создана от `main`). Коммиты — по одному на фазу.
Обновляется по ходу работы.

## Ключевые решения (подтверждены с пользователем)
- **Язык интерфейса:** русский (1:1 с макетами `design/third-variant.jpg`).
  Контент от бэкенда (вопросы, имена, AI-текст) рендерится как есть.
- **Режимы:** Карьерный → `HARDCORE` (20 вопросов), Быстрая игра → `CLASSIC` (10).
  Иконки: 🏆 карьерный / ⏱️ быстрый. Рендер data-driven из `/home`.
- Тесты не пишем (time-boxed UI MVP) — проверка через build + ручной прогон.

## Фазы реализации

- [x] **Фаза 1. Foundation** — уже была на `main` (deps, `api/client.ts`,
      сгенерированный `schema.d.ts`, `tokens.css`/`base.css`, env, Vite-прокси).
- [x] **Фаза 2. App shell** — `router/index.ts` (маршруты + guard на сессию),
      `main.ts` (pinia + router + условный старт MSW по `VITE_USE_MOCKS==='true'`),
      `App.vue` (`<RouterView>`), стор-скелеты `interview`/`home`, заглушки вьюх.
      Коммит `c3459da`.
- [x] **Фаза 3. Mock backend (MSW)** — `mocks/fixtures.ts` (движок игры: 4 кандидата,
      банк из 20 вопросов на русском, скоринг `10+2*(streak-1)`, timesChosen vs
      correctAnswers, ошибки 400/404/409), `mocks/handlers.ts` (по хендлеру на
      operationId, ai-result отдаёт 202→READY), `public/mockServiceWorker.js`.
      Движок проверен Node-симуляцией полной игры. Коммит `721d0fe`.
- [x] **Фаза 4. Home + старт** — UI-кит (`AppButton`, `AppCard`, `AppScreen`,
      `ProgressBar`, `StatTile`, `CandidateAvatar` с fallback-инициалами,
      `CandidateCard`), `HomeView` (экраны 1+3), `ModeSelectView` (2),
      `LobbyView` (4+5). Коммит `a245479`.
- [x] **Фаза 5. Игровой цикл** — `QuestionView` (экраны 6/7/10): вопрос, ответы
      кандидатов, фидбэк (зелёный/красный), промежуточная статистика; таймер
      `useCountdown`; стор пишет per-round outcomes. Коммит `ba66ce1`.
- [x] **Фаза 6. Результат + оффер** — `ResultView` (13/14: счёт + разбор),
      `OfferView` (15/16: карусель кандидатов по `correctAnswers`, оффер, команда).
      Коммит `2e85635`.
- [x] **Фаза 7. AI-итог** — `SummaryView` (17 + roadmap): поллинг 202→READY,
      verdict, план развития (`RoadmapItem` с приоритетами/ссылками). Коммит `f4e8908`.
- [x] **Фаза 8. Полировка** — `PauseModal` (20) + стор `ui`, триггер-пауза в
      шапке вопроса, keyed `<RouterView>` для чистого рестарта, состояния
      загрузки/ошибок/404/409, адаптив. Коммит `2ebfbf0`.

## Проверка (Definition of Done)

- [x] `npm run gen:api` → `src/api/schema.d.ts` генерируется.
- [x] `npm run type-check` — проходит.
- [x] `npm run lint` (oxlint + eslint) — проходит.
- [x] `npm run build` (vue-tsc + vite) — успешно (18 чанков, ~44 kB gzip core).
- [x] Движок мока проверен полной Node-симуляцией (скоринг, стрик, 409/404/400,
      статистика, AI PENDING→READY).
- [x] **Прогон в браузере против моков** (`VITE_USE_MOCKS=true`) — PASS
      (Playwright/Chromium, драйвер `scratchpad/drive.cjs`). Home → Быстрая игра →
      10 раундов (фидбэк зелёный/красный, промежуточная статистика) → результат
      2/10 + разбор из 10 строк → оффер (Алексей, «Верных ответов: 5 из 10») →
      команда → AI-итог (poll **202→READY**, verdict, 3 пункта roadmap).
      Guard: deep-link `/interview/999/question` → редирект на `/`.
      **0 ошибок в консоли, 0 упавших запросов.** Скриншоты: `scratchpad/shot-*.png`.
- [x] **Реальный бэкенд** (`VITE_USE_MOCKS=false`, Spring на :8080 + Postgres) —
      PASS (драйвер `scratchpad/drive-real.cjs`). Тот же полный прогон через
      Vite-прокси, CORS с :5173 — **0 упавших запросов**. Контент от бэкенда
      английский (Classic/Hardcore, Alexey/Maria/Dmitry/Sergey, «Alexey has joined
      your team!»), русская UI-обёртка рендерится поверх — как и задумано.
      Реальный `/ai-result` отдаёт `READY` сразу (без 202) — ожидаемо.
      Скриншоты: `scratchpad/real-*.png`.

**Итог: Definition of Done выполнен полностью.** Все 8 фаз + оба прогона.

## Заметки / открытые вопросы
- Аватары кандидатов (`/assets/candidates/*.png`) не создавались бинарно —
  `CandidateAvatar` рисует fallback-инициалы, поэтому 404 картинок не ломает UI
  (работает и для моков, и для реального бэкенда).
- Контент мока — на русском (под макет). У реального бэкенда контент может быть
  на английском; при переключне `VITE_USE_MOCKS=false` это ожидаемо.
- `frontend-build-prompt.md` случайно попал в первый коммит ветки — безвредно.
