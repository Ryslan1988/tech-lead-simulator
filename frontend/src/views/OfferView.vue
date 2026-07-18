<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import CandidateAvatar from '@/components/CandidateAvatar.vue'
import CandidateCard from '@/components/CandidateCard.vue'
import type { Schemas } from '@/api/client'
import { useInterviewStore } from '@/stores/interview'

const props = defineProps<{ id: string }>()

const router = useRouter()
const interview = useInterviewStore()

const index = ref(0)

onMounted(() => {
  if (!interview.statistic) interview.loadStatistic()
})

interface RankedCandidate {
  candidate: Schemas['Candidate']
  timesChosen: number
  correctAnswers: number
}

// Rank by objective competence (correctAnswers) — the intended hiring signal —
// merging the statistic with the full candidate (strengths, avatar) lineup.
const ranked = computed<RankedCandidate[]>(() => {
  const stats = interview.statistic?.perCandidate ?? []
  return stats
    .map((s) => {
      const candidate =
        interview.candidates.find((c) => c.id === s.candidateId) ??
        ({ id: s.candidateId, name: s.name, role: s.role } as Schemas['Candidate'])
      return { candidate, timesChosen: s.timesChosen, correctAnswers: s.correctAnswers }
    })
    .sort((a, b) => b.correctAnswers - a.correctAnswers)
})

const current = computed<RankedCandidate | undefined>(() => ranked.value[index.value])
const totalQuestions = computed(() => interview.statistic?.totalQuestions ?? 0)

function move(delta: number) {
  const n = ranked.value.length
  if (n === 0) return
  index.value = (index.value + delta + n) % n
}

async function accept() {
  const candidate = current.value?.candidate
  if (!candidate) return
  await interview.makeOffer(candidate.id)
}

function toSummary() {
  router.push({ name: 'summary', params: { id: props.id } })
}

// Team panel (design screen 16): fill up to 4 slots, hired first.
const TEAM_SLOTS = 4
const emptySlots = computed(() => Math.max(0, TEAM_SLOTS - 1))
</script>

<template>
  <AppScreen width="medium">
    <div v-if="interview.error && !interview.statistic" class="state">
      <p>Не удалось загрузить кандидатов.</p>
      <AppButton variant="secondary" @click="interview.loadStatistic()">Повторить</AppButton>
    </div>

    <div v-else-if="!interview.statistic" class="state">Загрузка кандидатов…</div>

    <!-- Team panel after the offer (design screen 16) -->
    <AppCard v-else-if="interview.offerResult" class="team">
      <h1 class="team__title">Ваша команда</h1>
      <p class="team__msg">{{ interview.offerResult.message }}</p>
      <div class="team__grid">
        <CandidateCard
          :candidate="interview.offerResult.hiredCandidate"
          :avatar-size="72"
        />
        <div v-for="n in emptySlots" :key="n" class="team__empty">
          <span class="team__plus" aria-hidden="true">+</span>
          <span class="team__empty-label">Место свободно</span>
        </div>
      </div>
      <AppButton @click="toSummary">Продолжить →</AppButton>
    </AppCard>

    <!-- Candidate carousel (design screen 15) -->
    <div v-else class="offer">
      <h1 class="offer__title">Выберите кандидата в команду</h1>

      <div class="carousel">
        <button class="carousel__arrow" aria-label="Предыдущий" @click="move(-1)">‹</button>

        <AppCard v-if="current" class="hero">
          <CandidateAvatar
            :name="current.candidate.name"
            :avatar-url="current.candidate.avatarUrl"
            :size="96"
          />
          <h2 class="hero__name">{{ current.candidate.name }}</h2>
          <p class="hero__role">{{ current.candidate.role }}</p>

          <div v-if="current.candidate.strengths?.length" class="hero__strengths">
            <span class="hero__strengths-label">Сильные стороны:</span>
            <ul>
              <li v-for="s in current.candidate.strengths" :key="s">{{ s }}</li>
            </ul>
          </div>

          <div class="hero__signals">
            <span class="signal signal--strong">
              Верных ответов: <strong>{{ current.correctAnswers }}</strong> из {{ totalQuestions }}
            </span>
            <span class="signal">Вы доверяли: {{ current.timesChosen }} раз</span>
          </div>
        </AppCard>

        <button class="carousel__arrow" aria-label="Следующий" @click="move(1)">›</button>
      </div>

      <div class="dots" aria-hidden="true">
        <span
          v-for="(r, i) in ranked"
          :key="r.candidate.id"
          :class="['dot', { 'dot--active': i === index }]"
        />
      </div>

      <AppButton :disabled="!current || interview.loading" @click="accept">
        Принять в команду
      </AppButton>
    </div>
  </AppScreen>
</template>

<style scoped>
.offer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
}
.offer__title {
  font-size: var(--text-lg);
  text-transform: uppercase;
  letter-spacing: 1px;
  text-align: center;
}
.carousel {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
}
.carousel__arrow {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background-color: var(--color-surface);
  font-size: 24px;
  color: var(--color-text-muted);
  line-height: 1;
}
.carousel__arrow:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-2);
}
.hero__name {
  font-size: var(--text-lg);
}
.hero__role {
  color: var(--color-text-muted);
}
.hero__strengths {
  align-self: stretch;
  margin-top: var(--space-2);
}
.hero__strengths-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-muted);
}
.hero__strengths ul {
  list-style: none;
  margin-top: var(--space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: center;
}
.hero__strengths li {
  background-color: var(--color-bg);
  border-radius: var(--radius-pill);
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-sm);
}
.hero__signals {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin-top: var(--space-3);
}
.signal {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
.signal--strong {
  color: var(--color-text);
}
.dots {
  display: flex;
  gap: var(--space-2);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-pill);
  background-color: var(--color-border);
}
.dot--active {
  background-color: var(--color-primary);
}
.team {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-4);
}
.team__title {
  font-size: var(--text-xl);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.team__msg {
  color: var(--color-text-muted);
}
.team__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  width: 100%;
  margin: var(--space-2) 0;
}
.team__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  color: var(--color-text-muted);
}
.team__plus {
  font-size: 28px;
  line-height: 1;
}
.team__empty-label {
  font-size: 12px;
}

@media (max-width: 620px) {
  .team__grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
