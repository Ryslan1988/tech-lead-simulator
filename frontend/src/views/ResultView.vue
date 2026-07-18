<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import StatTile from '@/components/StatTile.vue'
import { useInterviewStore } from '@/stores/interview'

const props = defineProps<{ id: string }>()

const router = useRouter()
const interview = useInterviewStore()

const showDetails = ref(false)

onMounted(() => {
  if (!interview.result) interview.loadResult()
})

const result = computed(() => interview.result)

function toOffer() {
  router.push({ name: 'offer', params: { id: props.id } })
}
</script>

<template>
  <AppScreen width="medium">
    <div v-if="interview.error && !result" class="state">
      <p>Не удалось загрузить результат.</p>
      <AppButton variant="secondary" @click="interview.loadResult()">Повторить</AppButton>
    </div>

    <div v-else-if="!result" class="state">Загрузка результата…</div>

    <div v-else class="result">
      <!-- Summary (design screen 13) -->
      <AppCard class="summary">
        <h1 class="summary__title">Собеседование завершено!</h1>
        <p class="summary__label">Ваш результат</p>
        <p class="summary__score">
          {{ result.correctCount }}<span class="summary__total">/{{ result.totalQuestions }}</span>
        </p>
        <div class="summary__stats">
          <StatTile icon="🎯" label="Очки" :value="result.totalPoints" />
          <StatTile icon="🔥" label="Лучший стрик" :value="result.bestStreak" />
        </div>
        <div class="summary__actions">
          <AppButton variant="secondary" @click="showDetails = !showDetails">
            {{ showDetails ? 'Скрыть детали' : 'Посмотреть детально' }}
          </AppButton>
          <AppButton @click="toOffer">К выбору команды →</AppButton>
        </div>
      </AppCard>

      <!-- Detailed breakdown (design screen 14) -->
      <AppCard v-if="showDetails" class="details">
        <h2 class="section-title">Детальная статистика</h2>
        <ul class="breakdown">
          <li v-for="(q, i) in result.breakdown" :key="q.questionId ?? i" class="breakdown__row">
            <span class="breakdown__text">{{ q.text }}</span>
            <span
              :class="['breakdown__mark', q.correct ? 'is-ok' : 'is-no']"
              aria-hidden="true"
            >
              {{ q.correct ? '✓' : '✗' }}
            </span>
          </li>
        </ul>
      </AppCard>
    </div>
  </AppScreen>
</template>

<style scoped>
.result {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-3);
}
.summary__title {
  font-size: var(--text-xl);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.summary__label {
  color: var(--color-text-muted);
}
.summary__score {
  font-size: 64px;
  font-weight: 800;
  color: var(--color-primary);
  line-height: 1;
}
.summary__total {
  color: var(--color-text-muted);
  font-size: 32px;
}
.summary__stats {
  display: flex;
  gap: var(--space-8);
  margin: var(--space-2) 0 var(--space-4);
}
.summary__actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  justify-content: center;
}
.section-title {
  font-size: var(--text-base);
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--color-text-muted);
  margin-bottom: var(--space-4);
}
.breakdown {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.breakdown__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--color-border);
}
.breakdown__row:last-child {
  border-bottom: none;
}
.breakdown__mark {
  font-weight: 700;
  font-size: var(--text-lg);
  flex-shrink: 0;
}
.is-ok {
  color: var(--color-success);
}
.is-no {
  color: var(--color-error);
}
.state {
  text-align: center;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
}

@media (max-width: 520px) {
  .summary__actions {
    flex-direction: column;
    align-self: stretch;
  }
}
</style>
