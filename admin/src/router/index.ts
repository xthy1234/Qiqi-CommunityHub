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
      // 文章管理
      {
        path: 'articles',
        component: () => import('@/views/article/list.vue'),
        meta: { title: '文章管理' }
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
  
  // 404 页面 - 重定向到登录页（因为所有页面都需要登录）
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
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

  // 🔍 调试日志：路由守卫触发
  console.log('🔒 [路由守卫] 从:', from.path, '-> 到:', to.path)
  
  // 白名单路由（不需要登录）
  const whiteList = ['/login', '/register']
  
  // 🔍 调试日志：当前 token 状态
  const token = localStorage.getItem('Token')

  if (!whiteList.includes(to.path)) {
    // 除登录/注册外的所有页面都需要登录
    if (!token) {
      // 🔍 调试日志：未登录，需要重定向
      console.log('⚠️ [路由守卫] 未登录，访问需要认证的页面，重定向到登录页')
      
      // 未登录，保存当前要访问的路径，登录后跳转
      localStorage.setItem('redirectPath', to.fullPath)
      console.log('💾 [路由守卫] 已保存 redirectPath:', to.fullPath)
      
      // 重定向到登录页
      next('/login')
      return
    } else {
      console.log('✅ [路由守卫] 已登录，允许访问')
    }
  } else {
    console.log('ℹ️ [路由守卫] 访问白名单页面:', to.path)
  }

  console.log('➡️ [路由守卫] 继续执行')
  next()
})

export default router
