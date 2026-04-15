<template>
  <node-view-wrapper class="video-node-wrapper">
    <div
      ref="videoContainerRef"
      class="video-container"
    >
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
        ref="markersRef"
        class="annotation-markers"
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
            <div class="tooltip-title">
              {{ annotation.title }}
            </div>
            <div class="tooltip-time">
              {{ formatTimeRange(annotation.startTime, annotation.endTime) }}
            </div>
            <div class="tooltip-content">
              {{ annotation.content }}
            </div>
          </div>
        </div>
      </div>

      <!-- 当前时间附近的注释提示（只读模式）- 支持收起 -->
      <transition name="slide-fade">
        <div
          v-if="currentAnnotation && !isEditable && !isAnnotationCollapsed"
          class="current-annotation-tip"
        >
          <div class="tip-header">
            <n-icon
              size="18"
              color="#2080f0"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
                />
              </svg>
            </n-icon>
            <span class="tip-title">{{ currentAnnotation.title }}</span>
            <n-button
              text
              size="tiny"
              class="collapse-btn"
              @click="toggleAnnotationCollapse"
            >
              <template #icon>
                <n-icon>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                  >
                    <path
                      fill="currentColor"
                      d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"
                    />
                  </svg>
                </n-icon>
              </template>
            </n-button>
          </div>
          <div class="tip-time">
            {{ formatTimeRange(currentAnnotation.startTime, currentAnnotation.endTime) }}
          </div>
          <div class="tip-content">
            {{ currentAnnotation.content }}
          </div>
        </div>
      </transition>

      <!-- 收起状态的注释提示 -->
      <transition name="slide-fade">
        <div
          v-if="currentAnnotation && !isEditable && isAnnotationCollapsed"
          class="collapsed-annotation-tip"
          @click="toggleAnnotationCollapse"
        >
          <n-icon
            size="16"
            color="#2080f0"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
              />
            </svg>
          </n-icon>
        </div>
      </transition>

      <!-- ⚠️ 关键修复：使用 Canvas 作为弹幕容器 -->
      <canvas
        v-if="showDanmaku && !isEditable"
        ref="danmakuCanvasRef"
        class="danmaku-container"
      />
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
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
                />
              </svg>
            </n-icon>
          </template>
          {{ annotations.length }} 个注释
        </n-tag>

        <!-- 编辑器模式下显示管理按钮 -->
        <n-button
          v-if="showAnnotationManager"
          size="small"
          type="primary"
          @click="openAnnotationEditor"
        >
          <template #icon>
            <n-icon>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83l3.75 3.75l1.83-1.83z"
              />
            </svg>
          </n-icon>
        </template>
        管理注释
      </n-button>
      </div>

      <!-- 弹幕控制区域（移到信息卡片下方） -->
      <div
        v-if="!isEditable && showDanmakuToggle"
        class="danmaku-controls-bottom"
      >
        <n-button
          size="small"
          :type="showDanmaku ? 'primary' : 'default'"
          @click="toggleDanmaku"
        >
          <template #icon>
            <n-icon>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4V6h16v12zM6 10h2v2H6zm0 4h2v2H6zm4-4h2v2h-2zm0 4h2v2h-2zm4-4h2v2h-2zm0 4h2v2h-2z"
                />
              </svg>
            </n-icon>
          </template>
          {{ showDanmaku ? '关闭弹幕' : '开启弹幕' }}
        </n-button>

        <!-- 弹幕发送框 -->
        <transition name="slide-down">
          <div
            v-if="showDanmaku"
            class="danmaku-input-area-bottom"
          >
            <n-input
              v-model:value="danmakuInput"
              placeholder="输入弹幕内容..."
              size="small"
              maxlength="200"
              show-count
              @keydown.enter="sendDanmaku"
            >
              <template #suffix>
                <n-button
                  text
                  type="primary"
                  :disabled="!danmakuInput.trim()"
                  @click="sendDanmaku"
                >
                  <template #icon>
                    <n-icon>
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                      >
                        <path
                          fill="currentColor"
                          d="M2.01 21L23 12L2.01 3L2 10l15 2l-15 2z"
                        />
                      </svg>
                    </n-icon>
                  </template>
                </n-button>
              </template>
            </n-input>

            <!-- 弹幕设置 -->
            <div class="danmaku-settings">
              <n-popover
                trigger="click"
                placement="bottom-end"
              >
                <template #trigger>
                  <n-button
                    text
                    size="tiny"
                  >
                    <template #icon>
                      <n-icon>
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          viewBox="0 0 24 24"
                        >
                          <path
                            fill="currentColor"
                            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8s8 3.59 8 8s-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5L9.99 9.99L6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1s-1.1-.49-1.1-1.1s.49-1.1 1.1-1.1z"
                          />
                        </svg>
                      </n-icon>
                    </template>
                  </n-button>
                </template>
                <div class="danmaku-config">
                  <div class="config-item">
                    <span class="label">颜色:</span>
                    <n-color-picker
                      v-model:value="danmakuColor"
                      :swatches="['#FFFFFF', '#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF']"
                      size="small"
                    />
                  </div>
                  <div class="config-item">
                    <span class="label">位置:</span>
                    <n-select
                      v-model:value="danmakuPosition"
                      :options="[
                        { label: '滚动', value: 0 },
                        { label: '顶部', value: 1 },
                        { label: '底部', value: 2 }
                      ]"
                      size="small"
                    />
                  </div>
                  <div class="config-item">
                    <span class="label">大小:</span>
                    <n-slider
                      v-model:value="danmakuFontSize"
                      :min="12"
                      :max="40"
                      :step="2"
                      :marks="{ 12: '小', 25: '中', 40: '大' }"
                    />
                  </div>
                </div>
              </n-popover>
            </div>
          </div>
        </transition>
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
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { NIcon, NTag, NButton, NInput, NPopover, NColorPicker, NSelect, NSlider, useMessage } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'
import Plyr from 'plyr'
import 'plyr/dist/plyr.css'
import { normalizeFileUrl } from '@/utils/fileUrl'
import VideoAnnotationEditor from './VideoAnnotationEditor.vue'
import { videoDanmakuAPI } from '@/api/videoDanmaku'

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
const danmakuCanvasRef = ref<HTMLCanvasElement | null>(null)
let player: Plyr | null = null

// 生成唯一的视频节点ID（用于注释引用跳转）
const videoNodeId = computed(() => {
  // ⚠️ 优先使用已保存的 ID
  if (props.node?.attrs?.id) {
    return props.node.attrs.id
  }

  // ⚠️ 如果没有 ID，使用视频 URL 的哈希作为稳定 ID
  const src = props.node?.attrs?.src || ''
  if (src) {
    let hash = 0
    for (let i = 0; i < src.length; i++) {
      const char = src.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash
    }
    return `video_${Math.abs(hash).toString(36)}`
  }

  // 最后才使用时间戳
  return `video_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
})

// 注释相关状态
const annotationEditorVisible = ref(false)
const currentAnnotation = ref<Annotation | null>(null)
const isAnnotationCollapsed = ref(false)

// 弹幕相关状态
interface DanmakuItem {
  text: string
  time: number
  color: string
  fontSize: number
  position: 'scroll' | 'top' | 'bottom'
  x?: number
  y?: number
  speed?: number
}

const showDanmaku = ref(false)
const showDanmakuToggle = ref(false)
const danmakuData = ref<any[]>([])
const danmakuInput = ref('')
const danmakuColor = ref('#FFFFFF')
const danmakuPosition = ref(0)
const danmakuFontSize = ref(25)
const activeDanmakus = ref<DanmakuItem[]>([])  // 当前活跃的弹幕
let animationFrameId: number | null = null
let lastVideoTime = 0

// 从本地存储恢复弹幕开关状态
const DANMAKU_STORAGE_KEY = 'video_danmaku_enabled'
const savedDanmakuState = localStorage.getItem(DANMAKU_STORAGE_KEY)
if (savedDanmakuState === 'true') {
  showDanmaku.value = true
}

// 判断是否为可编辑模式
const isEditable = computed(() => {
  return props.editor?.isEditable === true
})
// ⚠️ 关键修复：判断是否在聊天场景中
const isInChatContext = computed(() => {
  // 检查路由是否包含 chat
  const path = window.location.pathname
  const hash = window.location.hash
  return path.includes('/chat') || hash.includes('chat')
})

// 是否显示管理注释按钮（仅在文章编辑模式显示）
const showAnnotationManager = computed(() => {
  return isEditable.value && !isInChatContext.value
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

// 切换注释提示框收起状态
const toggleAnnotationCollapse = () => {
  isAnnotationCollapsed.value = !isAnnotationCollapsed.value
}

// 切换弹幕显示
const toggleDanmaku = async () => {
  showDanmaku.value = !showDanmaku.value

  // 保存状态到本地存储
  localStorage.setItem(DANMAKU_STORAGE_KEY, String(showDanmaku.value))

  if (showDanmaku.value) {
    await nextTick()
    await initDanmaku()
  } else {
    destroyDanmaku()
  }
}

// 发送弹幕
const sendDanmaku = async () => {
  if (!danmakuInput.value.trim()) {
    message.warning('请输入弹幕内容')
    return
  }

  if (!player || !videoSrc.value) {
    message.error('视频未就绪')
    return
  }

  // 从全局变量获取文章ID
  const articleId = (window as any).detailArticleData?.id

  if (!articleId) {
    message.error('无法获取文章信息')
    return
  }

  try {
    const currentTime = player.currentTime

    // 调用 API 发送弹幕（携带 articleId）
    const response = await videoDanmakuAPI.send({
      articleId,
      videoUrl: videoSrc.value,
      content: danmakuInput.value.trim(),
      time: currentTime,
      color: danmakuColor.value,
      position: danmakuPosition.value,
      fontSize: danmakuFontSize.value
    })

    if (response.data.code === 0) {
      message.success('弹幕发送成功')

      // 添加到本地弹幕列表
      const newDanmaku = response.data.data
      danmakuData.value.push(newDanmaku)

      // ⚠️ 关键修复：立即添加到活跃弹幕列表，使其显示在屏幕上
      const canvas = danmakuCanvasRef.value
      if (canvas) {
        activeDanmakus.value.push({
          text: newDanmaku.content,
          time: newDanmaku.time,
          color: newDanmaku.color || '#FFFFFF',
          fontSize: newDanmaku.fontSize || 25,
          position: getPositionText(newDanmaku.position),
          x: canvas.width,
          y: Math.random() * (canvas.height - 100) + 20,
          speed: 2
        })
      }

      // 清空输入框
      danmakuInput.value = ''
    } else {
      message.error(response.data.msg || '弹幕发送失败')
    }
  } catch (error: any) {
    console.error('[弹幕] 发送失败:', error)

    if (error.response?.status === 401) {
      message.error('请先登录')
    } else {
      message.error('弹幕发送失败，请重试')
    }
  }
}

// 初始化弹幕（自定义 Canvas 实现）
const initDanmaku = async () => {
  if (!danmakuCanvasRef.value || !player || !videoSrc.value) {
    return
  }

  try {
    // 加载弹幕数据
    await loadDanmakuData()

    // 设置 Canvas 尺寸
    const canvas = danmakuCanvasRef.value
    const container = videoContainerRef.value

    if (!container) {return}

    const resizeCanvas = () => {
      const rect = container.getBoundingClientRect()
      canvas.width = rect.width
      canvas.height = rect.height - 52
    }

    resizeCanvas()
    window.addEventListener('resize', resizeCanvas)

    // 转换弹幕数据
    const comments: DanmakuItem[] = danmakuData.value.map(item => ({
      text: item.content,
      time: item.time,
      color: item.color || '#FFFFFF',
      fontSize: item.fontSize || 25,
      position: getPositionText(item.position),
      x: canvas.width,
      speed: 2
    }))

    // 监听视频时间更新
    player.on('timeupdate', () => {
      const currentTime = player?.currentTime || 0

      // 检查是否有新弹幕需要显示
      comments.forEach(comment => {
        if (Math.abs(currentTime - comment.time) < 0.1 && !activeDanmakus.value.includes(comment)) {
          activeDanmakus.value.push({
            ...comment,
            x: canvas.width,
            y: Math.random() * (canvas.height - 100) + 20
          })
        }
      })

      lastVideoTime = currentTime
    })

    // 启动动画循环
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return
    }

    let isPaused = false

    const animate = () => {
      if (!showDanmaku.value) {
        animationFrameId = null
        return
      }

      if (!isPaused) {
        ctx.clearRect(0, 0, canvas.width, canvas.height)

        activeDanmakus.value = activeDanmakus.value.filter(danmaku => {
          if (danmaku.position === 'scroll') {
            danmaku.x! -= danmaku.speed!
          }

          ctx.font = `${danmaku.fontSize}px Arial`
          ctx.fillStyle = danmaku.color
          ctx.textBaseline = 'top'

          const x = danmaku.x!
          let y = danmaku.y!

          if (danmaku.position === 'top') {
            y = 20
          } else if (danmaku.position === 'bottom') {
            y = canvas.height - danmaku.fontSize - 20
          }

          ctx.fillText(danmaku.text, x, y)

          return x > -200
        })
      }

      animationFrameId = requestAnimationFrame(animate)
    }

    animate()

    // 监听视频播放/暂停事件
    player.on('play', () => {
      isPaused = false
    })

    player.on('pause', () => {
      isPaused = true
    })

  } catch (error) {
    console.error('弹幕初始化失败:', error)
    message.error('弹幕加载失败')
  }
}

// 销毁弹幕
const destroyDanmaku = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  activeDanmakus.value = []
}

// 加载弹幕数据
const loadDanmakuData = async () => {
  try {
    const articleId = (window as any).detailArticleData?.id

    if (!articleId) {
      return
    }

    if (!videoSrc.value) {
      return
    }

    const response = await videoDanmakuAPI.getLatest(videoSrc.value, 200, articleId)

    if (response.data.code === 0) {
      danmakuData.value = response.data.data || []
    }
  } catch (error) {
    console.error('弹幕加载失败:', error)
  }
}

// 转换位置类型
const getPositionText = (position: number): 'top' | 'bottom' | 'scroll' => {
  switch (position) {
    case 1: return 'top'
    case 2: return 'bottom'
    default: return 'scroll'
  }
}

// 计算标记点样式
const getMarkerStyle = (annotation: Annotation): Record<string, string> => {
  const duration = videoDuration.value || player?.duration || 0
  if (!duration || duration === 0) {return { left: '0%' }}

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
  if (!seconds || seconds === 0) {return '00:00'}
  
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 格式化时间
const formatTime = (seconds: number): string => {
  if (!seconds || seconds === 0) {return '00:00'}

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
    if (startDiff <= threshold) {return true}

    if (annotation.endTime) {
      const endDiff = Math.abs(annotation.endTime - currentTime)
      if (endDiff <= threshold) {return true}

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
  if (!videoRef.value) {return}

  try {
    player = new Plyr(videoRef.value, {
      controls: [
        'play-large',
        'play',
        'progress',
        'current-time',
        'duration',  // 改为显示总时长而不是剩余时间
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

    player.on('timeupdate', (event : Event) => {
      const plyrEvent = event as CustomEvent
      const currentTime = plyrEvent.detail?.plyr?.currentTime || player?.currentTime || 0
      
      const annotation = findCurrentAnnotation(currentTime)
      currentAnnotation.value = annotation
    })

    player.on('error', (error : Error) => {
      console.error('[VideoNode] 视频播放错误:', error)
      message.error('视频加载失败，请检查网络连接')
    })

  } catch (error) {
    console.error('[VideoNode] 播放器初始化失败:', error)
    message.error('视频播放器初始化失败')
  }
}

const renderAnnotationMarkers = () => {
  if (!markersRef.value || annotations.value.length === 0) {return}
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

// 处理来自引用节点的跳转请求
const handleScrollToAnnotation = (event: Event) => {
  const customEvent = event as CustomEvent
  const { videoNodeId: targetVideoId, annotationId, time, title } = customEvent.detail

  // 检查是否是当前视频节点
  if (!videoNodeId.value || videoNodeId.value !== targetVideoId) {
    console.warn('⚠️ [VideoNode] videoNodeId 不匹配，忽略跳转')
    return
  }

  // 滚动到视频容器
  if (videoContainerRef.value) {

    videoContainerRef.value.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
  }

  // 延迟跳转，等待滚动完成
  setTimeout(() => {
    if (player) {

      jumpToTime(time)
      message.success(`已跳转到注释: ${title}`)
    } else {
      console.error('[VideoNode] 播放器未就绪')
      message.error('视频播放器未就绪')
    }
  }, 500)
}

onMounted(() => {
  setTimeout(() => {
    initPlayer()
  }, 100)

  // ⚠️ 关键修复：如果节点没有 ID，立即生成并保存
  if (!props.node?.attrs?.id) {
    const generatedId = `video_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

    // 使用 nextTick 确保在下一个 tick 更新
    nextTick(() => {
      props.updateAttributes({
        id: generatedId
      })

    })
  } else {

  }

  // 监听来自引用节点的跳转事件
  window.addEventListener('scroll-to-video-annotation', handleScrollToAnnotation)

  // 检测是否在文章详情页
  setTimeout(() => {
    checkIfInArticleDetail()

    // 如果之前开启了弹幕，自动初始化
    if (showDanmaku.value) {
      nextTick(() => {
        initDanmaku()
      })
    }
  }, 200)
})

onBeforeUnmount(() => {
  destroyPlayer()
  destroyDanmaku()

  // 清理事件监听
  window.removeEventListener('scroll-to-video-annotation', handleScrollToAnnotation)
})

watch(() => props.node?.attrs?.annotations, (newAnnotations: Annotation[] | undefined, oldAnnotations: Annotation[] | undefined) => {
  // 注释更新时的处理
}, { deep: true })

// 检测是否在文章详情页
const checkIfInArticleDetail = () => {
  const path = window.location.pathname
  const hash = window.location.hash

  // 支持多种路由格式（hash 模式和 history 模式）
  const isArticleDetail =
    path.includes('/article/detail') ||
    path.includes('/article/version') ||
    hash.includes('articleDetail') ||
    hash.includes('versionDetail')

  if (isArticleDetail) {
    showDanmakuToggle.value = true
  }
}

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

  .danmaku-container {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: calc(100% - 52px);
    pointer-events: none;
    z-index: 100 !important;
  }

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
        flex: 1;
      }

      .collapse-btn {
        opacity: 0.6;
        transition: opacity 0.3s;

        &:hover {
          opacity: 1;
        }
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

  .collapsed-annotation-tip {
    position: absolute;
    top: 16px;
    right: 16px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 8px;
    border-radius: 50%;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    cursor: pointer;
    z-index: 20;
    transition: all 0.3s ease;

    &:hover {
      transform: scale(1.1);
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
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
  flex-direction: column;
  gap: 12px;

  .video-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .video-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      min-width: 0;
    }

    .video-duration {
      font-size: 13px;
      color: #999;
      flex-shrink: 0;
    }
  }

  .video-actions {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
  }

  .danmaku-controls-bottom {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-top: 8px;
    border-top: 1px solid #e4e7ed;

    .danmaku-input-area-bottom {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .danmaku-settings {
        display: flex;
        justify-content: flex-end;
      }

      .danmaku-config {
        padding: 8px;
        min-width: 240px;

        .config-item {
          margin-bottom: 12px;

          &:last-child {
            margin-bottom: 0;
          }

          .label {
            display: block;
            font-size: 12px;
            color: #666;
            margin-bottom: 6px;
          }
        }
      }
    }
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

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  transform-origin: top;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}
</style>
