<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(
  defineProps<{ name: string; avatarUrl?: string; size?: number }>(),
  { size: 56 },
)

// Seed avatar images (/assets/candidates/*.png) may not exist; fall back to a
// coloured initials disc so avatars never render as a broken image.
const imageFailed = ref(false)

const showImage = computed(() => !!props.avatarUrl && !imageFailed.value)

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
  <img
    v-if="showImage"
    class="avatar avatar--img"
    :src="avatarUrl"
    :alt="name"
    :style="dims"
    @error="imageFailed = true"
  />
  <span
    v-else
    class="avatar avatar--initials"
    :style="{ ...dims, backgroundColor: bgColor }"
    :aria-label="name"
    role="img"
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
.avatar--initials {
  color: var(--color-text-inverse);
  font-weight: 700;
  user-select: none;
}
</style>
