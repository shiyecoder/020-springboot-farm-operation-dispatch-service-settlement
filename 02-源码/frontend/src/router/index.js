import { createRouter, createWebHistory } from 'vue-router'

// 路由与后端菜单 path 一一对应，侧边栏由 /auth/me 下发的菜单动态生成
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    redirect: '/dashboard'
  },
  { path: '/dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '运营看板' } },
  { path: '/request', component: () => import('../views/Request.vue'), meta: { title: '服务需求池' } },
  { path: '/quotation', component: () => import('../views/Quotation.vue'), meta: { title: '报价确认' } },
  { path: '/dispatch', component: () => import('../views/Dispatch.vue'), meta: { title: '智能排程台' } },
  { path: '/work', component: () => import('../views/Work.vue'), meta: { title: '作业执行' } },
  { path: '/acceptance', component: () => import('../views/Acceptance.vue'), meta: { title: '验收管理' } },
  { path: '/machine', component: () => import('../views/Machine.vue'), meta: { title: '机具档案' } },
  { path: '/operator', component: () => import('../views/Operator.vue'), meta: { title: '农机手档案' } },
  { path: '/price', component: () => import('../views/Price.vue'), meta: { title: '服务价目' } },
  { path: '/maintain', component: () => import('../views/Maintain.vue'), meta: { title: '保养维修' } },
  { path: '/settlement', component: () => import('../views/Settlement.vue'), meta: { title: '结算管理' } },
  { path: '/rule', component: () => import('../views/Rule.vue'), meta: { title: '派单规则配置' } },
  { path: '/portal', component: () => import('../views/Portal.vue'), meta: { title: '农户门户' } },
  { path: '/land', component: () => import('../views/Land.vue'), meta: { title: '我的地块' } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
  { path: '/system/:tab', component: () => import('../views/System.vue'), meta: { title: '系统管理' } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return next('/login')
  if (to.path === '/login' && token) return next('/dashboard')
  next()
})

export default router
