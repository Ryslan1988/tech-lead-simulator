<script setup lang="ts">
import CandidateAvatar from '@/components/CandidateAvatar.vue'
import type { Schemas } from '@/api/client'

withDefaults(
  defineProps<{
    candidate: Schemas['Candidate']
    /** Highlights the tile as the active speaker. */
    speaking?: boolean
    muted?: boolean
    state?: 'connecting' | 'live'
    /** Compact filmstrip sizing, used on the question screen. */
    dense?: boolean
  }>(),
  { speaking: false, muted: true, state: 'live', dense: false },
)
</script>

<template>
  <div
    :class="[
      'tile',
      { 'tile--speaking': speaking, 'tile--connecting': state === 'connecting', 'tile--dense': dense },
    ]"
  >
    <div class="tile__stage">
      <CandidateAvatar
        :name="candidate.name"
        :avatar-url="candidate.avatarUrl"
        :size="dense ? 56 : 96"
        decorative
      />
    </div>

    <span v-if="state === 'connecting'" class="tile__status">Подключается…</span>

    <div class="tile__bar">
      <span class="tile__ident">
        <span class="tile__name">{{ candidate.name }}</span>
        <span v-if="candidate.role" class="tile__role">{{ candidate.role }}</span>
      </span>
      <span
        v-if="state !== 'connecting'"
        class="tile__mic"
        role="img"
        :aria-label="muted ? 'Микрофон выключен' : 'Микрофон включён'"
      >
        {{ muted ? '🔇' : '🎤' }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.tile {
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: linear-gradient(160deg, #dfe6f5 0%, #eef1f7 100%);
  border: 2px solid transparent;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.tile--speaking {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}
.tile--dense {
  min-height: 100px;
}
.tile__stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
}
.tile--connecting .tile__stage {
  opacity: 0.6;
}
.tile--dense .tile__stage {
  padding: var(--space-2);
}
.tile__status {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);
  font-size: 12px;
  color: var(--color-text-muted);
  background-color: var(--color-surface);
  border-radius: var(--radius-pill);
  padding: 2px var(--space-2);
}
.tile__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background-color: rgba(255, 255, 255, 0.85);
}
.tile--dense .tile__bar {
  padding: var(--space-1) var(--space-2);
}
.tile--dense .tile__role {
  display: none;
}
.tile__ident {
  display: flex;
  flex-direction: column;
  text-align: left;
  min-width: 0;
}
.tile__name {
  font-weight: 700;
  font-size: var(--text-sm);
  color: var(--color-text);
}
.tile__role {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tile__mic {
  font-size: var(--text-sm);
  line-height: 1;
}
</style>
