<template>
  <div class="countdown_component">
    <!-- 倒计时显示 -->
    <p v-if="timeData.isVisible" class="countdown_display">
      <span v-if="isBeforeStart">{{ beforeStartText }}:</span>
      <span v-else>{{ beforeEndText }}:</span>

      <!-- 天数 -->
      <span v-if="timeData.days > 0" class="time_unit">
        <span class="time_value">{{ timeData.days }}</span>
        <i class="time_separator">{{ daySeparator }}</i>
      </span>

      <!-- 小时 -->
      <span class="time_unit">
        <span class="time_value">{{ timeData.hours }}</span>
        <i class="time_separator">{{ hourSeparator }}</i>
      </span>

      <!-- 分钟 -->
      <span class="time_unit">
        <span class="time_value">{{ timeData.minutes }}</span>
        <i class="time_separator">{{ minuteSeparator }}</i>
      </span>

      <!-- 秒数 -->
      <span class="time_unit">
        <span class="time_value">{{ timeData.seconds }}</span>
        <i class="time_separator">{{ secondSeparator }}</i>
      </span>
    </p>

    <!-- 倒计时结束显示 -->
    <p v-else class="countdown_end">{{ endTimeText }}</p>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  toRefs,
  watch,
  onMounted,
  onUnmounted
} from 'vue'

// 定义事件发射
const emit = defineEmits<{
  (e: 'endCallback'): void
  (e: 'startCallback', isVisible: boolean): void
}>()

// Props接口定义
interface CountdownProps {
  // 距离开始提示文字
  beforeStartText?: string
  // 距离结束提示文字
  beforeEndText?: string
  // 组件ID
  componentId?: string
  // 当前时间戳
  currentTime?: number
  // 活动开始时间戳
  startTime: number
  // 活动结束时间戳
  endTime: number
  // 倒计时结束显示文本
  endTimeText?: string
  // 天单位分隔符
  daySeparator?: string
  // 小时单位分隔符
  hourSeparator?: string
  // 分钟单位分隔符
  minuteSeparator?: string
  // 秒单位分隔符
  secondSeparator?: string
  // 是否固定秒表倒计时（未实现）
  fixedSeconds?: boolean
}

// 默认props值
const props = withDefaults(defineProps<CountdownProps>(), {
  beforeStartText: '距离开始',
  beforeEndText: '距离结束',
  componentId: '1',
  endTimeText: '已结束',
  daySeparator: ':',
  hourSeparator: ':',
  minuteSeparator: ':',
  secondSeparator: ':',
  fixedSeconds: false
})

// 解构props
const {
  beforeStartText,
  beforeEndText,
  componentId,
  currentTime,
  startTime,
  endTime,
  endTimeText,
  daySeparator,
  hourSeparator,
  minuteSeparator,
  secondSeparator,
  fixedSeconds
} = toRefs(props)

// 类型定义
interface TimeData {
  isVisible: boolean
  days: string
  hours: string
  minutes: string
  seconds: string
}

// 响应式数据
const isBeforeStart = ref<boolean>(true)
const timeData = ref<TimeData>({
  isVisible: false,
  days: '00',
  hours: '00',
  minutes: '00',
  seconds: '00'
})

// 时间变量
const startTimeMs = ref<number>(0)
const endTimeMs = ref<number>(0)
const currentTimeMs = ref<number>(0)
let timerId: number | null = null

/**
 * 格式化时间戳（处理秒和毫秒）
 * @param timestamp 时间戳
 * @returns 毫秒时间戳
 */
const formatTimestamp = (timestamp: number): number => {
  return timestamp.toString().length === 10 ? timestamp * 1000 : timestamp
}

/**
 * 初始化时间数据
 */
const initializeTimeData = (): void => {
  startTimeMs.value = formatTimestamp(startTime.value)
  endTimeMs.value = formatTimestamp(endTime.value)

  if (currentTime?.value) {
    currentTimeMs.value = formatTimestamp(currentTime.value)
  } else {
    currentTimeMs.value = Date.now()
  }
}

/**
 * 开始倒计时
 */
const startCountdown = (): void => {
  initializeTimeData()
  const now = currentTimeMs.value

  if (endTimeMs.value < now) {
    // 活动已结束
    timeData.value.isVisible = false
    handleEndTime()
  } else if (now < startTimeMs.value) {
    // 活动尚未开始
    isBeforeStart.value = true
    timeData.value.isVisible = true
    timerId = window.setTimeout(() => {
      runTimer(startTimeMs.value, now, handleStartTime)
    }, 1)
  } else if (endTimeMs.value > now && startTimeMs.value <= now) {
    // 活动进行中
    isBeforeStart.value = false
    timeData.value.isVisible = true
    emit('startCallback', timeData.value.isVisible)
    timerId = window.setTimeout(() => {
      runTimer(endTimeMs.value, now, handleEndTime, true)
    }, 1)
  }
}

/**
 * 运行定时器
 * @param targetTime 目标时间
 * @param currentTime 当前时间
 * @param callback 回调函数
 * @param isCountingDown 是否倒计时
 */
const runTimer = (
  targetTime: number,
  currentTime: number,
  callback: () => void,
  isCountingDown: boolean = false
): void => {
  const timeDifference = targetTime - currentTime

  if (timeDifference > 0) {
    // 计算时间差
    const days = Math.floor(timeDifference / 86400000)
    let remainingTime = timeDifference - days * 86400000

    const hours = Math.floor(remainingTime / 3600000)
    remainingTime -= hours * 3600000

    const minutes = Math.floor(remainingTime / 60000)
    remainingTime -= minutes * 60000

    const seconds = Math.floor(remainingTime / 1000)

    // 更新时间显示
    timeData.value = {
      isVisible: true,
      days: days > 0 ? days.toString() : '0',
      hours: hours.toString().padStart(2, '0'),
      minutes: minutes.toString().padStart(2, '0'),
      seconds: seconds.toString().padStart(2, '0')
    }

    // 继续计时
    const startTime = performance.now()
    timerId = window.setTimeout(() => {
      const newCurrentTime = currentTime + 1000
      if (isCountingDown) {
        runTimer(endTimeMs.value, newCurrentTime, callback, true)
      } else {
        runTimer(startTimeMs.value, newCurrentTime, callback)
      }
    }, 1000 - (performance.now() - startTime))
  } else {
    callback()
  }
}

/**
 * 处理开始时间到达
 */
const handleStartTime = (): void => {
  isBeforeStart.value = false
  emit('startCallback', timeData.value.isVisible)
  timerId = window.setTimeout(() => {
    runTimer(endTimeMs.value, startTimeMs.value, handleEndTime, true)
  }, 1)
}

/**
 * 处理结束时间到达
 */
const handleEndTime = (): void => {
  timeData.value.isVisible = false
  if (currentTime?.value && currentTime.value <= 0) {
    return
  }
  emit('endCallback')
}

/**
 * 清理定时器
 */
const cleanupTimer = (): void => {
  if (timerId) {
    window.clearTimeout(timerId)
    timerId = null
  }
}

// 监听当前时间变化
watch(currentTime, () => {
  cleanupTimer()
  startCountdown()
})

// 组件挂载
onMounted(() => {
  startCountdown()
})

// 组件卸载时清理
onUnmounted(() => {
  cleanupTimer()
})
</script>

<style lang="scss" scoped>
.countdown_component {
  .countdown_display {
    margin: 0;
    font-size: 16px;
    font-weight: 500;
    color: #333;

    .time_unit {
      display: inline-flex;
      align-items: center;
      margin-right: 4px;

      .time_value {
        background: #f5f5f5;
        padding: 2px 8px;
        border-radius: 4px;
        font-family: 'Courier New', monospace;
        font-weight: bold;
        color: #666;
      }

      .time_separator {
        margin: 0 2px;
        color: #999;
      }
    }
  }

  .countdown_end {
    margin: 0;
    color: #999;
    font-style: italic;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .countdown_component {
    .countdown_display {
      font-size: 14px;

      .time_unit {
        .time_value {
          padding: 1px 6px;
          font-size: 14px;
        }
      }
    }
  }
}
</style>