<template>
  <div class="message-wrapper" :class="{ 'own': isOwn }">
    <n-avatar 
      v-if="!isOwn" 
      :src="message.fromUser?.avatar" 
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
      
      <div class="message-meta">
        <span class="message-time">{{ formatTime(message.createTime) }}</span>
        <span v-if="isOwn" class="message-status">
          {{ message.status === 1 ? '已读' : '已发送' }}
        </span>
      </div>
    </div>
    
    <n-avatar 
      v-if="isOwn" 
      :src="message.toUser?.avatar || myAvatar"
      size="small" 
      round
      class="message-avatar"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Message } from '@/types/message'
import dayjs from 'dayjs'
import { useGlobalProperties } from '@/utils/globalProperties'

const appContext = useGlobalProperties()

interface Props {
  message: Message
  isOwn: boolean
}

const props = defineProps<Props>()

const myAvatar = computed(() => {
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
  
  &.own {
    flex-direction: row-reverse;
    
    .message-bubble {
      background: #18a058;
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
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  background: #f0f0f0;
  border-radius: 12px;
  position: relative;
  
  &.own {
    background: #18a058;
  }
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
  align-items: center;
  margin-top: 6px;
  font-size: 12px;
  
  .message-time,
  .message-status {
    color: #999;
  }
}
</style>
