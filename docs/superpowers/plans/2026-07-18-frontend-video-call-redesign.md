# Video Call Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Собеседование выглядит как видеозвонок с сеткой участников и панелью управления, а у кандидатов есть нарисованные аватары вместо инициалов.

**Architecture:** Аватар рисуется одним параметризованным inline-SVG компонентом (`AvatarArt.vue`), палитра выбирается по слагу из `avatarUrl`. Экран звонка собран из трёх компонентов — плитка участника, панель управления, сетка-обёртка — и встраивается в `QuestionView` над карточкой вопроса и в `LobbyView` вместо текущей сетки карточек.

**Tech Stack:** Vue 3 (`<script setup>`, TypeScript), Vite, Pinia, vue-router, MSW для моков. Стили — scoped CSS на токенах из `src/assets/styles/tokens.css`.

## Global Constraints

- **Бэкенд не трогать вообще.** Ни `backend/**`, ни `openapi.yaml`. Английский seed-контент (имена, роли, вопросы, ответы) остаётся как есть.
- **Игровую логику не трогать.** `src/stores/**`, `src/router/index.ts`, `src/api/**` не изменяются.
- **Имена и роли кандидатов не переводить** — они приходят из данных.
- **Название приложения остаётся английским:** `TECH LEAD SIMULATOR` в `HomeView.vue:42-43`.
- **Новые подписи в интерфейсе — на русском.**
- **Тест-раннера в проекте нет.** Цикл проверки каждой задачи: `npm run type-check`, затем `npm run lint`, затем визуальная проверка в `npm run dev`. Не добавлять vitest/jest.
- Все команды выполняются из каталога `frontend/`.
- Стилевые значения брать только из токенов (`var(--color-*)`, `var(--space-*)`, `var(--radius-*)`), кроме цветов внутри SVG-иллюстраций.

## File Structure

| Файл | Ответственность |
|---|---|
| `src/components/AvatarArt.vue` | Создаётся. Inline-SVG портрет, параметризованный палитрой. |
| `src/components/avatarPalettes.ts` | Создаётся. Слаг → палитра; резолв слага из `avatarUrl`. |
| `src/components/CandidateAvatar.vue` | Изменяется. Цепочка: `AvatarArt` → `avatarUrl` → инициалы. |
| `src/components/VideoTile.vue` | Создаётся. Плитка одного участника звонка. |
| `src/components/CallControlBar.vue` | Создаётся. Панель управления звонком. |
| `src/components/VideoCallStage.vue` | Создаётся. Сетка плиток + панель. |
| `src/views/LobbyView.vue` | Изменяется. Плитки в состоянии «подключается». |
| `src/views/QuestionView.vue` | Изменяется. Сетка звонка над карточкой вопроса. |

Задачи идут снизу вверх по зависимостям: сначала аватары (их используют плитки), потом плитка, панель, сетка, потом вьюхи.

---

### Task 1: Палитры аватаров и резолв слага

**Files:**
- Create: `frontend/src/components/avatarPalettes.ts`

**Interfaces:**
- Consumes: ничего.
- Produces:
  - `export interface AvatarPalette { bg: string; skin: string; hair: string; shirt: string; longHair: boolean }`
  - `export function paletteFor(avatarUrl?: string): AvatarPalette | null` — `null`, если слаг неизвестен.

**Почему объединение слагов.** Бэкенд (`backend/src/main/resources/db/migration/V2__seed.sql`) отдаёт `alexey, maria, dmitry, sergey`. Моки (`frontend/src/mocks/fixtures.ts`) отдают `maria, alexey, igor, dmitriy`. Карта покрывает все шесть, иначе часть кандидатов останется без картинки.

- [ ] **Step 1: Создать файл палитр**

```ts
/**
 * Palettes for the generated candidate portraits (see AvatarArt.vue).
 *
 * Keys are the file stem of `Candidate.avatarUrl` (e.g. `/assets/candidates/alexey.png`
 * -> `alexey`), not the candidate name: names arrive in different languages and cases,
 * the URL path is fixed by the API contract.
 *
 * The set covers BOTH sources of seed data, which do not use the same slugs:
 *   backend V2__seed.sql -> alexey, maria, dmitry, sergey
 *   frontend mocks       -> maria, alexey, igor, dmitriy
 * `dmitriy` and `dmitry` are the same person spelled two ways and share a palette.
 */
export interface AvatarPalette {
  bg: string
  skin: string
  hair: string
  shirt: string
  /** Draws an extra hair mass behind the shoulders. */
  longHair: boolean
}

const MARIA: AvatarPalette = {
  bg: '#e8eefc',
  skin: '#f2c9a8',
  hair: '#6b4230',
  shirt: '#ffffff',
  longHair: true,
}
const ALEXEY: AvatarPalette = {
  bg: '#fdeadf',
  skin: '#f0c19c',
  hair: '#a4532a',
  shirt: '#2f4a7a',
  longHair: false,
}
const IGOR: AvatarPalette = {
  bg: '#e6f4ec',
  skin: '#e8b98f',
  hair: '#2f2a26',
  shirt: '#d9762b',
  longHair: false,
}
const DMITRY: AvatarPalette = {
  bg: '#eef0f4',
  skin: '#f2c9a8',
  hair: '#c8a262',
  shirt: '#5a6472',
  longHair: false,
}
const SERGEY: AvatarPalette = {
  bg: '#fdf3e0',
  skin: '#e0aa80',
  hair: '#1f1c1a',
  shirt: '#3f7d54',
  longHair: false,
}

const PALETTES: Record<string, AvatarPalette> = {
  maria: MARIA,
  alexey: ALEXEY,
  igor: IGOR,
  dmitry: DMITRY,
  dmitriy: DMITRY,
  sergey: SERGEY,
}

/** `/assets/candidates/alexey.png` -> the ALEXEY palette; unknown slug -> null. */
export function paletteFor(avatarUrl?: string): AvatarPalette | null {
  if (!avatarUrl) return null
  const file = avatarUrl.split('/').pop() ?? ''
  const stem = file.replace(/\.[^.]+$/, '').toLowerCase()
  return PALETTES[stem] ?? null
}
```

- [ ] **Step 2: Проверить типы**

Run: `npm run type-check`
Expected: успешно, без ошибок.

- [ ] **Step 3: Проверить линтер**

Run: `npm run lint`
Expected: успешно, без ошибок.

- [ ] **Step 4: Коммит**

```bash
git add src/components/avatarPalettes.ts
git commit -m "feat(frontend): add candidate avatar palettes and slug resolver"
```

---

### Task 2: Компонент портрета AvatarArt

**Files:**
- Create: `frontend/src/components/AvatarArt.vue`

**Interfaces:**
- Consumes: `AvatarPalette` из `@/components/avatarPalettes`.
- Produces: компонент с props `{ palette: AvatarPalette; title: string }`. Рендерит `<svg>` без фиксированного размера — масштабируется по контейнеру.

Геометрия одна на всех, различаются только цвета — поэтому один параметризованный компонент, а не пять почти одинаковых файлов.

- [ ] **Step 1: Создать компонент**

```vue
<script setup lang="ts">
import type { AvatarPalette } from '@/components/avatarPalettes'

defineProps<{ palette: AvatarPalette; title: string }>()
</script>

<template>
  <!-- Flat portrait matching design/third-variant.jpg: everyone wears glasses,
       identity comes from the palette. -->
  <svg class="art" viewBox="0 0 100 100" role="img" :aria-label="title">
    <rect width="100" height="100" :fill="palette.bg" />

    <!-- Long hair sits behind the shoulders, so it is drawn first. -->
    <path
      v-if="palette.longHair"
      d="M26 44 C26 70 30 78 32 88 L68 88 C70 78 74 70 74 44 Z"
      :fill="palette.hair"
    />

    <!-- Shoulders -->
    <path d="M12 100 C12 80 30 72 50 72 C70 72 88 80 88 100 Z" :fill="palette.shirt" />
    <!-- Neck -->
    <rect x="43" y="58" width="14" height="18" rx="7" :fill="palette.skin" />
    <!-- Head -->
    <ellipse cx="50" cy="44" rx="20" ry="23" :fill="palette.skin" />
    <!-- Hair on top -->
    <path
      d="M28 44 C28 24 44 17 50 17 C56 17 72 24 72 44 C72 35 64 31 50 31 C36 31 28 35 28 44 Z"
      :fill="palette.hair"
    />

    <!-- Glasses -->
    <g fill="none" stroke="#2b3444" stroke-width="2">
      <circle cx="42" cy="45" r="7" />
      <circle cx="58" cy="45" r="7" />
      <path d="M49 45 H51" />
    </g>

    <!-- Mouth -->
    <path
      d="M45 56 Q50 60 55 56"
      stroke="#b4756a"
      stroke-width="2"
      fill="none"
      stroke-linecap="round"
    />
  </svg>
</template>

<style scoped>
.art {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
```

- [ ] **Step 2: Проверить типы**

Run: `npm run type-check`
Expected: успешно.

- [ ] **Step 3: Проверить линтер**

Run: `npm run lint`
Expected: успешно.

- [ ] **Step 4: Коммит**

```bash
git add src/components/AvatarArt.vue
git commit -m "feat(frontend): add parameterised candidate portrait SVG"
```

---

### Task 3: Подключить портреты в CandidateAvatar

**Files:**
- Modify: `frontend/src/components/CandidateAvatar.vue` (полная замена содержимого)

**Interfaces:**
- Consumes: `paletteFor` из `@/components/avatarPalettes`, `AvatarArt` из `@/components/AvatarArt.vue`.
- Produces: props компонента не меняются — `{ name: string; avatarUrl?: string; size?: number }`. Все существующие места использования (`CandidateCard.vue`, `AnswerOptionRow.vue`, `OfferView.vue`) продолжают работать без правок.

Порядок резолва: известный слаг → нарисованный портрет; неизвестный слаг, но есть `avatarUrl` → пробуем загрузить картинку; не загрузилась или URL нет → цветной диск с инициалами.

- [ ] **Step 1: Заменить содержимое файла**

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'

import AvatarArt from '@/components/AvatarArt.vue'
import { paletteFor } from '@/components/avatarPalettes'

const props = withDefaults(
  defineProps<{ name: string; avatarUrl?: string; size?: number }>(),
  { size: 56 },
)

// Resolution order: drawn portrait -> remote image -> coloured initials disc.
// The seed `avatarUrl`s (/assets/candidates/*.png) point at files that do not
// exist, so without the portrait every avatar would fall through to initials.
const imageFailed = ref(false)

const palette = computed(() => paletteFor(props.avatarUrl))
const showImage = computed(() => !palette.value && !!props.avatarUrl && !imageFailed.value)

const initials = computed(() =>
  props.name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join(''),
)

const PALETTE = ['#2f6bff', '#22a06b', '#f4b740', '#8b5cf6', '#ef6461', '#0ea5e9']
const bgColor = computed(() => {
  let hash = 0
  for (let i = 0; i < props.name.length; i++) {
    hash = (hash * 31 + props.name.charCodeAt(i)) % 997
  }
  return PALETTE[hash % PALETTE.length]
})

const dims = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.38)}px`,
}))
</script>

<template>
  <span v-if="palette" class="avatar avatar--art" :style="dims">
    <AvatarArt :palette="palette" :title="name" />
  </span>
  <img
    v-else-if="showImage"
    class="avatar avatar--img"
    :src="avatarUrl"
    :alt="name"
    :style="dims"
    @error="imageFailed = true"
  />
  <span
    v-else
    class="avatar avatar--initials"
    :style="{ ...dims, backgroundColor: bgColor }"
    :aria-label="name"
    role="img"
  >
    {{ initials }}
  </span>
</template>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.avatar--art {
  overflow: hidden;
}
.avatar--initials {
  color: var(--color-text-inverse);
  font-weight: 700;
  user-select: none;
}
</style>
```

- [ ] **Step 2: Проверить типы и линтер**

Run: `npm run type-check && npm run lint`
Expected: оба успешно.

- [ ] **Step 3: Проверить в браузере**

Run: `npm run dev`
Открыть `http://localhost:5173`, нажать «Новая игра», выбрать режим, дойти до лобби.
Expected: в лобби у всех четырёх кандидатов нарисованные портреты (разные цвета волос и одежды), а не буквы. На экране вопроса портреты видны в строках ответов.

- [ ] **Step 4: Коммит**

```bash
git add src/components/CandidateAvatar.vue
git commit -m "feat(frontend): render drawn portraits for known candidates"
```

---

### Task 4: Плитка участника VideoTile

**Files:**
- Create: `frontend/src/components/VideoTile.vue`

**Interfaces:**
- Consumes: `CandidateAvatar` из `@/components/CandidateAvatar.vue`, тип `Schemas` из `@/api/client`.
- Produces: компонент с props
  `{ candidate: Schemas['Candidate']; speaking?: boolean; muted?: boolean; state?: 'connecting' | 'live' }`.
  Значения по умолчанию: `speaking: false`, `muted: true`, `state: 'live'`.

- [ ] **Step 1: Создать компонент**

```vue
<script setup lang="ts">
import CandidateAvatar from '@/components/CandidateAvatar.vue'
import type { Schemas } from '@/api/client'

withDefaults(
  defineProps<{
    candidate: Schemas['Candidate']
    /** Highlights the tile as the active speaker. */
    speaking?: boolean
    muted?: boolean
    state?: 'connecting' | 'live'
  }>(),
  { speaking: false, muted: true, state: 'live' },
)
</script>

<template>
  <div :class="['tile', { 'tile--speaking': speaking, 'tile--connecting': state === 'connecting' }]">
    <div class="tile__stage">
      <CandidateAvatar :name="candidate.name" :avatar-url="candidate.avatarUrl" :size="96" />
    </div>

    <span v-if="state === 'connecting'" class="tile__status">Подключается…</span>

    <div class="tile__bar">
      <span class="tile__ident">
        <span class="tile__name">{{ candidate.name }}</span>
        <span v-if="candidate.role" class="tile__role">{{ candidate.role }}</span>
      </span>
      <span class="tile__mic" :aria-label="muted ? 'Микрофон выключен' : 'Микрофон включён'">
        {{ muted ? '🔇' : '🎤' }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.tile {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: linear-gradient(160deg, #dfe6f5 0%, #eef1f7 100%);
  border: 2px solid transparent;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.tile--speaking {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}
.tile--connecting {
  opacity: 0.6;
}
.tile__stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
}
.tile__status {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);
  font-size: 12px;
  color: var(--color-text-muted);
  background-color: var(--color-surface);
  border-radius: var(--radius-pill);
  padding: 2px var(--space-2);
}
.tile__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background-color: rgba(255, 255, 255, 0.85);
}
.tile__ident {
  display: flex;
  flex-direction: column;
  text-align: left;
  min-width: 0;
}
.tile__name {
  font-weight: 700;
  font-size: var(--text-sm);
  color: var(--color-text);
}
.tile__role {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tile__mic {
  font-size: var(--text-sm);
  line-height: 1;
}
</style>
```

- [ ] **Step 2: Проверить типы и линтер**

Run: `npm run type-check && npm run lint`
Expected: оба успешно.

- [ ] **Step 3: Коммит**

```bash
git add src/components/VideoTile.vue
git commit -m "feat(frontend): add video call participant tile"
```

---

### Task 5: Панель управления звонком CallControlBar

**Files:**
- Create: `frontend/src/components/CallControlBar.vue`

**Interfaces:**
- Consumes: ничего.
- Produces: компонент без props, эмитит `hangup`.

Все кнопки кроме «Завершить» — декоративные: телефонии за ними нет. Поэтому они `aria-hidden="true"` и `tabindex="-1"`, чтобы скринридер и Tab не предлагали пользователю несуществующие действия.

- [ ] **Step 1: Создать компонент**

```vue
<script setup lang="ts">
defineEmits<{ hangup: [] }>()

// Decorative only — there is no real telephony behind these. They exist to make
// the interview read as a video call (design screen 5). Kept out of the a11y
// tree and tab order so nobody is offered an action that does nothing.
const DECORATIVE = ['🎤', '📹', '🖥', '⏺', '💬', '👥', '⚙'] as const
</script>

<template>
  <div class="bar">
    <button
      v-for="icon in DECORATIVE"
      :key="icon"
      type="button"
      class="bar__btn"
      aria-hidden="true"
      tabindex="-1"
    >
      {{ icon }}
    </button>
    <button type="button" class="bar__btn bar__btn--end" @click="$emit('hangup')">
      Завершить
    </button>
  </div>
</template>

<style scoped>
.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3);
}
.bar__btn {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background-color: var(--color-surface);
  font-size: var(--text-sm);
  line-height: 1;
}
.bar__btn--end {
  width: auto;
  padding: 0 var(--space-4);
  background-color: var(--color-error);
  border-color: var(--color-error);
  color: var(--color-text-inverse);
  font-weight: 600;
}
.bar__btn--end:hover {
  filter: brightness(0.94);
}
</style>
```

- [ ] **Step 2: Проверить типы и линтер**

Run: `npm run type-check && npm run lint`
Expected: оба успешно.

- [ ] **Step 3: Коммит**

```bash
git add src/components/CallControlBar.vue
git commit -m "feat(frontend): add call control bar"
```

---

### Task 6: Сетка звонка VideoCallStage

**Files:**
- Create: `frontend/src/components/VideoCallStage.vue`

**Interfaces:**
- Consumes: `VideoTile` и `CallControlBar`, тип `Schemas` из `@/api/client`.
- Produces: компонент с props
  `{ candidates: Schemas['Candidate'][]; speakingId?: number | null; state?: 'connecting' | 'live' }`
  (по умолчанию `speakingId: null`, `state: 'live'`), эмитит `hangup`.

- [ ] **Step 1: Создать компонент**

```vue
<script setup lang="ts">
import CallControlBar from '@/components/CallControlBar.vue'
import VideoTile from '@/components/VideoTile.vue'
import type { Schemas } from '@/api/client'

withDefaults(
  defineProps<{
    candidates: Schemas['Candidate'][]
    /** Candidate currently highlighted as the speaker. */
    speakingId?: number | null
    state?: 'connecting' | 'live'
  }>(),
  { speakingId: null, state: 'live' },
)

defineEmits<{ hangup: [] }>()
</script>

<template>
  <section class="call">
    <div class="call__grid">
      <VideoTile
        v-for="c in candidates"
        :key="c.id"
        :candidate="c"
        :state="state"
        :speaking="state === 'live' && c.id === speakingId"
        :muted="c.id !== speakingId"
      />
    </div>
    <CallControlBar @hangup="$emit('hangup')" />
  </section>
</template>

<style scoped>
.call {
  background-color: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-4);
}
.call__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

@media (max-width: 720px) {
  .call__grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 2: Проверить типы и линтер**

Run: `npm run type-check && npm run lint`
Expected: оба успешно.

- [ ] **Step 3: Коммит**

```bash
git add src/components/VideoCallStage.vue
git commit -m "feat(frontend): add video call stage grid"
```

---

### Task 7: Лобби на плитках звонка

**Files:**
- Modify: `frontend/src/views/LobbyView.vue` (полная замена содержимого)

**Interfaces:**
- Consumes: `VideoCallStage` из Task 6.
- Produces: ничего для последующих задач.

`CandidateCard` перестаёт использоваться в этой вьюхе, но остаётся в проекте — он всё ещё нужен в других местах. Удалять его не надо.

- [ ] **Step 1: Заменить содержимое файла**

```vue
<script setup lang="ts">
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import VideoCallStage from '@/components/VideoCallStage.vue'
import { useInterviewStore } from '@/stores/interview'

const props = defineProps<{ id: string }>()

const router = useRouter()
const interview = useInterviewStore()

function begin() {
  router.push({ name: 'question', params: { id: props.id } })
}
</script>

<template>
  <AppScreen width="wide">
    <AppCard class="lobby">
      <h1 class="lobby__title">Ожидание собеседования</h1>
      <p class="lobby__subtitle">Кандидаты подключаются к звонку. Скоро начнём!</p>

      <VideoCallStage
        class="lobby__call"
        :candidates="interview.candidates"
        state="connecting"
        @hangup="router.push({ name: 'home' })"
      />

      <AppButton class="lobby__start" @click="begin">Начать собеседование →</AppButton>
    </AppCard>
  </AppScreen>
</template>

<style scoped>
.lobby {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  text-align: center;
}
.lobby__title {
  font-size: var(--text-xl);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.lobby__subtitle {
  color: var(--color-text-muted);
}
.lobby__call {
  width: 100%;
  margin: var(--space-2) 0;
}
.lobby__start {
  margin-top: var(--space-2);
}
</style>
```

- [ ] **Step 2: Проверить типы и линтер**

Run: `npm run type-check && npm run lint`
Expected: оба успешно.

- [ ] **Step 3: Проверить в браузере**

Run: `npm run dev`
Дойти до лобби.
Expected: сетка 2×2 приглушённых плиток с подписью «Подключается…», у каждой — имя, роль, иконка микрофона; снизу панель кнопок с красной «Завершить». Кнопка «Завершить» возвращает на главную. Tab по странице не заходит на декоративные кнопки.

- [ ] **Step 4: Коммит**

```bash
git add src/views/LobbyView.vue
git commit -m "feat(frontend): show lobby as a connecting video call"
```

---

### Task 8: Экран вопроса со звонком

**Files:**
- Modify: `frontend/src/views/QuestionView.vue`

**Interfaces:**
- Consumes: `VideoCallStage` из Task 6.
- Produces: ничего для последующих задач.

Меняются три вещи: импорт компонента, вычисляемый `speakingId`, и блок `<template>` — над карточкой вопроса добавляется сетка звонка. Скрипт ниже показывает только добавляемое; остальной `<script setup>` и `<style>` остаются как есть.

**`speakingId`:** до раскрытия ответа подсвечивается кандидат, чей вариант выбран игроком; после раскрытия — автор правильного варианта. Пока ничего не выбрано — `null`, никто не подсвечен.

- [ ] **Step 1: Добавить импорт**

В блоке импортов, после строки с `AppScreen` (`frontend/src/views/QuestionView.vue:7`), добавить:

```ts
import VideoCallStage from '@/components/VideoCallStage.vue'
```

- [ ] **Step 2: Добавить вычисление говорящего**

После объявления `candidateById` (заканчивается на строке 33) добавить:

```ts
// Ties the call grid to the game: highlight whoever "owns" the answer in focus —
// the player's pick while the round is open, the correct author once revealed.
const speakingId = computed(() => {
  const answers = question.value?.answers ?? []
  const focusId = revealed.value ? correctAnswerId.value : chosenAnswerId.value
  if (focusId === null) return null
  return answers.find((a) => a.answerId === focusId)?.candidateId ?? null
})
```

- [ ] **Step 3: Вставить сетку звонка в шаблон**

Внутри `<div v-else class="game">` перед комментарием `<!-- Question (design screen 6) -->` добавить:

```html
      <!-- Video call (design screen 5) -->
      <VideoCallStage
        :candidates="interview.candidates"
        :speaking-id="speakingId"
        @hangup="ui.open()"
      />
```

- [ ] **Step 4: Проверить типы и линтер**

Run: `npm run type-check && npm run lint`
Expected: оба успешно.

- [ ] **Step 5: Проверить в браузере**

Run: `npm run dev`
Пройти лобби → «Начать собеседование», ответить на несколько вопросов.
Expected:
- Над вопросом сетка 2×2 с портретами кандидатов, снизу панель звонка.
- До выбора ответа никто не подсвечен; после клика по варианту подсвечивается плитка его автора; после раскрытия подсветка переходит на автора правильного ответа.
- Кнопка «Завершить» открывает модалку паузы (то же, что «⏸» в шапке вопроса).
- Таймер, прогресс и промежуточная статистика работают как раньше.

- [ ] **Step 6: Коммит**

```bash
git add src/views/QuestionView.vue
git commit -m "feat(frontend): stage the interview as a video call"
```

---

### Task 9: Финальная проверка

**Files:** изменений нет.

- [ ] **Step 1: Полная сборка**

Run: `npm run build`
Expected: `type-check` и `vite build` проходят, `dist/` собирается без ошибок.

- [ ] **Step 2: Пройти игру целиком**

Run: `npm run dev`
Пройти путь: главная → выбор режима → лобби → все 10 вопросов → результат → оффер → итог.
Expected: портреты на всех экранах, где есть кандидаты (лобби, сетка звонка, строки ответов, экран оффера); ни одного цветного диска с инициалами; ни одной битой картинки в консоли (`404` на `/assets/candidates/*.png` быть не должно — портреты рисуются, а не грузятся).

- [ ] **Step 3: Проверить адаптив**

В DevTools переключить ширину на 600px.
Expected: сетка звонка в одну колонку, кнопки панели переносятся на вторую строку, «Завершить» видна, горизонтального скролла нет.

- [ ] **Step 4: Проверить фолбэк на инициалы**

Временно изменить `avatarUrl` первого кандидата в `frontend/src/mocks/fixtures.ts:21` на `/assets/candidates/unknown.png`, перезагрузить лобби.
Expected: этот кандидат отрисован цветным диском с инициалами, остальные — портретами. Затем вернуть значение обратно и убедиться, что портрет вернулся.

- [ ] **Step 5: Коммит, если что-то правилось**

```bash
git status
# если есть изменения после проверок:
git add -A && git commit -m "fix(frontend): address video call redesign review findings"
```
