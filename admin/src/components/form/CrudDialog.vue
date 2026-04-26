<template>
  <NModal
    v-model:show="visibleModel"
    preset="dialog"
    :title="computedTitle"
    :style="{ width: width }"
    :mask-closable="maskClosable"
    @after-leave="handleAfterLeave"
  >
    <NForm
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-placement="labelPlacement"
      :label-width="labelWidth"
      :require-mark-placement="requireMarkPlacement"
    >
      <slot name="form-content">
        <!-- 表单内容插槽 -->
      </slot>
    </NForm>

    <template #action>
      <slot name="action">
        <NSpace justify="end">
          <NButton @click="handleCancel">取消</NButton>
          <NButton 
            type="primary" 
            @click="handleSubmit" 
            :loading="submitting"
          >
            {{ confirmText }}
          </NButton>
        </NSpace>
      </slot>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NModal, NForm, NButton, NSpace, type FormInst, type FormRules } from 'naive-ui'

interface Props {
  /** 对话框显示状态（支持 v-model:visible） */
  visible: boolean
  /** 标题（新增模式） */
  createTitle?: string
  /** 标题（编辑模式） */
  editTitle?: string
  /** 是否为编辑模式 */
  isEdit?: boolean
  /** 表单数据 */
  formData: Record<string, any>
  /** 表单验证规则 */
  formRules?: FormRules
  /** 对话框宽度 */
  width?: string
  /** 标签位置 */
  labelPlacement?: 'left' | 'top'
  /** 标签宽度 */
  labelWidth?: number | string
  /** 必填标记位置 */
  requireMarkPlacement?: 'right' | 'left'
  /** 确认按钮文本 */
  confirmText?: string
  /** 是否可点击遮罩关闭 */
  maskClosable?: boolean
  /** 提交中状态 */
  submitting?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  createTitle: '新建',
  editTitle: '编辑',
  isEdit: false,
  formRules: () => ({}),
  width: '600px',
  labelPlacement: 'left',
  labelWidth: 100,
  requireMarkPlacement: 'right',
  confirmText: '确定',
  maskClosable: false,
  submitting: false
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  submit: [formData: Record<string, any>]
  cancel: []
  afterLeave: []
}>()

const formRef = ref<FormInst | null>(null)

// 双向绑定 visible
const visibleModel = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 计算标题
const computedTitle = computed(() => {
  return props.isEdit ? props.editTitle : props.createTitle
})

// 处理取消
const handleCancel = () => {
  emit('cancel')
  visibleModel.value = false
}

// 处理提交
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('submit', props.formData)
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 对话框关闭后回调
const handleAfterLeave = () => {
  emit('afterLeave')
  // 重置表单（可选）
  formRef.value?.restoreValidation()
}

// 暴露方法给父组件
defineExpose({
  validate: async () => {
    if (!formRef.value) return false
    try {
      await formRef.value.validate()
      return true
    } catch (error) {
      return false
    }
  },
  resetValidation: () => {
    formRef.value?.restoreValidation()
  }
})
</script>
