<script setup lang="ts">
defineEmits<{ hangup: [] }>()

// Decorative only — there is no real telephony behind these. They exist to make
// the interview read as a video call (design screen 5). Kept out of the a11y
// tree and tab order so nobody is offered an action that does nothing.
const DECORATIVE = ['🎤', '📹', '🖥️', '⏺️', '💬', '👥', '⚙️'] as const
</script>

<template>
  <div class="bar">
    <span
      v-for="icon in DECORATIVE"
      :key="icon"
      class="bar__btn"
      aria-hidden="true"
    >
      {{ icon }}
    </span>
    <button type="button" class="bar__btn bar__btn--end" @click="$emit('hangup')">
      Завершить
    </button>
  </div>
</template>

<style scoped>
.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3);
}
.bar__btn {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background-color: var(--color-surface);
  font-size: var(--text-sm);
  line-height: 1;
}
.bar__btn--end {
  width: auto;
  padding: 0 var(--space-4);
  background-color: var(--color-error);
  border-color: var(--color-error);
  color: var(--color-text-inverse);
  font-weight: 600;
}
.bar__btn--end:hover {
  filter: brightness(0.94);
}
</style>
