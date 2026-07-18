<script setup lang="ts">
withDefaults(defineProps<{ width?: 'narrow' | 'medium' | 'wide' }>(), {
  width: 'medium',
})
</script>

<template>
  <main class="screen">
    <div :class="['screen__inner', `screen__inner--${width}`]">
      <slot />
    </div>
  </main>
</template>

<style scoped>
.screen {
  min-height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--space-8) var(--space-4);
}
.screen__inner {
  width: 100%;
}
.screen__inner--narrow {
  max-width: 440px;
}
.screen__inner--medium {
  max-width: 720px;
}
.screen__inner--wide {
  max-width: var(--container-max);
}
</style>
