import { setupWorker } from 'msw/browser'

import { handlers } from './handlers'

/** The browser mock worker — started from `main.ts` when `VITE_USE_MOCKS=true`. */
export const worker = setupWorker(...handlers)
