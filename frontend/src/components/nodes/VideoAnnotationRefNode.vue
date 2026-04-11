<!-- src/components/nodes/VideoAnnotationRefNode.vue -->
<template>
  <node-view-wrapper class="annotation-ref-wrapper">
    <span 
      class="annotation-ref"
      :title="tooltipText"
      @click="handleClick"
    >
      <n-icon size="14" color="#2080f0">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
          <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5l-6 4.5z"/>
        </svg>
      </n-icon>
      <span class="ref-number">{{ refNumber }}</span>
    </span>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'

interface Props {
  node: {
    attrs: {
      videoNodeId: string
      annotationId: string
      displayText?: string
    }
  }
  editor: any
}

const props = defineProps<Props>()

// 从文档中查找对应的视频节点和注释
const annotationData = computed(() => {
  const doc = props.editor?.state?.doc
  if (!doc) return null
  
  let foundAnnotation = null
  
  // 遍历文档找到对应的视频节点
  doc.descendants((node: any) => {
    if (node.type.name === 'videoNode') {
      const annotations = node.attrs.annotations || []
      const annotation = annotations.find((a: any) => a.id === props.node.attrs.annotationId)
      if (annotation) {
        foundAnnotation = {
          ...annotation,
          videoTitle: node.attrs.title || '未命名视频'
        }
      }
    }
  })
  
  return foundAnnotation
})

const refNumber = computed(() => {
  return annotationData.value ? '①' : '?'  // 可以计算是第几个引用
})

const tooltipText = computed(() => {
  if (!annotationData.value) return '注释不存在'
  const { startTime, endTime, title, videoTitle } = annotationData.value
  const timeRange = endTime 
    ? `${formatTime(startTime)}-${formatTime(endTime)}`
    : formatTime(startTime)
  return `${videoTitle} @ ${timeRange}: ${title}`
})

const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const handleClick = () => {
  if (!annotationData.value) return
  
  // TODO: 滚动到视频节点并跳转到对应时间
  console.log('跳转到:', annotationData.value)
}
</script>

<style scoped lang="scss">
.annotation-ref-wrapper {
  display: inline;
}

.annotation-ref {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  background: #ecf5ff;
  border: 1px solid #2080f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
  vertical-align: super;
  
  &:hover {
    background: #2080f0;
    color: #fff;
    
    .ref-number {
      color: #fff;
    }
  }
  
  .ref-number {
    font-weight: 600;
    color: #2080f0;
  }
}
</style>
