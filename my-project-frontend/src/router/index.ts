import { createRouter, createWebHistory } from 'vue-router'
import WelcomeView from '@/views/WelcomeView.vue'
import LoginPage from '@/views/welcome/LoginPage.vue'
import { unauthorized } from '@/net'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: WelcomeView,
      children: [
        { path: '', name: 'welcome-login', component: LoginPage },
        { path: 'register', name: 'welcome-register', component: () => import('@/views/welcome/RegisterPage.vue') },
        { path: 'reset', name: 'welcome-reset', component: () => import('@/views/welcome/ResetPage.vue') },
      ],
    },
    { path: '/index', name: 'index', component: () => import('@/views/IndexView.vue') },
    { path: '/post/new', name: 'post-create', component: () => import('@/views/PostCreateView.vue') },
    { path: '/post/:id(\\d+)/edit', name: 'post-edit', component: () => import('@/views/PostEditView.vue') },
    { path: '/post/:id(\\d+)', name: 'post-detail', component: () => import('@/views/PostDetailView.vue') },
    { path: '/my/posts', name: 'my-posts', component: () => import('@/views/MyPostsView.vue') },
    { path: '/my/favorites', name: 'my-favorites', component: () => import('@/views/MyFavoritesView.vue') },
    { path: '/notifications', name: 'notifications', component: () => import('@/views/NotificationView.vue') },
    { path: '/:pathMatch(.*)*', component: () => import('@/views/NotFoundView.vue') },
  ],
})

router.beforeEach((to, _from, next) => {
  const isWelcome = typeof to.name === 'string' && to.name.startsWith('welcome-')

  if (isWelcome) {
    if (!unauthorized()) {
      next('/index')
      return
    }
  } else {
    if (unauthorized()) {
      next('/')
      return
    }
  }

  next()
})

export default router
