<template>
  <PageContainer
    header-title="游戏社区"
    :show-back="false"
  >
    <!-- 轮播图区域 -->
    <CarouselComponent @click="handleCarouselClick" />

    <!-- 每日签到组件 -->
    <DailySignIn
      v-if="isLoggedIn"
      ref="signInRef"
      @refresh="handleSignInRefresh"
    />

    <!-- 热门文章列表 -->
    <ArticleGridList
      :articles="hotArticles"
      :loading="isLoadingArticles"
      :loading-count="6"
      empty-text="暂无热门文章"
    />

    <!-- 查看更多按钮 -->
    <div class="view-more-container">
      <n-button
        type="primary"
        size="large"
        class="view-more-btn"
        @click="navigateToArticleList"
      >
        <template #icon>
          <Icon icon="ri:apps-line" />
        </template>
        查看更多文章
      </n-button>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'
import { Icon } from '@iconify/vue'
import { articleAPI } from '@/api/article'
import ArticleGridList from '@/components/article/ArticleGridList.vue'
import PageContainer from "@/components/layout/PageContainer.vue";
import DailySignIn from '@/components/common/DailySignIn.vue'
import CarouselComponent from '@/components/common/CarouselComponent.vue'

interface ArticleItem {
  id: number | string
  title: string
  coverUrl: string
  categoryId?: number | string
  categoryName?: string
  authorId?: number | string
  authorNickname?: string
  authorAvatar?: string
  likeCount?: number
  favoriteCount?: number
  shareCount?: number
  viewCount?: number
  publishTime?: string
  createTime?: string
  [key: string]: any
}

const appContext = useGlobalProperties()
const router = useRouter()

const hotArticles = ref<ArticleItem[]>([])
const isLoadingArticles = ref<boolean>(false)
const signInRef = ref<InstanceType<typeof DailySignIn> | null>(null)

// 检查是否已登录
const isLoggedIn = computed(() => {
  const token = appContext?.$toolUtil?.storageGet('Token')
  return !!token
})

const fetchHotArticles = async (): Promise<void> => {
  isLoadingArticles.value = true
  try {
    const params = {
      page: 1,
      limit: 6,
      sort: 'createTime',
      order: 'desc',
      auditStatus: '1'
    }
    const response = await articleAPI.getList(params)
    const apiData = response.data?.data || response.data || {}
    hotArticles.value = apiData.list || (Array.isArray(apiData) ? apiData : [])
  } catch (error) {
    console.error('[HomeView] 加载文章失败:', error)
  } finally {
    isLoadingArticles.value = false
  }
}

const navigateToArticleList = (): void => {
  router.push('/index/articleList')
}

const handleCarouselClick = (item: any) => {
  // 如果轮播图有跳转链接，可以在这里处理
  if (item.linkUrl) {
    window.open(item.linkUrl, '_blank')
  }
}

/**
 * 签到刷新回调
 */
const handleSignInRefresh = () => {
  // 可以在这里执行其他需要刷新的操作
}

onMounted(() => {
  fetchHotArticles()
})
</script>

<style lang="scss" scoped>
.view-more-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 30px 0;

  .view-more-btn {
    min-width: 200px;
    height: 48px;
    font-size: 16px;
    border-radius: 24px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(24, 160, 88, 0.3);
    }
  }
}

@media (max-width: 768px) {
  .view-more-container {
    .view-more-btn {
      min-width: 160px;
      height: 44px;
      font-size: 14px;
    }
  }
}
</style>
