<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { NModal, NButton, NIcon, NSpace, NFormItem, NInput, NInputNumber, NCheckbox, NAlert, NTooltip, useMessage } from 'naive-ui'

interface Annotation {
  id: string
  startTime: number
  endTime?: number
  title: string
  content: string
}

interface Props {
  show: boolean
  videoSrc: string
  posterUrl?: string
  annotations?: Annotation[]
  videoDuration?: number
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  videoSrc: '',
  posterUrl: '',
  annotations: () => [],
  videoDuration: 0
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  'submit': [annotation: Omit<Annotation, 'id'>]
  'delete': [index: number]
  'jump': [time: number]
}>()

const message = useMessage()

const MAX_ANNOTATIONS = 20

const visible = computed({
  get: () => props.show,
  set: (value: boolean) => emit('update:show', value)
})

const previewVideoRef = ref<HTMLVideoElement | null>(null)
const useTimeRange = ref(false)

const startTime = ref(0)
const endTime = ref(-1)
const title = ref('')
const content = ref('')

const actualDuration = ref(0)

const maxDuration = computed(() => {
  if (actualDuration.value > 0) {return actualDuration.value}
  if (props.videoDuration && props.videoDuration > 0) {return props.videoDuration}
  return 3600
})

const existingAnnotations = computed(() => props.annotations || [])

const sortedAnnotations = computed(() => {
  return [...existingAnnotations.value].sort((a, b) => a.startTime - b.startTime)
})

const canSubmit = computed(() => {
  const hasTitle = title.value.trim().length > 0 && title.value.trim().length <= 50
  const hasValidTime = startTime.value >= 0

  let hasValidRange = true
  if (useTimeRange.value) {
    hasValidRange = endTime.value >= 0 && endTime.value > startTime.value
  }

  const notExceedLimit = existingAnnotations.value.length < MAX_ANNOTATIONS

  let withinDuration = true
  if (actualDuration.value > 0) {
    if (startTime.value > actualDuration.value) {
      withinDuration = false
    }
    if (useTimeRange.value && endTime.value > actualDuration.value) {
      withinDuration = false
    }
  }

  return hasTitle && hasValidTime && hasValidRange && notExceedLimit && withinDuration
})

const generateId = (): string => {
  return `ann_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

const handleStartTimeChange = (val: number | null) => {
  if (val !== null && val >= 0) {
    startTime.value = val
  }
}

const handleEndTimeChange = (val: number | null) => {
  if (val !== null && val >= 0) {
    endTime.value = val
  }
}

const captureCurrentTime = () => {
  if (!previewVideoRef.value) {return}

  const currentTime = Math.round(previewVideoRef.value.currentTime * 100) / 100

  if (useTimeRange.value) {
    startTime.value = currentTime
    endTime.value = parseFloat((currentTime + 1).toFixed(2))
    message.success(`已设置时间段: ${formatTime(startTime.value)} - ${formatTime(endTime.value)}`)
  } else {
    startTime.value = currentTime
    endTime.value = -1
  }
}

const formatTimeRange = (startTime: number, endTime?: number): string => {
  if (endTime !== undefined && endTime >= 0 && endTime > startTime) {
    return `${formatTime(startTime)}-${formatTime(endTime)}`
  }
  return formatTime(startTime)
}

const formatTime = (seconds: number): string => {
  if (seconds < 0) {return '00:00'}

  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  const ms = Math.floor((seconds % 1) * 100)

  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}`
}

const jumpToTime = (time: number) => {
  if (previewVideoRef.value) {
    previewVideoRef.value.currentTime = time
    previewVideoRef.value.play()
  }
  emit('jump', time)
}

const deleteAnnotation = (index: number) => {
  emit('delete', index)
}

const handleSubmit = () => {
  if (!canSubmit.value) {
    if (existingAnnotations.value.length >= MAX_ANNOTATIONS) {
      message.error(`最多只能添加 ${MAX_ANNOTATIONS} 个注释`)
    } else if (useTimeRange.value && endTime.value < 0) {
      message.error('请设置结束时间')
    } else if (useTimeRange.value && endTime.value <= startTime.value) {
      message.error('结束时间必须大于开始时间')
    } else if (actualDuration.value > 0 && startTime.value > actualDuration.value) {
      message.error(`开始时间不能超过视频时长 (${formatTime(actualDuration.value)})`)
    } else if (actualDuration.value > 0 && useTimeRange.value && endTime.value > actualDuration.value) {
      message.error(`结束时间不能超过视频时长 (${formatTime(actualDuration.value)})`)
    } else {
      message.error('请检查表单填写是否正确')
    }
    return
  }

  emit('submit', {
    startTime: startTime.value,
    endTime: useTimeRange.value && endTime.value >= 0 ? endTime.value : undefined,
    title: title.value.trim(),
    content: content.value.trim()
  })

  resetForm()
}

const resetForm = () => {
  startTime.value = 0
  endTime.value = -1
  title.value = ''
  content.value = ''
  useTimeRange.value = false
}

const handleCancel = () => {
  visible.value = false
  resetForm()
}

const updateActualDuration = () => {
  const video = previewVideoRef.value
  if (video) {
    const dur = video.duration
    if (isFinite(dur) && dur > 0) {
      actualDuration.value = dur
    }
  }
}

const onLoadedMetadata = () => {
  updateActualDuration()
}

const onDurationChange = () => {
  updateActualDuration()
}

watch(useTimeRange, (newValue: boolean) => {
  endTime.value = -1
})

watch(() => props.show, (newVal: boolean) => {
  if (newVal) {
    resetForm()
  }
})

onMounted(() => {
  const video = previewVideoRef.value
  if (video) {
    video.addEventListener('loadedmetadata', onLoadedMetadata)
    video.addEventListener('durationchange', onDurationChange)

    if (video.readyState >= 1) {
      updateActualDuration()
    }
  }
})

onBeforeUnmount(() => {
  const video = previewVideoRef.value
  if (video) {
    video.removeEventListener('loadedmetadata', onLoadedMetadata)
    video.removeEventListener('durationchange', onDurationChange)
  }
})
</script>

<template>
  <n-modal
    v-model:show="visible"
    preset="dialog"
    title="管理视频注释"
    :style="{ width: '700px' }"
  >
    <div class="annotation-editor">
      <div class="video-preview">
        <video
          ref="previewVideoRef"
          :src="videoSrc"
          :poster="posterUrl"
          controls
          crossorigin
          class="preview-video"
        />
      </div>

      <div class="time-input-section">
        <n-form-item
          label="时间范围"
          required
        >
          <n-space vertical>
            <n-space align="center">
              <n-checkbox v-model:checked="useTimeRange">
                使用时间段
              </n-checkbox>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-icon
                    size="16"
                    color="#999"
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
                </template>
                勾选后可设置开始和结束时间
              </n-tooltip>
            </n-space>

            <n-space>
              <n-input-number
                v-model:value="startTime"
                :min="0"
                :max="maxDuration"
                :precision="2"
                placeholder="开始时间"
                style="width: 150px"
                @update:value="handleStartTimeChange"
              >
                <template #suffix>
                  秒
                </template>
              </n-input-number>

              <template v-if="useTimeRange">
                <span>至</span>
                <n-input-number
                  v-model:value="endTime"
                  :min="0"
                  :max="maxDuration"
                  :precision="2"
                  placeholder="结束时间"
                  style="width: 150px"
                  @update:value="handleEndTimeChange"
                >
                  <template #suffix>
                    秒
                  </template>
                </n-input-number>
              </template>

              <n-button
                size="small"
                @click="captureCurrentTime"
              >
                <template #icon>
                  <n-icon>
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
                </template>
                获取当前时间
              </n-button>
            </n-space>
          </n-space>
        </n-form-item>

        <n-alert
          type="info"
          :show-icon="false"
          style="margin-top: 8px;"
        >
          已添加 {{ existingAnnotations.length }} / {{ MAX_ANNOTATIONS }} 个注释
        </n-alert>
      </div>

      <div class="content-section">
        <n-form-item
          label="标题"
          required
        >
          <n-input
            v-model:value="title"
            placeholder="请输入注释标题（最多50字）"
            maxlength="50"
            show-count
          />
        </n-form-item>
        
        <n-form-item label="详细内容">
          <n-input
            v-model:value="content"
            type="textarea"
            placeholder="请输入注释详细内容（可选）"
            :rows="4"
            maxlength="500"
            show-count
          />
        </n-form-item>
      </div>

      <div
        v-if="existingAnnotations.length > 0"
        class="existing-annotations"
      >
        <div class="section-title">
          <n-icon size="16">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"
              />
            </svg>
          </n-icon>
          <span>已有注释 ({{ existingAnnotations.length }}/{{ MAX_ANNOTATIONS }})</span>
        </div>
        <div class="annotation-list">
          <div
            v-for="(annotation, index) in sortedAnnotations"
            :key="annotation.id"
            class="annotation-item"
          >
            <div class="annotation-time">
              {{ formatTimeRange(annotation.startTime, annotation.endTime) }}
            </div>
            <div class="annotation-content">
              <div class="annotation-title">
                {{ annotation.title }}
              </div>
              <div
                v-if="annotation.content"
                class="annotation-desc"
              >
                {{ annotation.content }}
              </div>
            </div>
            <div class="annotation-actions">
              <n-button
                text
                size="tiny"
                @click="jumpToTime(annotation.startTime)"
              >
                跳转
              </n-button>
              <n-button
                text
                size="tiny"
                type="error"
                @click="deleteAnnotation(index)"
              >
                删除
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #action>
      <n-space justify="end">
        <n-button @click="handleCancel">
          取消
        </n-button>
        <n-button
          type="primary"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          确定
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<style scoped lang="scss">
.annotation-editor {
  .video-preview {
    margin-bottom: 16px;

    .preview-video {
      width: 100%;
      max-height: 300px;
      border-radius: 8px;
      background: #000;
    }
  }

  .time-input-section {
    margin-bottom: 16px;
  }

  .content-section {
    margin-bottom: 16px;
  }

  .existing-annotations {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid #e4e7ed;

    .section-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 600;
      color: #333;
      margin-bottom: 12px;
    }

    .annotation-list {
      max-height: 200px;
      overflow-y: auto;

      .annotation-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 10px;
        background: #f5f7fa;
        border-radius: 6px;
        margin-bottom: 8px;
        transition: all 0.3s ease;

        &:hover {
          background: #ecf5ff;
        }

        .annotation-time {
          flex-shrink: 0;
          font-size: 13px;
          font-weight: 600;
          color: #2080f0;
          font-family: 'Courier New', monospace;
          min-width: 100px;
        }

        .annotation-content {
          flex: 1;
          min-width: 0;

          .annotation-title {
            font-size: 14px;
            font-weight: 500;
            color: #333;
            margin-bottom: 4px;
          }

          .annotation-desc {
            font-size: 12px;
            color: #666;
            line-height: 1.5;
          }
        }

        .annotation-actions {
          flex-shrink: 0;
          display: flex;
          gap: 4px;
        }
      }
    }
  }
}
</style>
