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

      <!-- 注释标记点容器 -->
      <div
        v-if="annotations.length > 0"
        class="annotation-markers"
        ref="markersRef"
      >
        <div
          v-for="(annotation, index) in annotations"
          :key="index"
          class="annotation-marker"
          :style="{ left: getMarkerPosition(annotation.time) + '%' }"
          :title="annotation.title"
          @click.stop="jumpToTime(annotation.time)"
        >
          <div class="marker-dot" />
          <div class="marker-tooltip">
            <div class="tooltip-title">{{ annotation.title }}</div>
            <div class="tooltip-content">{{ annotation.content }}</div>
          </div>
        </div>
      </div>

      <!-- 当前时间附近的注释提示 -->
      <transition name="fade">
        <div
          v-if="currentAnnotation"
          class="current-annotation-tip"
        >
          <div class="tip-header">
            <n-icon size="18" color="#2080f0">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="18"
                height="18"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
                />
              </svg>
            </n-icon>
            <span class="tip-title">{{ currentAnnotation.title }}</span>
          </div>
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
      <div
        v-if="annotations.length > 0"
        class="annotation-count"
      >
        <n-tag
          type="info"
          size="small"
        >
          <template #icon>
            <n-icon>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="14"
                height="14"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
                />
              </svg>
            </n-icon>
          </template>
          {{ annotations.length }} 个注释点
        </n-tag>
      </div>
    </div>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { NIcon, NTag } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'
import Plyr from 'plyr'
import 'plyr/dist/plyr.css'
import { useGlobalProperties } from '@/utils/globalProperties'
import { normalizeFileUrl } from '@/utils/fileUrl'

interface Annotation {
  time: number
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
}

const props = withDefaults(defineProps<Props>(), {
  editor: null,
  node: () => ({ attrs: {} }),
  decorations: () => []
})

const appContext = useGlobalProperties()

// Refs
const videoRef = ref<HTMLVideoElement | null>(null)
const videoContainerRef = ref<HTMLDivElement | null>(null)
const markersRef = ref<HTMLDivElement | null>(null)
let player: Plyr | null = null

// 【修复】从 node.attrs 读取属性并标准化 URL
const videoSrc = computed(() => {
  const src = props.node?.attrs?.src || ''
  // 使用 normalizeFileUrl 确保 URL 正确拼接 baseUrl
  return normalizeFileUrl(src)
})

const posterUrl = computed(() => {
  const poster = props.node?.attrs?.poster || ''
  // 如果有封面图，也进行 URL 标准化
  return normalizeFileUrl(poster)
})

const videoTitle = computed(() => props.node?.attrs?.title || '')
const videoDuration = computed(() => props.node?.attrs?.duration || 0)
const annotations = computed<Annotation[]>(() => props.node?.attrs?.annotations || [])

// 当前显示的注释
const currentAnnotation = ref<Annotation | null>(null)
let currentTimeUpdateHandler: ((event: Event) => void) | null = null

// 计算标记点位置（百分比）
const getMarkerPosition = (time: number): number => {
  const duration = videoDuration.value || player?.duration || 0
  if (!duration || duration === 0) return 0
  return (time / duration) * 100
}

// 格式化时长
const formatDuration = (seconds: number): string => {
  if (!seconds || seconds === 0) return '00:00'
  
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 跳转到指定时间
const jumpToTime = (time: number) => {
  if (player) {
    player.currentTime = time
    player.play()
    
    // 显示提示
    appContext?.$message.success(`已跳转到 ${formatDuration(time)}`)
  }
}

// 查找当前时间附近的注释（前后5秒范围内）
const findCurrentAnnotation = (currentTime: number): Annotation | null => {
  const threshold = 5 // 5秒阈值
  
  return annotations.value.find(annotation => 
    Math.abs(annotation.time - currentTime) <= threshold
  ) || null
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

    // 监听元数据加载完成
    player.on('loadedmetadata', () => {

      
      // 更新视频时长（如果后端未提供）
      if (!videoDuration.value && player?.duration) {
        // 可以在这里触发更新 node.attrs.duration
      }
      
      // 渲染注释标记点
      renderAnnotationMarkers()
    })

    // 监听时间更新
    currentTimeUpdateHandler = (event: Event) => {
      const plyrEvent = event as CustomEvent
      const currentTime = plyrEvent.detail?.plyr?.currentTime || player?.currentTime || 0
      
      const annotation = findCurrentAnnotation(currentTime)
      currentAnnotation.value = annotation
    }
    
    player.on('timeupdate', currentTimeUpdateHandler)

    // 监听播放
    player.on('play', () => {

    })

    // 监听暂停
    player.on('pause', () => {

    })

    // 监听错误
    player.on('error', (error) => {
      console.error('❌ [VideoNode] 视频播放错误:', error)
      appContext?.$message.error('视频加载失败，请检查网络连接')
    })

  } catch (error) {
    console.error('❌ [VideoNode] 播放器初始化失败:', error)
    appContext?.$message.error('视频播放器初始化失败')
  }
}

// 渲染注释标记点
const renderAnnotationMarkers = () => {
  if (!markersRef.value || annotations.value.length === 0) return

  
  // 标记点已通过 Vue 模板渲染，无需手动操作 DOM
}

// 销毁播放器
const destroyPlayer = () => {
  if (player) {
    player.destroy()
    player = null

  }
}

// 生命周期
onMounted(() => {

  
  // 延迟初始化，确保 DOM 完全渲染
  setTimeout(() => {
    initPlayer()
  }, 100)
})

onBeforeUnmount(() => {
  destroyPlayer()
})

// 监听 annotations 变化
watch(() => props.node?.attrs?.annotations, (newAnnotations) => {

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

  // Plyr 样式覆盖
  :deep(.plyr) {
    --plyr-color-main: #2080f0;
    --plyr-video-background: #000;
    --plyr-audio-background: #fff;
    
    border-radius: 12px;
    overflow: hidden;
  }

  // 注释标记点容器
  .annotation-markers {
    position: absolute;
    bottom: 52px; // Plyr 控制栏高度
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
        padding: 8px 12px;
        border-radius: 6px;
        white-space: nowrap;
        opacity: 0;
        visibility: hidden;
        transition: all 0.3s ease;
        pointer-events: none;
        min-width: 150px;
        max-width: 250px;
        white-space: normal;

        .tooltip-title {
          font-size: 13px;
          font-weight: 600;
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

  // 当前注释提示
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

    .tip-content {
      font-size: 13px;
      color: #666;
      line-height: 1.5;
    }
  }
}

// 视频信息卡片
.video-info-card {
  margin-top: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;

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

  .annotation-count {
    flex-shrink: 0;
    margin-left: 12px;
  }
}

// 淡入淡出动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
