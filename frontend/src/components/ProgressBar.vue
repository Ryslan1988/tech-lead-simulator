<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    max?: number
    tone?: 'primary' | 'success'
  }>(),
  { max: 1, tone: 'primary' },
)

const percent = computed(() => {
  if (props.max <= 0) return 0
  return Math.max(0, Math.min(100, (props.value / props.max) * 100))
})
</script>

<template>
  <div
    class="bar"
    role="progressbar"
    :aria-valuenow="value"
    :aria-valuemin="0"
    :aria-valuemax="max"
  >
    <div class="bar__fill" :class="`bar__fill--${tone}`" :style="{ width: `${percent}%` }" />
  </div>
</template>

<style scoped>
.bar {
  width: 100%;
  height: 8px;
  border-radius: var(--radius-pill);
  background-color: var(--color-border);
  overflow: hidden;
}
.bar__fill {
  height: 100%;
  border-radius: var(--radius-pill);
  transition: width 0.3s ease;
}
.bar__fill--primary {
  background-color: var(--color-primary);
}
.bar__fill--success {
  background-color: var(--color-success);
}
</style>
