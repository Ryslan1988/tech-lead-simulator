<script setup lang="ts">
withDefaults(defineProps<{ padded?: boolean; interactive?: boolean }>(), {
  padded: true,
  interactive: false,
})
</script>

<template>
  <div :class="['card', { 'card--padded': padded, 'card--interactive': interactive }]">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.card--padded {
  padding: var(--space-6);
}
.card--interactive {
  cursor: pointer;
  transition:
    box-shadow 0.15s ease,
    border-color 0.15s ease,
    transform 0.1s ease;
}
.card--interactive:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
  transform: translateY(-2px);
}
</style>
