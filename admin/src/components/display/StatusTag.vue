<template>
  <NTag :type="computedType" :bordered="bordered" size="small">
    {{ computedLabel }}
  </NTag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NTag } from 'naive-ui'

type TagType = 'default' | 'info' | 'success' | 'warning' | 'error'

interface StatusOption {
  value: any
  label: string
  type: TagType
}

interface Props {
  /** 状态值 */
  status: any
  /** 状态映射选项 */
  options?: StatusOption[]
  /** 默认类型（当没有找到匹配的状态时使用） */
  defaultType?: TagType
  /** 默认标签文本 */
  defaultLabel?: string
  /** 是否带边框 */
  bordered?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  options: () => [],
  defaultType: 'default',
  defaultLabel: '未知',
  bordered: false
})

const computedType = computed<TagType>(() => {
  if (!props.options || props.options.length === 0) {
    return props.defaultType
  }
  
  const matched = props.options.find(opt => opt.value === props.status)
  return matched?.type || props.defaultType
})

const computedLabel = computed<string>(() => {
  if (!props.options || props.options.length === 0) {
    return props.defaultLabel
  }
  
  const matched = props.options.find(opt => opt.value === props.status)
  return matched?.label || props.defaultLabel
})
</script>
