<script setup lang="ts">
import CandidateAvatar from '@/components/CandidateAvatar.vue'
import type { Schemas } from '@/api/client'

withDefaults(
  defineProps<{ candidate: Schemas['Candidate']; avatarSize?: number }>(),
  { avatarSize: 64 },
)
</script>

<template>
  <div class="candidate">
    <CandidateAvatar
      :name="candidate.name"
      :avatar-url="candidate.avatarUrl"
      :size="avatarSize"
    />
    <span class="candidate__name">{{ candidate.name }}</span>
    <span v-if="candidate.role" class="candidate__role">{{ candidate.role }}</span>
    <slot />
  </div>
</template>

<style scoped>
.candidate {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-2);
}
.candidate__name {
  font-weight: 700;
  color: var(--color-text);
}
.candidate__role {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
</style>
