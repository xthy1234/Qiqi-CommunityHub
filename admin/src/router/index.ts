import {
  createRouter,
  createWebHashHistory,
  RouteRecordRaw
} from 'vue-router'

// 导入新的布局组件
import MainLayout from '@/views/MainLayout.vue'
import LoginView from '@/views/auth/Login.vue'
import UserRegisterView from '@/views/auth/Register.vue'
import HomeView from "@/views/dashboard/HomeView.vue";

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
        component: () => import('@/views/dashboard/HomeView.vue'),
        meta: { title: '首页' }
      },
      // 兼容带 /index 前缀的路径
      {
        path: 'index/home',
        component: () => import('@/views/dashboard/HomeView.vue'),
        meta: { title: '首页' }
      },
      // 用户管理
      {
        path: 'users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'users/edit',
        component: () => import('@/views/user/UserEdit.vue'),
        meta: { title: '用户编辑' }
      },
      // 文章管理
      {
        path: 'articles',
        component: () => import('@/views/content/article/ArticleList.vue'),
        meta: { title: '文章管理' }
      },
      {
        path: 'articles/audit',
        component: () => import('@/views/content/article/ArticleAudit.vue'),
        meta: { title: '文章审核' }
      },
      // 评论管理
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/content/comment/CommentList.vue'),
        meta: { requiresAuth: true }
      },
      // 分类管理
      {
        path: 'admin/categories',
        component: () => import('@/views/content/category/CategoryList.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'admin/categories/edit',
        component: () => import('@/views/content/category/CategoryEdit.vue'),
        meta: { title: '分类编辑' }
      },
      // 举报管理
      {
        path: 'admin/reports',
        component: () => import('@/views/content/report/ReportList.vue'),
        meta: { title: '举报管理' }
      },
      // 轮播图管理
      {
        path: 'admin/swipers',
        component: () => import('@/views/content/swiper/SwiperList.vue'),
        meta: { title: '轮播图管理' }
      },
      // 积分管理
      {
        path: 'points-rules',
        name: 'PointsRules',
        component: () => import('@/views/points/PointsRuleList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'points-transactions',
        name: 'PointsTransactions',
        component: () => import('@/views/points/PointsTransactionList.vue'),
        meta: { requiresAuth: true }
      },
      // 系统管理
      {
        path: 'admin/roles',
        component: () => import('@/views/system/role/RoleList.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'admin/menus',
        component: () => import('@/views/system/menu/MenuList.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'admin/role-menus',
        component: () => import('@/views/system/role/RoleMenu.vue'),
        meta: { title: '权限分配' }
      },
      // 个人中心
      {
        path: 'profile',
        component: () => import('@/views/user/UserProfile/Index.vue'),
        meta: { title: '个人资料' }
      },
      {
        path: 'profile/edit',
        component: () => import('@/views/user/UserProfile/Edit.vue'),
        meta: { title: '编辑资料' }
      },
      {
        path: 'profile/password',
        component: () => import('@/views/user/UserProfile/PasswordChange.vue'),
        meta: { title: '修改密码' }
      },
      // 通知管理
      {
        path: 'notification/send',
        component: () => import('@/views/notification/NotificationSend.vue'),
        meta: { title: '发送通知' }
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
    document.title = `${to.meta.title} - 游戏社区管理平台`
  }

  // 调试日志：路由守卫触发

  
  // 白名单路由（不需要登录）
  const whiteList = ['/login', '/register']
  
  // 调试日志：当前 token 状态
  const token = localStorage.getItem('Token')

  if (!whiteList.includes(to.path)) {
    // 除登录/注册外的所有页面都需要登录
    if (!token) {
      // 调试日志：未登录，需要重定向

      
      // 未登录，保存当前要访问的路径，登录后跳转
      localStorage.setItem('redirectPath', to.fullPath)

      
      // 重定向到登录页
      next('/login')
      return
    } else {

    }
  } else {

  }

  next()
})

export default router
