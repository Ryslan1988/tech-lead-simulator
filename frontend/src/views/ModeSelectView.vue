<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import type { Schemas } from '@/api/client'
import { useHomeStore } from '@/stores/home'
import { useInterviewStore } from '@/stores/interview'

const router = useRouter()
const home = useHomeStore()
const interview = useInterviewStore()

const starting = ref<Schemas['Mode'] | null>(null)

onMounted(() => {
  if (!home.homePage) home.loadHome()
})

const modes = computed(() => home.homePage?.modes ?? [])

// Trophy for the long "career" path, stopwatch for the quick run.
function iconFor(mode: Schemas['Mode']): string {
  return mode === 'HARDCORE' ? '🏆' : '⏱️'
}

async function choose(mode: Schemas['Mode']) {
  if (starting.value) return
  starting.value = mode
  const interviewId = await interview.start({ mode, difficulty: 'MEDIUM' })
  starting.value = null
  if (interviewId != null) {
    router.push({ name: 'lobby', params: { id: String(interviewId) } })
  }
}
</script>

<template>
  <AppScreen width="medium">
    <div class="mode">
      <h1 class="mode__title">Выберите режим</h1>

      <p v-if="interview.error" class="mode__error">
        Не удалось начать игру. Попробуйте ещё раз.
      </p>

      <div class="mode__grid">
        <AppCard
          v-for="m in modes"
          :key="m.mode"
          interactive
          class="mode-card"
          role="button"
          tabindex="0"
          @click="choose(m.mode)"
          @keydown.enter="choose(m.mode)"
          @keydown.space.prevent="choose(m.mode)"
        >
          <span class="mode-card__icon" aria-hidden="true">{{ iconFor(m.mode) }}</span>
          <h2 class="mode-card__title">{{ m.title }}</h2>
          <p class="mode-card__desc">{{ m.description }}</p>
          <span class="mode-card__count">{{ m.questionCount }} вопросов</span>
          <span v-if="starting === m.mode" class="mode-card__loading">Запуск…</span>
        </AppCard>
      </div>

      <RouterLink class="mode__back" to="/">← На главную</RouterLink>
    </div>
  </AppScreen>
</template>

<style scoped>
.mode {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-6);
}
.mode__title {
  font-size: var(--text-xl);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.mode__error {
  color: var(--color-error);
}
.mode__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
  width: 100%;
}
.mode-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-3);
  padding: var(--space-8) var(--space-6);
}
.mode-card__icon {
  font-size: 48px;
  line-height: 1;
}
.mode-card__title {
  font-size: var(--text-lg);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.mode-card__desc {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
.mode-card__count {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-primary);
}
.mode-card__loading {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
.mode__back {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
.mode__back:hover {
  color: var(--color-primary);
}

@media (max-width: 560px) {
  .mode__grid {
    grid-template-columns: 1fr;
  }
}
</style>
