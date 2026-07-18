import { createApp } from 'vue'
import { createPinia } from 'pinia'

import './assets/styles/tokens.css'
import './assets/styles/base.css'

import App from './App.vue'
import router from './router'

/**
 * Start the MSW mock backend before mounting, but only when explicitly enabled.
 * `VITE_USE_MOCKS` is a string env var, so compare against `'true'` — otherwise
 * the string `'false'` would be truthy and mocks would always start.
 */
async function enableMocking(): Promise<void> {
  if (import.meta.env.VITE_USE_MOCKS !== 'true') return
  const { worker } = await import('./mocks/browser')
  await worker.start({ onUnhandledRequest: 'bypass' })
}

const app = createApp(App)
app.use(createPinia())
app.use(router)

enableMocking().then(() => app.mount('#app'))
