<template>
  <PageContainer header-title="文章管理">
    <template #header-extra>
      <NSpace>
        <NButton @click="handleRefresh">
          <template #icon>
            <Icon icon="ri:refresh-line" />
          </template>
          刷新
        </NButton>
      </NSpace>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
        v-model:value="searchForm.keyword"
        placeholder="请输入文章标题或内容"
        clearable
        style="width: 250px"
      />

      <NSelect
        v-model:value="searchForm.type"
        placeholder="文章类型"
        :options="typeOptions"
        clearable
        style="width: 150px"
      />

      <NSelect
        v-model:value="searchForm.auditStatus"
        placeholder="审核状态"
        :options="auditStatusOptions"
        clearable
        style="width: 150px"
      />

      <NDatePicker
        v-model:value="dateRange"
        type="daterange"
        placeholder="选择日期范围"
        clearable
        style="width: 240px"
        @update:value="handleDateChange"
      />

      <NButton type="primary" @click="handleSearch">
        <template #icon>
          <Icon icon="ri:search-line" />
        </template>
        搜索
      </NButton>

      <NButton @click="handleReset">重置</NButton>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions" v-if="checkedRowKeys.length > 0">
      <NSpace>
        <span>已选择 {{ checkedRowKeys.length }} 篇文章</span>
        <NButton
          type="warning"
          size="small"
          @click="handleBatchSetFeatured(1)"
        >
          批量推荐
        </NButton>
        <NButton
          type="error"
          size="small"
          @click="handleBatchSetTop(1)"
        >
          批量置顶
        </NButton>
        <NButton
          type="default"
          size="small"
          @click="handleBatchResetOperation"
        >
          取消推荐/置顶
        </NButton>
        <NButton
          type="error"
          size="small"
          @click="handleBatchDelete"
        >
          批量删除
        </NButton>
        <NButton
          size="small"
          @click="checkedRowKeys = []"
        >
          取消选择
        </NButton>
      </NSpace>
    </div>

    <!-- 表格 -->
    <NDataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="pagination"
      :remote="true"
      :row-key="(row) => row.id"
      :checked-row-keys="checkedRowKeys"
      @update:checked-row-keys="onChecked"
      striped
    />
    <!-- 文章详情对话框 -->
    <NModal
      v-model:show="detailVisible"
      preset="card"
      title="文章详情"
      style="width: 800px; max-height: 80vh; overflow-y: auto;"
    >
      <div v-if="currentArticle" class="article-detail">
        <div class="detail-header">
          <h2>{{ currentArticle.title }}</h2>
          <NSpace>
            <NTag :type="getAuditStatusType(currentArticle.auditStatus)">
              {{ getAuditStatusLabel(currentArticle.auditStatus) }}
            </NTag>
            <NTag v-if="currentArticle.categoryName">
              {{ currentArticle.categoryName }}
            </NTag>
          </NSpace>
        </div>
        
        <div class="detail-info">
          <p><strong>作者：</strong>{{ currentArticle.authorNickname || '-' }}</p>
          <p v-if="currentArticle.publishTime"><strong>发布时间：</strong>{{ currentArticle.publishTime }}</p>
          <p v-if="currentArticle.createTime"><strong>创建时间：</strong>{{ currentArticle.createTime }}</p>
          <p v-if="currentArticle.updateTime"><strong>最后修改：</strong>{{ currentArticle.updateTime }}</p>
          <p><strong>浏览量：</strong>{{ currentArticle.viewCount || 0 }}</p>
          <p><strong>点赞数：</strong>{{ currentArticle.likeCount || 0 }}</p>
          <p><strong>点踩数：</strong>{{ currentArticle.dislikeCount || 0 }}</p>
        </div>
        
        <div class="detail-content" v-html="currentArticle.content"></div>
      </div>
    </NModal>

    <!-- 批量审核对话框 -->
    <NModal
      v-model:show="auditDialogVisible"
      preset="card"
      title="批量审核"
      style="width: 500px;"
    >
      <NForm>
        <NFormItem label="审核结果">
          <NRadioGroup v-model:value="auditForm.status">
            <NRadio :value="1">通过</NRadio>
            <NRadio :value="2">拒绝</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="审核意见">
          <NInput
            v-model:value="auditForm.reply"
            type="textarea"
            placeholder="请输入审核意见（可选）"
            :rows="3"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="auditDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="confirmBatchAudit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { 
  NButton, NTag, NSpace, NInput, NSelect, NDatePicker,
  NModal, NForm, NFormItem, NRadioGroup, NRadio,
  useMessage, useDialog
} from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import articleApiService from '@/api/article'
import { normalizeFileUrl } from '@/utils/fileUrl'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

interface PageResponse<T> {
  list: T[]
  totalCount: number
  pageSize: number
  totalPage: number
  currPage: number
}

interface ArticleItem {
  id: number
  title: string
  content: string
  summary?: string
  coverUrl?: string
  authorId: number
  authorNickname?: string
  authorAvatar?: string
  categoryId?: number
  categoryName?: string
  auditStatus: number | string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  createTime: string
  updateTime?: string
  publishTime?: string
  isTop?: number
  topLevel?: number
  isFeatured?: number
  featuredLevel?: number
}

const FEATURED_LEVEL_CONFIG = {
  0: { label: '普通', type: 'default' as const },
  1: { label: '推荐', type: 'success' as const },
  2: { label: '热门', type: 'warning' as const }
}

const TOP_LEVEL_CONFIG = {
  0: { label: '不置顶', type: 'default' as const },
  1: { label: '普通置顶', type: 'info' as const },
  2: { label: '重要置顶', type: 'error' as const }
}

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  keyword: '',
  type: null as string | null,
  auditStatus: null as number | null,
  categoryId: null as number | null
})

const dateRange = ref<[number, number] | null>(null)
const searchStartDate = ref<string>('')
const searchEndDate = ref<string>('')

const typeOptions = [
  { label: '全部文章', value: 'all' },
  { label: '待审核', value: 'pending' }
]

const auditStatusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 }
]

const loading = ref(false)
const tableData = ref<ArticleItem[]>([])
const checkedRowKeys = ref<number[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  }
})

const detailVisible = ref(false)
const currentArticle = ref<ArticleItem | null>(null)
const auditDialogVisible = ref(false)
const auditForm = ref({
  status: 1,
  reply: ''
})

const columns: DataTableColumns = [
  {
    type: 'selection',
    fixed: 'left',
    width: 50
  },
  {
    title: 'ID',
    key: 'id',
    width: 70
  },
  {
    title: '封面',
    key: 'coverUrl',
    width: 80,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      if (rowData.coverUrl) {
        return h('img', {
          src: rowData.coverUrl,
          style: { width: '60px', height: '40px', objectFit: 'cover', borderRadius: '4px' }
        })
      }
      return h('span', { style: { color: '#999', fontSize: '12px' } }, '无')
    }
  },
  {
    title: '文章标题',
    key: 'title',
    minWidth: 200,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h('div', {
        style: { cursor: 'pointer', color: '#18a058', fontWeight: '500' },
        onClick: () => handleViewDetail(rowData)
      }, rowData.title)
    }
  },
  {
    title: '作者',
    key: 'authorNickname',
    width: 100,
    ellipsis: { tooltip: true },
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h('span', {}, rowData.authorNickname || '-')
    }
  },
  {
    title: '分类',
    key: 'categoryName',
    width: 100,
    ellipsis: { tooltip: true }
  },
  {
    title: '运营状态',
    key: 'operationStatus',
    width: 180,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      const featuredLevel = rowData.featuredLevel ?? 0
      const topLevel = rowData.topLevel ?? 0

      return h(NSpace, { size: 'small', wrap: true }, {
        default: () => [
          // 推荐状态标签（点击切换）
          h(NTag, {
            size: 'small',
            type: featuredLevel === 2 ? 'warning' : (featuredLevel === 1 ? 'success' : 'default'),
            bordered: false,
            style: { cursor: 'pointer' },
            onClick: () => handleToggleFeatured(rowData)
          }, {
            default: () => featuredLevel === 2 ? '热门' : (featuredLevel === 1 ? '推荐' : '普通')
          }),

          // 置顶状态标签（点击切换）
          h(NTag, {
            size: 'small',
            type: topLevel === 2 ? 'error' : (topLevel === 1 ? 'info' : 'default'),
            bordered: false,
            style: { cursor: 'pointer' },
            onClick: () => handleToggleTop(rowData)
          }, {
            default: () => topLevel === 2 ? '重要置顶' : (topLevel === 1 ? '置顶' : '不置顶')
          })
        ]
      })
    }
  },
  {
    title: '审核状态',
    key: 'auditStatus',
    width: 90,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h(NTag, {
        size: 'small',
        type: getAuditStatusType(rowData.auditStatus),
        bordered: false
      }, {
        default: () => getAuditStatusLabel(rowData.auditStatus)
      })
    }
  },
  {
    title: '数据',
    key: 'stats',
    width: 120,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h('div', { style: { fontSize: '12px', color: '#666' } }, {
        default: () => `阅:${rowData.viewCount || 0} 赞:${rowData.likeCount || 0}`
      })
    }
  },
  {
    title: '发布时间',
    key: 'createTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, {
            size: 'tiny',
            type: 'primary',
            text: true,
            onClick: () => handleViewDetail(rowData)
          }, {
            default: () => '详情'
          }),
          h(NButton, {
            size: 'tiny',
            type: 'error',
            text: true,
            onClick: () => handleDelete(rowData)
          }, {
            default: () => '删除'
          })
        ]
      })
    }
  }
]

function getAuditStatusType(status: number | string): 'default' | 'info' | 'success' | 'warning' | 'error' {
  const typeMap: Record<string, any> = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'error',
    0: 'warning',
    1: 'success',
    2: 'error'
  }
  return typeMap[status] || 'default'
}

function getAuditStatusLabel(status: number | string): string {
  const labelMap: Record<string | number, string> = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝',
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return labelMap[status] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      limit: pagination.pageSize,
      ...searchForm.value
    }
    
    if (searchStartDate.value) {
      params.startDate = searchStartDate.value
    }
    if (searchEndDate.value) {
      params.endDate = searchEndDate.value
    }

    const response = await articleApiService.getArticleList(params)

    if (response.code === 0 || response.code === 200) {
      const list = response.data.list || []
      tableData.value = list.map((item: ArticleItem) => ({
        ...item,
        coverUrl: item.coverUrl ? normalizeFileUrl(item.coverUrl) : '',
        authorAvatar: item.authorAvatar ? normalizeFileUrl(item.authorAvatar) : ''
      }))
      pagination.itemCount = response.data.totalCount || 0
    } else {
      message.error(response.msg || '获取文章列表失败')
    }
  } catch (error: any) {
    console.error('获取文章列表失败:', error)
    message.error(error.response?.data?.msg || '获取文章列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    type: null,
    auditStatus: null,
    categoryId: null
  }
  dateRange.value = null
  searchStartDate.value = ''
  searchEndDate.value = ''
  pagination.page = 1
  loadData()
}

const handleDateChange = (value: [number, number] | null) => {
  if (value && Array.isArray(value) && value.length === 2) {
    const startDate = new Date(value[0])
    const endDate = new Date(value[1])
    searchStartDate.value = formatDate(startDate)
    searchEndDate.value = formatDate(endDate)
  } else {
    searchStartDate.value = ''
    searchEndDate.value = ''
  }
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const handleViewDetail = (row: ArticleItem) => {
  currentArticle.value = { ...row }
  detailVisible.value = true
}

const handleSingleAudit = async (row: ArticleItem, status: number) => {
  const statusText = status === 1 ? '通过' : '拒绝'
  
  dialog.warning({
    title: '确认审核',
    content: `确定要${statusText}这篇文章吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await articleApiService.batchAuditArticles({
          ids: [row.id],
          status: status,
          reply: ''
        })

        if (response.code === 0 || response.code === 200) {
          message.success('审核成功')
          loadData()
        } else {
          message.error(response.msg || '审核失败')
        }
      } catch (error: any) {
        console.error('审核失败:', error)
        message.error(error.response?.data?.msg || '审核失败')
      }
    }
  })
}

const handleBatchAudit = (status: number) => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择文章')
    return
  }
  
  auditForm.value.status = status
  auditForm.value.reply = ''
  auditDialogVisible.value = true
}

const confirmBatchAudit = async () => {
  try {
    const response = await articleApiService.batchAuditArticles({
      ids: checkedRowKeys.value,
      status: auditForm.value.status,
      reply: auditForm.value.reply
    })

    if (response.code === 0 || response.code === 200) {
      message.success('批量审核成功')
      auditDialogVisible.value = false
      checkedRowKeys.value = []
      loadData()
    } else {
      message.error(response.msg || '批量审核失败')
    }
  } catch (error: any) {
    console.error('批量审核失败:', error)
    message.error(error.response?.data?.msg || '批量审核失败')
  }
}

const handleDelete = async (row: ArticleItem) => {
  dialog.warning({
    title: '警告',
    content: `确定要删除文章 "${row.title}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await articleApiService.deleteArticle(row.id)

        if (response.code === 0 || response.code === 200) {
          message.success('删除成功')
          loadData()
        } else {
          message.error(response.msg || '删除失败')
        }
      } catch (error: any) {
        console.error('删除失败:', error)
        message.error(error.response?.data?.msg || '删除失败')
      }
    }
  })
}

const handleBatchDelete = async () => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择文章')
    return
  }

  dialog.warning({
    title: '警告',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 篇文章吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await articleApiService.batchDeleteArticles(checkedRowKeys.value)

        if (response.code === 0 || response.code === 200) {
          message.success('批量删除成功')
          checkedRowKeys.value = []
          loadData()
        } else {
          message.error(response.msg || '批量删除失败')
        }
      } catch (error: any) {
        console.error('批量删除失败:', error)
        message.error(error.response?.data?.msg || '批量删除失败')
      }
    }
  })
}

const handleRefresh = () => {
  loadData()
}

const onChecked = (keys: number[]) => {
  checkedRowKeys.value = keys
}

/**
 * 切换推荐状态（循环：普通 -> 推荐 -> 热门 -> 普通）
 */
const handleToggleFeatured = async (row: ArticleItem) => {
  const currentLevel = row.featuredLevel ?? 0
  // 循环逻辑：0 -> 1 -> 2 -> 0
  const nextLevel = currentLevel >= 2 ? 0 : currentLevel + 1

  try {
    const response = await articleApiService.setArticleFeatured(
      row.id,
      nextLevel > 0,
      nextLevel
    )

    if (response.code === 0 || response.code === 200) {
      message.success(`已设置为${nextLevel === 1 ? '推荐' : (nextLevel === 2 ? '热门' : '普通')}`)
      row.featuredLevel = nextLevel
      row.isFeatured = nextLevel > 0 ? 1 : 0
    } else {
      message.error(response.msg || '设置失败')
    }
  } catch (error: any) {
    console.error('设置推荐状态失败:', error)
    message.error(error.response?.data?.msg || '设置失败')
  }
}

/**
 * 切换置顶状态（循环：不置顶 -> 普通置顶 -> 重要置顶 -> 不置顶）
 */
const handleToggleTop = async (row: ArticleItem) => {
  const currentLevel = row.topLevel ?? 0
  // 循环逻辑：0 -> 1 -> 2 -> 0
  const nextLevel = currentLevel >= 2 ? 0 : currentLevel + 1

  try {
    const response = await articleApiService.setArticleTop(
      row.id,
      nextLevel > 0,
      nextLevel
    )

    if (response.code === 0 || response.code === 200) {
      message.success(`已设置为${nextLevel === 1 ? '普通置顶' : (nextLevel === 2 ? '重要置顶' : '不置顶')}`)
      row.topLevel = nextLevel
      row.isTop = nextLevel > 0 ? 1 : 0
    } else {
      message.error(response.msg || '设置失败')
    }
  } catch (error: any) {
    console.error('设置置顶状态失败:', error)
    message.error(error.response?.data?.msg || '设置失败')
  }
}

/**
 * 批量设置推荐状态
 */
const handleBatchSetFeatured = async (level: number) => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择文章')
    return
  }

  dialog.warning({
    title: '批量设置推荐',
    content: `确定将选中的 ${checkedRowKeys.value.length} 篇文章设置为${level === 1 ? '推荐' : '热门'}吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 注意：由于后端没有提供批量接口，这里需要循环调用
        const promises = checkedRowKeys.value.map(id =>
          articleApiService.setArticleFeatured(id, true, level)
        )

        const results = await Promise.all(promises)
        const successCount = results.filter(r => r.code === 0 || r.code === 200).length

        message.success(`成功设置 ${successCount} 篇文章`)
        checkedRowKeys.value = []
        loadData()
      } catch (error: any) {
        console.error('批量设置失败:', error)
        message.error('批量设置失败')
      }
    }
  })
}

/**
 * 批量设置置顶状态
 */
const handleBatchSetTop = async (level: number) => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择文章')
    return
  }

  dialog.warning({
    title: '批量设置置顶',
    content: `确定将选中的 ${checkedRowKeys.value.length} 篇文章设置为${level === 1 ? '普通置顶' : '重要置顶'}吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const promises = checkedRowKeys.value.map(id =>
          articleApiService.setArticleTop(id, true, level)
        )

        const results = await Promise.all(promises)
        const successCount = results.filter(r => r.code === 0 || r.code === 200).length

        message.success(`成功设置 ${successCount} 篇文章`)
        checkedRowKeys.value = []
        loadData()
      } catch (error: any) {
        console.error('批量设置失败:', error)
        message.error('批量设置失败')
      }
    }
  })
}

/**
 * 批量取消推荐和置顶
 */
const handleBatchResetOperation = async () => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择文章')
    return
  }

  dialog.warning({
    title: '批量取消运营状态',
    content: `确定取消选中文章的推荐和置顶状态吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const promises = checkedRowKeys.value.flatMap(id => [
          articleApiService.setArticleFeatured(id, false, 0),
          articleApiService.setArticleTop(id, false, 0)
        ])

        const results = await Promise.all(promises)
        const successCount = results.filter(r => r.code === 0 || r.code === 200).length / 2

        message.success(`成功重置 ${successCount} 篇文章`)
        checkedRowKeys.value = []
        loadData()
      } catch (error: any) {
        console.error('批量重置失败:', error)
        message.error('批量重置失败')
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.batch-actions {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;
}

.article-detail {
  .detail-header {
    margin-bottom: 20px;
    
    h2 {
      margin: 0 0 12px 0;
      font-size: 20px;
      color: #333;
    }
  }
  
  .detail-info {
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;
    margin-bottom: 20px;
    
    p {
      margin: 8px 0;
      font-size: 14px;
      color: #606266;
      
      strong {
        color: #303133;
      }
    }
  }
  
  .detail-content {
    line-height: 1.8;
    color: #333;
    
    :deep(img) {
      max-width: 100%;
      height: auto;
    }
  }
}
</style>
