<script setup lang="ts">
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import { useInterviewStore } from '@/stores/interview'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const interview = useInterviewStore()
const router = useRouter()

async function restart() {
  const mode = interview.session?.mode
  ui.close()
  if (!mode) {
    router.push('/')
    return
  }
  const interviewId = await interview.start({ mode, difficulty: 'MEDIUM' })
  if (interviewId != null) {
    router.push({ name: 'question', params: { id: String(interviewId) } })
  } else {
    router.push('/')
  }
}

function exitToMenu() {
  ui.close()
  interview.reset()
  router.push('/')
}
</script>

<template>
  <Transition name="fade">
    <div v-if="ui.paused" class="backdrop" @click.self="ui.close()">
      <div class="modal" role="dialog" aria-modal="true" aria-label="Пауза">
        <button class="modal__close" aria-label="Продолжить" @click="ui.close()">×</button>
        <h2 class="modal__title">Пауза</h2>
        <div class="modal__actions">
          <AppButton block @click="restart">Начать заново</AppButton>
          <AppButton variant="secondary" block disabled>Настройки</AppButton>
          <AppButton variant="secondary" block @click="exitToMenu">Выйти в меню</AppButton>
        </div>
        <button class="modal__resume" @click="ui.close()">Продолжить игру</button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(16, 24, 40, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  z-index: 50;
}
.modal {
  position: relative;
  width: 100%;
  max-width: 360px;
  background-color: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-8) var(--space-6) var(--space-6);
  text-align: center;
}
.modal__close {
  position: absolute;
  top: var(--space-3);
  right: var(--space-4);
  background: none;
  border: none;
  font-size: 24px;
  color: var(--color-text-muted);
  line-height: 1;
}
.modal__title {
  font-size: var(--text-xl);
  text-transform: uppercase;
  letter-spacing: 2px;
  margin-bottom: var(--space-6);
}
.modal__actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.modal__resume {
  margin-top: var(--space-4);
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
.modal__resume:hover {
  color: var(--color-primary);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
