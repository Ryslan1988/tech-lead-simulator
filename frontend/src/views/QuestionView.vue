<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import VideoCallStage from '@/components/VideoCallStage.vue'
import AnswerOptionRow from '@/components/AnswerOptionRow.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import StatTile from '@/components/StatTile.vue'
import type { Schemas } from '@/api/client'
import { useCountdown } from '@/composables/useCountdown'
import { useInterviewStore } from '@/stores/interview'
import { useUiStore } from '@/stores/ui'

const props = defineProps<{ id: string }>()

const router = useRouter()
const interview = useInterviewStore()
const ui = useUiStore()
const countdown = useCountdown()

const chosenAnswerId = ref<number | null>(null)

const question = computed(() => interview.currentQuestion)
const revealed = computed(() => interview.lastFeedback !== null)
const correctAnswerId = computed(() => interview.lastFeedback?.correctAnswerId ?? null)

const candidateById = computed(() => {
  const map = new Map<number, Schemas['Candidate']>()
  for (const c of interview.candidates) map.set(c.id, c)
  return map
})

// Ties the call grid to the game: highlight whoever "owns" the answer in focus —
// the player's pick while the round is open, the correct author once revealed.
const speakingId = computed(() => {
  const answers = question.value?.answers ?? []
  const focusId = revealed.value ? correctAnswerId.value : chosenAnswerId.value
  if (focusId === null) return null
  return answers.find((a) => a.answerId === focusId)?.candidateId ?? null
})

onMounted(startRound)

async function startRound() {
  chosenAnswerId.value = null
  await interview.loadQuestion()
  // 409 -> the run is over; jump to the result screen.
  if (interview.finished && !interview.currentQuestion) {
    goToResult()
    return
  }
  const limit = interview.currentQuestion?.timeLimitSeconds
  if (limit) countdown.start(limit, onTimeout)
}

async function choose(answerId: number) {
  if (revealed.value || interview.loading) return
  chosenAnswerId.value = answerId
  countdown.stop()
  await interview.submitAnswer(answerId)
}

function onTimeout() {
  if (revealed.value) return
  // Time is a hint, but the API needs an answer — auto-pick one so the round
  // resolves and the game keeps moving.
  const options = question.value?.answers ?? []
  if (options.length === 0) return
  const pick = options[Math.floor(Math.random() * options.length)]
  if (pick) choose(pick.answerId)
}

function next() {
  if (interview.finished) {
    goToResult()
  } else {
    startRound()
  }
}

function goToResult() {
  router.push({ name: 'result', params: { id: props.id } })
}
</script>

<template>
  <AppScreen width="medium">
    <div v-if="interview.error && !question" class="state">
      <p>Не удалось загрузить вопрос.</p>
      <AppButton variant="secondary" @click="startRound">Повторить</AppButton>
    </div>

    <div v-else-if="!question" class="state">Загрузка вопроса…</div>

    <div v-else class="game">
      <!-- Video call (design screen 5) -->
      <VideoCallStage
        dense
        :candidates="interview.candidates"
        :speaking-id="speakingId"
        @hangup="ui.open()"
      />

      <!-- Question (design screen 6) -->
      <AppCard class="game__question">
        <div class="qhead">
          <span class="qhead__index">Вопрос {{ question.index }} из {{ question.total }}</span>
          <div class="qhead__right">
            <span v-if="question.timeLimitSeconds" class="qhead__timer">
              ⏱ {{ countdown.formatted.value }}
            </span>
            <button class="qhead__pause" aria-label="Пауза" @click="ui.open()">⏸</button>
          </div>
        </div>
        <h1 class="qtext">{{ question.text }}</h1>
        <ProgressBar :value="question.index - 1" :max="question.total" />
      </AppCard>

      <!-- Answers (design screen 7) -->
      <AppCard class="game__answers">
        <h2 class="section-title">Выберите правильный ответ</h2>
        <div class="answers">
          <AnswerOptionRow
            v-for="a in question.answers"
            :key="a.answerId"
            :text="a.text"
            :candidate="candidateById.get(a.candidateId)"
            :revealed="revealed"
            :correct="revealed && a.answerId === correctAnswerId"
            :chosen="a.answerId === chosenAnswerId"
            :disabled="revealed || interview.loading"
            @select="choose(a.answerId)"
          />
        </div>
      </AppCard>

      <!-- Intermediate stats (design screen 10) -->
      <AppCard v-if="revealed" class="game__stats">
        <h2 class="section-title">Прогресс игры</h2>
        <div class="stats-row">
          <StatTile label="Правильных ответов" :value="interview.correctCount" />
          <StatTile label="Текущая серия" :value="interview.currentStreak" />
          <StatTile label="Очки" :value="interview.totalPoints" />
        </div>
        <div class="marks" aria-hidden="true">
          <span
            v-for="(ok, i) in interview.outcomes"
            :key="i"
            :class="['mark', ok ? 'mark--ok' : 'mark--no']"
          >
            {{ ok ? '✓' : '✗' }}
          </span>
        </div>
        <div class="game__footer">
          <span class="game__count">Вопрос {{ interview.answeredCount }} из {{ question.total }}</span>
          <AppButton @click="next">
            {{ interview.finished ? 'Смотреть результат →' : 'Далее →' }}
          </AppButton>
        </div>
      </AppCard>
    </div>
  </AppScreen>
</template>

<style scoped>
.game {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.qhead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}
.qhead__index {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.qhead__right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.qhead__timer {
  font-size: var(--text-sm);
  font-weight: 700;
  color: var(--color-primary);
}
.qhead__pause {
  background: none;
  border: none;
  font-size: var(--text-base);
  color: var(--color-text-muted);
  line-height: 1;
}
.qhead__pause:hover {
  color: var(--color-primary);
}
.qtext {
  font-size: var(--text-lg);
  margin-bottom: var(--space-4);
}
.section-title {
  font-size: var(--text-base);
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--color-text-muted);
  margin-bottom: var(--space-4);
}
.answers {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.marks {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}
.mark {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  font-weight: 700;
  font-size: var(--text-sm);
}
.mark--ok {
  background-color: var(--color-success-soft);
  color: var(--color-success);
}
.mark--no {
  background-color: var(--color-error-soft);
  color: var(--color-error);
}
.game__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.game__count {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
.state {
  text-align: center;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
}
</style>
