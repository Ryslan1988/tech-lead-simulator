import { http, HttpResponse, type RequestHandler } from 'msw'

import type { Schemas } from '@/api/client'
import * as backend from './fixtures'

/**
 * One handler per operationId, backed by the in-memory engine in `fixtures.ts`.
 * Paths are prefixed with `/api` to match the client's `VITE_API_BASE`.
 */
const BASE = '/api'

function respond<T>(result: backend.MockResult<T>): Response {
  if ('error' in result) {
    return HttpResponse.json(result.error, { status: result.status })
  }
  return HttpResponse.json(result.data as Record<string, unknown>, {
    status: result.status,
  })
}

async function readJson<T>(request: Request): Promise<T | null> {
  try {
    return (await request.json()) as T
  } catch {
    return null
  }
}

const badRequest = HttpResponse.json(
  { code: 'BAD_REQUEST', message: 'Malformed request body.' },
  { status: 400 },
)

export const handlers: RequestHandler[] = [
  http.get(`${BASE}/home`, () => HttpResponse.json(backend.getHomePage())),

  http.post(`${BASE}/interviews`, async ({ request }) => {
    const body = await readJson<Schemas['StartInterviewRequest']>(request)
    if (!body) return badRequest
    return respond(backend.startInterview(body))
  }),

  http.get(`${BASE}/interviews/:interviewId/question`, ({ params }) =>
    respond(backend.getQuestion(Number(params.interviewId))),
  ),

  http.post(`${BASE}/interviews/:interviewId/answers`, async ({ params, request }) => {
    const body = await readJson<Schemas['AnswerRequest']>(request)
    if (!body) return badRequest
    return respond(backend.saveAnswer(Number(params.interviewId), body))
  }),

  http.get(`${BASE}/interviews/:interviewId/statistic`, ({ params }) =>
    respond(backend.getStatistic(Number(params.interviewId))),
  ),

  http.post(`${BASE}/interviews/:interviewId/offer`, async ({ params, request }) => {
    const body = await readJson<Schemas['OfferRequest']>(request)
    if (!body) return badRequest
    return respond(backend.offer(Number(params.interviewId), body))
  }),

  http.get(`${BASE}/interviews/:interviewId/result`, ({ params }) =>
    respond(backend.getResult(Number(params.interviewId))),
  ),

  http.get(`${BASE}/interviews/:interviewId/ai-result`, ({ params }) => {
    const result = backend.getAiResult(Number(params.interviewId))
    if ('error' in result) {
      return HttpResponse.json(result.error, { status: result.status })
    }
    // PENDING is served as HTTP 202 to exercise the client's poll path.
    const status = result.data.status === 'PENDING' ? 202 : 200
    return HttpResponse.json(result.data, { status })
  }),
]
