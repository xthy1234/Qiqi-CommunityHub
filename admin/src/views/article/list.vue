<template>
  <div class="page-container">
    <NCard title="文章管理" size="large">
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
            type="success" 
            size="small"
            @click="handleBatchAudit(1)"
          >
            批量通过
          </NButton>
          <NButton 
            type="warning" 
            size="small"
            @click="handleBatchAudit(0)"
          >
            批量待审
          </NButton>
          <NButton 
            type="error" 
            size="small"
            @click="handleBatchAudit(2)"
          >
            批量拒绝
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
    </NCard>

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
  </div>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { 
  NButton, NTag, NSpace, NInput, NSelect, NDatePicker,
  NModal, NCard, NForm, NFormItem, NRadioGroup, NRadio,
  useMessage, useDialog
} from 'naive-ui'
import apiService from '@/api'

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
  isHot?: number
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
    width: 80
  },
  {
    title: '文章标题',
    key: 'title',
    width: 300,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h(NSpace, { align: 'center' }, {
        default: () => [
          h('span', { 
            style: { cursor: 'pointer', color: '#18a058' },
            onClick: () => handleViewDetail(rowData)
          }, rowData.title),
          rowData.isTop === 1 ? h(NTag, { type: 'error', size: 'small' }, { default: () => '置顶' }) : null,
          rowData.isHot === 1 ? h(NTag, { type: 'warning', size: 'small' }, { default: () => '热门' }) : null
        ]
      })
    }
  },
  {
    title: '作者',
    key: 'authorNickname',
    width: 120,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h(NSpace, { align: 'center' }, {
        default: () => [
          rowData.authorAvatar ? h('img', {
            src: rowData.authorAvatar,
            style: { width: '24px', height: '24px', borderRadius: '50%', marginRight: '8px' }
          }) : null,
          h('span', {}, {
            default: () => rowData.authorNickname || '-'
          })
        ]
      })
    }
  },
  {
    title: '分类',
    key: 'categoryName',
    width: 120
  },
  {
    title: '审核状态',
    key: 'auditStatus',
    width: 100,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h(NTag, {
        type: getAuditStatusType(rowData.auditStatus)
      }, {
        default: () => getAuditStatusLabel(rowData.auditStatus)
      })
    }
  },
  {
    title: '浏览/点赞/评论',
    key: 'stats',
    width: 150,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h('span', {}, {
        default: () => `${rowData.viewCount || 0} / ${rowData.likeCount || 0} / ${rowData.commentCount || 0}`
      })
    }
  },
  {
    title: '发布时间',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    fixed: 'right',
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h(NSpace, {}, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'primary',
            text: true,
            onClick: () => handleViewDetail(rowData)
          }, {
            default: () => '查看'
          }),
          rowData.auditStatus === 0 ? h(NButton, {
            size: 'small',
            type: 'success',
            text: true,
            onClick: () => handleSingleAudit(rowData, 1)
          }, {
            default: () => '通过'
          }) : null,
          rowData.auditStatus === 0 ? h(NButton, {
            size: 'small',
            type: 'error',
            text: true,
            onClick: () => handleSingleAudit(rowData, 2)
          }, {
            default: () => '拒绝'
          }) : null,
          h(NButton, {
            size: 'small',
            type: 'error',
            text: true,
            onClick: () => handleDelete(rowData)
          }, {
            default: () => '删除'
          })
        ].filter(Boolean)
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

    const response = await apiService.article.getArticleList(params)

    if (response.data.code === 0 || response.data.code === 200) {
      tableData.value = response.data.data.list || []
      pagination.itemCount = response.data.data.totalCount || 0
    } else {
      message.error(response.data.msg || '获取文章列表失败')
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
        const response = await apiService.article.batchAuditArticles({
          ids: [row.id],
          status: status,
          reply: ''
        })

        if (response.data.code === 0 || response.data.code === 200) {
          message.success('审核成功')
          loadData()
        } else {
          message.error(response.data.msg || '审核失败')
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
    const response = await apiService.article.batchAuditArticles({
      ids: checkedRowKeys.value,
      status: auditForm.value.status,
      reply: auditForm.value.reply
    })

    if (response.data.code === 0 || response.data.code === 200) {
      message.success('批量审核成功')
      auditDialogVisible.value = false
      checkedRowKeys.value = []
      loadData()
    } else {
      message.error(response.data.msg || '批量审核失败')
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
        const response = await apiService.article.deleteArticle(row.id)

        if (response.data.code === 0 || response.data.code === 200) {
          message.success('删除成功')
          loadData()
        } else {
          message.error(response.data.msg || '删除失败')
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
        const response = await apiService.article.batchDeleteArticles(checkedRowKeys.value)

        if (response.data.code === 0 || response.data.code === 200) {
          message.success('批量删除成功')
          checkedRowKeys.value = []
          loadData()
        } else {
          message.error(response.data.msg || '批量删除失败')
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

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 0;
}

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
