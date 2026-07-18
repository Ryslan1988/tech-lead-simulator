<script setup lang="ts">
import { computed } from 'vue'

import type { Schemas } from '@/api/client'

const props = defineProps<{ item: Schemas['RoadmapItem'] }>()

const priorityLabel: Record<string, string> = {
  HIGH: 'Высокий',
  MEDIUM: 'Средний',
  LOW: 'Низкий',
}

const priority = computed(() => props.item.priority ?? 'MEDIUM')
</script>

<template>
  <div class="roadmap">
    <div class="roadmap__head">
      <h3 class="roadmap__topic">{{ item.topic }}</h3>
      <span :class="['badge', `badge--${priority.toLowerCase()}`]">
        {{ priorityLabel[priority] ?? priority }}
      </span>
    </div>
    <p v-if="item.reason" class="roadmap__reason">{{ item.reason }}</p>
    <ul v-if="item.resources?.length" class="roadmap__resources">
      <li v-for="(r, i) in item.resources" :key="i">
        <a v-if="r.url" :href="r.url" target="_blank" rel="noopener noreferrer">
          {{ r.title }} ↗
        </a>
        <span v-else>{{ r.title }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.roadmap {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
  text-align: left;
}
.roadmap__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
.roadmap__topic {
  font-size: var(--text-base);
  font-weight: 700;
}
.roadmap__reason {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  margin-top: var(--space-2);
}
.roadmap__resources {
  list-style: none;
  margin-top: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.roadmap__resources a {
  color: var(--color-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}
.roadmap__resources a:hover {
  text-decoration: underline;
}
.badge {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-pill);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.badge--high {
  background-color: var(--color-error-soft);
  color: var(--color-error);
}
.badge--medium {
  background-color: #fef3c7;
  color: #b45309;
}
.badge--low {
  background-color: var(--color-border);
  color: var(--color-text-muted);
}
</style>
