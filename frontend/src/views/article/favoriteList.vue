<template>
  <PageContainer
    header-title="我的收藏"
    @back="navigateToHome"
  >
    <template #headerExtra>
      <n-select
        v-model:value="selectedCategoryId"
        :options="categoryOptions"
        placeholder="全部分类"
        clearable
        style="width: 200px;"
        @update:value="handleCategoryChange"
      />
    </template>

    <!-- 收藏列表 -->
    <ArticleGridList
      :articles="articleList"
      :loading="isLoading"
      :loading-count="pagination.limit"
      empty-text="暂无收藏"
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NPagination, NSelect } from 'naive-ui'
import { Icon } from '@iconify/vue'
import ArticleGridList from '@/components/article/ArticleGridList.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useGlobalProperties } from '@/utils/globalProperties'

interface Article {
  id: number | string
  title: string
  coverUrl: string
  categoryId?: number | string
  categoryName?: string
  authorId?: number | string
  authorNickname?: string
  authorAvatar?: string | null
  likeCount?: number
  dislikeCount?: number
  favoriteCount?: number
  shareCount?: number
  viewCount?: number
  publishTime?: string
  createTime?: string
  [key: string]: any
}

interface Pagination {
  page: number
  limit: number
}

const router = useRouter()
const appContext = useGlobalProperties()

const articleList = ref<Article[]>([])
const pagination = ref<Pagination>({ page: 1, limit: 10 })
const totalCount = ref<number>(0)
const isLoading = ref<boolean>(false)

const selectedCategoryId = ref<number | string | undefined>(undefined)
const categoryOptions = ref<any[]>([])

// 获取分类列表
const fetchCategoryList = async (): Promise<void> => {
  try {
    const response = await appContext?.$http.get('/categories')
    const apiData = response.data


    // 根据实际返回格式解析数据
    let categories: any[] = []

    if (apiData && apiData.data && apiData.data.list) {
      // 标准分页格式：{ code: 0, data: { list: [...] } }
      categories = Array.isArray(apiData.data.list) ? apiData.data.list : []
    } else if (apiData && apiData.data) {
      // 直接数据格式：{ code: 0, data: [...] }
      categories = Array.isArray(apiData.data) ? apiData.data : []
    } else if (Array.isArray(apiData)) {
      // 直接数组：[...]
      categories = apiData
    }


    categoryOptions.value = [
      { label: '全部分类', value: undefined },
      ...categories.map((cat: any) => ({
        label: cat.categoryName || cat.name,
        value: cat.id
      }))
    ]

  } catch (error) {
    console.error('获取分类列表失败:', error)
    categoryOptions.value = [{ label: '全部分类', value: undefined }]
  }
}

const handleCategoryChange = (categoryId: number | string | undefined) => {
  selectedCategoryId.value = categoryId
  pagination.value.page = 1
  fetchFavoriteList()
}

const fetchFavoriteList = async (): Promise<void> => {
  isLoading.value = true
  
  try {
    const params: any = {
      page: pagination.value.page,
      limit: pagination.value.limit,
      type: 'favorite',
      sort: sortType.value,
      order: sortOrder.value
    }

    // 如果选择了分类，添加到参数中
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }

    const response = await appContext?.$http({
      url: '/articles',
      method: 'get',
      params
    })

    const apiData = response.data.data
    articleList.value = apiData.list || []
    totalCount.value = apiData.totalCount || 0

  } catch (error) {
    console.error('获取收藏列表失败:', error)
    articleList.value = []
  } finally {
    isLoading.value = false
  }
}


const navigateToHome = (): void => {
  router.push('/index/home')
}

const navigateToArticleList = (): void => {
  router.push('/index/articleList')
}

const handlePageChange = (page: number): void => {
  pagination.value.page = page
  fetchFavoriteList()
}

const handlePageSizeChange = (size: number): void => {
  pagination.value.limit = size
  pagination.value.page = 1
  fetchFavoriteList()
}
// 在 script 中添加
const sortType = ref<string>('createTime')
const sortOrder = ref<'asc' | 'desc'>('desc')

const sortOptions = [
  { label: '最新发布', value: 'createTime', order: 'desc' },
  { label: '最早发布', value: 'createTime', order: 'asc' },
  { label: '最多浏览', value: 'viewCount', order: 'desc' },
  { label: '最多点赞', value: 'likeCount', order: 'desc' },
  { label: '最新发布', value: 'publishTime', order: 'desc' }
]

const handleSortChange = (value: string, option: any) => {
  sortType.value = value
  sortOrder.value = option.order as 'asc' | 'desc'
  pagination.value.page = 1
  fetchFavoriteList()
}
onMounted(() => {
  fetchCategoryList()
  fetchFavoriteList()
})
</script>
