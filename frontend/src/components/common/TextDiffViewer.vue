<template>
  <div class="text-diff-viewer">
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

    <div v-if="diffHtml" class="diff-html" v-html="diffHtml"></div>
    <div v-else-if="loading" class="loading">计算差异中...</div>
    <div v-else class="no-diff">无差异</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import * as Diff from 'diff'
import { html as Diff2Html } from 'diff2html'
import 'diff2html/bundles/css/diff2html.min.css'
import { tiptapToText } from '@/utils/tiptapToText'

const props = withDefaults(defineProps<{
  source?: any
  target?: any
  sourceLabel?: string
  targetLabel?: string
  sourceTime?: string
  targetTime?: string
  showHeader?: boolean
}>(), {
  sourceLabel: '原文',
  targetLabel: '修改后',
  showHeader: true
})

const diffHtml = ref('')
const loading = ref(false)

const generateDiff = () => {

  if (!props.source || !props.target) {
    console.warn('[TextDiffViewer] 警告：source 或 target 为空!')
    diffHtml.value = ''
    return
  }

  loading.value = true
  try {
    // 【调试】转换为文本并打印
    const sourceText = tiptapToText(props.source)
    const targetText = tiptapToText(props.target)

    const patch = Diff.createTwoFilesPatch(
      'source.txt', 'target.txt',
      sourceText, targetText,
      props.sourceLabel, props.targetLabel,
      { context: 3 }
    )

    diffHtml.value = Diff2Html(patch, {
      drawFileList: false,
      matching: 'lines',
      outputFormat: 'side-by-side',
      renderNothingWhenEmpty: true
    })

  } catch (error) {
    console.error('生成差异失败', error)
    diffHtml.value = '<div class="error">差异生成失败</div>'
  } finally {
    loading.value = false
  }
}

watch(() => [props.source, props.target], generateDiff, { deep: true, immediate: true })
</script>

<style scoped>
.text-diff-viewer {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.version-info {
  display: flex;
  gap: 12px;
  font-size: 14px;
}

.label {
  font-weight: 600;
  color: #303133;
}

.time {
  color: #909399;
  font-size: 12px;
}

.diff-html {
  padding: 16px;
  overflow-x: auto;
  max-height: 500px;
}

.loading, .no-diff {
  padding: 40px;
  text-align: center;
  color: #909399;
}

.error {
  color: #d73a49;
  padding: 20px;
  text-align: center;
}
</style>
