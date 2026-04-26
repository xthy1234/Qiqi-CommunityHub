<template>
  <PageContainer
      :header-title="'举报管理'"
      @back="goBack"
  >
    <template #header-extra>
      <NSpace>
        <NBadge :value="pendingCount" :show="pendingCount > 0" :max="99">
          <NButton type="warning" @click="handleFilterPending">
            <template #icon>
              <Icon icon="ri:alert-line" />
            </template>
            待审核 ({{ pendingCount }})
          </NButton>
        </NBadge>
        <NButton @click="handleBatchAudit" :disabled="checkedRowKeys.length === 0">
          <template #icon>
            <Icon icon="ri:checkbox-multiple-line" />
          </template>
          批量审核
        </NButton>
        <NButton type="error" @click="handleBatchDelete" :disabled="checkedRowKeys.length === 0">
          <template #icon>
            <Icon icon="ri:delete-bin-line" />
          </template>
          批量删除
        </NButton>
      </NSpace>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
          v-model:value="searchForm.keyword"
          placeholder="请输入举报人/被举报人/内容标题"
          clearable
          style="width: 250px"
      />
      <NSelect
          v-model:value="searchForm.reviewStatus"
          placeholder="审核状态"
          :options="reviewStatusOptions"
          clearable
          style="width: 150px"
      />
      <NDatePicker
          v-model:value="dateRange"
          type="daterange"
          placeholder="选择时间范围"
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

    <!-- 表格 -->
    <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        :row-key="(row) => row.id"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="handleCheckAll"
        striped
    />

    <!-- 审核对话框 -->
    <NModal
        v-model:show="auditDialogVisible"
        preset="dialog"
        :title="`审核举报 #${currentReport?.id || ''}`"
        style="width: 650px"
    >
      <NForm
          ref="auditFormRef"
          :model="auditFormData"
          :rules="auditFormRules"
          label-placement="left"
          label-width="120px"
      >
        <NAlert title="举报信息" type="info" style="margin-bottom: 16px;">
          <NDescriptions bordered :column="1" size="small">
            <NDescriptionsItem label="举报人">
              {{ currentReport?.reporterUserInfo?.nickname || currentReport?.reporterName || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="被举报人">
              {{ currentReport?.reportedUserInfo?.nickname || currentReport?.reportedName || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="内容标题">
              {{ currentReport?.contentTitle || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="举报原因">
              {{ currentReport?.reportReason }}
            </NDescriptionsItem>
            <NDescriptionsItem label="举报详情" v-if="currentReport?.reportDesc">
              {{ currentReport?.reportDesc }}
            </NDescriptionsItem>
          </NDescriptions>
        </NAlert>

        <NFormItem label="审核结果" path="reviewStatus">
          <NRadioGroup v-model:value="auditFormData.reviewStatus">
            <NSpace>
              <NRadioButton :value="ReviewStatus.APPROVED">通过</NRadioButton>
              <NRadioButton :value="ReviewStatus.REJECTED">拒绝</NRadioButton>
            </NSpace>
          </NRadioGroup>
        </NFormItem>

        <NFormItem label="处理动作" path="action" v-if="auditFormData.reviewStatus === ReviewStatus.APPROVED">
          <NSelect
              v-model:value="auditFormData.action"
              :options="actionOptions"
              placeholder="请选择处理动作"
          />
        </NFormItem>

        <NFormItem label="处理备注" path="replyContent">
          <NInput
              v-model:value="auditFormData.replyContent"
              type="textarea"
              placeholder="请说明处理原因..."
              :rows="3"
          />
        </NFormItem>

        <NFormItem label="积分处理" v-if="auditFormData.reviewStatus === ReviewStatus.APPROVED">
          <NSpace vertical>
            <NCheckbox v-model:checked="auditFormData.rewardReporter">
              奖励举报人 10 积分
            </NCheckbox>
            <NCheckbox v-model:checked="auditFormData.penalizeReportedUser">
              扣除被举报人 20 积分
            </NCheckbox>
          </NSpace>
        </NFormItem>
      </NForm>

      <template #action>
        <NButton @click="auditDialogVisible = false">取消</NButton>
        <NButton type="primary" @click="handleAuditSubmit" :loading="auditing">
          确定
        </NButton>
      </template>
    </NModal>

    <!-- 批量审核对话框 -->
    <NModal
        v-model:show="batchAuditDialogVisible"
        preset="dialog"
        title="批量审核举报"
        style="width: 650px"
    >
      <NForm
          ref="batchAuditFormRef"
          :model="batchAuditFormData"
          :rules="auditFormRules"
          label-placement="left"
          label-width="120px"
      >
        <NAlert :title="`已选择 ${checkedRowKeys.length} 条举报`" type="warning" style="margin-bottom: 16px;" />

        <NFormItem label="审核结果" path="reviewStatus">
          <NRadioGroup v-model:value="batchAuditFormData.reviewStatus">
            <NSpace>
              <NRadioButton :value="ReviewStatus.APPROVED">通过</NRadioButton>
              <NRadioButton :value="ReviewStatus.REJECTED">拒绝</NRadioButton>
            </NSpace>
          </NRadioGroup>
        </NFormItem>

        <NFormItem label="处理动作" path="action" v-if="batchAuditFormData.reviewStatus === ReviewStatus.APPROVED">
          <NSelect
              v-model:value="batchAuditFormData.action"
              :options="actionOptions"
              placeholder="请选择处理动作"
          />
        </NFormItem>

        <NFormItem label="处理备注" path="replyContent">
          <NInput
              v-model:value="batchAuditFormData.replyContent"
              type="textarea"
              placeholder="请说明处理原因..."
              :rows="3"
          />
        </NFormItem>

        <NFormItem label="积分处理" v-if="batchAuditFormData.reviewStatus === ReviewStatus.APPROVED">
          <NSpace vertical>
            <NCheckbox v-model:checked="batchAuditFormData.rewardReporter">
              奖励举报人 10 积分
            </NCheckbox>
            <NCheckbox v-model:checked="batchAuditFormData.penalizeReportedUser">
              扣除被举报人 20 积分
            </NCheckbox>
          </NSpace>
        </NFormItem>
      </NForm>

      <template #action>
        <NButton @click="batchAuditDialogVisible = false">取消</NButton>
        <NButton type="primary" @click="handleBatchAuditSubmit" :loading="auditing">
          确定
        </NButton>
      </template>
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns, FormRules, FormInst } from 'naive-ui'
import { NButton, NTag, NSpace, useMessage, useDialog, NModal, NForm, NFormItem, NInput, NAlert, NDescriptions, NDescriptionsItem, NRadioGroup, NRadioButton, NBadge, NDatePicker, NSelect, NCheckbox } from 'naive-ui'
import { reportApi, type ReportVO, type ReportReviewWithActionDTO, ReviewStatus, ReportAction } from '@/api/report'
import PageContainer from "@/components/layout/PageContainer.vue"

interface ReportItem {
  id: number
  reporterName: string
  reportedName: string
  contentTitle: string
  reportReason: string
  reviewStatus: number
  createTime: string
  reporterUserInfo?: {
    nickname: string
  }
  reportedUserInfo?: {
    nickname: string
  }
  reportDesc?: string
}

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  keyword: '',
  reviewStatus: null as ReviewStatus | null
})

const dateRange = ref<[number, number] | null>(null)
const searchStartDate = ref<string | undefined>()
const searchEndDate = ref<string | undefined>()

const reviewStatusOptions = [
  { label: '待审核', value: ReviewStatus.PENDING },
  { label: '已通过', value: ReviewStatus.APPROVED },
  { label: '已拒绝', value: ReviewStatus.REJECTED }
]

const actionOptions = [
  { label: '屏蔽文章（作者可见但不可传播）', value: ReportAction.BLOCK },
  { label: '删除文章（软删除）', value: ReportAction.DELETE },
  { label: '仅警告（不修改文章状态）', value: ReportAction.WARN },
  { label: '忽略举报（不处理文章）', value: ReportAction.IGNORE }
]

const loading = ref(false)
const tableData = ref<ReportVO[]>([])
const pendingCount = ref(0)
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

const checkedRowKeys = ref<number[]>([])

const auditDialogVisible = ref(false)
const batchAuditDialogVisible = ref(false)
const auditing = ref(false)
const currentReport = ref<ReportVO | null>(null)
const auditFormRef = ref<FormInst | null>(null)
const batchAuditFormRef = ref<FormInst | null>(null)

const auditFormData = ref<ReportReviewWithActionDTO>({
  reviewStatus: ReviewStatus.APPROVED,
  replyContent: '',
  action: ReportAction.BLOCK,
  rewardReporter: true,
  penalizeReportedUser: true
})

const batchAuditFormData = ref<ReportReviewWithActionDTO>({
  reviewStatus: ReviewStatus.APPROVED,
  replyContent: '',
  action: ReportAction.BLOCK,
  rewardReporter: true,
  penalizeReportedUser: true
})

const auditFormRules: FormRules = {
  reviewStatus: {
    required: true,
    type: 'number',
    message: '请选择审核结果',
    trigger: 'change'
  },
  replyContent: {
    trigger: ['blur']
  },
  action: {
    required: true,
    message: '请选择处理动作',
    trigger: 'change'
  }
}

const columns: DataTableColumns = [
  {
    type: 'selection',
    width: 50
  },
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '举报人',
    key: 'reporterName',
    width: 120,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      return row.reporterUserInfo?.nickname || row.reporterName || '-'
    }
  },
  {
    title: '被举报人',
    key: 'reportedName',
    width: 120,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      return row.reportedUserInfo?.nickname || row.reportedName || '-'
    }
  },
  {
    title: '内容标题',
    key: 'contentTitle',
    width: 200,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '举报原因',
    key: 'reportReason',
    width: 150,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '审核状态',
    key: 'reviewStatus',
    width: 100,
    render: (row) => {
      const statusMap: Record<number, { label: string; type: 'warning' | 'success' | 'error' }> = {
        [ReviewStatus.PENDING]: { label: '待审核', type: 'warning' },
        [ReviewStatus.APPROVED]: { label: '已通过', type: 'success' },
        [ReviewStatus.REJECTED]: { label: '已拒绝', type: 'error' }
      }
      const status = statusMap[row.reviewStatus] || { label: '未知', type: 'default' }
      return h(NTag, {
        type: status.type
      }, {
        default: () => status.label
      })
    }
  },
  {
    title: '举报时间',
    key: 'createTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, {}, {
        default: () => [
          h(NButton, {
            size: 'small',
            onClick: () => handleAudit(row)
          }, {
            default: () => '审核'
          }),
          h(NButton, {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row)
          }, {
            default: () => '删除'
          })
        ]
      })
    }
  }
]

const goBack = () => {
  router.back()
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await reportApi.getReportList({
      page: pagination.page,
      limit: pagination.pageSize,
      reviewStatus: searchForm.value.reviewStatus ?? undefined,
      startDate: searchStartDate.value,
      endDate: searchEndDate.value
    })
    
    if (res.code === 0 || res.code === 200) {
      tableData.value = res.data.list
      pagination.itemCount = res.data.totalCount
    } else {
      message.error(res.msg || '加载失败')
    }
  } catch (error) {
    message.error('加载失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadPendingCount = async () => {
  try {
    const res = await reportApi.countByStatus(ReviewStatus.PENDING)
    if (res.code === 0 || res.code === 200) {
      pendingCount.value = res.data
    }
  } catch (error) {
    console.error('加载待审核数量失败', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value.keyword = ''
  searchForm.value.reviewStatus = null
  dateRange.value = null
  searchStartDate.value = undefined
  searchEndDate.value = undefined
  pagination.page = 1
  loadData()
}

const handleDateChange = (value: [number, number] | null) => {
  if (value) {
    searchStartDate.value = new Date(value[0]).toISOString().split('T')[0]
    searchEndDate.value = new Date(value[1]).toISOString().split('T')[0]
  } else {
    searchStartDate.value = undefined
    searchEndDate.value = undefined
  }
}

const handleFilterPending = () => {
  searchForm.value.reviewStatus = ReviewStatus.PENDING
  pagination.page = 1
  loadData()
}

const handleAudit = (row: ReportVO) => {
  currentReport.value = row
  auditFormData.value = {
    reviewStatus: ReviewStatus.APPROVED,
    replyContent: '',
    action: ReportAction.BLOCK,
    rewardReporter: true,
    penalizeReportedUser: true
  }
  auditDialogVisible.value = true
}

const handleAuditSubmit = async () => {
  await auditFormRef.value?.validate(async (errors) => {
    if (errors) {
      message.error('请完善表单信息：审核结果、处理动作和处理备注为必填项')
      return
    }
    
    if (!currentReport.value?.id) return
    
    auditing.value = true
    try {
      const res = await reportApi.reviewReportWithAction(currentReport.value.id, auditFormData.value)
      if (res.code === 0 || res.code === 200) {
        message.success('审核成功')
        auditDialogVisible.value = false
        loadData()
        loadPendingCount()
      } else {
        message.error(res.msg || '审核失败')
      }
    } catch (error) {
      message.error('审核失败')
      console.error(error)
    } finally {
      auditing.value = false
    }
  })
}

const handleBatchAudit = () => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请选择要审核的举报')
    return
  }
  
  batchAuditFormData.value = {
    reviewStatus: ReviewStatus.APPROVED,
    replyContent: '',
    action: ReportAction.BLOCK,
    rewardReporter: true,
    penalizeReportedUser: true
  }
  batchAuditDialogVisible.value = true
}

const handleBatchAuditSubmit = async () => {
  await batchAuditFormRef.value?.validate(async (errors) => {
    if (errors) {
      message.error('请完善表单信息：审核结果、处理动作和处理备注为必填项')
      return
    }
    
    auditing.value = true
    try {
      const res = await reportApi.batchReviewReports({
        reportIds: checkedRowKeys.value,
        reviewStatus: batchAuditFormData.value.reviewStatus,
        replyContent: batchAuditFormData.value.replyContent
      })
      
      if (res.code === 0 || res.code === 200) {
        message.success(`批量审核成功，共处理 ${checkedRowKeys.value.length} 条`)
        batchAuditDialogVisible.value = false
        checkedRowKeys.value = []
        loadData()
        loadPendingCount()
      } else {
        message.error(res.msg || '批量审核失败')
      }
    } catch (error) {
      message.error('批量审核失败')
      console.error(error)
    } finally {
      auditing.value = false
    }
  })
}

const handleDelete = (row: ReportVO) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除举报记录 #${row.id} 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await reportApi.deleteReport(row.id)
        if (res.code === 0 || res.code === 200) {
          message.success('删除成功')
          loadData()
          loadPendingCount()
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
        console.error(error)
      }
    }
  })
}

const handleBatchDelete = () => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请选择要删除的举报记录')
    return
  }
  
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 条举报记录吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await reportApi.batchDeleteReports(checkedRowKeys.value)
        if (res.code === 0 || res.code === 200) {
          message.success('批量删除成功')
          checkedRowKeys.value = []
          loadData()
          loadPendingCount()
        } else {
          message.error(res.msg || '批量删除失败')
        }
      } catch (error) {
        message.error('批量删除失败')
        console.error(error)
      }
    }
  })
}

const handleCheckAll = (keys: any) => {
  checkedRowKeys.value = keys as number[]
}

onMounted(() => {
  loadData()
  loadPendingCount()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
