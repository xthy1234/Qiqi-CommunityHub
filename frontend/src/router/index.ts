import { 
  createRouter, 
  createWebHashHistory, 
  RouteRecordRaw
} from 'vue-router'

// 导入新的布局组件
import MainLayout from '@/views/MainLayout.vue'
import LoginView from '../views/auth/login.vue'

// 文章相关页面

import ArticleFormView from '@/views/article/edit.vue'
import ArticleDetailView from '@/views/article/detail.vue'
import ArticleEditorView from '@/views/article/edit.vue'
import ArticleDraftView from '@/views/article/draftList.vue'
import FavoriteListView from '@/views/article/favoriteList.vue'
import SuggestionReviewListView from '@/views/cooperation/SuggestionReviewList.vue'
import VersionListView from '@/views/article/VersionList.vue'
import SuggestionEditorView from '@/views/cooperation/SuggestionEditor.vue'
import MySuggestionsView from '@/views/cooperation/MySuggestions.vue'
import SuggestionReviewDetailView from '@/views/cooperation/SuggestionReviewDetail.vue'

// 用户相关页面 - 已精简，只保留个人中心相关页面
import UserRegisterView from '../views/auth/register.vue'
import UserProfileView from '@/views/user/UserProfile.vue'
import UserProfileEditView from '@/views/user/UserProfileEdit.vue'
import UserPasswordChangeView from '@/views/user/UserPasswordChange.vue'
import UserPublicProfile from '@/views/user/UserPublicProfile.vue'
import UserFollowingList from '@/views/user/UserFollowingList.vue'
import UserFollowersList from '@/views/user/UserFollowersList.vue'
import HomeView from "@/views/HomeView.vue";

// 发现相关页面
import RecommendUsersView from '@/views/discover/RecommendUsers.vue'
import RecommendCirclesView from '@/views/discover/RecommendCircles.vue'
import ArticleListView from '@/views/discover/RecommendArticles.vue'
// 消息相关页面
import ChatView from '@/views/message/chat.vue'

// 圈子聊天相关页面
import CircleChatView from '@/views/circle-chat/circle-chat.vue'
import CircleEditorView from '@/views/circle/CircleEditor.vue'

// 通知中心
import NotificationsView from '@/views/notification/Notifications.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'Home', component: HomeView },
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

      // 文章管理
      {
        path: 'articleList',
        component: ArticleListView,
        meta: { title: '文章列表' }
      },
      {
        path: 'articleDetail',
        component: ArticleDetailView,
        meta: { title: '文章详情' }
      },
      {
        path: 'articleAdd',
        component: ArticleFormView,
        meta: { title: '发布文章' }
      },
      {
        path: 'article/editor',
        component: ArticleEditorView,
        meta: { title: '创作文章' }
      },
      {
        path: 'article/draftList',
        component: ArticleDraftView,
        meta: { title: '草稿箱' }
      },
      // 待审核建议列表（作者专用）
      {
        path: 'article/suggestions',
        component: SuggestionReviewListView,
        meta: { title: '待审核建议' }
      },
      {
        path: 'index/article/suggestions',
        component: SuggestionReviewListView,
        meta: { title: '待审核建议' }
      },
      // 版本历史列表
      {
        path: 'article/versions',
        component: VersionListView,
        meta: { title: '版本历史' }
      },
      {
        path: 'index/article/versions',
        component: VersionListView,
        meta: { title: '版本历史' }
      },
      // 提交修改建议
      {
        path: 'article/:articleId/suggest',
        component: SuggestionEditorView,
        meta: { title: '提交修改建议' }
      },
      {
        path: 'index/article/:articleId/suggest',
        component: SuggestionEditorView,
        meta: { title: '提交修改建议' }
      },
      // 我的建议列表（查看自己提交的建议）
      {
        path: 'my-suggestions',
        component: MySuggestionsView,
        meta: { title: '我的建议' }
      },
      {
        path: 'index/my-suggestions',
        component: MySuggestionsView,
        meta: { title: '我的建议' }
      },
      // 审核建议详情页（独立页面）
      {
        path: 'suggestion/:id',
        component: SuggestionReviewDetailView,
        meta: { title: '审核建议详情' }
      },
      {
        path: 'index/suggestion/:id',
        component: SuggestionReviewDetailView,
        meta: { title: '审核建议详情' }
      },
      // 个人信息展示
      {
        path: 'user/profile',
        component: UserProfileView,
        meta: { title: '个人信息' }
      },
      // 修改个人信息
      {
        path: 'user/profile-edit',
        component: UserProfileEditView,
        meta: { title: '编辑资料' }
      },
      // 修改密码
      {
        path: 'user/password-change',
        component: UserPasswordChangeView,
        meta: { title: '修改密码' }
      },

      // 兼容带 /index 前缀的路径
      {
        path: 'index/articleList',
        component: ArticleListView,
        meta: { title: '文章列表' }
      },
      // 推荐用户
      {
        path: 'recommend-users',
        component: RecommendUsersView,
        meta: { title: '推荐用户' }
      },
      {
        path: 'index/recommend-users',
        component: RecommendUsersView,
        meta: { title: '推荐用户' }
      },
      // 推荐圈子
      {
        path: 'recommend-circles',
        component: RecommendCirclesView,
        meta: { title: '推荐圈子' }
      },
      {
        path: 'index/recommend-circles',
        component: RecommendCirclesView,
        meta: { title: '推荐圈子' }
      },
      {
        path: 'index/articleDetail',
        component: ArticleDetailView,
        meta: { title: '文章详情' }
      },
      {
        path: 'index/articleAdd',
        component: ArticleFormView,
        meta: { title: '发布文章' }
      },
      {
        path: 'index/article/editor',
        component: ArticleEditorView,
        meta: { title: '创作文章' }
      },
      {
        path: 'index/article/draftList',
        component: ArticleDraftView,
        meta: { title: '草稿箱' }
      },
      {
        path: 'favoriteList',
        component: FavoriteListView,
        meta: { title: '我的收藏' }
      },
      {
        path: 'index/favoriteList',
        component: FavoriteListView,
        meta: { title: '我的收藏' }
      },
      
      // 消息中心
      {
        path: 'chat',
        component: ChatView,
        meta: { title: '消息中心' }
      },
      // 指定聊天对象的路由
      {
        path: 'chat/:userId',
        component: ChatView,
        meta: { title: '私信聊天' }
      },
      // 兼容带 /index 前缀的消息中心路径
      {
        path: 'index/chat',
        component: ChatView,
        meta: { title: '消息中心' }
      },
      // 兼容带 /index 前缀的指定聊天对象路径
      {
        path: 'index/chat/:userId',
        component: ChatView,
        meta: { title: '私信聊天' }
      },

      // 通知中心
      {
        path: 'notifications',
        component: NotificationsView,
        meta: { title: '通知中心' }
      },
      // 兼容带 /index 前缀的通知中心路径
      {
        path: 'index/notifications',
        component: NotificationsView,
        meta: { title: '通知中心' }
      },

      // 圈子聊天
      {
        path: 'circle-chat',
        component: CircleChatView,
        meta: { title: '圈子聊天' }
      },
      // 兼容带 /index 前缀的圈子聊天路径
      {
        path: 'index/circle-chat',
        component: CircleChatView,
        meta: { title: '圈子聊天' }
      },
      // 创建/编辑圈子
      {
        path: 'circle/editor',
        component: CircleEditorView,
        meta: { title: '创建圈子' }
      },
      {
        path: 'index/circle/editor',
        component: CircleEditorView,
        meta: { title: '创建圈子' }
      },

      {
        path: 'index/user/profile',
        component: UserProfileView,
        meta: { title: '个人信息' }
      },
      {
        path: 'index/user/profile-edit',
        component: UserProfileEditView,
        meta: { title: '编辑资料' }
      },
      {
        path: 'index/user/password-change',
        component: UserPasswordChangeView,
        meta: { title: '修改密码' }
      },
      // 用户中心
      {
        path: 'userCenter',
        component: UserProfileView,
        meta: { title: '个人中心' }
      },
      {
        path: 'index/userCenter',
        component: UserProfileView,
        meta: { title: '个人中心' }
      },
      // 公开的个人主页（无需登录）
      {
        path: 'user/:id',
        component: UserPublicProfile,
        meta: { title: '个人主页' }
      },
      // 关注列表
      {
        path: 'user/:id/following',
        component: UserFollowingList,
        meta: { title: '关注列表' }
      },
      // 粉丝列表
      {
        path: 'user/:id/followers',
        component: UserFollowersList,
        meta: { title: '粉丝列表' }
      }
    ]
  },
  {
    path: '/login',
    component: LoginView,
    meta: { title: '用户登录' }
  },
  {
    path: '/userRegister',
    component: UserRegisterView,
    meta: { title: '用户注册' }
  },
  // 公开的个人主页路由（顶层路径）
  {
    path: '/user/:id',
    component: UserPublicProfile,
    meta: { title: '个人主页', requiresAuth: false }
  },
  // 关注列表路由（顶层路径）
  {
    path: '/user/:id/following',
    component: UserFollowingList,
    meta: { title: '关注列表', requiresAuth: false }
  },
  // 粉丝列表路由（顶层路径）
  {
    path: '/user/:id/followers',
    component: UserFollowersList,
    meta: { title: '粉丝列表', requiresAuth: false }
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
 * 路由守卫 - 仅设置页面标题
 */
router.beforeEach((to: any, from: any, next: any) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 游戏社区交流平台`
  }
  
  next()
})

export default router
