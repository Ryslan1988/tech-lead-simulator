import { defineStore } from 'pinia'
import { ref } from 'vue'

import { api, type Schemas } from '@/api/client'

/** Landing-screen data: title, available game modes and the player's stats. */
export const useHomeStore = defineStore('home', () => {
  const homePage = ref<Schemas['HomePage'] | null>(null)
  const loading = ref(false)
  const error = ref<Schemas['Error'] | null>(null)

  async function loadHome(): Promise<void> {
    loading.value = true
    error.value = null
    const { data, error: err } = await api.GET('/home')
    loading.value = false
    if (err || !data) {
      error.value =
        err && typeof err === 'object' && 'code' in err
          ? (err as Schemas['Error'])
          : { code: 'INTERNAL_ERROR', message: 'Failed to load the home screen.' }
      return
    }
    homePage.value = data
  }

  return { homePage, loading, error, loadHome }
})
