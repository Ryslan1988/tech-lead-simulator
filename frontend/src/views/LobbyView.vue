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
