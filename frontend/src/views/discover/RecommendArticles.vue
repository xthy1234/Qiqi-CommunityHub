<template>
  <PageContainer
    :header-title="isFromPersonalCenter ? '个人文章管理' : '文章列表'"
    @back="navigateToHome"
  >
    <template #headerExtra>
      <n-button v-if="showAddButton" type="primary" @click="navigateToAdd">
        <Icon icon="ri:add-circle-line" style="margin-right: 4px;" />
        发布新文章
      </n-button>
    </template>

    <!-- 搜索和筛选区域 -->
    <n-card :bordered="false" class="filter-section">
      <n-space align="center" :size="12">        <!-- 分类选择器 -->
        <ArticleCategorySelect
          v-model="selectedCategoryId"
          placeholder="选择分类"
          @change="handleCategoryChange"
        />
        <!-- 搜索输入框 -->
        <n-input
            v-model:value="searchCriteria.keyword"
            placeholder="请输入文章标题或关键词"
            clearable
            style="width: 300px;"
            @keyup.enter="handleSearch"
        >
          <template #prefix>
            <Icon icon="ri:search-line" />
          </template>
        </n-input>

        <!-- 搜索按钮 -->
        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <Icon icon="ri:search-eye-line" />
          </template>
          搜索
        </n-button>

        <!-- 重置按钮 -->
        <n-button @click="handleReset">
          <template #icon>
            <Icon icon="ri:restart-line" />
          </template>
          重置
        </n-button>

        <!-- 新建草稿按钮 -->
<!--        <n-button type="success" @click="handleCreateDraft">-->
<!--          <template #icon>-->
<!--            <Icon icon="ri:add-line" />-->
<!--          </template>-->
<!--          新建草稿-->
<!--        </n-button>-->
      </n-space>
    </n-card>

    <!-- 文章列表 -->
    <ArticleGridList
      :articles="articleList"
      :loading="isLoading"
      :loading-count="pagination.limit"
      empty-text="暂无文章"
      :cols="3"
    />

    <!-- 分页组件 -->
    <n-pagination
      v-model:page="pagination.page"
      :item-count="totalCount"
      :page-size="pagination.limit"
      show-size-picker
      :page-sizes="[5, 10, 20]"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
      style="margin-top: 20px; justify-content: center;"
    >
      <template #prefix="{ itemCount }">
        共 {{ itemCount }} 条
      </template>
    </n-pagination>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NMessageProvider, useMessage, NButton, NInput, NSelect, NPagination, NCard, NAvatar, NTag, NSkeleton, NGrid, NGridItem, NEmpty, NSpace } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handleImageError, getFullUrl } from '@/utils/userUtils'
import { articleAPI } from '@/api/article'
import ArticleGridList from '@/components/article/ArticleGridList.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ArticleCategorySelect from '@/components/article/ArticleCategorySelect.vue'

interface Article {
  id: number | string
  title: string
  coverUrl: string
  categoryId?: number | string
  categoryName?: string
  authorId?: number | string
  authorNickname?: string
  favoriteCount?: number
  content?: string
  publishTime?: string
  likeCount?: number
  dislikeCount?: number
  auditStatus?: string
  viewCount?: number
  createTime?: string
  updateTime?: string
  categoryStrName?: string | null
  authorAvatar?: string | null
  attachment?: any
  auditReply?: string
  deleted?: boolean
  [key: string]: any
}

interface Pagination {
  page: number
  limit: number
}

interface SearchCriteria {
  keyword?: string
  categoryId?: number | string
  startDate?: string
  endDate?: string
}

const appContext = useGlobalProperties()
const router = useRouter()
const route = useRoute()
const message = useMessage()

const FORM_NAME = '帖子'
const breadcrumbItems = ref([{ name: FORM_NAME }])
const articleList = ref<Article[]>([])
const pagination = ref<Pagination>({ page: 1, limit: 20 })
const totalCount = ref<number>(0)
const isLoading = ref<boolean>(false)
const isFromPersonalCenter = ref<boolean>(false)
const searchCriteria = ref<SearchCriteria>({})
const selectedCategoryId = ref<number | string | undefined>(undefined)

// 获取 baseUrl（优先使用正确的配置）
const baseUrl = computed(() => {
  const configUrl = appContext?.$config?.url
  // 如果配置了 8082，强制使用 8080
  if (configUrl && configUrl.includes('8082')) {
    return 'http://localhost:8080'
  }
  return configUrl || 'http://localhost:8080'
})

const showAddButton = computed(() => !isFromPersonalCenter.value)

const navigateToHome = (): void => {
  router.push('/index/home')
}

const navigateToAdd = (): void => {
  router.push('/index/article/editor')
}

const initializePage = (): void => {
  if (route.query.centerType) {
    isFromPersonalCenter.value = true
  }
  fetchArticleList()
}

const handleSearch = (): void => {
  pagination.value.page = 1
  fetchArticleList()
}

const handleCategoryChange = (categoryId: number | string | undefined): void => {
  selectedCategoryId.value = categoryId
  pagination.value.page = 1
  fetchArticleList()
}

const handleReset = (): void => {
  searchCriteria.value.keyword = ''
  selectedCategoryId.value = undefined
  pagination.value.page = 1
  fetchArticleList()
}

const handleCreateDraft = (): void => {
  router.push('/index/article/editor')
}

const handlePageChange = (page: number): void => {
  pagination.value.page = page
  fetchArticleList()
}

const handlePageSizeChange = (size: number): void => {
  pagination.value.limit = size
  pagination.value.page = 1
  fetchArticleList()
}

const fetchArticleList = async (): Promise<void> => {
  isLoading.value = true

  try {
    const params: any = {
      page: pagination.value.page,
      limit: pagination.value.limit
    }

    // 添加分类筛选
    if (selectedCategoryId.value !== undefined && selectedCategoryId.value !== '') {
      params.categoryId = selectedCategoryId.value
    }

    // 添加关键词搜索
    if (searchCriteria.value.keyword && searchCriteria.value.keyword.trim()) {
      // 使用全文搜索接口
      const searchResponse = await appContext?.$http({
        url: '/articles/search',
        method: 'get',
        params: {
          keyword: searchCriteria.value.keyword.trim(),
          categoryId: selectedCategoryId.value,
          limit: pagination.value.limit
        }
      })

      articleList.value = searchResponse.data.data || []
      totalCount.value = articleList.value.length
    } else {
      // 否则使用普通列表接口
      if (!isFromPersonalCenter.value) {
        params.auditStatus = '1'
      }

      const response = await articleAPI.getList(params)
      const apiData = response.data?.data || response.data || {}
      articleList.value = apiData.list || []
      totalCount.value = apiData.totalCount || 0
    }

  } catch (error) {
    console.error('获取文章列表失败:', error)
    message.error('获取文章列表失败，请重试')
    articleList.value = []
  } finally {
    isLoading.value = false
  }
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(() => {
  initializePage()
})
</script>

<style lang="scss" scoped>
.filter-section {
  margin-bottom: 20px;
}
</style>