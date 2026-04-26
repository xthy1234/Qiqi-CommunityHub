<template>
  <PageContainer
    header-title="屏蔽规则管理"
    :show-back="true"
  >
    <div class="block-rule-management">
      <!-- 操作栏 -->
      <div class="action-bar">
        <n-button
          type="primary"
          @click="showAddModal = true"
        >
          <template #icon>
            <Icon icon="ri:add-line" />
          </template>
          添加屏蔽规则
        </n-button>
      </div>

      <!-- 规则列表 -->
      <div class="rule-list">
        <n-empty
          v-if="rules.length === 0 && !loading"
          description="暂无屏蔽规则"
        >
          <template #extra>
            <n-button
              size="small"
              @click="showAddModal = true"
            >
              添加第一条规则
            </n-button>
          </template>
        </n-empty>

        <n-data-table
          v-else
          :columns="columns"
          :data="rules"
          :loading="loading"
          :pagination="false"
          striped
        />
      </div>
    </div>

    <!-- 添加规则弹窗 -->
    <n-modal
      v-model:show="showAddModal"
      preset="card"
      title="添加屏蔽规则"
      style="width: 500px"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="80"
      >
        <n-form-item
          label="规则类型"
          path="ruleType"
        >
          <n-select
            v-model:value="formData.ruleType"
            :options="ruleTypeOptions"
            placeholder="请选择规则类型"
            @update:value="handleRuleTypeChange"
          />
        </n-form-item>

        <n-form-item
          v-if="formData.ruleType === 'keyword'"
          label="关键词"
          path="ruleValue"
        >
          <n-input
            v-model:value="formData.ruleValue"
            placeholder="请输入要屏蔽的关键词"
            maxlength="50"
            show-count
          />
        </n-form-item>

        <n-form-item
          v-else-if="formData.ruleType === 'author'"
          label="作者ID"
          path="ruleValue"
        >
          <n-input
            v-model:value="formData.ruleValue"
            placeholder="请输入作者ID"
          />
        </n-form-item>

        <n-form-item
          v-else-if="formData.ruleType === 'category'"
          label="分类"
          path="ruleValue"
        >
          <ArticleCategorySelect
            v-model="formData.ruleValue"
            placeholder="请选择要屏蔽的分类"
            :include-all-option="false"
            @change="handleCategoryChange"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showAddModal = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, NButton, NSwitch, NTag, NSpace, NEmpty, NDataTable, NModal, NForm, NFormItem, NInput, NSelect } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import ArticleCategorySelect from '@/components/article/ArticleCategorySelect.vue'
import { blockRuleAPI } from '@/api/blockRule'
import type { BlockRule, RuleType } from '@/types/blockrule'
import { getRuleTypeLabel } from '@/types/blockrule'

const router = useRouter()
const message = useMessage()

const rules = ref<BlockRule[]>([])
const loading = ref(false)
const showAddModal = ref(false)
const submitting = ref(false)
const formRef = ref('')

const formData = ref({
  ruleType: 'keyword' as RuleType,
  ruleValue: ''
})

const formRules = {
  ruleType: {
    required: true,
    message: '请选择规则类型',
    trigger: 'change'
  },
  ruleValue: {
    required: true,
    message: '请输入规则值',
    trigger: ['blur', 'change'],
    validator: (rule: any, value: string) => {
      // 自定义验证器：检查值是否为空或空白
      if (!value || value.trim() === '') {
        return new Error('请输入规则值')
      }
      return true
    }
  }
}

const ruleTypeOptions = [
  { label: '关键词', value: 'keyword' },
  { label: '作者', value: 'author' },
  { label: '分类', value: 'category' }
]

const columns = [
  {
    title: '规则类型',
    key: 'ruleType',
    width: 120,
    render: (row: BlockRule) => {
      return h(NTag, {
        type: row.ruleType === 'keyword' ? 'info' : row.ruleType === 'author' ? 'warning' : 'success',
        size: 'small'
      }, {
        default: () => getRuleTypeLabel(row.ruleType)
      })
    }
  },
  {
    title: '规则值',
    key: 'ruleValue',
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: (row: BlockRule) => {
      return h(NSwitch, {
        value: row.enabled,
        onUpdateValue: (value: boolean) => handleToggleEnable(row.id, value)
      })
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row: BlockRule) => {
      return h(NButton, {
        text: true,
        type: 'error',
        onClick: () => handleDelete(row.id)
      }, {
        default: () => '删除'
      })
    }
  }
]

const fetchRules = async () => {
  loading.value = true
  try {
    const response = await blockRuleAPI.getRules()
    rules.value = response.data.data || []
  } catch (error) {
    console.error('获取屏蔽规则失败:', error)
    message.error('获取屏蔽规则失败')
  } finally {
    loading.value = false
  }
}

const handleRuleTypeChange = () => {
  formData.value.ruleValue = ''
}

const handleCategoryChange = (value: number | string | undefined) => {
  // 将分类ID转换为字符串，确保表单验证通过
  if (value !== undefined && value !== null) {
    formData.value.ruleValue = String(value)
  } else {
    formData.value.ruleValue = ''
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true
    
    const response = await blockRuleAPI.addRule({
      ruleType: formData.value.ruleType,
      ruleValue: formData.value.ruleValue
    })
    
    const msg = response.data?.msg
    if (msg) {
      message.success(msg)
    } else {
      message.success('添加成功')
    }

    showAddModal.value = false
    resetForm()
    await fetchRules()
  } catch (error: any) {
    console.error('添加规则失败:', error)
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('添加失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

const handleToggleEnable = async (id: number, enabled: boolean) => {
  try {
    await blockRuleAPI.toggleEnable(id, enabled)
    message.success(enabled ? '已启用' : '已禁用')
    await fetchRules()
  } catch (error) {
    console.error('更新规则状态失败:', error)
    message.error('操作失败')
    await fetchRules()
  }
}

const handleDelete = (id: number) => {
  window.$dialog?.warning({
    title: '确认删除',
    content: '确定要删除这条屏蔽规则吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await blockRuleAPI.deleteRule(id)
        message.success('删除成功')
        await fetchRules()
      } catch (error) {
        console.error('删除规则失败:', error)
        message.error('删除失败')
      }
    }
  })
}

const resetForm = () => {
  formData.value = {
    ruleType: 'keyword',
    ruleValue: ''
  }
  formRef.value?.restoreValidation()
}

onMounted(() => {
  fetchRules()
})
</script>

<style lang="scss" scoped>
.block-rule-management {
  .action-bar {
    margin-bottom: 20px;
  }

  .rule-list {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
  }
}
</style>
