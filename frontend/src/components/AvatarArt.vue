<script setup lang="ts">
import type { AvatarPalette } from '@/components/avatarPalettes'

withDefaults(
  defineProps<{ palette: AvatarPalette; title: string; decorative?: boolean }>(),
  { decorative: false },
)
</script>

<template>
  <!-- Flat portrait matching design/third-variant.jpg: everyone wears glasses,
       identity comes from the palette. -->
  <svg
    class="art"
    viewBox="0 0 100 100"
    :role="decorative ? undefined : 'img'"
    :aria-label="decorative ? undefined : title"
    :aria-hidden="decorative ? 'true' : undefined"
  >
    <rect width="100" height="100" :fill="palette.bg" />

    <!-- Long hair sits behind the shoulders, so it is drawn first. -->
    <path
      v-if="palette.longHair"
      d="M20 44 C20 70 28 80 32 88 L68 88 C72 80 80 70 80 44 Z"
      :fill="palette.hair"
    />

    <!-- Shoulders -->
    <path d="M12 100 C12 80 30 72 50 72 C70 72 88 80 88 100 Z" :fill="palette.shirt" />
    <!-- Neck -->
    <rect x="43" y="58" width="14" height="18" rx="7" :fill="palette.skin" />
    <!-- Head -->
    <ellipse cx="50" cy="44" rx="20" ry="23" :fill="palette.skin" />
    <!-- Hair on top -->
    <path
      d="M28 44 C28 24 44 17 50 17 C56 17 72 24 72 44 C72 35 64 31 50 31 C36 31 28 35 28 44 Z"
      :fill="palette.hair"
    />

    <!-- Eyes (behind the lenses so the glasses strokes still read on top) -->
    <circle cx="42" cy="45" r="2.2" fill="#2b3444" />
    <circle cx="58" cy="45" r="2.2" fill="#2b3444" />

    <!-- Glasses -->
    <g fill="none" stroke="#2b3444" stroke-width="2">
      <circle cx="42" cy="45" r="7" />
      <circle cx="58" cy="45" r="7" />
      <path d="M49 45 H51" />
    </g>

    <!-- Mouth -->
    <path
      d="M45 56 Q50 60 55 56"
      stroke="#b4756a"
      stroke-width="2"
      fill="none"
      stroke-linecap="round"
    />
  </svg>
</template>

<style scoped>
.art {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
