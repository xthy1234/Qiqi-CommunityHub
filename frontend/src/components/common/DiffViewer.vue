<template>
  <div class="diff-viewer">
    <!-- 头部信息 -->
    <div v-if="showHeader" class="diff-header">
      <div class="version-info">
        <span class="label">源版本：</span>
        <span class="value">{{ sourceLabel }}</span>
        <span class="time">{{ sourceTime }}</span>
      </div>
      <div class="version-info">
        <span class="label">目标版本：</span>
        <span class="value">{{ targetLabel }}</span>
        <span class="time">{{ targetTime }}</span>
      </div>
    </div>

    <!-- 差异展示区域 -->
    <div class="diff-content-wrapper">
      <div class="diff-panel original">
        <div class="panel-title">
          <Icon :icon="sourceIcon" width="16" />
          <span>{{ sourceTitle }}</span>
        </div>
        <div class="diff-text-content">
          {{ sourceText }}
        </div>
      </div>

      <div class="diff-divider"></div>

      <div class="diff-panel modified">
        <div class="panel-title">
          <Icon :icon="targetIcon" width="16" />
          <span>{{ targetTitle }}</span>
        </div>
        <div class="diff-text-content">
          {{ targetText }}
        </div>
      </div>
    </div>

    <!-- 差异统计 -->
    <div v-if="showStats" class="diff-stats">
      <n-tag type="success" size="small">
        <template #icon>
          <Icon icon="ri:add-line" />
        </template>
        新增 {{ stats.added }} 行
      </n-tag>
      <n-tag type="error" size="small">
        <template #icon>
          <Icon icon="ri:subtract-line" />
        </template>
        删除 {{ stats.deleted }} 行
      </n-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { NTag } from 'naive-ui'
import * as jsondiffpatch from 'jsondiffpatch'
import { format } from 'jsondiffpatch/formatters/html'

const props = withDefaults(defineProps<{
  sourceContent?: object
  targetContent?: object
  sourceLabel?: string
  targetLabel?: string
  sourceTime?: string
  targetTime?: string
  sourceTitle?: string
  targetTitle?: string
  sourceIcon?: string
  targetIcon?: string
  showHeader?: boolean
  showStats?: boolean
}>(), {
  sourceLabel: '原文',
  targetLabel: '修改后',
  sourceTitle: '原文内容',
  targetTitle: '修改内容',
  sourceIcon: 'ri:file-text-line',
  targetIcon: 'ri:edit-line',
  showHeader: true,
  showStats: true
})

const stats = ref({ added: 0, deleted: 0 })
const delta = ref<any>(null)

// 从 TipTap JSON 格式提取纯文本
const extractTextFromDoc = (doc: any): string => {
  if (!doc || !doc.content) return ''

  const texts: string[] = []

  const traverse = (node: any) => {
    if (node.type === 'text' && node.text) {
      texts.push(node.text)
    } else if (node.content && Array.isArray(node.content)) {
      node.content.forEach(traverse)
    }
  }

  doc.content.forEach(traverse)
  return texts.join('\n')
}

// 计算原文和修改后的文本
const sourceText = computed(() => {
  if (!props.sourceContent) return ''
  return extractTextFromDoc(props.sourceContent)
})

const targetText = computed(() => {
  if (!props.targetContent) return ''
  return extractTextFromDoc(props.targetContent)
})

// 统计差异行数
const countDiffLines = () => {
  if (!delta.value) {
    stats.value = { added: 0, deleted: 0 }
    return
  }

  let added = 0
  let deleted = 0

  const traverse = (obj: any) => {
    if (Array.isArray(obj) && obj.length === 2) {
      // [oldValue, newValue]
      const [oldVal, newVal] = obj

      if (typeof oldVal === 'string') {
        const oldLines = oldVal.split('\n').filter(l => l.trim()).length
        if (oldLines > 0) deleted += oldLines
      }

      if (typeof newVal === 'string') {
        const newLines = newVal.split('\n').filter(l => l.trim()).length
        if (newLines > 0) added += newLines
      }
    } else if (typeof obj === 'object' && obj !== null) {
      Object.values(obj).forEach(traverse)
    }
  }

  traverse(delta.value)
  stats.value = { added, deleted }
}

// 监听 source 和 target 的变化，计算差异
watch(() => [props.sourceContent, props.targetContent], () => {
  if (!props.sourceContent || !props.targetContent) {
    stats.value = { added: 0, deleted: 0 }
    delta.value = null
    return
  }

  try {
    // 计算差异
    delta.value = jsondiffpatch.diff(props.sourceContent, props.targetContent)

    if (!delta.value) {
      // 无差异
      stats.value = { added: 0, deleted: 0 }
      return
    }

    // 统计行数
    countDiffLines()
  } catch (error) {
    console.error('差异计算失败:', error)
    stats.value = { added: 0, deleted: 0 }
  }
}, { immediate: true, deep: true })

</script>

<style lang="scss" scoped>
.diff-viewer {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;

  .version-info {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 14px;

    .label {
      color: #606266;
      font-weight: 500;
    }

    .value {
      color: #303133;
      font-weight: 600;
    }

    .time {
      color: #909399;
      font-size: 12px;
    }
  }
}

.diff-content-wrapper {
  display: grid;
  grid-template-columns: 1fr 4px 1fr;
  min-height: 400px;
  max-height: 600px;
  overflow: auto;

  .diff-panel {
    padding: 20px;
    background: #fafafa;

    .panel-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #e4e7ed;
      font-size: 14px;
      font-weight: 600;
      color: #606266;
    }

    .diff-content {
      font-size: 13px;
      line-height: 1.8;
      color: #303133;
      white-space: pre-wrap;
    }
  }

  .diff-divider {
    background: #e4e7ed;
    border-left: 1px solid #dcdfe6;
    border-right: 1px solid #dcdfe6;
  }
}

.diff-stats {
  display: flex;
  gap: 12px;
  padding: 12px 20px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;

  .n-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

// 滚动条美化
.diff-content-wrapper {
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f0f0f0;
  }

  &::-webkit-scrollbar-thumb {
    background: #c0c4cc;
    border-radius: 3px;

    &:hover {
      background: #909399;
    }
  }
}
</style>
