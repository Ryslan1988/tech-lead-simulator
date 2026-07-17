/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL for API calls (e.g. `/api`). */
  readonly VITE_API_BASE: string
  /** `"true"` enables the MSW mock backend in dev. */
  readonly VITE_USE_MOCKS: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
