<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppScreen from '@/components/AppScreen.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import StatTile from '@/components/StatTile.vue'
import { useHomeStore } from '@/stores/home'

const router = useRouter()
const home = useHomeStore()

onMounted(() => {
  if (!home.homePage) home.loadHome()
})

const stats = computed(() => home.homePage?.playerStats)
const winPercent = computed(() =>
  stats.value ? Math.round(stats.value.winRate * 100) : 0,
)

function newGame() {
  router.push('/mode')
}
</script>

<template>
  <AppScreen width="wide">
    <div v-if="home.error" class="state">
      <p>Не удалось загрузить данные. Проверьте, что бэкенд запущен.</p>
      <AppButton variant="secondary" @click="home.loadHome()">Повторить</AppButton>
    </div>

    <div v-else-if="!home.homePage" class="state">Загрузка…</div>

    <div v-else class="home">
      <!-- Menu / brand (design screen 1) -->
      <AppCard class="home__hero">
        <div class="brand">
          <span class="brand__badge">TECH LEAD</span>
          <span class="brand__title">SIMULATOR</span>
        </div>
        <p class="home__subtitle">{{ home.homePage.subtitle }}</p>
        <div class="illustration" aria-hidden="true">🧑‍💻🖥️👩‍💼👨‍💻</div>
        <div class="menu">
          <AppButton block @click="newGame">▶ Новая игра</AppButton>
          <AppButton variant="secondary" block disabled>⚙ Настройки</AppButton>
          <AppButton variant="ghost" block disabled>⏻ Выход</AppButton>
        </div>
      </AppCard>

      <!-- Stats (design screen 3) -->
      <AppCard class="home__stats">
        <h2 class="section-title">Ваша статистика</h2>
        <div class="stats-grid">
          <StatTile icon="🎮" label="Игр сыграно" :value="stats?.gamesPlayed ?? 0" />
          <StatTile icon="🏆" label="Процент побед" :value="`${winPercent}%`" />
          <StatTile icon="⭐" label="Лучший результат" :value="stats?.bestResult ?? 0" />
          <StatTile icon="👥" label="Команда создана" :value="stats?.candidatesHired ?? 0" />
        </div>
        <div class="last-game">
          <span class="last-game__label">Процент побед</span>
          <ProgressBar :value="winPercent" :max="100" tone="success" />
        </div>
      </AppCard>
    </div>
  </AppScreen>
</template>

<style scoped>
.home {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: var(--space-6);
  align-items: start;
}
.home__hero {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.brand {
  display: flex;
  flex-direction: column;
  line-height: 1;
}
.brand__badge {
  font-size: var(--text-2xl);
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--color-primary);
}
.brand__title {
  font-size: var(--text-2xl);
  font-weight: 800;
  letter-spacing: 6px;
  color: var(--color-text);
}
.home__subtitle {
  color: var(--color-text-muted);
}
.illustration {
  font-size: 40px;
  text-align: center;
  padding: var(--space-6) 0;
  background: linear-gradient(135deg, #eef2ff, #e0ecff);
  border-radius: var(--radius-md);
  letter-spacing: 6px;
}
.menu {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.home__stats {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}
.section-title {
  font-size: var(--text-lg);
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--color-text);
}
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6) var(--space-4);
}
.last-game {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.last-game__label {
  font-size: var(--text-sm);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--color-text-muted);
}
.state {
  text-align: center;
  color: var(--color-text-muted);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  align-items: center;
}

@media (max-width: 760px) {
  .home {
    grid-template-columns: 1fr;
  }
}
</style>
