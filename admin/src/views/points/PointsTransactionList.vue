<!-- src/views/points/PointsTransactionList.vue -->
<template>
  <PageContainer header-title="积分流水管理">
    <template #header-extra>
      <NButton type="primary" @click="handleAdjustPoints">
        <template #icon>
          <Icon icon="ri:coins-line" />
        </template>
        调整积分
      </NButton>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
        v-model:value="searchForm.userId"
        placeholder="用户ID"
        clearable
        style="width: 150px"
      />
      <NInput
        v-model:value="searchForm.ruleCode"
        placeholder="规则代码"
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
      striped
    />

    <!-- 调整积分对话框 -->
    <NModal
      v-model:show="adjustDialogVisible"
      preset="card"
      title="调整用户积分"
      style="width: 500px"
    >
      <NForm
        ref="adjustFormRef"
        :model="adjustFormData"
        :rules="adjustFormRules"
        label-placement="left"
        label-width="100px"
      >
        <NFormItem label="用户ID" path="userId">
          <NInputNumber
            v-model:value="adjustFormData.userId"
            placeholder="请输入用户ID"
            style="width: 100%"
          />
        </NFormItem>
        <NFormItem label="调整数量" path="amount">
          <NInputNumber
            v-model:value="adjustFormData.amount"
            placeholder="正数为增加，负数为扣除"
            style="width: 100%"
          />
        </NFormItem>
        <NFormItem label="调整原因" path="reason">
          <NInput
            v-model:value="adjustFormData.reason"
            type="textarea"
            placeholder="请输入调整原因"
            :rows="3"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="adjustDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="handleAdjustSubmit" :loading="adjusting">
            确定
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import type { DataTableColumns, FormRules, FormInst } from 'naive-ui'
import { NButton, NTag, NSpace, NInput, NInputNumber, NDatePicker, NModal, NForm, NFormItem, useMessage } from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import pointsApi, { type PointsTransaction } from '@/api/points'

const message = useMessage()

const searchForm = ref({
  userId: '' as string | number | null,
  ruleCode: ''
})

const dateRange = ref<[number, number] | null>(null)
const searchStartDate = ref<string>()
const searchEndDate = ref<string>()

const loading = ref(false)
const tableData = ref<PointsTransaction[]>([])

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

const adjustDialogVisible = ref(false)
const adjusting = ref(false)
const adjustFormRef = ref<FormInst | null>(null)
const adjustFormData = ref({
  userId: null as number | null,
  amount: 0,
  reason: ''
})

const adjustFormRules: FormRules = {
  userId: {
    required: true,
    type: 'number',
    message: '请输入用户ID',
    trigger: ['blur', 'change']
  },
  amount: {
    required: true,
    type: 'number',
    message: '请输入调整数量',
    trigger: ['blur', 'change']
  },
  reason: {
    required: true,
    message: '请输入调整原因',
    trigger: ['blur', 'change']
  }
}

const columns: DataTableColumns<PointsTransaction> = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '用户昵称',
    key: 'userNickname',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row) => row.user?.nickname || '-'
  },
  {
    title: '变动数量',
    key: 'amount',
    width: 120,
    render: (row) => {
      const color = row.amount > 0 ? '#52c41a' : '#ff4d4f'
      return h('span', { style: { color, fontWeight: 'bold' } }, row.amount > 0 ? `+${row.amount}` : row.amount)
    }
  },
  {
    title: '当前余额',
    key: 'balance',
    width: 120
  },
  {
    title: '积分来源',
    key: 'source',
    width: 150,
    render: (row) => {
      const sourceMap: Record<string, string> = {
        sign_in: '每日签到',
        post_article: '发布文章',
        comment: '发表评论',
        like_article: '点赞文章',
        share_article: '分享文章',
        admin_adjust: '管理员调整'
      }
      return h(NTag, {
        type: row.source === 'admin_adjust' ? 'warning' : 'success',
        size: 'small'
      }, { default: () => sourceMap[row.source] || row.source })
    }
  },
  {
    title: '说明',
    key: 'description',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: '关联ID',
    key: 'sourceId',
    width: 100,
    render: (row) => row.sourceId || '-'
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      limit: pagination.pageSize,
      userId: searchForm.value.userId || undefined,
      ruleCode: searchForm.value.ruleCode || undefined,
      startDate: searchStartDate.value,
      endDate: searchEndDate.value
    }
    
    const res = await pointsApi.getTransactions(params)
    
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

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value = {
    userId: null,
    ruleCode: ''
  }
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

const handleAdjustPoints = () => {
  adjustFormData.value = {
    userId: null,
    amount: 0,
    reason: ''
  }
  adjustDialogVisible.value = true
}

const handleAdjustSubmit = async () => {
  await adjustFormRef.value?.validate(async (errors) => {
    if (errors) return
    
    adjusting.value = true
    try {
      const res = await pointsApi.adjustPoints(adjustFormData.value as any)
      if (res.code === 0 || res.code === 200) {
        message.success('调整成功')
        adjustDialogVisible.value = false
        loadData()
      } else {
        message.error(res.msg || '调整失败')
      }
    } catch (error) {
      message.error('调整失败')
      console.error(error)
    } finally {
      adjusting.value = false
    }
  })
}

onMounted(() => {
  loadData()
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
