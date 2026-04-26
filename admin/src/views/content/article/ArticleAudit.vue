<template>
  <PageContainer
    header-title="文章审核"
  >
    <!-- 统计概览卡片 -->
    <div class="stats-cards">
      <NCard :title="String(dashboardStats.pendingCount || 0)" size="small">
        <template #header-extra>
          <NTag type="warning">待审核</NTag>
        </template>
        <div class="stat-content">
          <Icon icon="ri:file-list-3-line" size="24" />
          <span>待审核文章</span>
        </div>
      </NCard>
      
      <NCard :title="String(dashboardStats.todayApproved || 0)" size="small">
        <template #header-extra>
          <NTag type="success">今日通过</NTag>
        </template>
        <div class="stat-content">
          <Icon icon="ri:checkbox-circle-line" size="24" />
          <span>今日审核通过</span>
        </div>
      </NCard>
      
      <NCard :title="String(dashboardStats.todayRejected || 0)" size="small">
        <template #header-extra>
          <NTag type="error">今日拒绝</NTag>
        </template>
        <div class="stat-content">
          <Icon icon="ri:close-circle-line" size="24" />
          <span>今日审核拒绝</span>
        </div>
      </NCard>
      
      <NCard :title="String(dashboardStats.totalCount || 0)" size="small">
        <template #header-extra>
          <NTag type="info">总文章数</NTag>
        </template>
        <div class="stat-content">
          <Icon icon="ri:article-line" size="24" />
          <span>平台总文章</span>
        </div>
      </NCard>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
        v-model:value="searchForm.keyword"
        placeholder="请输入文章标题或作者"
        clearable
        style="width: 250px"
      />
      
      <NSelect
        v-model:value="searchForm.categoryId"
        placeholder="选择分类"
        :options="categoryOptions"
        clearable
        style="width: 150px"
      />
      
      <NDatePicker
        v-model:value="dateRange"
        type="daterange"
        placeholder="选择投稿日期范围"
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
      
      <NButton @click="loadData">
        <template #icon>
          <Icon icon="ri:refresh-line" />
        </template>
        刷新
      </NButton>
    </div>

    <!-- 批量操作提示 -->
    <NAlert
      v-if="checkedRowKeys.length > 0"
      type="info"
      style="margin-bottom: 16px;"
    >
      <template #header>
        <NSpace align="center">
          <span>已选择 {{ checkedRowKeys.length }} 篇文章</span>
          <NButton 
            type="success" 
            size="small"
            @click="handleBatchAudit(1)"
          >
            批量通过
          </NButton>
          <NButton 
            type="error" 
            size="small"
            @click="handleBatchAudit(2)"
          >
            批量拒绝
          </NButton>
          <NButton 
            size="small"
            @click="checkedRowKeys = []"
          >
            取消选择
          </NButton>
        </NSpace>
      </template>
    </NAlert>

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

    <!-- 审核对话框 -->
    <NModal
      v-model:show="auditDialogVisible"
      preset="card"
      title="文章审核"
      style="width: 600px;"
    >
      <NForm>
        <NFormItem label="审核结果">
          <NRadioGroup v-model:value="auditForm.status">
            <NRadio :value="1">
              <NTag type="success">通过</NTag>
            </NRadio>
            <NRadio :value="2">
              <NTag type="error">拒绝</NTag>
            </NRadio>
          </NRadioGroup>
        </NFormItem>
        
        <NFormItem label="审核意见">
          <NInput
            v-model:value="auditForm.reply"
            type="textarea"
            :placeholder="auditForm.status === 1 ? '请输入审核意见（可选）' : '请输入拒绝原因（必填）'"
            :rows="4"
            show-count
            maxlength="500"
          />
        </NFormItem>

        <NAlert
          v-if="auditForm.status === 2"
          type="error"
          title="注意"
        >
          拒绝审核后，作者将无法获得积分，且文章不会被公开展示。请谨慎操作！
        </NAlert>
      </NForm>
      
      <template #footer>
        <NSpace justify="end">
          <NButton @click="auditDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="confirmAudit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 审核历史对话框 -->
    <NModal
      v-model:show="historyDialogVisible"
      preset="card"
      title="审核历史"
      style="width: 600px; max-height: 70vh; overflow-y: auto;"
    >
      <NTimeline v-if="auditHistory.length > 0">
        <NTimelineItem
          v-for="(item, index) in auditHistory"
          :key="index"
          :type="getAuditStatusType(item.newStatus)"
          :title="getAuditStatusLabel(item.newStatus)"
          :content="`审核员：${item.auditorNickname}`"
          :time="item.auditTime"
        >
          <NCard size="small" style="margin-top: 8px;">
            <p v-if="item.reason"><strong>原因：</strong>{{ item.reason }}</p>
            <p><strong>原状态：</strong>{{ getAuditStatusLabel(item.oldStatus) }}</p>
          </NCard>
        </NTimelineItem>
      </NTimeline>
      <NEmpty v-else description="暂无审核记录" />
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { 
  NButton, NTag, NSpace, NInput, NSelect, NDatePicker,
  NModal, NCard, NForm, NFormItem, NRadioGroup, NRadio,
  useMessage, useDialog, NAlert, NTimeline, NTimelineItem, NEmpty
} from 'naive-ui'
import PageContainer from 'src/components/layout/PageContainer.vue'
import { adminUserApi } from '@/api/adminUser'
import articleApi from '@/api/article'
import statsApi, { type AuditOverviewStats } from '@/api/stats'
import { normalizeFileUrl } from '@/utils/fileUrl'
import {useGlobalProperties} from "@/utils/globalProperties";

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
  publishTime?: string
}

interface DashboardStats {
  pendingCount: number
  todayApproved: number
  todayRejected: number
  totalCount: number
}

const message = useMessage()
const dialog = useDialog()
const appContext = useGlobalProperties()

const searchForm = ref({
  keyword: '',
  categoryId: null as number | null,
})

const dateRange = ref<[number, number] | null>(null)
const searchStartDate = ref<string>('')
const searchEndDate = ref<string>('')

const categoryOptions = ref<any[]>([])

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

const dashboardStats = ref<AuditOverviewStats>({
  pendingCount: 0,
  todayApproved: 0,
  todayRejected: 0,
  totalCount: 0
})

const auditDialogVisible = ref(false)
const auditForm = ref({
  status: 1,
  reply: ''
})

const historyDialogVisible = ref(false)
const currentArticleId = ref<number | null>(null)
const auditHistory = ref<any[]>([])

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
    title: '文章信息',
    key: 'title',
    width: 350,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h('div', { style: { display: 'flex', gap: '12px', alignItems: 'center' } }, {
        default: () => [
          rowData.coverUrl ? h('img', {
            src: rowData.coverUrl,
            style: { width: '60px', height: '40px', objectFit: 'cover', borderRadius: '4px' }
          }) : null,
          h('div', { style: { flex: 1 } }, {
            default: () => [
              h('div', { style: { fontWeight: 'bold', marginBottom: '4px' } }, rowData.title),
              rowData.summary ? h('div', { style: { fontSize: '12px', color: '#999' } },
                rowData.summary.length > 50 ? rowData.summary.substring(0, 50) + '...' : rowData.summary
              ) : null
            ]
          })
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
            style: { width: '28px', height: '28px', borderRadius: '50%' }
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
    width: 100
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
    title: '数据',
    key: 'stats',
    width: 120,
    render: (row) => {
      const rowData = row as unknown as ArticleItem
      return h('div', { style: { fontSize: '12px' } }, {
        default: () => [
          h('span', { style: { marginRight: '8px' } }, `👁 ${rowData.viewCount || 0}`),
          h('span', { style: { marginRight: '8px' } }, `👍 ${rowData.likeCount || 0}`),
          h('span', {}, `💬 ${rowData.commentCount || 0}`)
        ]
      })
    }
  },
  {
    title: '投稿时间',
    key: 'createTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
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
            default: () => '详情'
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
            type: 'info',
            text: true,
            onClick: () => handleViewHistory(rowData)
          }, {
            default: () => '历史'
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

const fetchCategories = async () => {
  try {
    const response = await adminUserApi.getUserList({ page: 1, limit: 100 })
    const categories = [
      { label: '技术', value: 1 },
      { label: '生活', value: 2 },
      { label: '学习', value: 3 },
      { label: '其他', value: 4 }
    ]
    categoryOptions.value = categories
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchDashboardStats = async () => {
  try {
    const res = await statsApi.getAuditOverview()

    if (res.code === 0 || res.code === 200) {
      dashboardStats.value = res.data
    } else {
      console.error('获取审核统计数据失败:', res.msg)
    }
  } catch (error) {
    console.error('获取审核统计数据失败:', error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      limit: pagination.pageSize,
      auditStatus: 0,  // 强制只查询待审核文章
      type: 'pending',
      ...searchForm.value
    }
    
    if (searchStartDate.value) {
      params.startDate = searchStartDate.value
    }
    if (searchEndDate.value) {
      params.endDate = searchEndDate.value
    }

    const response = await articleApi.getArticleList(params)

    if (response.code === 0 || response.code === 200) {
      tableData.value = (response.data.list || []).map((item: ArticleItem) => ({
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
    categoryId: null,
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
  window.open(`/articles/${row.id}`, '_blank')
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
        const response = await articleApi.batchAuditArticles({
          ids: [row.id],
          status: status,
          reply: ''
        })

        if (response.code === 0 || response.code === 200) {
          message.success('审核成功')
          loadData()
          fetchDashboardStats()
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

const confirmAudit = async () => {
  if (auditForm.value.status === 2 && !auditForm.value.reply) {
    message.warning('拒绝审核时请填写拒绝原因')
    return
  }

  try {
    const response = await articleApi.batchAuditArticles({
      ids: checkedRowKeys.value,
      status: auditForm.value.status,
      reply: auditForm.value.reply
    })

    if (response.code === 0 || response.code === 200) {
      message.success('批量审核成功')
      auditDialogVisible.value = false
      checkedRowKeys.value = []
      loadData()
      fetchDashboardStats()
    } else {
      message.error(response.msg || '批量审核失败')
    }
  } catch (error: any) {
    console.error('批量审核失败:', error)
    message.error(error.response?.data?.msg || '批量审核失败')
  }
}

const handleViewHistory = async (row: ArticleItem) => {
  currentArticleId.value = row.id
  try {
    const response = await articleApi.getArticleSuggestions(row.id)
    auditHistory.value = response.data || []
    historyDialogVisible.value = true
  } catch (error: any) {
    console.error('获取审核历史失败:', error)
    message.error('获取审核历史失败')
  }
}

const onChecked = (keys: number[]) => {
  checkedRowKeys.value = keys
}

onMounted(() => {
  fetchCategories()
  fetchDashboardStats()
  loadData()
})
</script>

<style lang="scss" scoped>
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  
  .stat-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #666;
    font-size: 14px;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
</style>
