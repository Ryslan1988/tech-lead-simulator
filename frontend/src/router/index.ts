import { createRouter, createWebHistory } from 'vue-router'

import { useInterviewStore } from '@/stores/interview'

/**
 * Routes map 1:1 to the game screens. Everything under `/interview/:id/*`
 * requires a loaded session (see the guard below) so deep-links without an
 * active game bounce back to the home screen.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/mode',
      name: 'mode',
      component: () => import('@/views/ModeSelectView.vue'),
    },
    {
      path: '/interview/:id/lobby',
      name: 'lobby',
      component: () => import('@/views/LobbyView.vue'),
      props: true,
      meta: { requiresSession: true },
    },
    {
      path: '/interview/:id/question',
      name: 'question',
      component: () => import('@/views/QuestionView.vue'),
      props: true,
      meta: { requiresSession: true },
    },
    {
      path: '/interview/:id/result',
      name: 'result',
      component: () => import('@/views/ResultView.vue'),
      props: true,
      meta: { requiresSession: true },
    },
    {
      path: '/interview/:id/offer',
      name: 'offer',
      component: () => import('@/views/OfferView.vue'),
      props: true,
      meta: { requiresSession: true },
    },
    {
      path: '/interview/:id/summary',
      name: 'summary',
      component: () => import('@/views/SummaryView.vue'),
      props: true,
      meta: { requiresSession: true },
    },
    // Unknown paths fall back home.
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.requiresSession) return true
  const interview = useInterviewStore()
  const id = Number(to.params.id)
  if (!Number.isFinite(id) || !interview.hasSessionFor(id)) {
    // No active session for this interview — send the player home.
    return { name: 'home' }
  }
  return true
})

export default router
