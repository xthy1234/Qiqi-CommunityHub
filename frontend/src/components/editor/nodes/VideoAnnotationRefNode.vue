<script setup lang="ts">
import { computed } from 'vue'
import { NIcon, useMessage } from 'naive-ui'
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
const message = useMessage()

// 从文档中查找对应的视频节点和注释
const annotationData = computed(() => {
  const doc = props.editor?.state?.doc
  if (!doc) {return null}
  
  let foundAnnotation = null
  
  doc.descendants((node: any) => {
    if (node.type.name === 'videoNode') {
      const annotations = node.attrs.annotations || []
      const annotation = annotations.find((a: any) => a.id === props.node.attrs.annotationId)

      if (annotation) {
        let videoNodeId = node.attrs.id

        if (!videoNodeId) {
          const src = node.attrs.src || ''
          if (src) {
            let hash = 0
            for (let i = 0; i < src.length; i++) {
              const char = src.charCodeAt(i)
              hash = ((hash << 5) - hash) + char
              hash = hash & hash
            }
            videoNodeId = `video_${Math.abs(hash).toString(36)}`
          } else {
            videoNodeId = `video_${Date.now()}`
          }
        }

        foundAnnotation = {
          ...annotation,
          videoTitle: node.attrs.title || '未命名视频',
          videoNodeId
        }
      }
    }
  })
  
  return foundAnnotation
})

// 计算该引用是第几个引用相同注释的标记
const refIndex = computed(() => {
  if (!annotationData.value) {return -1}

  const doc = props.editor?.state?.doc
  if (!doc) {return -1}

  let count = 0
  let currentIndex = -1

  // 获取当前节点在文档中的位置
  let targetPos = -1
  doc.descendants((node: any, pos: number) => {
    if (node === props.node) {
      targetPos = pos
      return false
    }
  })

  // 通过位置匹配计算序号
  doc.descendants((node: any, pos: number) => {
    if (node.type.name === 'videoAnnotationRef' &&
        node.attrs.annotationId === props.node.attrs.annotationId) {
      count++

      if (pos === targetPos) {
        currentIndex = count
      }
    }
  })

  return currentIndex > 0 ? currentIndex : -1
})

// 显示文本：使用图标 + 序号
const refDisplay = computed(() => {
  if (refIndex.value > 0) {
    return `${refIndex.value}`
  }
  return '?'
})

const tooltipText = computed(() => {
  if (!annotationData.value) {return '注释不存在'}
  const { startTime, endTime, title, videoTitle } = annotationData.value
  const timeRange = endTime 
    ? `${formatTime(startTime)}-${formatTime(endTime)}`
    : formatTime(startTime)
  return `${videoTitle} @ ${timeRange}: ${title}`
})

const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const handleClick = () => {
  if (!annotationData.value) {
    message.warning('引用的注释不存在或已被删除')
    return
  }
  
  const { videoNodeId, startTime, title } = annotationData.value

  window.dispatchEvent(new CustomEvent('scroll-to-video-annotation', {
    detail: {
      videoNodeId,
      annotationId: props.node.attrs.annotationId,
      time: startTime,
      title,
      videoTitle: annotationData.value.videoTitle
    }
  }))
}

</script>

<template>
  <node-view-wrapper class="annotation-ref-wrapper">
    <span
      class="annotation-ref"
      :title="tooltipText"
      @click="handleClick"
    >
      <n-icon
        size="14"
        color="#2080f0"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
        >
          <path
            fill="currentColor"
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5l-6 4.5z"
          />
        </svg>
      </n-icon>
      <span class="ref-number">{{ refDisplay }}</span>
    </span>
  </node-view-wrapper>
</template>
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
