<!-- src/views/comment/ArticleList.vue -->
<template>
  <PageContainer header-title="评论管理">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
          v-model:value="searchForm.keyword"
          placeholder="请输入评论内容或用户名"
          clearable
          style="width: 250px"
      />

      <NSelect
          v-model:value="searchForm.status"
          placeholder="选择状态"
          :options="statusOptions"
          clearable
          style="width: 120px"
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
          <span>已选择 {{ checkedRowKeys.length }} 条评论</span>
          <NButton
              type="warning"
              size="small"
              @click="handleBatchUpdateStatus(1)"
          >
            批量禁用
          </NButton>
          <NButton
              type="success"
              size="small"
              @click="handleBatchUpdateStatus(0)"
          >
            批量启用
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

    <!-- 查看评论详情对话框 -->
    <NModal
        v-model:show="detailDialogVisible"
        preset="card"
        title="评论详情"
        style="width: 600px;"
    >
      <NDescriptions bordered :column="1">
        <NDescriptionsItem label="ID">{{ currentComment?.id }}</NDescriptionsItem>
        <NDescriptionsItem label="用户信息">
          <NSpace align="center">
            <NAvatar
                v-if="currentComment?.userAvatar"
                :src="currentComment.userAvatar"
                size="small"
                round
            />
            <span>{{ currentComment?.userNickname || '-' }}</span>
          </NSpace>
        </NDescriptionsItem>
        <NDescriptionsItem label="评论内容">
          <NCard size="small">{{ currentComment?.content }}</NCard>
        </NDescriptionsItem>
        <NDescriptionsItem label="关联内容">
          <NTag v-if="currentComment?.contentTitle" type="info">
            {{ currentComment.contentTitle }}
          </NTag>
          <span v-else>-</span>
        </NDescriptionsItem>
        <NDescriptionsItem label="状态">
          <NTag :type="getStatusLabel(currentComment?.status).type">
            {{ getStatusLabel(currentComment?.status).label }}
          </NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="IP 属地">{{ currentComment?.ipLocation || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="创建时间">{{ currentComment?.createTime }}</NDescriptionsItem>
        <NDescriptionsItem label="更新时间">{{ currentComment?.updateTime }}</NDescriptionsItem>
      </NDescriptions>
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton, NTag, NSpace, NInput, NSelect, NDatePicker,
  NModal, NCard, useMessage, useDialog, NAlert, NDescriptions, NDescriptionsItem,
  NAvatar
} from 'naive-ui'
import PageContainer from 'src/components/layout/PageContainer.vue'
import { commentApi } from '@/api/comment'
import { useGlobalProperties } from '@/utils/globalProperties'

interface CommentItem {
  id: number
  content: string
  userId: number
  userNickname?: string
  userAvatar?: string
  contentId: number
  contentTitle?: string
  parentId?: number
  status: number | string
  ipLocation?: string
  likeCount?: number
  createTime: string
  updateTime?: string
}

const message = useMessage()
const dialog = useDialog()
const appContext = useGlobalProperties()

const searchForm = ref({
  keyword: '',
  status: null as number | null,
  contentId: null as number | null,
  userId: null as number | null,
})

const dateRange = ref<[number, number] | null>(null)
const searchStartDate = ref<string>('')
const searchEndDate = ref<string>('')

const statusOptions = [
  { label: '正常', value: 0 },
  { label: '隐藏', value: 1 },
  { label: '已删除', value: 2 }
]

const loading = ref(false)
const tableData = ref<CommentItem[]>([])
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

const detailDialogVisible = ref(false)
const currentComment = ref<CommentItem | null>(null)

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
    title: '评论内容',
    key: 'content',
    width: 300,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '用户',
    key: 'userNickname',
    width: 120,
    render: (row) => {
      const rowData = row as unknown as CommentItem
      return h(NSpace, { align: 'center' }, {
        default: () => [
          rowData.userAvatar ? h(NAvatar, {
            src: rowData.userAvatar,
            size: 'small',
            round: true
          }) : null,
          h('span', {}, {
            default: () => rowData.userNickname || '-'
          })
        ]
      })
    }
  },
  {
    title: '关联内容',
    key: 'contentTitle',
    width: 150,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      const rowData = row as unknown as CommentItem
      return rowData.contentTitle ? h(NTag, { type: 'info' }, {
        default: () => rowData.contentTitle
      }) : h('span', '-', {})
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      const rowData = row as unknown as CommentItem
      const statusInfo = getStatusLabel(rowData.status)
      return h(NTag, { type: statusInfo.type }, {
        default: () => statusInfo.label
      })
    }
  },
  {
    title: 'IP 属地',
    key: 'ipLocation',
    width: 100
  },
  {
    title: '点赞数',
    key: 'likeCount',
    width: 80
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      const rowData = row as unknown as CommentItem
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
          rowData.status === 1 ? h(NButton, {
            size: 'small',
            type: 'success',
            text: true,
            onClick: () => handleUpdateStatus(rowData, 0)
          }, {
            default: () => '启用'
          }) : h(NButton, {
            size: 'small',
            type: 'warning',
            text: true,
            onClick: () => handleUpdateStatus(rowData, 1)
          }, {
            default: () => '禁用'
          }),
          h(NButton, {
            size: 'small',
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

function getStatusLabel(status: number | string): { label: string, type: 'default' | 'info' | 'success' | 'warning' | 'error' } {
  const statusMap: Record<string, { label: string, type: any }> = {
    0: { label: '正常', type: 'success' },
    1: { label: '隐藏', type: 'warning' },
    2: { label: '已删除', type: 'error' },
    'SHOW': { label: '正常', type: 'success' },
    'HIDDEN': { label: '隐藏', type: 'warning' },
    'DELETED': { label: '已删除', type: 'error' }
  }
  return statusMap[status] || { label: '未知', type: 'default' }
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

    const response = await commentApi.getCommentList(params)

    if (response.code === 0 || response.code === 200) {
      // 处理图片 URL 和数据映射
      const baseUrl = appContext?.$config?.url || 'http://localhost:8080'
      tableData.value = (response.data.list || []).map((item: any) => ({
        id: item.id,
        content: item.content,
        userId: item.userId,
        userNickname: item.user?.nickname || '-',
        userAvatar: item.user?.avatar ? getFullUrl(item.user.avatar, baseUrl) : undefined,
        contentId: item.contentId,
        contentTitle: item.contentTitle || '-',
        parentId: item.parentId,
        status: item.status,
        ipLocation: item.ipLocation,
        likeCount: item.likeCount,
        createTime: item.createTime,
        updateTime: item.updateTime
      }))
      pagination.itemCount = response.data.totalCount || 0
    } else {
      message.error(
response.msg || '获取评论列表失败')
    }
  } catch (error: any) {
    console.error('获取评论列表失败:', error)
    message.error(error.response?.data?.msg || '获取评论列表失败')
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
    status: null,
    contentId: null,
    userId: null,
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

const getFullUrl = (path: string, baseUrl?: string): string => {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  if (baseUrl) {
    return `${baseUrl}/${path}`
  }
  return path
}

const handleViewDetail = async (row: CommentItem) => {
  currentComment.value = row
  try {
    const response = await commentApi.getCommentById(row.id)
    if (response.code === 0 || response.code === 200) {
      currentComment.value = response.data
      detailDialogVisible.value = true
    }
  } catch (error: any) {
    message.error('获取评论详情失败')
  }
}

const handleUpdateStatus = async (row: CommentItem, status: number) => {
  const actionText = status === 0 ? '启用' : '禁用'

  dialog.warning({
    title: '确认操作',
    content: `确定要${actionText}这条评论吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await commentApi.updateCommentStatus(row.id, status)
        if (response.code === 0 || response.code === 200) {
          message.success(`${actionText}成功`)
          loadData()
        } else {
          message.error(
response.msg || `${actionText}失败`)
        }
      } catch (error: any) {
        message.error(error.response?.data?.msg || `${actionText}失败`)
      }
    }
  })
}

const handleDelete = async (row: CommentItem) => {
  dialog.warning({
    title: '确认删除',
    content: '删除后无法恢复，确定要删除这条评论吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await commentApi.deleteComment(row.id)
        if (response.code === 0 || response.code === 200) {
          message.success('删除成功')
          loadData()
        } else {
          message.error(
response.msg || '删除失败')
        }
      } catch (error: any) {
        message.error(error.response?.data?.msg || '删除失败')
      }
    }
  })
}

const handleBatchUpdateStatus = async (status: number) => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择评论')
    return
  }

  const actionText = status === 0 ? '启用' : '禁用'
  dialog.warning({
    title: `批量${actionText}`,
    content: `确定要${actionText}选中的 ${checkedRowKeys.value.length} 条评论吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await commentApi.batchUpdateStatus({ status, ids: checkedRowKeys.value })
        if (response.code === 0 || response.code === 200) {
          message.success(`批量${actionText}成功`)
          checkedRowKeys.value = []
          loadData()
        } else {
          message.error(
response.msg || `批量${actionText}失败`)
        }
      } catch (error: any) {
        message.error(error.response?.data?.msg || `批量${actionText}失败`)
      }
    }
  })
}

const handleBatchDelete = async () => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先选择评论')
    return
  }

  dialog.warning({
    title: '批量删除',
    content: `删除后无法恢复，确定要删除选中的 ${checkedRowKeys.value.length} 条评论吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const response = await commentApi.batchDeleteComments(checkedRowKeys.value)
        if (response.code === 0 || response.code === 200) {
          message.success('批量删除成功')
          checkedRowKeys.value = []
          loadData()
        } else {
          message.error(
response.msg || '批量删除失败')
        }
      } catch (error: any) {
        message.error(error.response?.data?.msg || '批量删除失败')
      }
    }
  })
}

const onChecked = (keys: number[]) => {
  checkedRowKeys.value = keys
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
</style>
