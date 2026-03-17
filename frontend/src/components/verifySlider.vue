<template>
  <div
    ref="sliderContainer"
    class="slider_container"
    :style="{ 'background-color': backgroundColor }"
  >
    <div
      ref="progressBar"
      class="progress_bar"
      :style="{ 'background-color': activeBackgroundColor }"
    />
    <div
      ref="sliderText"
      class="slider_text"
    >
      {{ sliderMessage }}
    </div>
    <div
      class="verification_tip"
      v-if="showVerificationPrompt"
    >
      请滑动验证
    </div>
    <div
      ref="sliderHandle"
      :class="{ 'handler_success': isVerificationSuccessful }"
      class="slider_handle handler_default"
      style="position: absolute; top: 0px; left: 0px;"
      @mousedown="handleMouseDown($event)"
    />
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  onMounted,
  onUnmounted,
  computed
} from 'vue'

// Props接口定义
interface SliderProps {
  backgroundColor?: string
  activeBackgroundColor?: string
}

// 默认props值
const props = withDefaults(defineProps<SliderProps>(), {
  backgroundColor: '#e8e8e8',
  activeBackgroundColor: '#7ac23c'
})

// 事件发射
const emit = defineEmits<{
  (e: 'verificationSuccess'): void
}>()

// 响应式数据
const beginClientX = ref<number>(0)
const isMousePressed = ref<boolean>(false)
const maxWidth = ref<number>(0)
const sliderMessage = ref<string>('按住滑块，拖动到最右边')
const isVerificationSuccessful = ref<boolean>(false)
const showVerificationPrompt = ref<boolean>(true)

// DOM引用
const sliderContainer = ref<HTMLDivElement | null>(null)
const progressBar = ref<HTMLDivElement | null>(null)
const sliderText = ref<HTMLDivElement | null>(null)
const sliderHandle = ref<HTMLDivElement | null>(null)

// 计算属性
const computedBackgroundColor = computed(() => props.backgroundColor)
const computedActiveBackgroundColor = computed(() => props.activeBackgroundColor)

/**
 * 初始化最大宽度
 */
const initializeMaxWidth = (): void => {
  if (sliderContainer.value && sliderHandle.value) {
    maxWidth.value = sliderContainer.value.clientWidth - sliderHandle.value.clientWidth
  }
}

/**
 * 鼠标按下事件处理
 */
const handleMouseDown = (event: MouseEvent): void => {
// console.log('鼠标按下事件触发')

  if (isVerificationSuccessful.value) return

  event.preventDefault()
  isMousePressed.value = true
  beginClientX.value = event.clientX
  initializeMaxWidth()
}

/**
 * 鼠标移动事件处理
 */
const handleMouseMove = (event: MouseEvent): void => {
  if (!isMousePressed.value) return

// console.log('鼠标移动事件触发')

  const movedDistance = event.clientX - beginClientX.value

  if (movedDistance > 0 && movedDistance <= maxWidth.value) {
    if (sliderHandle.value && progressBar.value) {
      sliderHandle.value.style.left = movedDistance + 'px'
      progressBar.value.style.width = movedDistance + 'px'
    }
  } else if (movedDistance > maxWidth.value) {
    handleVerificationSuccess()
  }
}

/**
 * 鼠标释放事件处理
 */
const handleMouseUp = (event: MouseEvent): void => {
// console.log('鼠标释放事件触发')

  isMousePressed.value = false
  const movedDistance = event.clientX - beginClientX.value

  if (movedDistance < maxWidth.value && sliderHandle.value && progressBar.value) {
    sliderHandle.value.style.left = '0px'
    progressBar.value.style.width = '0px'
  }
}

/**
 * 验证成功处理
 */
const handleVerificationSuccess = (): void => {
  isVerificationSuccessful.value = true
  sliderMessage.value = '验证通过'
  showVerificationPrompt.value = false

  // 移除事件监听器
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)

  // 更新样式
  if (sliderText.value && sliderHandle.value && progressBar.value) {
    sliderText.value.style.color = '#fff'
    sliderHandle.value.style.left = maxWidth.value + 'px'
    progressBar.value.style.width = maxWidth.value + 'px'
  }

  // 触发成功事件
  emit('verificationSuccess')
}

/**
 * 重置滑块到初始状态
 */
const resetSlider = (): void => {
  if (!sliderHandle.value || !progressBar.value || !sliderText.value) return

  // 重置样式
  sliderHandle.value.style.left = '0px'
  progressBar.value.style.width = '0px'
  sliderText.value.style.color = '#333'

  // 重置状态
  isVerificationSuccessful.value = false
  isMousePressed.value = false
  showVerificationPrompt.value = true
  sliderMessage.value = '按住滑块，拖动到最右边'

  // 重新计算最大宽度
  initializeMaxWidth()

  // 重新添加事件监听器
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

// 组件挂载时添加事件监听器
onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
})

// 组件卸载时移除事件监听器
onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

// 暴露方法给父组件
defineExpose({
  resetSlider
})
</script>

<style scoped lang="scss">
.slider_container {
  position: relative;
  background-color: #e8e8e8;
  width: 70%;
  height: 34px;
  line-height: 34px;
  text-align: center;
  border-radius: 4px;
  overflow: hidden;
}

.progress_bar {
  background-color: #7ac23c;
  height: 34px;
  width: 0px;
  border-radius: 4px 0 0 4px;
  transition: width 0.1s ease;
}

.slider_text {
  position: absolute;
  top: 0px;
  width: 100%;
  color: #333;
  text-align: center;
  font-size: 14px;
  user-select: none;
  pointer-events: none;
  z-index: 1;
}

.verification_tip {
  position: absolute;
  display: inline-block;
  top: 0px;
  right: -85px;
  color: #999;
  font-size: 12px;
}

.slider_handle {
  width: 40px;
  height: 32px;
  border: 1px solid #ccc;
  cursor: move;
  border-radius: 4px;
  z-index: 2;
  transition: left 0.1s ease;

  &.handler_default {
    background: #fff url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA3hpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNS1jMDIxIDc5LjE1NTc3MiwgMjAxNC8wMS8xMy0xOTo0NDowMCAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo0ZDhlNWY5My05NmI0LTRlNWQtOGFjYi03ZTY4OGYyMTU2ZTYiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6NTEyNTVEMURGMkVFMTFFNEI5NDBCMjQ2M0ExMDQ1OUYiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6NTEyNTVEMUNGMkVFMTFFNEI5NDBCMjQ2M0ExMDQ1OUYiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTQgKE1hY2ludG9zaCkiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDo2MTc5NzNmZS02OTQxLTQyOTYtYTIwNi02NDI2YTNkOWU5YmUiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6NGQ4ZTVmOTMtOTZiNC00ZTVkLThhY2ItN2U2ODhmMjE1NmU2Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+YiRG4AAAALFJREFUeNpi/P//PwMlgImBQkA9A+bOnfsIiBOxKcInh+yCaCDuByoswaIOpxwjciACFegBqZ1AvBSIS5OTk/8TkmNEjwWgQiUgtQuIjwAxUF3yX3xyGIEIFLwHpKyAWB+I1xGSwxULIGf9A7mQkBwTlhBXAFLHgPgqEAcTkmNCU6AL9d8WII4HOvk3ITkWJAXWUMlOoGQHmsE45ViQ2KuBuASoYC4Wf+OUYxz6mQkgwAAN9mIrUReCXgAAAABJRU5ErkJggg==') no-repeat center;
  }

  &.handler_success {
    background: #fff url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA3hpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNS1jMDIxIDc5LjE1NTc3MiwgMjAxNC8wMS8xMy0xOTo0NDowMCAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo0ZDhlNWY5My05NmI0LTRlNWQtOGFjYi03ZTY4OGYyMTU2ZTYiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6NDlBRDI3NjVGMkQ2MTFFNEI5NDBCMjQ2M0ExMDQ1OUYiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6NDlBRDI3NjRGMkQ2MTFFNEI5NDBCMjQ2M0ExMDQ1OUYiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTQgKE1hY2ludG9zaCkiPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDphNWEzMWNhMC1hYmViLTQxNWEtYTEwZS04Y2U5NzRlN2Q4YTEiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6NGQ4ZTVmOTMtOTZiNC00ZTVkLThhY2ItN2U2ODhmMjE1NmU2Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+k+sHwwAAASZJREFUeNpi/P//PwMyKD8uZw+kUoDYEYgloMIvgHg/EM/ptHx0EFk9I8wAoEZ+IDUPiIMY8IN1QJwENOgj3ACo5gNAbMBAHLgAxA4gQ5igAnNJ0MwAVTsX7IKyY7L2UNuJAf+AmAmJ78AEDTBiwGYg5gbifCSxFCZoaBMCy4A4GOjnH0D6DpK4IxNSVIHAfSDOAeLraJrjgJp/AwPbHMhejiQnwYRmUzNQ4VQgDQqXK0ia/0I17wJiPmQNTNBEAgMlQIWiQA2vgWw7QppBekGxsAjIiEUSBNnsBDWEAY9mEFgMMgBk00E0iZtA7AHEctDQ58MRuA6wlLgGFMoMpIG1QFeGwAIxGZo8GUhIysmwQGSAZgwHaEZhICIzOaBkJkqyM0CAAQDGx279Jf50AAAAAABJRU5ErkJggg==') no-repeat center;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .slider_container {
    width: 100%;
    height: 40px;
  }

  .slider_handle {
    width: 45px;
    height: 38px;
  }

  .progress_bar {
    height: 40px;
  }

  .slider_text {
    font-size: 16px;
    line-height: 40px;
  }
}
</style>
