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
  }>(),
  { speakingId: null, state: 'live' },
)

defineEmits<{ hangup: [] }>()
</script>

<template>
  <section class="call">
    <div class="call__grid">
      <VideoTile
        v-for="c in candidates"
        :key="c.id"
        :candidate="c"
        :state="state"
        :speaking="state === 'live' && c.id === speakingId"
        :muted="c.id !== speakingId"
      />
    </div>
    <CallControlBar @hangup="$emit('hangup')" />
  </section>
</template>

<style scoped>
.call {
  background-color: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-4);
}
.call__grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

@media (max-width: 720px) {
  .call__grid {
    grid-template-columns: 1fr;
  }
}
</style>
