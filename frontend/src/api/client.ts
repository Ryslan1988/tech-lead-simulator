import createClient from 'openapi-fetch'
import type { paths, components } from './schema'

/** Convenience alias for the generated request/response schemas. */
export type Schemas = components['schemas']

/**
 * Typed API client generated from `openapi.yaml`.
 *
 * Every call returns `{ data, error }`: `data` is present on a 2xx response,
 * `error` (matching the OpenAPI `Error` schema) on 4xx/5xx. `baseUrl` points at
 * `/api`, which MSW intercepts in dev and the Vite proxy forwards to the real
 * backend when `VITE_USE_MOCKS=false`.
 */
export const api = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE,
})
