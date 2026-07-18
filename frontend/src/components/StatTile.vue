<script setup lang="ts">
defineProps<{ label: string; value: string | number; icon?: string }>()
</script>

<template>
  <div class="tile">
    <span v-if="icon" class="tile__icon" aria-hidden="true">{{ icon }}</span>
    <div class="tile__body">
      <span class="tile__label">{{ label }}</span>
      <span class="tile__value">{{ value }}</span>
    </div>
  </div>
</template>

<style scoped>
.tile {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.tile__icon {
  font-size: 24px;
  line-height: 1;
}
.tile__body {
  display: flex;
  flex-direction: column;
}
.tile__label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
.tile__value {
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--color-text);
}
</style>
