<template>
  <node-view-wrapper class="video-node-wrapper">
    <div class="video-container" ref="videoContainerRef">
      <video
        ref="videoRef"
        :poster="posterUrl"
        playsinline
        controls
        crossorigin
      >
        <source
          v-if="videoSrc"
          :src="videoSrc"
          type="video/mp4"
        />
        您的浏览器不支持 HTML5 视频播放
      </video>

      <!-- 注释标记点容器（只读模式显示） -->
      <div
        v-if="annotations.length > 0 && !isEditable"
        class="annotation-markers"
        ref="markersRef"
      >
        <div
          v-for="annotation in annotations"
          :key="annotation.id"
          class="annotation-marker"
          :style="getMarkerStyle(annotation)"
          :title="annotation.title"
          @click.stop="jumpToTime(annotation.startTime)"
        >
          <div class="marker-dot" />
          <div class="marker-tooltip">
            <div class="tooltip-title">{{ annotation.title }}</div>
            <div class="tooltip-time">{{ formatTimeRange(annotation.startTime, annotation.endTime) }}</div>
            <div class="tooltip-content">{{ annotation.content }}</div>
          </div>
        </div>
      </div>

      <!-- 当前时间附近的注释提示（只读模式） -->
      <transition name="fade">
        <div
          v-if="currentAnnotation && !isEditable"
          class="current-annotation-tip"
        >
          <div class="tip-header">
            <n-icon size="18" color="#2080f0">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
              </svg>
            </n-icon>
            <span class="tip-title">{{ currentAnnotation.title }}</span>
          </div>
          <div class="tip-time">{{ formatTimeRange(currentAnnotation.startTime, currentAnnotation.endTime) }}</div>
          <div class="tip-content">{{ currentAnnotation.content }}</div>
        </div>
      </transition>
    </div>

    <!-- 视频信息卡片 -->
    <div class="video-info-card">
      <div class="video-meta">
        <div class="video-title">
          {{ videoTitle || '视频内容' }}
        </div>
        <div
          v-if="videoDuration"
          class="video-duration"
        >
          时长: {{ formatDuration(videoDuration) }}
        </div>
      </div>
      <div class="video-actions">
        <n-tag
          v-if="annotations.length > 0"
          type="info"
          size="small"
        >
          <template #icon>
            <n-icon>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
              </svg>
            </n-icon>
          </template>
          {{ annotations.length }} 个注释
        </n-tag>

        <!-- 编辑器模式下显示管理按钮 -->
        <n-button
          v-if="isEditable"
          size="small"
          type="primary"
          @click="openAnnotationEditor"
        >
          <template #icon>
            <n-icon>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                <path fill="currentColor" d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83l3.75 3.75l1.83-1.83z"/>
              </svg>
            </n-icon>
          </template>
          管理注释
        </n-button>
      </div>
    </div>

    <!-- 注释编辑对话框 -->
    <VideoAnnotationEditor
      v-model:show="annotationEditorVisible"
      :video-src="videoSrc"
      :poster-url="posterUrl"
      :annotations="annotations"
      :video-duration="videoDuration"
      @submit="handleAddAnnotation"
      @delete="handleDeleteAnnotation"
      @jump="jumpToTime"
    />
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { NIcon, NTag, NButton, useMessage } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'
import Plyr from 'plyr'
import 'plyr/dist/plyr.css'
import { normalizeFileUrl } from '@/utils/fileUrl'
import VideoAnnotationEditor from './VideoAnnotationEditor.vue'

interface Annotation {
  id: string
  startTime: number
  endTime?: number
  title: string
  content: string
}

interface Props {
  editor: any
  node: {
    attrs: {
      src?: string
      poster?: string
      title?: string
      duration?: number
      annotations?: Annotation[]
    }
  }
  decorations: any[]
  selected: boolean
  updateAttributes: (attrs: Record<string, any>) => void
}

const props = withDefaults(defineProps<Props>(), {
  editor: null,
  node: () => ({ attrs: {} }),
  decorations: () => [],
  selected: false,
  updateAttributes: () => {}
})

const message = useMessage()

// Refs
const videoRef = ref<HTMLVideoElement | null>(null)
const videoContainerRef = ref<HTMLDivElement | null>(null)
const markersRef = ref<HTMLDivElement | null>(null)
let player: Plyr | null = null

// 判断是否为可编辑模式
const isEditable = computed(() => {
  return props.editor?.isEditable === true
})

// 从 node.attrs 读取属性并标准化 URL
const videoSrc = computed(() => {
  const src = props.node?.attrs?.src || ''
  return normalizeFileUrl(src)
})

const posterUrl = computed(() => {
  const poster = props.node?.attrs?.poster || ''
  return normalizeFileUrl(poster)
})

const videoTitle = computed(() => props.node?.attrs?.title || '')
const videoDuration = computed(() => props.node?.attrs?.duration || 0)
const annotations = computed<Annotation[]>(() => props.node?.attrs?.annotations || [])

// 当前显示的注释
const currentAnnotation = ref<Annotation | null>(null)
let currentTimeUpdateHandler: ((event: Event) => void) | null = null

// 注释编辑器可见性
const annotationEditorVisible = ref(false)

// 计算标记点样式
const getMarkerStyle = (annotation: Annotation): Record<string, string> => {
  const duration = videoDuration.value || player?.duration || 0
  if (!duration || duration === 0) return { left: '0%' }

  const startPercent = (annotation.startTime / duration) * 100

  // 如果有结束时间，显示为区间
  if (annotation.endTime && annotation.endTime > annotation.startTime) {
    const endPercent = (annotation.endTime / duration) * 100
    const width = endPercent - startPercent
    return {
      left: `${startPercent}%`,
      width: `${width}%`
    }
  }

  return { left: `${startPercent}%` }
}

// 格式化时间范围
const formatTimeRange = (startTime: number, endTime?: number): string => {
  if (endTime && endTime > startTime) {
    return `${formatTime(startTime)}-${formatTime(endTime)}`
  }
  return formatTime(startTime)
}

// 格式化时长
const formatDuration = (seconds: number): string => {
  if (!seconds || seconds === 0) return '00:00'
  
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 格式化时间
const formatTime = (seconds: number): string => {
  if (!seconds || seconds === 0) return '00:00'

  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  const ms = Math.floor((seconds % 1) * 100)

  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}`
}

// 跳转到指定时间
const jumpToTime = (time: number) => {
  if (player) {
    player.currentTime = time
    player.play()
    
    message.success(`已跳转到 ${formatTime(time)}`)
  }
}

// 查找当前时间附近的注释（前后5秒范围内）
const findCurrentAnnotation = (currentTime: number): Annotation | null => {
  const threshold = 5
  return annotations.value.find(annotation => {
    const startDiff = Math.abs(annotation.startTime - currentTime)
    if (startDiff <= threshold) return true

    if (annotation.endTime) {
      const endDiff = Math.abs(annotation.endTime - currentTime)
      if (endDiff <= threshold) return true

      // 检查是否在时间段内
      if (currentTime >= annotation.startTime && currentTime <= annotation.endTime) {
        return true
      }
    }

    return false
  }) || null
}

// 初始化播放器
const initPlayer = () => {
  if (!videoRef.value) return

  try {
    player = new Plyr(videoRef.value, {
      controls: [
        'play-large',
        'play',
        'progress',
        'current-time',
        'mute',
        'volume',
        'captions',
        'settings',
        'pip',
        'airplay',
        'fullscreen'
      ],
      settings: ['captions', 'quality', 'speed'],
      speed: { selected: 1, options: [0.5, 0.75, 1, 1.25, 1.5, 2] },
      tooltips: { controls: true, seek: true },
      keyboard: { focused: true, global: false }
    })

    player.on('loadedmetadata', () => {
      renderAnnotationMarkers()
    })

    currentTimeUpdateHandler = (event: Event) => {
      const plyrEvent = event as CustomEvent
      const currentTime = plyrEvent.detail?.plyr?.currentTime || player?.currentTime || 0
      
      const annotation = findCurrentAnnotation(currentTime)
      currentAnnotation.value = annotation
    }
    
    player.on('timeupdate', currentTimeUpdateHandler)

    player.on('error', (error) => {
      console.error('❌ [VideoNode] 视频播放错误:', error)
      message.error('视频加载失败，请检查网络连接')
    })

  } catch (error) {
    console.error('❌ [VideoNode] 播放器初始化失败:', error)
    message.error('视频播放器初始化失败')
  }
}

const renderAnnotationMarkers = () => {
  if (!markersRef.value || annotations.value.length === 0) return
}

const destroyPlayer = () => {
  if (player) {
    player.destroy()
    player = null
  }
}

// 打开注释编辑器
const openAnnotationEditor = () => {
  annotationEditorVisible.value = true
}

// 处理添加注释
const handleAddAnnotation = (annotationData: Omit<Annotation, 'id'>) => {
  // 生成唯一ID
  const newAnnotation: Annotation = {
    id: `ann_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    ...annotationData
  }

  const newAnnotations = [...annotations.value, newAnnotation]
  props.updateAttributes({
    annotations: newAnnotations
  })
  message.success('注释添加成功')
}

// 处理删除注释
const handleDeleteAnnotation = (index: number) => {
  const newAnnotations = annotations.value.filter((_, i) => i !== index)
  props.updateAttributes({
    annotations: newAnnotations
  })
  message.success('注释已删除')
}

onMounted(() => {
  setTimeout(() => {
    initPlayer()
  }, 100)
})

onBeforeUnmount(() => {
  destroyPlayer()
})

watch(() => props.node?.attrs?.annotations, (newAnnotations) => {
  // 注释更新时的处理
}, { deep: true })
</script>

<style scoped lang="scss">
.video-node-wrapper {
  margin: 16px 0;
  display: block;
}

.video-container {
  position: relative;
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  background: #000;

  :deep(.plyr) {
    --plyr-color-main: #2080f0;
    --plyr-video-background: #000;
    --plyr-audio-background: #fff;
    
    border-radius: 12px;
    overflow: hidden;
  }

  .annotation-markers {
    position: absolute;
    bottom: 52px;
    left: 0;
    right: 0;
    height: 20px;
    pointer-events: none;
    z-index: 10;

    .annotation-marker {
      position: absolute;
      bottom: 0;
      transform: translateX(-50%);
      cursor: pointer;
      pointer-events: auto;
      transition: all 0.3s ease;

      .marker-dot {
        width: 12px;
        height: 12px;
        background: #2080f0;
        border-radius: 50%;
        border: 2px solid #fff;
        box-shadow: 0 2px 8px rgba(32, 128, 240, 0.4);
        transition: all 0.3s ease;
      }

      .marker-tooltip {
        position: absolute;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%) scale(0.8);
        background: rgba(0, 0, 0, 0.9);
        color: #fff;
        padding: 10px 14px;
        border-radius: 6px;
        opacity: 0;
        visibility: hidden;
        transition: all 0.3s ease;
        pointer-events: none;
        min-width: 180px;
        max-width: 280px;

        .tooltip-title {
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 4px;
        }

        .tooltip-time {
          font-size: 12px;
          color: #2080f0;
          font-family: 'Courier New', monospace;
          margin-bottom: 4px;
        }

        .tooltip-content {
          font-size: 12px;
          opacity: 0.9;
          line-height: 1.4;
        }

        &::after {
          content: '';
          position: absolute;
          top: 100%;
          left: 50%;
          transform: translateX(-50%);
          border: 6px solid transparent;
          border-top-color: rgba(0, 0, 0, 0.9);
        }
      }

      &:hover {
        .marker-dot {
          transform: scale(1.3);
          background: #ff6b6b;
          box-shadow: 0 4px 12px rgba(255, 107, 107, 0.6);
        }

        .marker-tooltip {
          opacity: 1;
          visibility: visible;
          transform: translateX(-50%) scale(1);
        }
      }
    }
  }

  .current-annotation-tip {
    position: absolute;
    top: 16px;
    right: 16px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 12px 16px;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    max-width: 300px;
    z-index: 20;

    .tip-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 6px;

      .tip-title {
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }
    }

    .tip-time {
      font-size: 12px;
      color: #2080f0;
      font-family: 'Courier New', monospace;
      margin-bottom: 6px;
    }

    .tip-content {
      font-size: 13px;
      color: #666;
      line-height: 1.5;
    }
  }
}

.video-info-card {
  margin-top: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  .video-meta {
    flex: 1;
    min-width: 0;

    .video-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .video-duration {
      font-size: 13px;
      color: #999;
    }
  }

  .video-actions {
    flex-shrink: 0;
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
