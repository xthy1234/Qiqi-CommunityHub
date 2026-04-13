<!-- src/views/points/PointsRuleList.vue -->
<template>
  <PageContainer header-title="积分规则管理">
    <template #header-extra>
      <NSpace>
        <NButton type="primary" @click="handleCreate">
          <template #icon>
            <Icon icon="ri:add-line" />
          </template>
          新增规则
        </NButton>
        <NButton 
          type="error" 
          @click="handleBatchDelete" 
          :disabled="checkedRowKeys.length === 0"
        >
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
        placeholder="请输入规则名称或代码"
        clearable
        style="width: 250px"
      />
      <NSelect
        v-model:value="searchForm.isEnabled"
        placeholder="状态"
        :options="statusOptions"
        clearable
        style="width: 120px"
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

    <!-- 编辑对话框 -->
    <CrudDialog
      v-model:show="dialogVisible"
      :title="dialogTitle"
      :form-data="formData"
      :form-rules="formRules"
      :loading="submitting"
      @confirm="handleSubmit"
      @cancel="dialogVisible = false"
    >
      <NFormItem label="规则代码" path="ruleCode">
        <NInput
          v-model:value="formData.ruleCode"
          placeholder="例如：ARTICLE_PUBLISH"
          :disabled="isEdit"
        />
      </NFormItem>
      <NFormItem label="规则名称" path="ruleName">
        <NInput v-model:value="formData.ruleName" placeholder="例如：发布文章" />
      </NFormItem>
      <NFormItem label="规则描述" path="description">
        <NInput
          v-model:value="formData.description"
          type="textarea"
          placeholder="规则说明"
          :rows="3"
        />
      </NFormItem>
      <NFormItem label="积分数量" path="points">
        <NInputNumber
          v-model:value="formData.points"
          placeholder="正数为奖励，负数为扣除"
          style="width: 100%"
        />
      </NFormItem>
      <NFormItem label="每日限制" path="dailyLimit">
        <NInputNumber
          v-model:value="formData.dailyLimit"
          placeholder="0表示无限制"
          style="width: 100%"
        />
      </NFormItem>
      <NFormItem label="是否启用" path="isEnabled">
        <NRadioGroup v-model:value="formData.isEnabled">
          <NRadioButton :value="true">启用</NRadioButton>
          <NRadioButton :value="false">禁用</NRadioButton>
        </NRadioGroup>
      </NFormItem>
    </CrudDialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import type { DataTableColumns, FormRules } from 'naive-ui'
import { NButton, NTag, NSpace, NInput, NSelect, NInputNumber, NRadioGroup, NRadioButton, useMessage, useDialog } from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import CrudDialog from '@/components/common/CrudDialog.vue'
import pointsApi, { type PointsRule } from '@/api/points'

const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  keyword: '',
  isEnabled: null as boolean | null
})

const statusOptions = [
  { label: '启用', value: true },
  { label: '禁用', value: false }
]

const loading = ref(false)
const tableData = ref<PointsRule[]>([])
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

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formData = ref<Partial<PointsRule>>({
  ruleCode: '',
  ruleName: '',
  description: '',
  points: 0,
  dailyLimit: 0,
  isEnabled: true
})

const formRules: FormRules = {
  ruleCode: {
    required: true,
    message: '请输入规则代码',
    trigger: ['blur', 'change']
  },
  ruleName: {
    required: true,
    message: '请输入规则名称',
    trigger: ['blur', 'change']
  },
  points: {
    required: true,
    type: 'number',
    message: '请输入积分数量',
    trigger: ['blur', 'change']
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
    title: '规则代码',
    key: 'ruleCode',
    width: 180,
    ellipsis: { tooltip: true }
  },
  {
    title: '规则名称',
    key: 'ruleName',
    width: 150,
    ellipsis: { tooltip: true }
  },
  {
    title: '积分数量',
    key: 'points',
    width: 100,
    render: (row) => {
      const color = row.points > 0 ? '#52c41a' : '#ff4d4f'
      return h('span', { style: { color, fontWeight: 'bold' } }, row.points > 0 ? `+${row.points}` : row.points)
    }
  },
  {
    title: '每日限制',
    key: 'dailyLimit',
    width: 100,
    render: (row) => row.dailyLimit || '无限制'
  },
  {
    title: '状态',
    key: 'isEnabled',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.isEnabled ? 'success' : 'default'
      }, {
        default: () => row.isEnabled ? '启用' : '禁用'
      })
    }
  },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, {}, {
        default: () => [
          h(NButton, {
            size: 'small',
            onClick: () => handleEdit(row)
          }, {
            default: () => '编辑'
          }),
          h(NButton, {
            size: 'small',
            type: row.isEnabled ? 'warning' : 'success',
            onClick: () => handleToggleStatus(row)
          }, {
            default: () => row.isEnabled ? '禁用' : '启用'
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

const loadData = async () => {
  loading.value = true
  try {
    const res = await pointsApi.getPointsRules({
      page: pagination.page,
      limit: pagination.pageSize,
      ...searchForm.value
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

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    isEnabled: null
  }
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  isEdit.value = false
  formData.value = {
    ruleCode: '',
    ruleName: '',
    description: '',
    points: 0,
    dailyLimit: 0,
    isEnabled: true
  }
  dialogVisible.value = true
}

const handleEdit = (row: PointsRule) => {
  isEdit.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    let res
    if (isEdit.value && formData.value.id) {
      res = await pointsApi.updateRule(formData.value.id, formData.value)
    } else {
      res = await pointsApi.createRule(formData.value as any)
    }
    
    if (res.code === 0 || res.code === 200) {
      message.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadData()
    } else {
      message.error(res.msg || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row: PointsRule) => {
  try {
    const res = await pointsApi.toggleRule(row.id)
    if (res.code === 0 || res.code === 200) {
      message.success('状态切换成功')
      loadData()
    } else {
      message.error(res.msg || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  }
}

const handleDelete = (row: PointsRule) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除规则 "${row.ruleName}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await pointsApi.deleteRule(row.id)
        if (res.code === 0 || res.code === 200) {
          message.success('删除成功')
          loadData()
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
    message.warning('请选择要删除的规则')
    return
  }
  
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 条规则吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await pointsApi.batchDeleteRules(checkedRowKeys.value)
        if (res.code === 0 || res.code === 200) {
          message.success('批量删除成功')
          checkedRowKeys.value = []
          loadData()
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

const dialogTitle = computed(() => isEdit.value ? '编辑规则' : '新增规则')

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
