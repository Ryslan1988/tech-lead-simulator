<script setup lang="ts">
import type { AvatarPalette } from '@/components/avatarPalettes'

defineProps<{ palette: AvatarPalette; title: string }>()
</script>

<template>
  <!-- Flat portrait matching design/third-variant.jpg: everyone wears glasses,
       identity comes from the palette. -->
  <svg class="art" viewBox="0 0 100 100" role="img" :aria-label="title">
    <rect width="100" height="100" :fill="palette.bg" />

    <!-- Long hair sits behind the shoulders, so it is drawn first. -->
    <path
      v-if="palette.longHair"
      d="M26 44 C26 70 30 78 32 88 L68 88 C70 78 74 70 74 44 Z"
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
