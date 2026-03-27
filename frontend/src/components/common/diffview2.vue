<template>
  <div class="diff-viewer">
    <!-- 头部信息 -->
    <div v-if="showHeader" class="diff-header">
      <div class="version-info">
        <span class="label">{{ sourceLabel }}</span>
        <span v-if="sourceTime" class="time">{{ sourceTime }}</span>
      </div>
      <div class="version-info">
        <span class="label">{{ targetLabel }}</span>
        <span v-if="targetTime" class="time">{{ targetTime }}</span>
      </div>
    </div>

    <!-- 差异视图容器 -->
    <div ref="diffContainer" class="diff-container"></div>

    <!-- 差异统计 -->
    <div v-if="showStats" class="diff-stats">
      <n-tag type="success" size="small">
        <template #icon><Icon icon="ri:add-line" /></template>
        新增 {{ stats.added }}
      </n-tag>
      <n-tag type="error" size="small">
        <template #icon><Icon icon="ri:subtract-line" /></template>
        删除 {{ stats.deleted }}
      </n-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { Icon } from '@iconify/vue'
import { NTag } from 'naive-ui'
import * as jsondiffpatch from 'jsondiffpatch'
import * as htmlFormatterModule from 'jsondiffpatch/formatters/html'

const props = withDefaults(defineProps<{
  source?: any      // 源 JSON 对象
  target?: any      // 目标 JSON 对象
  sourceLabel?: string
  targetLabel?: string
  sourceTime?: string
  targetTime?: string
  showHeader?: boolean
  showStats?: boolean
}>(), {
  sourceLabel: '源版本',
  targetLabel: '目标版本',
  showHeader: true,
  showStats: true
})

const diffContainer = ref<HTMLElement | null>(null)
const delta = ref<any>(null)

// 统计差异数量（遍历 delta 对象）
const stats = ref({ added: 0, deleted: 0 })

function countDelta(deltaObj: any) {
  let added = 0
  let deleted = 0

  const traverse = (obj: any) => {
    if (Array.isArray(obj) && obj.length === 2) {
      // jsondiffpatch 的数组差异形式：[oldValue, newValue]
      if (obj[0] !== undefined && obj[1] !== undefined) {
        // 修改
      } else if (obj[0] === undefined) {
        // 新增
        added++
      } else if (obj[1] === undefined) {
        // 删除
        deleted++
      }
    } else if (typeof obj === 'object' && obj !== null) {
      Object.values(obj).forEach(traverse)
    }
  }

  traverse(deltaObj)
  return { added, deleted }
}

// 渲染差异
function renderDiff() {
  if (!diffContainer.value || !props.source || !props.target) {
    if (diffContainer.value) diffContainer.value.innerHTML = ''
    return
  }

  try {
    const diff = jsondiffpatch.diff(props.source, props.target)
    if (!diff) {
      diffContainer.value.innerHTML = '<div class="no-diff">无差异</div>'
      stats.value = { added: 0, deleted: 0 }
      delta.value = null
      return
    }

    delta.value = diff
    // 尝试多种可能的调用方式
    let html
    if (typeof htmlFormatterModule === 'function') {
      html = htmlFormatterModule(diff, props.source)
    } else if (htmlFormatterModule.format) {
      html = htmlFormatterModule.format(diff, props.source)
    } else if (htmlFormatterModule.default?.format) {
      html = htmlFormatterModule.default.format(diff, props.source)
    } else {
      throw new Error('HTML formatter not found')
    }
    diffContainer.value.innerHTML = html
    stats.value = countDelta(diff)
  } catch (error) {
    console.error('差异计算失败:', error)
    diffContainer.value.innerHTML = '<div class="error">差异计算失败</div>'
  }
}

// 监听源和目标变化
watch(() => [props.source, props.target], renderDiff, { deep: true, immediate: true })

// 组件销毁前清理（如果有必要）
onBeforeUnmount(() => {
  if (diffContainer.value) diffContainer.value.innerHTML = ''
})
</script>

<style lang="scss" scoped>
.diff-viewer {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  font-family: monospace;
  font-size: 14px;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;

  .version-info {
    display: flex;
    gap: 12px;
    font-size: 14px;

    .label {
      font-weight: 600;
      color: #303133;
    }
    .time {
      color: #909399;
      font-size: 12px;
    }
  }
}

.diff-container {
  padding: 16px;
  overflow-x: auto;
  max-height: 500px;
  background: #fff;
}

.diff-stats {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

// 覆盖 jsondiffpatch 默认样式，与项目风格统一
:deep(.jsondiffpatch-delta) {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  font-size: 13px;
  line-height: 1.5;
}
:deep(.jsondiffpatch-added) {
  background-color: #e6ffed;
  border-left: 3px solid #28a745;
}
:deep(.jsondiffpatch-deleted) {
  background-color: #ffeef0;
  border-left: 3px solid #d73a49;
}
:deep(.jsondiffpatch-modified) {
  background-color: #fff5b1;
}
:deep(.jsondiffpatch-equal) {
  background-color: transparent;
}
:deep(.jsondiffpatch-property) {
  font-weight: 600;
  color: #24292e;
}
:deep(.jsondiffpatch-value) {
  color: #0366d6;
}
</style>