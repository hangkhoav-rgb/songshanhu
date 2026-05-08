import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/list',
    redirect: '/article'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/first-reset',
    name: 'FirstReset',
    component: () => import('@/views/auth/FirstReset.vue'),
    meta: { title: '首次重置密码', guest: true }
  },
  {
    path: '/article',
    name: 'ArticleList',
    component: () => import('@/views/article/List.vue'),
    meta: { title: '文章列表' }
  },
  {
    path: '/article/mine',
    name: 'MyArticleList',
    component: () => import('@/views/article/MyList.vue'),
    meta: { title: '我的文章', auth: true }
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/article/Detail.vue'),
    meta: { title: '文章详情' }
  },
  {
    path: '/article/create',
    name: 'ArticleCreate',
    component: () => import('@/views/article/Create.vue'),
    meta: { title: '创建文章', auth: true }
  },
  {
    path: '/article/actions',
    name: 'MyActions',
    component: () => import('@/views/article/Actions.vue'),
    meta: { title: '我的收藏/点赞', auth: true }
  },
  {
    path: '/article/edit/:id',
    name: 'ArticleEdit',
    component: () => import('@/views/article/Edit.vue'),
    meta: { title: '编辑文章', auth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/Index.vue'),
    meta: { title: '数据看板', auth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人资料', auth: true }
  },
  {
    path: '/location',
    name: 'Location',
    component: () => import('@/views/Location.vue'),
    meta: { title: '定位', auth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/Index.vue'),
    meta: { title: '管理后台', auth: true, admin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      return { el: to.hash, top: 90, behavior: 'smooth' }
    }
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const devToken = import.meta.env.DEV && typeof to.query.__token === 'string' ? String(to.query.__token) : ''
  if (devToken) {
    localStorage.setItem('token', devToken)
    const q: Record<string, any> = { ...to.query }
    delete q.__token
    next({ path: to.path, query: q, hash: to.hash, replace: true })
    return
  }

  const token = localStorage.getItem('token')
  const userStore = useUserStore()

  document.title = `${to.meta.title || '松山湖博客'} - 松山湖可视化博客平台`

  // 需要登录的页面
  if (to.meta.auth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 游客页面（已登录不能访问）
  if (to.meta.guest && token) {
    next('/')
    return
  }

  // 管理员页面
  if (to.meta.admin && !(userStore.role === 'admin' || userStore.role === 'ROLE_ADMIN' || userStore.role.includes('ADMIN'))) {
    next('/')
    return
  }

  next()
})

export default router
