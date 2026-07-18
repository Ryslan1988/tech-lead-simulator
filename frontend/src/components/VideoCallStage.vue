<script setup lang="ts">
import CallControlBar from '@/components/CallControlBar.vue'
import VideoTile from '@/components/VideoTile.vue'
import type { Schemas } from '@/api/client'

withDefaults(
  defineProps<{
    candidates: Schemas['Candidate'][]
    /** Candidate currently highlighted as the speaker. */
    speakingId?: number | null
    state?: 'connecting' | 'live'
    /** Compact single-row filmstrip layout, used on the question screen. */
    dense?: boolean
  }>(),
  { speakingId: null, state: 'live', dense: false },
)

defineEmits<{ hangup: [] }>()
</script>

<template>
  <section class="call" aria-label="Видеозвонок с кандидатами">
    <div :class="['call__grid', { 'call__grid--dense': dense }]">
      <VideoTile
        v-for="c in candidates"
        :key="c.id"
        :candidate="c"
        :state="state"
        :speaking="state === 'live' && c.id === speakingId"
        :muted="c.id !== speakingId"
        :dense="dense"
      />
    </div>
    <CallControlBar @hangup="$emit('hangup')" />
  </section>
</template>

<style scoped>
.call {
  background-color: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-4);
}
.call__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}
.call__grid--dense {
  grid-template-columns: repeat(4, 1fr);
}

@media (max-width: 720px) {
  .call__grid {
    grid-template-columns: 1fr;
  }
  .call__grid--dense {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
