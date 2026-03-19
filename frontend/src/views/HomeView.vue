<template>
  <PageContainer header-title="游戏社区" :show-back="false">

    <!-- 轮播图区域 -->
    <n-card :bordered="false" class="carousel-card">
      <n-spin v-if="isLoadingCarousel" description="加载轮播图中...">
        <n-skeleton text style="height: 300px; width: 1200px" />
      </n-spin>
      <n-carousel v-else show-arrow autoplay class="custom-carousel">
        <img
          v-for="(item, index) in carouselImages"
          :key="item.id || index"
          class="carousel-image"
          :src="baseUrl + '/' + (item.imageUrl || item.linkUrl || '')"
          :alt="item.title || '轮播图'"
          @error="handleImageError"
        >
      </n-carousel>
    </n-card>

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
        @click="navigateToArticleList"
        class="view-more-btn"
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NCarousel, NCarouselItem, NButton, NSkeleton } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'
import { Icon } from '@iconify/vue'
import { articleAPI } from '@/api/article'
import ArticleGridList from '@/components/article/ArticleGridList.vue'
import PageContainer from "@/components/common/PageContainer.vue";

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

interface CarouselImage {
  id?: number | string
  imageUrl?: string
  linkUrl?: string
  title?: string
}

const appContext = useGlobalProperties()
const router = useRouter()

const carouselImages = ref<CarouselImage[]>([])
const hotArticles = ref<ArticleItem[]>([])
const isLoadingCarousel = ref<boolean>(false)
const isLoadingArticles = ref<boolean>(false)
const baseUrl = appContext?.$config?.url || 'http://localhost:8080'

const fetchCarouselImages = async (): Promise<void> => {
  isLoadingCarousel.value = true
  try {
    const response = await appContext?.$http.get('swipers/enabled')
    const apiData = response.data.data
    carouselImages.value = Array.isArray(apiData) ? apiData : (apiData?.list || [])
  } catch (error) {
    console.error('加载轮播图失败:', error)
    carouselImages.value = []
  } finally {
    isLoadingCarousel.value = false
  }
}

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

    // 调试：打印文章数据

    if (hotArticles.value.length > 0) {

    }
  } catch (error) {
    console.error('加载文章失败:', error)
  } finally {
    isLoadingArticles.value = false
  }
}

const getDefaultAvatar = (): string => {
  return '/placeholder.svg'
}

const isHttpUrl = (url: string): boolean => {
  if (!url) return false
  return url.startsWith('http')
}

const getCoverImageUrl = (coverUrl: string): string => {
  if (!coverUrl || coverUrl === 'null') {

    return '/placeholder.svg'
  }

  // 如果已经是完整 URL，直接返回
  if (isHttpUrl(coverUrl)) {

    return coverUrl
  }

  // 如果是相对路径，拼接 baseUrl
  const fullUrl = baseUrl + '/' + coverUrl

  return fullUrl
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatNumber = (num: number | undefined | null): string => {
  if (num === undefined || num === null) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return String(num)
}

const navigateToArticleDetail = (id: number | string): void => {
  router.push(`/index/articleDetail?id=${id}`)
}

const navigateToArticleList = (): void => {
  router.push('/index/articleList')
}

onMounted(() => {
  fetchCarouselImages()
  fetchHotArticles()
})
</script>

<style lang="scss" scoped>
.home-view-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.carousel-card {
  margin-bottom: 30px;
  overflow: hidden;

  :deep(.n-carousel) {
    max-width: 100%;
  }

  .carousel-image {
    width: 100%;
    height: 350px;
    object-fit: cover;
    cursor: pointer;
    border-radius: 8px;
  }
}

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
  .home-view-container {
    padding: 10px;
  }

  .carousel-card {
    .carousel-image {
      height: 200px;
    }
  }

  .view-more-container {
    .view-more-btn {
      min-width: 160px;
      height: 44px;
      font-size: 14px;
    }
  }
}
</style>
