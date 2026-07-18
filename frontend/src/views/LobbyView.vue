<script setup lang="ts">
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import CandidateCard from '@/components/CandidateCard.vue'
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
      <p class="lobby__subtitle">Кандидаты готовятся. Скоро начнём!</p>

      <div class="lobby__grid">
        <div v-for="c in interview.candidates" :key="c.id" class="lobby__tile">
          <CandidateCard :candidate="c" :avatar-size="72" />
          <span class="lobby__live" aria-hidden="true">● в сети</span>
        </div>
      </div>

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
.lobby__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-6);
  width: 100%;
  margin: var(--space-4) 0;
}
.lobby__tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4);
  background-color: var(--color-bg);
  border-radius: var(--radius-md);
}
.lobby__live {
  font-size: 12px;
  color: var(--color-success);
  font-weight: 600;
}
.lobby__start {
  margin-top: var(--space-2);
}

@media (max-width: 720px) {
  .lobby__grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
