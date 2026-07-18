import { computed, onUnmounted, ref } from 'vue'

/**
 * A 1-second-tick countdown for the question timer. `timeLimitSeconds` is a
 * client-side hint only (the server never rejects a late answer), so this drives
 * display and an optional `onExpire` callback — it does not gate submission.
 */
export function useCountdown() {
  const remaining = ref(0)
  const running = ref(false)
  let handle: ReturnType<typeof setInterval> | undefined
  let onExpire: (() => void) | undefined

  function stop() {
    if (handle !== undefined) {
      clearInterval(handle)
      handle = undefined
    }
    running.value = false
  }

  function start(seconds: number, expireCb?: () => void) {
    stop()
    remaining.value = seconds
    onExpire = expireCb
    running.value = true
    handle = setInterval(() => {
      remaining.value -= 1
      if (remaining.value <= 0) {
        remaining.value = 0
        stop()
        onExpire?.()
      }
    }, 1000)
  }

  const formatted = computed(() => {
    const total = Math.max(0, remaining.value)
    const mm = String(Math.floor(total / 60)).padStart(2, '0')
    const ss = String(total % 60).padStart(2, '0')
    return `${mm}:${ss}`
  })

  onUnmounted(stop)

  return { remaining, formatted, running, start, stop }
}
