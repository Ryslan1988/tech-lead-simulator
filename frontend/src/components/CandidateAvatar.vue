<script setup lang="ts">
import { computed, ref } from 'vue'

import AvatarArt from '@/components/AvatarArt.vue'
import { paletteFor } from '@/components/avatarPalettes'

const props = withDefaults(
  defineProps<{ name: string; avatarUrl?: string; size?: number; decorative?: boolean }>(),
  { size: 56, decorative: false },
)

// Resolution order: drawn portrait -> remote image -> coloured initials disc.
// The seed `avatarUrl`s (/assets/candidates/*.png) point at files that do not
// exist, so without the portrait every avatar would fall through to initials.
const imageFailed = ref(false)

const palette = computed(() => paletteFor(props.avatarUrl))
const showImage = computed(() => !palette.value && !!props.avatarUrl && !imageFailed.value)

const initials = computed(() =>
  props.name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join(''),
)

const PALETTE = ['#2f6bff', '#22a06b', '#f4b740', '#8b5cf6', '#ef6461', '#0ea5e9']
const bgColor = computed(() => {
  let hash = 0
  for (let i = 0; i < props.name.length; i++) {
    hash = (hash * 31 + props.name.charCodeAt(i)) % 997
  }
  return PALETTE[hash % PALETTE.length]
})

const dims = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.38)}px`,
}))
</script>

<template>
  <span v-if="palette" class="avatar avatar--art" :style="dims">
    <AvatarArt :palette="palette" :title="name" :decorative="decorative" />
  </span>
  <img
    v-else-if="showImage"
    class="avatar avatar--img"
    :src="avatarUrl"
    :alt="decorative ? '' : name"
    :aria-hidden="decorative ? 'true' : undefined"
    :style="dims"
    @error="imageFailed = true"
  />
  <span
    v-else
    class="avatar avatar--initials"
    :style="{ ...dims, backgroundColor: bgColor }"
    :aria-label="decorative ? undefined : name"
    :aria-hidden="decorative ? 'true' : undefined"
    :role="decorative ? undefined : 'img'"
  >
    {{ initials }}
  </span>
</template>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--color-surface);
  box-shadow: var(--shadow-sm);
}
.avatar--art {
  overflow: hidden;
}
.avatar--initials {
  color: var(--color-text-inverse);
  font-weight: 700;
  user-select: none;
}
</style>
