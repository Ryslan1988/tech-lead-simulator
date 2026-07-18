import type { Schemas } from '@/api/client'

/**
 * In-memory mock backend. Keeps a per-session game state so the mocked API
 * plays a real, consistent 10/20-round interview: it tracks the answered index,
 * knows which candidate is objectively correct per question, accumulates
 * score/streak, and derives the statistic / result / ai-result payloads from
 * the same source of truth. Content is in Russian to match the design sheet.
 */

type Mode = Schemas['Mode']
type Candidate = Schemas['Candidate']

// --- Candidates (design screen 4/15) --------------------------------------
// IDs are stable; competence differs (see QUESTION_BANK correctCandidateId).
export const CANDIDATES: Candidate[] = [
  {
    id: 1,
    name: 'Мария',
    role: 'Frontend Developer',
    avatarUrl: '/assets/candidates/maria.png',
    strengths: ['UI/UX', 'Доступность', 'Тестирование'],
  },
  {
    id: 2,
    name: 'Алексей',
    role: 'Backend Developer',
    avatarUrl: '/assets/candidates/alexey.png',
    strengths: ['Алгоритмы', 'Базы данных', 'Оптимизация'],
  },
  {
    id: 3,
    name: 'Игорь',
    role: 'DevOps Engineer',
    avatarUrl: '/assets/candidates/igor.png',
    strengths: ['CI/CD', 'Kubernetes', 'Мониторинг'],
  },
  {
    id: 4,
    name: 'Дмитрий',
    role: 'QA Engineer',
    avatarUrl: '/assets/candidates/dmitriy.png',
    strengths: ['Автотесты', 'Нагрузка', 'Регресс'],
  },
]

interface BankQuestion {
  questionId: number
  text: string
  /** Candidate id → that candidate's answer text. */
  answers: Record<number, string>
  /** Whose answer is objectively correct. */
  correctCandidateId: number
}

/**
 * 20 questions (CLASSIC uses the first 10, HARDCORE all 20). Alexey (backend)
 * is the strongest candidate by design, so hiring him reads as a good hire.
 */
export const QUESTION_BANK: BankQuestion[] = [
  {
    questionId: 1,
    text: 'Как бы вы оптимизировали медленный SQL-запрос?',
    answers: {
      1: 'Перепишу его на клиенте.',
      2: 'Добавлю индексы на поля из условия и посмотрю план запроса.',
      3: 'Подниму ещё одну реплику базы.',
      4: 'Прогоню нагрузочный тест.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 2,
    text: 'Что такое SOLID?',
    answers: {
      1: 'Пять принципов объектно-ориентированного проектирования.',
      2: 'Название паттерна проектирования.',
      3: 'Инструмент для CI/CD.',
      4: 'Методика ручного тестирования.',
    },
    correctCandidateId: 1,
  },
  {
    questionId: 3,
    text: 'Как обеспечить нулевое время простоя при деплое?',
    answers: {
      1: 'Деплоить только ночью.',
      2: 'Кэшировать все ответы.',
      3: 'Blue-green или rolling-деплой за балансировщиком.',
      4: 'Отключать пользователей на время релиза.',
    },
    correctCandidateId: 3,
  },
  {
    questionId: 4,
    text: 'REST или gRPC для внутренней коммуникации сервисов?',
    answers: {
      1: 'Всегда только REST — он проще.',
      2: 'gRPC: контракт, строгая типизация и меньше накладных расходов.',
      3: 'GraphQL вместо обоих.',
      4: 'Обмен файлами по расписанию.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 5,
    text: 'Как работает сборщик мусора в Java?',
    answers: {
      1: 'Удаляет файлы из временной папки.',
      2: 'Освобождает память недостижимых объектов по поколениям.',
      3: 'Чистит кэш браузера.',
      4: 'Запускается только вручную.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 6,
    text: 'Как выявить причину деградации сервиса в проде?',
    answers: {
      1: 'Перезапустить сервер и забыть.',
      2: 'Спросить у пользователей.',
      3: 'Смотреть метрики, логи и трейсы; искать корреляции.',
      4: 'Откатить последний фронтенд.',
    },
    correctCandidateId: 3,
  },
  {
    questionId: 7,
    text: 'Что такое индексация в базе данных?',
    answers: {
      1: 'Структура, ускоряющая поиск строк ценой записи.',
      2: 'Способ сжатия таблиц.',
      3: 'Резервное копирование.',
      4: 'Нумерация строк для тестов.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 8,
    text: 'Как спроектировать доступный (a11y) интерфейс?',
    answers: {
      1: 'Семантический HTML, ARIA и навигация с клавиатуры.',
      2: 'Добавить побольше анимаций.',
      3: 'Увеличить размер сервера.',
      4: 'Писать только автотесты.',
    },
    correctCandidateId: 1,
  },
  {
    questionId: 9,
    text: 'Какой подход к тестированию регрессий вы выберете?',
    answers: {
      1: 'Тестировать только вручную перед релизом.',
      2: 'Совсем не тестировать — экономит время.',
      3: 'Полагаться на пользователей.',
      4: 'Автотесты в CI на каждый пул-реквест.',
    },
    correctCandidateId: 4,
  },
  {
    questionId: 10,
    text: 'Как безопасно хранить пароли пользователей?',
    answers: {
      1: 'В открытом виде в базе.',
      2: 'Хешировать с солью (bcrypt/argon2).',
      3: 'Шифровать симметрично общим ключом.',
      4: 'В логах приложения.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 11,
    text: 'Что делать при внезапном скачке нагрузки?',
    answers: {
      1: 'Автоскейлинг и ограничение частоты запросов.',
      2: 'Просто ждать, пока пройдёт.',
      3: 'Выключить мониторинг, чтобы не пугал.',
      4: 'Удалить часть данных.',
    },
    correctCandidateId: 3,
  },
  {
    questionId: 12,
    text: 'Как уменьшить размер JS-бандла фронтенда?',
    answers: {
      1: 'Code-splitting, tree-shaking и ленивая загрузка.',
      2: 'Добавить больше зависимостей.',
      3: 'Перенести всё на сервер.',
      4: 'Отключить кэширование.',
    },
    correctCandidateId: 1,
  },
  {
    questionId: 13,
    text: 'Зачем нужны нагрузочные тесты?',
    answers: {
      1: 'Чтобы проверить орфографию.',
      2: 'Чтобы найти пределы системы до того, как их найдут пользователи.',
      3: 'Чтобы ускорить сборку.',
      4: 'Они не нужны в проде.',
    },
    correctCandidateId: 4,
  },
  {
    questionId: 14,
    text: 'Что такое идемпотентность HTTP-метода?',
    answers: {
      1: 'Метод, который всегда возвращает 200.',
      2: 'Повторный вызов даёт тот же результат без побочных эффектов.',
      3: 'Метод, работающий только один раз.',
      4: 'Синоним кэширования.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 15,
    text: 'Как организовать надёжный CI/CD-пайплайн?',
    answers: {
      1: 'Собирать вручную по пятницам.',
      2: 'Один общий скрипт без тестов.',
      3: 'Этапы build → test → deploy с автопроверками и откатом.',
      4: 'Деплоить прямо из IDE разработчика.',
    },
    correctCandidateId: 3,
  },
  {
    questionId: 16,
    text: 'Как предотвратить утечку состояния между тестами?',
    answers: {
      1: 'Запускать тесты в случайном порядке и надеяться.',
      2: 'Изолировать данные и сбрасывать состояние перед каждым тестом.',
      3: 'Использовать одну общую базу для всех тестов.',
      4: 'Тестировать только в проде.',
    },
    correctCandidateId: 4,
  },
  {
    questionId: 17,
    text: 'Что такое N+1 проблема запросов?',
    answers: {
      1: 'Ошибка компиляции.',
      2: 'Лишний запрос на каждую запись вместо одного пакетного.',
      3: 'Проблема вёрстки.',
      4: 'Сбой сети.',
    },
    correctCandidateId: 2,
  },
  {
    questionId: 18,
    text: 'Как обеспечить наблюдаемость (observability) системы?',
    answers: {
      1: 'Логи, метрики и распределённые трейсы вместе.',
      2: 'Достаточно одних логов.',
      3: 'Спрашивать разработчиков напрямую.',
      4: 'Скриншоты от пользователей.',
    },
    correctCandidateId: 3,
  },
  {
    questionId: 19,
    text: 'Как управлять состоянием в крупном SPA?',
    answers: {
      1: 'Хранить всё в глобальных переменных window.',
      2: 'Предсказуемый store (Pinia/Redux) с чёткими действиями.',
      3: 'Держать состояние только в DOM.',
      4: 'Перезагружать страницу на каждое изменение.',
    },
    correctCandidateId: 1,
  },
  {
    questionId: 20,
    text: 'Что важно проверить в code review?',
    answers: {
      1: 'Только форматирование.',
      2: 'Корректность, тестируемость, безопасность и читаемость.',
      3: 'Ничего, лишь бы собиралось.',
      4: 'Количество строк.',
    },
    correctCandidateId: 2,
  },
]

// --- Session state --------------------------------------------------------

interface SessionState {
  interviewId: number
  mode: Mode
  totalQuestions: number
  answeredIndex: number
  correctCount: number
  currentStreak: number
  bestStreak: number
  totalPoints: number
  timesChosen: Record<number, number>
  outcomes: Schemas['QuestionOutcome'][]
  offeredCandidateId: number | null
  aiPolls: number
}

const sessions = new Map<number, SessionState>()
let nextInterviewId = 1001

const TIME_LIMIT_SECONDS = 45

function answerId(questionId: number, candidateId: number): number {
  return questionId * 100 + candidateId
}

/** How many rounds each candidate is objectively correct on, for this run. */
function correctAnswersFor(candidateId: number, totalQuestions: number): number {
  return QUESTION_BANK.slice(0, totalQuestions).filter(
    (q) => q.correctCandidateId === candidateId,
  ).length
}

// --- Result helpers -------------------------------------------------------

export type MockResult<T> =
  | { status: 200 | 201; data: T }
  | { status: number; error: Schemas['Error'] }

function err(status: number, code: string, message: string): { status: number; error: Schemas['Error'] } {
  return { status, error: { code, message, timestamp: '2026-07-18T00:00:00Z' } }
}

function notFound(id: number) {
  return err(404, 'INTERVIEW_NOT_FOUND', `No interview session with id ${id}.`)
}

// --- Endpoint implementations ---------------------------------------------

export function getHomePage(): Schemas['HomePage'] {
  const modes: Schemas['GameMode'][] = [
    {
      mode: 'HARDCORE',
      title: 'Карьерный режим',
      description: 'Полный путь тех-лида. От собеседований до построения команды.',
      questionCount: 20,
    },
    {
      mode: 'CLASSIC',
      title: 'Быстрая игра',
      description: 'Одно собеседование на время. Проверьте свои навыки.',
      questionCount: 10,
    },
  ]
  const playerStats: Schemas['PlayerStats'] = {
    gamesPlayed: 12,
    winRate: 0.75,
    bestResult: 8,
    candidatesHired: 7,
  }
  return {
    title: 'Tech Lead Simulator',
    subtitle: 'Пройдите собеседование и соберите лучшую команду!',
    modes,
    playerStats,
  }
}

export function startInterview(
  body: Schemas['StartInterviewRequest'],
): MockResult<Schemas['InterviewSession']> {
  const mode = body?.mode
  if (mode !== 'CLASSIC' && mode !== 'HARDCORE') {
    return err(400, 'BAD_REQUEST', `Invalid mode: ${String(mode)}.`)
  }
  const totalQuestions = mode === 'HARDCORE' ? 20 : 10
  const interviewId = nextInterviewId++
  sessions.set(interviewId, {
    interviewId,
    mode,
    totalQuestions,
    answeredIndex: 0,
    correctCount: 0,
    currentStreak: 0,
    bestStreak: 0,
    totalPoints: 0,
    timesChosen: {},
    outcomes: [],
    offeredCandidateId: null,
    aiPolls: 0,
  })
  return {
    status: 201,
    data: {
      interviewId,
      mode,
      difficulty: body.difficulty ?? 'MEDIUM',
      totalQuestions,
      candidates: CANDIDATES,
    },
  }
}

export function getQuestion(id: number): MockResult<Schemas['Question']> {
  const s = sessions.get(id)
  if (!s) return notFound(id)
  if (s.answeredIndex >= s.totalQuestions) {
    return err(409, 'NO_QUESTION_AVAILABLE', 'All rounds have been answered.')
  }
  const q = QUESTION_BANK[s.answeredIndex]
  if (!q) return err(500, 'INTERNAL_ERROR', 'Question bank exhausted.')
  return {
    status: 200,
    data: {
      questionId: q.questionId,
      index: s.answeredIndex + 1,
      total: s.totalQuestions,
      text: q.text,
      timeLimitSeconds: TIME_LIMIT_SECONDS,
      answers: CANDIDATES.map((c) => ({
        answerId: answerId(q.questionId, c.id),
        candidateId: c.id,
        text: q.answers[c.id] ?? '',
      })),
    },
  }
}

export function saveAnswer(
  id: number,
  body: Schemas['AnswerRequest'],
): MockResult<Schemas['AnswerResult']> {
  const s = sessions.get(id)
  if (!s) return notFound(id)

  if (s.answeredIndex >= s.totalQuestions) {
    return err(409, 'QUESTION_ALREADY_ANSWERED', 'All rounds already answered.')
  }
  const current = QUESTION_BANK[s.answeredIndex]
  if (!current) return err(500, 'INTERNAL_ERROR', 'Question bank exhausted.')
  if (!body || typeof body.questionId !== 'number' || typeof body.answerId !== 'number') {
    return err(400, 'BAD_REQUEST', 'questionId and answerId are required.')
  }
  const bankQuestion = QUESTION_BANK.find((q) => q.questionId === body.questionId)
  if (!bankQuestion) {
    return err(400, 'BAD_REQUEST', `Unknown questionId ${body.questionId}.`)
  }
  if (body.questionId !== current.questionId) {
    return err(409, 'QUESTION_ALREADY_ANSWERED', 'That question was already answered.')
  }
  const validAnswerIds = CANDIDATES.map((c) => answerId(current.questionId, c.id))
  if (!validAnswerIds.includes(body.answerId)) {
    return err(400, 'BAD_REQUEST', 'answerId does not belong to that question.')
  }

  const chosenCandidateId = body.answerId % 100
  const correctAnswerId = answerId(current.questionId, current.correctCandidateId)
  const correct = body.answerId === correctAnswerId

  s.timesChosen[chosenCandidateId] = (s.timesChosen[chosenCandidateId] ?? 0) + 1
  if (correct) {
    s.currentStreak += 1
    s.bestStreak = Math.max(s.bestStreak, s.currentStreak)
    s.correctCount += 1
    s.totalPoints += 10 + 2 * (s.currentStreak - 1)
  } else {
    s.currentStreak = 0
  }
  s.outcomes.push({ questionId: current.questionId, text: current.text, correct })
  s.answeredIndex += 1
  const finished = s.answeredIndex >= s.totalQuestions

  return {
    status: 200,
    data: {
      correct,
      correctAnswerId,
      pointsAwarded: correct ? 10 + 2 * (s.currentStreak - 1) : 0,
      correctCount: s.correctCount,
      currentStreak: s.currentStreak,
      totalPoints: s.totalPoints,
      answeredCount: s.answeredIndex,
      totalQuestions: s.totalQuestions,
      finished,
    },
  }
}

export function getStatistic(id: number): MockResult<Schemas['InterviewStatistic']> {
  const s = sessions.get(id)
  if (!s) return notFound(id)
  return {
    status: 200,
    data: {
      totalQuestions: s.totalQuestions,
      correctCount: s.correctCount,
      perCandidate: CANDIDATES.map((c) => ({
        candidateId: c.id,
        name: c.name,
        role: c.role,
        timesChosen: s.timesChosen[c.id] ?? 0,
        correctAnswers: correctAnswersFor(c.id, s.totalQuestions),
      })),
    },
  }
}

export function offer(
  id: number,
  body: Schemas['OfferRequest'],
): MockResult<Schemas['OfferResult']> {
  const s = sessions.get(id)
  if (!s) return notFound(id)
  const candidate = CANDIDATES.find((c) => c.id === body?.personId)
  if (!candidate) {
    return err(400, 'BAD_REQUEST', `Unknown candidate ${String(body?.personId)}.`)
  }
  s.offeredCandidateId = candidate.id
  return {
    status: 200,
    data: {
      interviewId: id,
      hiredCandidate: candidate,
      message: `${candidate.name} присоединяется к вашей команде!`,
    },
  }
}

export function getResult(id: number): MockResult<Schemas['InterviewResult']> {
  const s = sessions.get(id)
  if (!s) return notFound(id)
  return {
    status: 200,
    data: {
      interviewId: id,
      correctCount: s.correctCount,
      totalQuestions: s.totalQuestions,
      totalPoints: s.totalPoints,
      bestStreak: s.bestStreak,
      breakdown: s.outcomes,
    },
  }
}

/**
 * Exercises the FE's 202-poll path once: the first call reports PENDING
 * (served as HTTP 202), every call after that returns the READY analysis.
 */
export function getAiResult(id: number): MockResult<Schemas['AiInterviewResult']> {
  const s = sessions.get(id)
  if (!s) return notFound(id)
  s.aiPolls += 1
  if (s.aiPolls === 1) {
    return { status: 200, data: { interviewId: id, status: 'PENDING' } }
  }

  const hired = s.offeredCandidateId
  const strongest = [...CANDIDATES].sort(
    (a, b) =>
      correctAnswersFor(b.id, s.totalQuestions) - correctAnswersFor(a.id, s.totalQuestions),
  )[0]
  if (!strongest) return err(500, 'INTERNAL_ERROR', 'No candidates.')
  const goodHire = hired === strongest.id
  const hiredCandidate = CANDIDATES.find((c) => c.id === hired)

  const missed = s.outcomes.filter((o) => !o.correct)
  const roadmap: Schemas['RoadmapItem'][] = [
    {
      topic: 'Оптимизация запросов и индексация',
      reason:
        missed.length > 0
          ? `Вы ошиблись в ${missed.length} вопрос(ах) — стоит закрепить основы.`
          : 'Отличный результат — углубите знания для роста.',
      priority: missed.length > 3 ? 'HIGH' : 'MEDIUM',
      resources: [
        { title: 'Use The Index, Luke!', url: 'https://use-the-index-luke.com/' },
      ],
    },
    {
      topic: 'Архитектурные компромиссы (REST vs gRPC)',
      reason: 'Помогает принимать взвешенные решения на позиции тех-лида.',
      priority: 'MEDIUM',
      resources: [{ title: 'gRPC docs', url: 'https://grpc.io/docs/' }],
    },
    {
      topic: 'Наблюдаемость и надёжность в проде',
      reason: 'Логи, метрики и трейсы — основа работы дежурной команды.',
      priority: 'LOW',
      resources: [
        { title: 'Google SRE Book', url: 'https://sre.google/books/' },
      ],
    },
  ]

  return {
    status: 200,
    data: {
      interviewId: id,
      status: 'READY',
      summary: `Вы ответили верно на ${s.correctCount} из ${s.totalQuestions} вопросов и набрали ${s.totalPoints} очков.`,
      verdict:
        hired == null
          ? undefined
          : goodHire
            ? `Удачный найм — ${hiredCandidate?.name} сильнее всех отвечал по существу.`
            : `Спорный найм — ${hiredCandidate?.name} отвечал верно реже, чем ${strongest.name}.`,
      hiredCandidateId: hired ?? undefined,
      roadmap,
    },
  }
}
