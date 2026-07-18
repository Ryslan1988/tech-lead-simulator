<script setup lang="ts">
import { RouterView } from 'vue-router'

import PauseModal from '@/components/PauseModal.vue'
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <component :is="Component" :key="route.fullPath" />
  </RouterView>
  <PauseModal />
</template>

<style scoped></style>
