import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { api, type Schemas } from '@/api/client'

/**
 * The game's spine: one interview session and all the per-round state that
 * drives the question, result, offer and AI-summary screens. Every action wraps
 * a single endpoint from `openapi.yaml` via the typed `api` client.
 */
export const useInterviewStore = defineStore('interview', () => {
  // --- Session + running game state ---
  const session = ref<Schemas['InterviewSession'] | null>(null)
  const currentQuestion = ref<Schemas['Question'] | null>(null)
  const lastFeedback = ref<Schemas['AnswerResult'] | null>(null)

  // Running totals, mirrored from each AnswerResult so screens can show them
  // without re-fetching.
  const correctCount = ref(0)
  const currentStreak = ref(0)
  const totalPoints = ref(0)
  const answeredCount = ref(0)
  const finished = ref(false)
  // Per-round correctness, in order — drives the intermediate-stats checkmarks.
  const outcomes = ref<boolean[]>([])

  // --- Post-run payloads ---
  const statistic = ref<Schemas['InterviewStatistic'] | null>(null)
  const offerResult = ref<Schemas['OfferResult'] | null>(null)
  const result = ref<Schemas['InterviewResult'] | null>(null)
  const aiResult = ref<Schemas['AiInterviewResult'] | null>(null)

  // --- UI status ---
  const loading = ref(false)
  const error = ref<Schemas['Error'] | null>(null)

  const totalQuestions = computed(() => session.value?.totalQuestions ?? 0)
  const candidates = computed(() => session.value?.candidates ?? [])
  const progress = computed(() =>
    totalQuestions.value ? answeredCount.value / totalQuestions.value : 0,
  )

  /** The route guard uses this: is there a loaded session for this id? */
  function hasSessionFor(interviewId: number): boolean {
    return session.value?.interviewId === interviewId
  }

  function resetTotals() {
    correctCount.value = 0
    currentStreak.value = 0
    totalPoints.value = 0
    answeredCount.value = 0
    finished.value = false
    outcomes.value = []
    currentQuestion.value = null
    lastFeedback.value = null
    statistic.value = null
    offerResult.value = null
    result.value = null
    aiResult.value = null
  }

  function reset() {
    session.value = null
    resetTotals()
    error.value = null
  }

  async function start(
    context: Schemas['StartInterviewRequest'],
  ): Promise<number | null> {
    loading.value = true
    error.value = null
    const { data, error: err } = await api.POST('/interviews', { body: context })
    loading.value = false
    if (err || !data) {
      error.value = normalizeError(err)
      return null
    }
    session.value = data
    resetTotals()
    return data.interviewId
  }

  async function loadQuestion(): Promise<void> {
    if (!session.value) return
    loading.value = true
    error.value = null
    lastFeedback.value = null
    const { data, error: err } = await api.GET(
      '/interviews/{interviewId}/question',
      { params: { path: { interviewId: session.value.interviewId } } },
    )
    loading.value = false
    if (err) {
      // 409 NO_QUESTION_AVAILABLE is the "run is over" signal, not a real error.
      if (err.code === 'NO_QUESTION_AVAILABLE') {
        finished.value = true
        currentQuestion.value = null
        return
      }
      error.value = normalizeError(err)
      return
    }
    currentQuestion.value = data ?? null
  }

  async function submitAnswer(answerId: number): Promise<boolean> {
    if (!session.value || !currentQuestion.value) return false
    loading.value = true
    error.value = null
    const { data, error: err } = await api.POST(
      '/interviews/{interviewId}/answers',
      {
        params: { path: { interviewId: session.value.interviewId } },
        body: { questionId: currentQuestion.value.questionId, answerId },
      },
    )
    loading.value = false
    if (err || !data) {
      error.value = normalizeError(err)
      return false
    }
    lastFeedback.value = data
    correctCount.value = data.correctCount
    currentStreak.value = data.currentStreak
    totalPoints.value = data.totalPoints ?? totalPoints.value
    answeredCount.value = data.answeredCount
    finished.value = data.finished ?? false
    outcomes.value.push(data.correct)
    return true
  }

  async function loadStatistic(): Promise<void> {
    if (!session.value) return
    loading.value = true
    error.value = null
    const { data, error: err } = await api.GET(
      '/interviews/{interviewId}/statistic',
      { params: { path: { interviewId: session.value.interviewId } } },
    )
    loading.value = false
    if (err || !data) {
      error.value = normalizeError(err)
      return
    }
    statistic.value = data
  }

  async function makeOffer(personId: number): Promise<boolean> {
    if (!session.value) return false
    loading.value = true
    error.value = null
    const { data, error: err } = await api.POST(
      '/interviews/{interviewId}/offer',
      {
        params: { path: { interviewId: session.value.interviewId } },
        body: { personId },
      },
    )
    loading.value = false
    if (err || !data) {
      error.value = normalizeError(err)
      return false
    }
    offerResult.value = data
    return true
  }

  async function loadResult(): Promise<void> {
    if (!session.value) return
    loading.value = true
    error.value = null
    const { data, error: err } = await api.GET(
      '/interviews/{interviewId}/result',
      { params: { path: { interviewId: session.value.interviewId } } },
    )
    loading.value = false
    if (err || !data) {
      error.value = normalizeError(err)
      return
    }
    result.value = data
  }

  /**
   * Fetch the AI analysis, polling while the backend reports it is still being
   * generated (`status: PENDING`, served as HTTP 202). The real MVP backend
   * always returns READY synchronously, but the mock exercises the poll once.
   */
  async function loadAiResult(maxAttempts = 5): Promise<void> {
    if (!session.value) return
    loading.value = true
    error.value = null
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      const { data, error: err } = await api.GET(
        '/interviews/{interviewId}/ai-result',
        { params: { path: { interviewId: session.value.interviewId } } },
      )
      if (err || !data) {
        error.value = normalizeError(err)
        break
      }
      aiResult.value = data
      if (data.status !== 'PENDING') break
      await delay(800)
    }
    loading.value = false
  }

  return {
    session,
    currentQuestion,
    lastFeedback,
    correctCount,
    currentStreak,
    totalPoints,
    answeredCount,
    finished,
    outcomes,
    statistic,
    offerResult,
    result,
    aiResult,
    loading,
    error,
    totalQuestions,
    candidates,
    progress,
    hasSessionFor,
    reset,
    start,
    loadQuestion,
    submitAnswer,
    loadStatistic,
    makeOffer,
    loadResult,
    loadAiResult,
  }
})

/** Coerce an unknown client error into the contract `Error` shape for the UI. */
function normalizeError(err: unknown): Schemas['Error'] {
  if (err && typeof err === 'object' && 'code' in err && 'message' in err) {
    return err as Schemas['Error']
  }
  return { code: 'INTERNAL_ERROR', message: 'Something went wrong. Please try again.' }
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
