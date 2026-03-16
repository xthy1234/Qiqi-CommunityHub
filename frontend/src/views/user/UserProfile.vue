<template>
  <PageContainer header-title="个人信息" :show-back="false">
    <div class="profile-content">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar-display">
          <n-avatar
            v-if="userInfo.avatar" 
            :src="getAvatarUrl(userInfo.avatar)" 
            class="avatar-image"
            :size="150"
            round
          />
          <n-avatar
            v-else
            class="avatar-placeholder"
            :size="150"
            round
          >
            {{ getAvatarInitials() }}
          </n-avatar>
        </div>
      </div>

      <!-- 信息列表 -->
      <div class="info-list">
        <div class="info-item">
          <span class="info-label">用户账号：</span>
          <span class="info-value">{{ userInfo.account || '-' }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">用户姓名：</span>
          <span class="info-value">{{ userInfo.nickname || '-' }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">性别：</span>
          <span class="info-value">{{ getGenderText(userInfo.gender) }}</span>
        </div>

        <div class="info-item" v-if="userInfo.phone">
          <span class="info-label">手机号码：</span>
          <span class="info-value">{{ userInfo.phone }}</span>
        </div>

        <div class="info-item" v-if="userInfo.email">
          <span class="info-label">邮箱：</span>
          <span class="info-value">{{ userInfo.email }}</span>
        </div>

        <div class="info-item" v-if="userInfo.birthday">
          <span class="info-label">生日：</span>
          <span class="info-value">{{ userInfo.birthday }}</span>
        </div>

        <div class="info-item" v-if="userInfo.signature">
          <span class="info-label">个性签名：</span>
          <span class="info-value signature-text">{{ userInfo.signature }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">注册时间：</span>
          <span class="info-value">{{ formatDateTime(userInfo.createTime) }}</span>
        </div>

        <div class="info-item" v-if="userInfo.lastLoginTime">
          <span class="info-label">最后登录：</span>
          <span class="info-value">{{ formatDateTime(userInfo.lastLoginTime) }}</span>
        </div>
      </div>

      <!-- 作者的文章列表 -->
      <div class="author-articles-section">
        <h3 class="section-title">TA 的文章</h3>

        <!-- 使用 ArticleGridList 组件 -->
        <ArticleGridList
          :articles="articleList"
          :loading="articlesLoading"
          :loading-count="5"
          empty-text="暂无文章"
          :cols="1"
        />

        <!-- 分页组件 -->
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NAvatar, NPagination } from 'naive-ui'
import { getAvatarUrl, getGenderText, formatDateTime } from '@/utils/userUtils'
import PageContainer from '@/components/PageContainer.vue'
import ArticleGridList from '@/components/ArticleGridList.vue'
import userService from '@/api/user'

interface UserInfo {
  id?: number
  account: string
  nickname?: string
  avatar?: string
  gender?: number
  phone?: string
  email?: string
  birthday?: string
  signature?: string
  createTime?: string
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

const router = useRouter()
const appContext = useGlobalProperties()
const $http = appContext.$http

const userInfo = ref<UserInfo>({
  account: '',
  nickname: '',
  avatar: '',
  gender: 0,
  phone: '',
  email: '',
  birthday: undefined,
  signature: ''
})

const articleList = ref<Article[]>([])
const articlesLoading = ref<boolean>(false)
const articleTotalCount = ref<number>(0)
const articlePagination = ref<Pagination>({ page: 1, limit: 5 })

const fetchUserInfo = async () => {
  try {
    const currentUser = await userService.getCurrentUser()
    if (!currentUser?.id) {
      appContext?.$toolUtil.message('请先登录', 'error')
      router.push('/login')
      return
    }

    const response = await $http({
      url: `users/${currentUser.id}`,
      method: 'get',
      params: { detail: true },
      credentials: 'include'
    })
    
    const userData = response.data.data
    if (userData) {
      userInfo.value = {
        id: userData.id,
        account: userData.account || '-',
        nickname: userData.nickname || userData.username || '-',
        avatar: userData.avatar || '',
        gender: userData.gender ?? 0,
        phone: userData.phone || '',
        email: userData.email || '',
        birthday: userData.birthday || null,
        signature: userData.signature || '',
        createTime: userData.createTime,
        lastLoginTime: userData.lastLoginTime,
        lastLoginIp: userData.lastLoginIp
      }
      
      appContext?.$toolUtil.storageSet('userid', userData.id)
      appContext?.$toolUtil.storageSet('nickname', userData.account)
      appContext?.$toolUtil.storageSet('avatar', userData.avatar)

      fetchUserArticles()
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

onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.profile-content {
  .avatar-section {
    display: flex;
    justify-content: center;
    margin-bottom: 40px;
    
    .avatar-display {
      .avatar-image,
      .avatar-placeholder {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
      
      .avatar-placeholder {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        font-size: 60px;
        font-weight: bold;
        color: #fff;
      }
    }
  }
  
  .info-list {
    margin-bottom: 40px;
    
    .info-item {
      display: flex;
      padding: 16px 0;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .info-label {
        width: 120px;
        font-weight: 500;
        color: #666;
        flex-shrink: 0;
      }
      
      .info-value {
        flex: 1;
        color: #333;
        
        &.signature-text {
          white-space: pre-wrap;
          word-break: break-all;
        }
      }
    }
  }
  
  .author-articles-section {
    margin-bottom: 40px;
    padding-top: 30px;
    border-top: 2px solid #f0f0f0;

    .section-title {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin-bottom: 20px;
      padding-left: 15px;
      border-left: 4px solid #18a058;
    }

    .article-pagination {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
}
</style>
