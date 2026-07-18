import { defineStore } from 'pinia'
import { ref } from 'vue'

/** Client-only UI state — currently just the global pause overlay. */
export const useUiStore = defineStore('ui', () => {
  const paused = ref(false)

  function open() {
    paused.value = true
  }
  function close() {
    paused.value = false
  }

  return { paused, open, close }
})
