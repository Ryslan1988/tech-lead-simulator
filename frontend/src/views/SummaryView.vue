<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import RoadmapItem from '@/components/RoadmapItem.vue'
import { useInterviewStore } from '@/stores/interview'

const interview = useInterviewStore()
const router = useRouter()

onMounted(() => {
  if (!interview.result) interview.loadResult()
  interview.loadAiResult()
})

const ai = computed(() => interview.aiResult)
const ready = computed(() => ai.value?.status === 'READY')
const analyzing = computed(() => !ai.value || ai.value.status === 'PENDING')

const finalPoints = computed(
  () => interview.result?.totalPoints ?? interview.totalPoints,
)

function goHome() {
  interview.reset()
  router.push('/')
}
</script>

<template>
  <AppScreen width="medium">
    <div class="summary">
      <!-- Celebration (design screen 17) -->
      <AppCard class="celebrate">
        <div class="confetti" aria-hidden="true">🎉✨🎊</div>
        <h1 class="celebrate__title">Поздравляем!</h1>
        <p class="celebrate__subtitle">Вы прошли собеседование и собрали команду!</p>
        <div class="trophy" aria-hidden="true">🏆</div>
        <p class="celebrate__label">Итоговые очки</p>
        <p class="celebrate__points">{{ finalPoints }}</p>
      </AppCard>

      <!-- AI analysis + roadmap (GET /ai-result; no mockup, matches the flat style) -->
      <AppCard class="ai">
        <h2 class="section-title">AI-анализ и план развития</h2>

        <div v-if="analyzing" class="ai__loading">
          <span class="spinner" aria-hidden="true" />
          Анализируем ваши решения…
        </div>

        <template v-else-if="ready">
          <p v-if="ai?.summary" class="ai__text">{{ ai.summary }}</p>
          <p v-if="ai?.verdict" class="ai__verdict">{{ ai.verdict }}</p>

          <div v-if="ai?.roadmap?.length" class="ai__roadmap">
            <h3 class="ai__roadmap-title">Что изучить дальше</h3>
            <RoadmapItem v-for="(item, i) in ai.roadmap" :key="i" :item="item" />
          </div>
        </template>

        <p v-else class="ai__error">Не удалось получить AI-анализ.</p>
      </AppCard>

      <AppButton class="summary__home" @click="goHome">На главную</AppButton>
    </div>
  </AppScreen>
</template>

<style scoped>
.summary {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
}
.celebrate {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-2);
}
.confetti {
  font-size: 28px;
  letter-spacing: 8px;
}
.celebrate__title {
  font-size: var(--text-2xl);
  color: var(--color-primary);
}
.celebrate__subtitle {
  color: var(--color-text-muted);
}
.trophy {
  font-size: 72px;
  line-height: 1;
  margin: var(--space-2) 0;
}
.celebrate__label {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.celebrate__points {
  font-size: 56px;
  font-weight: 800;
  color: var(--color-gold);
  line-height: 1;
}
.ai {
  align-self: stretch;
}
.section-title {
  font-size: var(--text-base);
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--color-text-muted);
  margin-bottom: var(--space-4);
}
.ai__loading {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text-muted);
  padding: var(--space-4) 0;
}
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: var(--radius-pill);
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.ai__text {
  color: var(--color-text);
  margin-bottom: var(--space-3);
}
.ai__verdict {
  font-weight: 600;
  color: var(--color-primary);
  padding: var(--space-3);
  background-color: #f5f8ff;
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}
.ai__roadmap {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.ai__roadmap-title {
  font-size: var(--text-base);
  font-weight: 700;
}
.ai__error {
  color: var(--color-error);
}
.summary__home {
  min-width: 220px;
}
</style>
