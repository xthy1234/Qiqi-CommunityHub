<template>
  <PageContainer
    header-title="草稿箱"
    @back="handleBack"
  >
    <template #headerExtra>
      <n-button type="primary" @click="navigateToAdd">
        <Icon icon="ri:add-circle-line" style="margin-right: 4px;" />
        新建草稿
      </n-button>
    </template>

    <!-- 搜索和筛选区域 -->
    <n-card :bordered="false" class="filter-section">
      <n-space align="center">
        <n-input
          v-model:value="searchCriteria.title"
          placeholder="请输入文章标题"
          clearable
          style="width: 300px;"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <Icon icon="ri:search-line" />
          </template>
        </n-input>

        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <Icon icon="ri:search-eye-line" />
          </template>
          搜索
        </n-button>

        <n-button type="success" @click="handleCreateDraft">
          <template #icon>
            <Icon icon="ri:add-line" />
          </template>
          新建草稿
        </n-button>
      </n-space>
    </n-card>

    <!-- 草稿列表 -->
    <div class="draft-grid">
      <n-spin :show="isLoading">
        <template v-if="draftList.length > 0">
          <DraftCard
            v-for="(item, index) in draftList"
            :key="item.id || index"
            :draft="item"
            @select="handleSingleSelect"
            @click="viewDraftDetail"
            @edit="editDraft"
            @submit="submitDraft"
            @delete="deleteDraft"
          />
        </template>

        <n-empty
          v-else-if="!isLoading"
          description="暂无草稿，快去创建一篇文章吧~"
          size="large"
        />
      </n-spin>
    </div>

    <!-- 分页组件 -->
    <n-pagination
      v-model:page="pagination.page"
      :item-count="totalCount"
      :page-size="pagination.limit"
      show-size-picker
      :page-sizes="[20, 50, 100]"
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
import { NMessageProvider, useMessage, NButton, NInput, NSelect, NPagination, NImage, NTag, NCheckbox, NModal, NSkeleton } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handleImageError, getFullUrl } from '@/utils/userUtils'
import { articleAPI } from '@/api/article'
import ArticleGridList from '@/components/ArticleGridList.vue'
import PageContainer from '@/components/PageContainer.vue'
import PageHeader from '@/components/PageHeader.vue'
import DraftCard from '@/components/DraftCard.vue'

interface DraftItem {
  id: number | string
  title: string
  coverUrl?: string
  categoryId: number | string
  categoryName?: string
  createTime?: string
  isSelected?: boolean
  [key: string]: any
}

interface Pagination {
  page: number
  limit: number
}

interface SearchCriteria {
  title?: string
}

const appContext = useGlobalProperties()
const router = useRouter()
const route = useRoute()
const message = useMessage()

const draftList = ref<DraftItem[]>([])
const pagination = ref<Pagination>({ page: 1, limit: 20 })
const totalCount = ref<number>(0)
const isLoading = ref<boolean>(false)
const isFromPersonalCenter = ref<boolean>(false)
const searchCriteria = ref<SearchCriteria>({})
const selectedDrafts = ref<Array<number | string>>([])

// 模态框控制
const showSubmitModal = ref(false)
const showDeleteModal = ref(false)
const currentOperatingItem = ref<DraftItem | null>(null)

const isSelectAll = computed({
  get: () => draftList.value.length > 0 && draftList.value.every(item => item.isSelected),
  set: (value: boolean) => {
    draftList.value.forEach(item => {
      item.isSelected = value
    })
    updateSelectedDrafts()
  }
})

const handleBack = (): void => {
  router.back()
}

const navigateToAdd = (): void => {
  router.push('/index/article/editor')
}

const initializePage = (): void => {
  if (route.query.centerType) {
    isFromPersonalCenter.value = true
  }
  fetchDraftList()
}

const handleSearch = (): void => {
  pagination.value.page = 1
  fetchDraftList()
}

const handleCreateDraft = (): void => {
  router.push('/index/article/editor')
}

const handlePageSizeChange = (size: number): void => {
  pagination.value.limit = size
  pagination.value.page = 1
  fetchDraftList()
}

const handlePageChange = (page: number): void => {
  pagination.value.page = page
  fetchDraftList()
}

const handleSingleSelect = (item: DraftItem): void => {
  updateSelectedDrafts()
}

const updateSelectedDrafts = (): void => {
  selectedDrafts.value = draftList.value
    .filter(item => item.isSelected)
    .map(item => item.id)
}

const fetchDraftList = async (): Promise<void> => {
  isLoading.value = true

  try {
    const params: any = {
      ...pagination.value,
      type: 'draft'  // 添加 type 参数，指定获取草稿
    }

    if (searchCriteria.value.title && searchCriteria.value.title.trim()) {
      params.title = `%${searchCriteria.value.title.trim()}%`
    }

    const response = await articleAPI.getList(params)

    draftList.value = (response.data.data?.list || []).map((item: DraftItem) => ({
      ...item,
      isSelected: false
    }))
    totalCount.value = Number(response.data.data?.total || 0)

  } catch (error) {
    console.error('获取草稿列表失败:', error)
    message.error('获取草稿列表失败，请重试')
  } finally {
    isLoading.value = false
  }
}

const viewDraftDetail = (item: DraftItem): void => {
  editDraft(item)
}

const editDraft = (item: DraftItem): void => {
  router.push({
    path: '/index/article/editor',
    query: { draftId: item.id }
  })
}

const submitDraft = (item: DraftItem): void => {
  currentOperatingItem.value = item
  showSubmitModal.value = true
}

const deleteDraft = (item: DraftItem): void => {
  currentOperatingItem.value = item
  showDeleteModal.value = true
}

const handleSubmitBatch = (): void => {
  if (selectedDrafts.value.length === 0) {
    message.warning('请选择要提交的草稿')
    return
  }

  currentOperatingItem.value = null
  showSubmitModal.value = true
}

const handleDeleteBatch = (): void => {
  if (selectedDrafts.value.length === 0) {
    message.warning('请选择要删除的草稿')
    return
  }

  currentOperatingItem.value = null
  showDeleteModal.value = true
}

// 确认提交审核
const confirmSubmit = async () => {
  try {
    if (currentOperatingItem.value) {
      // 单个提交
      const response = await articleAPI.submitDraft(currentOperatingItem.value.id)
      if (response.data.code === 200 || response.data.success) {
        message.success('提交审核成功')
        fetchDraftList()
      } else {
        message.error(response.data.message || '提交审核失败')
      }
    } else {
      // 批量提交
      const promises = selectedDrafts.value.map(id => articleAPI.submitDraft(id))
      await Promise.all(promises)
      message.success('批量提交审核成功')
      selectedDrafts.value = []
      fetchDraftList()
    }
  } catch (error: any) {
    console.error('提交审核失败:', error)
    message.error(error.response?.data?.message || '提交审核失败，请重试')
  } finally {
    showSubmitModal.value = false
    currentOperatingItem.value = null
  }
}

// 确认删除
const confirmDelete = async () => {
  try {
    if (currentOperatingItem.value) {
      // 单个删除
      const response = await articleAPI.deleteDraft(currentOperatingItem.value.id)
      if (response.data.code === 200 || response.data.success) {
        message.success('删除成功')
        fetchDraftList()
      } else {
        message.error(response.data.message || '删除失败')
      }
    } else {
      // 批量删除
      const response = await articleAPI.batchDeleteDrafts(selectedDrafts.value as number[])
      if (response.data.code === 200 || response.data.success) {
        message.success('批量删除成功')
        selectedDrafts.value = []
        fetchDraftList()
      } else {
        message.error(response.data.message || '批量删除失败')
      }
    }
  } catch (error: any) {
    console.error('删除失败:', error)
    message.error(error.response?.data?.message || '删除失败，请重试')
  } finally {
    showDeleteModal.value = false
    currentOperatingItem.value = null
  }
}

const getCoverImageUrl = (coverUrl?: string): string => {
  if (!coverUrl) return getDefaultCoverImage()

  const baseUrl = appContext?.$config?.url || ''

  if (coverUrl.startsWith('http')) {
    return coverUrl
  }

  return `${baseUrl}/${coverUrl.split(',')[0]}`
}

const getDefaultCoverImage = () => {
  return '/placeholder.svg'
}

const formatDate = (dateStr?: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const skeletonDraft = {
  id: 'skeleton',
  title: '',
  coverUrl: '',
  categoryName: '',
  createTime: '',
  isSelected: false
}

onMounted(() => {
  initializePage()
})
</script>

<style lang="scss" scoped>
.draft-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
</style>
