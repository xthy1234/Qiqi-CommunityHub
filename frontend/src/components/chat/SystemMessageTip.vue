<template>
  <div class="system-message-tip">
    <div class="tip-content">
      <!-- 撤回消息 -->
      <template v-if="type === 'recall'">
        <n-icon size="14" color="#909399">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24">
            <path fill="currentColor" d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/>
          </svg>
        </n-icon>
        <span class="tip-text">
          <template v-if="isOwn">你</template>
          <template v-else>{{ username }}</template>
          撤回了一条消息
        </span>
      </template>
      
      <!-- 删除消息 -->
      <template v-else-if="type === 'delete'">
        <n-icon size="14" color="#909399">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24">
            <path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
          </svg>
        </n-icon>
        <span class="tip-text">
          <template v-if="isOwn">你</template>
          <template v-else>{{ username }}</template>
          删除了一条消息
        </span>
      </template>
      
      <!-- 会话开始 -->
      <template v-else-if="type === 'session-start'">
        <n-icon size="14" color="#18a058">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24">
            <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
          </svg>
        </n-icon>
        <span class="tip-text">
          {{ startTimeText }}
        </span>
      </template>
      
      <!-- 其他自定义类型 -->
      <template v-else>
        <slot>
          <span class="tip-text">{{ content }}</span>
        </slot>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import dayjs from 'dayjs'

interface Props {
  type?: 'recall' | 'delete' | 'session-start' | 'custom'
  username?: string
  isOwn?: boolean
  timestamp?: string | number
  content?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'custom',
  username: '',
  isOwn: false,
  timestamp: undefined,
  content: ''
})

// 计算会话开始时间文本
const startTimeText = computed(() => {
  if (!props.timestamp) {
    return '会话已开始'
  }
  
  const time = dayjs(props.timestamp)
  const now = dayjs()
  const diff = now.diff(time, 'minute')
  
  if (diff < 1) {
    return '刚刚会话开始'
  } else if (diff < 60) {
    return `${diff}分钟前会话开始`
  } else if (diff < 1440) {
    return `${Math.floor(diff / 60)}小时前会话开始`
  } else {
    return time.format('MM-DD HH:mm') + ' 会话开始'
  }
})
</script>

<style scoped lang="scss">
.system-message-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0;
  margin: 12px 0;
}

.tip-content {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 12px;
  font-size: 12px;
  color: #909399;
}

.tip-text {
  line-height: 1.5;
  white-space: nowrap;
}
</style>
