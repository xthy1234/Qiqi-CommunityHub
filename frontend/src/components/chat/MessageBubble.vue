<template>
  <div class="message-wrapper" :class="{ 'own': isOwn }">
    <n-avatar
      :src="avatarUrl"
      size="small"
      round
      class="message-avatar"
    />

    <div class="message-bubble" :class="{ 'own': isOwn }">
      <div class="message-content">
        <!-- 文本消息 -->
        <template v-if="message.msgType === 0">
          {{ message.content }}
        </template>
        
        <!-- 图片消息 -->
        <template v-else-if="message.msgType === 1">
          <n-image 
            :src="message.content" 
            :preview-src="message.content"
            class="message-image"
            object-fit="cover"
          />
        </template>
        
        <!-- 文件消息 -->
        <template v-else-if="message.msgType === 2">
          <div class="message-file">
            <n-icon size="24" color="#18a058">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
                <path fill="currentColor" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
              </svg>
            </n-icon>
            <span class="file-name">{{ getFileName(message.content) }}</span>
            <n-button text size="small" @click="downloadFile">
              下载
            </n-button>
          </div>
        </template>
      </div>
    </div>
    <div class="message-meta">
      <span class="message-time">{{ formatTime(message.createTime) }}</span>
      <span v-if="isOwn" class="message-status">
        <!-- 🔥 显示发送中状态 -->
        <template v-if="message._sending || message.status === 'SENDING'">
          <n-spin size="small" :stroke-width="3" />
        </template>
        <template v-else-if="message.status === 1 || message.status === 'READ'">
          已读
        </template>
        <template v-else-if="message.status === 'SENT' || message.status === 'DELIVERED'">
          已发送
        </template>
        <template v-else-if="message.status === 'FAILED'">
          失败
        </template>
        <template v-else>
          已发送
        </template>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Message } from '@/types/message'
import dayjs from 'dayjs'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NAvatar, NImage, NIcon, NButton } from 'naive-ui'

const appContext = useGlobalProperties()

interface Props {
  message: Message
  isOwn: boolean
}

const props = defineProps<Props>()

const avatarUrl = computed(() => {
// console.log('props.isOwn', props.isOwn)
  // 🔥 始终使用 fromUser 的头像（发送者的头像）
  if (props.message.fromUser?.avatar) {
    return props.message.fromUser.avatar
  }

  // 降级方案：从本地存储获取
  const avatar = appContext?.$toolUtil?.storageGet('avatar')
  if (avatar) {
    const baseUrl = appContext?.$config?.url || 'http://localhost:8080'
    return avatar.startsWith('http') ? avatar : `${baseUrl}/${avatar}`
  }
  return ''
})

const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

const getFileName = (url: string) => {
  const parts = url.split('/')
  return parts[parts.length - 1]
}

const downloadFile = () => {
  window.open(props.message.content, '_blank')
}
</script>

<style scoped lang="scss">
.message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 100%;
  margin-bottom: 16px;

  &.own {
    flex-direction: row-reverse;  // 🔥 自己的消息：反向排列（头像在右）
    
    .message-bubble {
      background: #18a058;  // 🔥 绿色背景（自己）
      color: #fff;
      
      .message-meta {
        .message-time,
        .message-status {
          color: rgba(255, 255, 255, 0.8);
        }
      }
    }
  }
}

.message-avatar {
  flex-shrink: 0;  // 🔥 头像不压缩
}

.message-bubble {
  max-width: 70%;  // 🔥 气泡最大宽度 70%
  padding: 12px 16px;
  background: #f0f0f0;  // 🔥 灰色背景（对方）
  border-radius: 12px;
  position: relative;
  word-wrap: break-word;  // 🔥 长文本换行
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-all;
}

.message-image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 8px;
  cursor: pointer;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .file-name {
    flex: 1;
    font-size: 13px;
  }
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 6px;
  font-size: 12px;
  
  .message-time,
  .message-status {
    color: #999;
  }
}
</style>
