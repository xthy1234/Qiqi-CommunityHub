<template>
  <PageContainer :header-title="pageTitle"   @back="goBack">
    <div class="user-public-profile">
      <!-- 用户信息卡片 -->
      <div class="profile-header">
        <div class="avatar-section">
          <n-avatar
            v-if="userInfo.avatar"
            :src="getAvatarUrl(userInfo.avatar)"
            :size="120"
            round
            class="avatar-image"
          />
          <n-avatar
            v-else
            :size="120"
            round
            class="avatar-placeholder"
          >
            {{ getAvatarInitials() }}
          </n-avatar>
        </div>

        <div class="user-basic-info">
          <h2 class="user-nickname">{{ userInfo.nickname || userInfo.account }}</h2>
          <p v-if="userInfo.signature" class="user-signature">
            {{ userInfo.signature }}
          </p>
          <div class="user-stats">
            <div class="stat-item" @click="goToFollowingList">
              <span class="stat-value">{{ userInfo.followingCount || 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item" @click="goToFollowerList">
              <span class="stat-value">{{ userInfo.followerCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ articleTotalCount }}</span>
              <span class="stat-label">文章</span>
            </div>
          </div>
        </div>

        <div class="action-section">
          <n-button
            v-if="!isCurrentUser && isLoggedIn"
            :type="userInfo.isFollowed ? 'default' : 'primary'"
            size="large"
            :loading="followActionLoading"
            @click="handleFollowToggle"
          >
            {{ userInfo.isFollowed ? '已关注' : '关注' }}
          </n-button>
          <n-button
            v-if="isFriend"
            type="primary"
            size="large"
            secondary
            @click="handleSendMessage"
          >
            发消息
          </n-button>
        </div>
      </div>

      <!-- 详细信息 -->
      <div class="profile-details">
        <div class="info-item" v-if="userInfo.gender">
          <Icon icon="ri:genderless-line" :size="18" />
          <span>{{ getGenderText(userInfo.gender) }}</span>
        </div>
        <div class="info-item" v-if="userInfo.birthday">
          <Icon icon="ri:calendar-line" :size="18" />
          <span>{{ userInfo.birthday }}</span>
        </div>
        <div class="info-item">
          <Icon icon="ri:time-line" :size="18" />
          <span>加入于 {{ formatDateTime(userInfo.createTime) }}</span>
        </div>

      </div>

      <!-- 作者的文章列表 -->
      <div class="articles-section">
        <h3 class="section-title">
          <Icon icon="ri:article-line" :size="20" />
          TA 的文章
        </h3>

        <ArticleGridList
          :articles="articleList"
          :loading="articlesLoading"
          :loading-count="5"
          empty-text="暂无文章"
          :cols="1"
        />

        <n-pagination
          v-if="articleList.length > 0 && !articlesLoading"
          v-model:page="articlePagination.page"
          :item-count="articleTotalCount"
          :page-size="articlePagination.limit"
          show-size-picker
          :page-sizes="[5, 10, 20]"
          @update:page="handleArticlePageChange"
          @update:page-size="handlePageSizeChange"
          class="article-pagination"
        >
          <template #prefix="{ itemCount }">
            共 {{ itemCount }} 条
          </template>
        </n-pagination>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGlobalProperties } from '@/utils/globalProperties'
import { Icon } from '@iconify/vue'
import { NAvatar, NPagination } from 'naive-ui'
import { getAvatarUrl, getGenderText, formatDateTime } from '@/utils/userUtils'
import PageContainer from '@/components/common/PageContainer.vue'
import ArticleGridList from '@/components/article/ArticleGridList.vue'
import userService from '@/api/user'
import followApi from '@/api/follow'

interface UserInfo {
  id?: number
  account: string
  nickname?: string
  avatar?: string
  gender?: number
  birthday?: string
  signature?: string
  createTime?: string
  followerCount?: number
  followingCount?: number
  articleCount?: number
  isFollowed?: boolean
  // 详细信息字段（仅自己可见）
  phone?: string
  email?: string
  roleId?: number
  status?: number
  lastLoginTime?: string
  lastLoginIp?: string
}

interface Article {
  id: number | string
  title: string
  coverUrl: string
  categoryId?: number | string
  categoryName?: string
  authorId?: number | string
  authorNickname?: string
  favoriteCount?: number
  viewCount?: number
  status?: number
  createTime?: string
  [key: string]: any
}

interface Pagination {
  page: number
  limit: number
}

const route = useRoute()
const router = useRouter()
const appContext = useGlobalProperties()
const $http = appContext.$http

const userInfo = ref<UserInfo>({
  account: '',
  nickname: '',
  avatar: '',
  gender: 0,
  birthday: undefined,
  signature: '',
  followerCount: 0,
  followingCount: 0,
  articleCount: 0,
  isFollowed: false
})

const articleList = ref<Article[]>([])
const articlesLoading = ref<boolean>(false)
const articleTotalCount = ref<number>(0)
const articlePagination = ref<Pagination>({ page: 1, limit: 5 })
const isFriend = ref<boolean>(false)
const followActionLoading = ref<boolean>(false)
const isLoggedIn = ref<boolean>(false)
const currentUserId = ref<number | null>(null)

const pageTitle = computed(() => {
  return userInfo.value.nickname ? `${userInfo.value.nickname}的个人主页` : '用户主页'
})

const isCurrentUser = computed(() => {
  if (!currentUserId.value || !userInfo.value.id) return false
  return currentUserId.value === userInfo.value.id
})

const fetchUserInfo = async () => {
  try {
    const userId = route.params.id
    const response = await $http({
      url: `users/${userId}`,
      method: 'get',
      params: { detail: false }
    })
    
    const userData = response.data.data
    if (userData) {
      userInfo.value = {
        id: userData.id,
        account: userData.account || '',
        nickname: userData.nickname || userData.username || '',
        avatar: userData.avatar || '',
        gender: userData.gender ?? 0,
        birthday: userData.birthday || null,
        signature: userData.signature || '',
        createTime: userData.createTime,
        followerCount: userData.followerCount || 0,
        followingCount: userData.followingCount || 0,
        articleCount: userData.articleCount || 0,
        isFollowed: userData.isFollowed || false
      }

      await fetchUserArticles()
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    appContext?.$toolUtil.message('获取用户信息失败', 'error')
  }
}

const fetchUserArticles = async () => {
  articlesLoading.value = true

  try {
    const params = {
      page: articlePagination.value.page,
      limit: articlePagination.value.limit,
      authorId: userInfo.value.id
    }

    const response = await $http({
      url: '/articles',
      method: 'get',
      params
    })

    const apiData = response.data.data
    articleList.value = apiData.list || []
    articleTotalCount.value = apiData.totalCount || 0
  } catch (error) {
    console.error('获取用户文章列表失败:', error)
  } finally {
    articlesLoading.value = false
  }
}

const handleArticlePageChange = (page: number) => {
  articlePagination.value.page = page
  fetchUserArticles()
}

const handlePageSizeChange = (size: number) => {
  articlePagination.value.limit = size
  articlePagination.value.page = 1
  fetchUserArticles()
}

const getAvatarInitials = (): string => {
  const name = userInfo.value.nickname || userInfo.value.account || '用户'
  return name.charAt(0).toUpperCase()
}

const checkLoginStatus = async () => {
  try {
    const currentUser = await userService.getCurrentUser()
    isLoggedIn.value = true
    currentUserId.value = currentUser.id
  } catch (error) {
    isLoggedIn.value = false
    currentUserId.value = null
  }
}

const handleFollowToggle = async () => {
  if (followActionLoading.value) return

  followActionLoading.value = true
  try {
    await followApi.followOrUnfollow({
      userId: userInfo.value.id!,
      action: userInfo.value.isFollowed ? 'unfollow' : 'follow'
    })

    userInfo.value.isFollowed = !userInfo.value.isFollowed

    if (userInfo.value.isFollowed) {
      userInfo.value.followerCount = (userInfo.value.followerCount || 0) + 1
    } else {
      userInfo.value.followerCount = Math.max(0, (userInfo.value.followerCount || 0) - 1)
    }

    appContext?.$toolUtil.message(
      userInfo.value.isFollowed ? '关注成功' : '已取消关注',
      'success'
    )
  } catch (error: any) {
    console.error('关注操作失败:', error)
    appContext?.$toolUtil.message(error.message || '操作失败', 'error')
  } finally {
    followActionLoading.value = false
  }
}
const goBack = () => {
  router.back()
}
const handleSendMessage = () => {
  appContext?.$toolUtil.message('私信功能开发中', 'info')
}

const goToFollowingList = () => {
  router.push(`/user/${userInfo.value.id}/following`)
}

const goToFollowerList = () => {
  router.push(`/user/${userInfo.value.id}/followers`)
}

onMounted(() => {
  Promise.all([
    checkLoginStatus(),
    fetchUserInfo()
  ])
})
</script>

<style lang="scss" scoped>
.user-public-profile {
  max-width: 900px;
  margin: 0 auto;

  .profile-header {
    display: flex;
    align-items: center;
    gap: 30px;
    padding: 30px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;

    .avatar-section {
      flex-shrink: 0;

      .avatar-image,
      .avatar-placeholder {
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
      }

      .avatar-placeholder {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        font-size: 48px;
        font-weight: bold;
        color: #fff;
      }
    }

    .user-basic-info {
      flex: 1;
      min-width: 0;

      .user-nickname {
        font-size: 28px;
        font-weight: 600;
        color: #303133;
        margin: 0 0 12px 0;
      }

      .user-signature {
        font-size: 14px;
        color: #909399;
        margin: 0 0 16px 0;
        line-height: 1.6;
      }

      .user-stats {
        display: flex;
        gap: 24px;

        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          cursor: pointer;
          transition: transform 0.3s;

          &:hover {
            transform: scale(1.1);
          }

          .stat-value {
            font-size: 20px;
            font-weight: 600;
            color: #18a058;
          }

          .stat-label {
            font-size: 13px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
    }

    .action-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
      align-items: center;
    }
  }

  .profile-details {
    display: flex;
    gap: 20px;
    padding: 20px 30px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;
    flex-wrap: wrap;

    .info-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: #606266;
    }
  }

  .articles-section {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 30px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 20px;
      padding-bottom: 15px;
      border-bottom: 2px solid #f0f0f0;
    }

    .article-pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
