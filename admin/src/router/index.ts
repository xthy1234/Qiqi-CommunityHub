import {
  createRouter,
  createWebHashHistory,
  RouteRecordRaw
} from 'vue-router'

// 导入新的布局组件
import MainLayout from '@/views/MainLayout.vue'
import LoginView from '../views/auth/login.vue'
import UserRegisterView from '../views/auth/register.vue'
import HomeView from "@/views/HomeView.vue";

const routes: Array<RouteRecordRaw> = [
  // 公开路由 - 不需要登录
  {
    path: '/login',
    component: LoginView,
    meta: { title: '管理员登录' }
  },
  {
    path: '/register',
    component: UserRegisterView,
    meta: { title: '管理员注册' }
  },
  
  // 所有管理端页面 - 需要登录
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      { 
        path: '', 
        name: 'Home', 
        component: HomeView,
        meta: { title: '首页' }
      },
      // 首页
      {
        path: 'index',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' }
      },
      // 兼容带 /index 前缀的路径
      {
        path: 'index/home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' }
      },
      // 用户管理
      {
        path: 'users',
        component: () => import('@/views/user/list.vue'),
        meta: { title: '用户管理' }
      },
      // 评论管理
      {
        path: 'comments',
        component: () => import('@/views/comment/list.vue'),
        meta: { title: '评论管理' }
      },
      // 管理员管理
      {
        path: 'admins',
        component: () => import('@/views/admin/list.vue'),
        meta: { title: '管理员管理' }
      },
      // 个人中心
      {
        path: 'profile',
        component: () => import('@/views/profile/UserProfile.vue'),
        meta: { title: '个人资料' }
      },
      {
        path: 'profile/edit',
        component: () => import('@/views/profile/UserProfileEdit.vue'),
        meta: { title: '编辑资料' }
      },
      {
        path: 'profile/password',
        component: () => import('@/views/profile/UserPasswordChange.vue'),
        meta: { title: '修改密码' }
      }
    ]
  },
  
  // 404 页面 - 重定向到首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

/**
 * 创建路由器实例
 */
const router = createRouter({
  history: createWebHashHistory(process.env.BASE_URL),
  routes
})

/**
 * 全局路由守卫 - 页面标题设置和登录验证
 */
router.beforeEach((to: any, from: any, next: any) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 中文社区管理平台`
  }

  // 检查是否需要登录验证
  const requiresAuth = to.meta?.requiresAuth || (to.path !== '/login' && to.path !== '/register')
  
  if (requiresAuth) {
    const token = localStorage.getItem('Token')
    
    if (!token) {
      // 未登录，保存当前要访问的路径，登录后跳转
      localStorage.setItem('redirectPath', to.fullPath)
      
      // 重定向到登录页
      next('/login')
      return
    }
  }

  // 如果已登录且访问登录页或注册页，重定向到首页
  if (to.path === '/login' || to.path === '/register') {
    const token = localStorage.getItem('Token')
    if (token) {
      next('/')
      return
    }
  }

  next()
})

export default router
