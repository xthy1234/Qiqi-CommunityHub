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
        <div
          class="diff-content"
          v-html="originalHtml"
        ></div>
      </div>

      <div class="diff-divider"></div>

      <div class="diff-panel modified">
        <div class="panel-title">
          <Icon :icon="targetIcon" width="16" />
          <span>{{ targetTitle }}</span>
        </div>
        <div
          class="diff-content"
          v-html="modifiedHtml"
        ></div>
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
import { ref, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { NTag } from 'naive-ui'
import { 
  calculateDiff, 
  renderDiffHtml, 
  jsonToReadableText,
  countDiffLines 
} from '@/utils/articleDiff'

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

const originalHtml = ref('')
const modifiedHtml = ref('')
const stats = ref({ added: 0, deleted: 0 })

// 计算并渲染差异
const computeAndRenderDiff = () => {
  if (!props.sourceContent || !props.targetContent) {
    originalHtml.value = '<p style="color: #999;">无内容</p>'
    modifiedHtml.value = '<p style="color: #999;">无内容</p>'
    stats.value = { added: 0, deleted: 0 }
    return
  }

  try {
    // 转换为可读文本
    const sourceText = jsonToReadableText(props.sourceContent)
    const targetText = jsonToReadableText(props.targetContent)

    // 计算差异
    const delta = calculateDiff(sourceText, targetText)

    // 渲染 HTML
    originalHtml.value = renderDiffHtml(delta, 'source')
    modifiedHtml.value = renderDiffHtml(delta, 'target')

    // 统计行数
    const diffStats = countDiffLines(delta)
    stats.value = diffStats
  } catch (error) {
    console.error('差异计算失败:', error)
    originalHtml.value = '<p style="color: #f00;">差异计算失败</p>'
    modifiedHtml.value = '<p style="color: #f00;">差异计算失败</p>'
  }
}

watch(() => [props.sourceContent, props.targetContent], () => {
  computeAndRenderDiff()
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

      :deep(ins) {
        background: #e6ffed;
        color: #24292e;
        text-decoration: none;
        padding: 2px 6px;
        border-radius: 4px;
        display: inline-block;
        margin: 2px 0;
      }

      :deep(del) {
        background: #ffeef0;
        color: #cb2431;
        text-decoration: line-through;
        padding: 2px 6px;
        border-radius: 4px;
        display: inline-block;
        margin: 2px 0;
      }

      :deep(span) {
        color: #606266;
      }
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
