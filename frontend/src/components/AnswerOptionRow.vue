<script setup lang="ts">
import { computed } from 'vue'

import CandidateAvatar from '@/components/CandidateAvatar.vue'
import type { Schemas } from '@/api/client'

const props = defineProps<{
  text: string
  candidate?: Schemas['Candidate']
  /** Feedback has been revealed for this round. */
  revealed: boolean
  /** This option is the objectively correct one. */
  correct: boolean
  /** The player picked this option. */
  chosen: boolean
  disabled: boolean
}>()

const emit = defineEmits<{ select: [] }>()

const state = computed(() => {
  if (!props.revealed) return props.chosen ? 'active' : 'idle'
  if (props.correct) return 'correct'
  if (props.chosen) return 'incorrect'
  return 'muted'
})

function onClick() {
  if (props.disabled) return
  emit('select')
}
</script>

<template>
  <button
    type="button"
    :class="['answer', `answer--${state}`]"
    :disabled="disabled"
    @click="onClick"
  >
    <CandidateAvatar
      v-if="candidate"
      class="answer__avatar"
      :name="candidate.name"
      :avatar-url="candidate.avatarUrl"
      :size="40"
    />
    <span class="answer__text">{{ text }}</span>
    <span v-if="state === 'correct'" class="answer__mark answer__mark--ok">✓</span>
    <span v-else-if="state === 'incorrect'" class="answer__mark answer__mark--no">✗</span>
  </button>
</template>

<style scoped>
.answer {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  text-align: left;
  padding: var(--space-3) var(--space-4);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}
.answer:disabled {
  cursor: default;
}
.answer--idle:not(:disabled):hover {
  border-color: var(--color-primary);
  background-color: #f5f8ff;
}
.answer--active {
  border-color: var(--color-primary);
  background-color: #eef3ff;
}
.answer--correct {
  border-color: var(--color-success);
  background-color: var(--color-success-soft);
}
.answer--incorrect {
  border-color: var(--color-error);
  background-color: var(--color-error-soft);
}
.answer--muted {
  opacity: 0.55;
}
.answer__text {
  flex: 1;
  color: var(--color-text);
}
.answer__mark {
  font-weight: 700;
  font-size: var(--text-lg);
}
.answer__mark--ok {
  color: var(--color-success);
}
.answer__mark--no {
  color: var(--color-error);
}
</style>
