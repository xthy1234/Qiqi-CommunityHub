<template>
  <div class="message-wrapper" :class="{ 'own': isOwn }">
    <!--  使用独立的头像组件 -->
    <ChatAvatar
      :user-id="message.fromUserId"
      :avatar="message.fromUser?.avatar"
      size="small"
      round
      clickable
      @click="handleAvatarClick"
    />

    <div class="message-bubble" :class="{ 'own': isOwn }" @contextmenu.prevent="showContextMenu">
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

    <!-- 右键菜单 -->
    <div v-if="contextMenuVisible" class="message-context-menu" :style="contextMenuStyle">
      <n-dropdown
        :options="menuOptions"
        :x="contextMenuX"
        :y="contextMenuY"
        :show="contextMenuVisible"
        :on-clickoutside="hideContextMenu"
        @select="handleMenuSelect"
      />
    </div>

    <div class="message-meta">
      <span class="message-time">{{ formatTime(message.createTime) }}</span>
      <span v-if="isOwn" class="message-status">
        <!--  显示发送中状态 -->
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
import { computed, ref } from 'vue'
import type { Message } from '@/types/message'
import dayjs from 'dayjs'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NAvatar, NImage, NIcon, NButton, NDropdown } from 'naive-ui'
import ChatAvatar from '@/components/chat/ChatAvatar.vue'
import { getWebSocket } from '@/utils/websocket'

const appContext = useGlobalProperties()

interface Props {
  message: Message
  isOwn: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'recall': [messageId: number]
  'delete': [messageId: number]
  'copy': [content: string]
}>()

//  右键菜单相关状态
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)

//  菜单选项
const menuOptions = computed(() => {
  const options = [
    {
      label: '复制',
      key: 'copy',
      icon: () => '📋'
    },
    {
      label: '删除',
      key: 'delete',
      icon: () => '🗑️'
    }
  ]

  if (props.isOwn) {
    options.unshift(
      {
        label: '撤回',
        key: 'recall',
        icon: () => '↩️'
      }
    )
  }

  return options
})

const contextMenuStyle = computed(() => ({
  position: 'fixed',
  left: `${contextMenuX.value}px`,
  top: `${contextMenuY.value}px`
}))

const avatarUrl = computed(() => {

  //  始终使用 fromUser 的头像（发送者的头像）
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

//  处理头像点击
const handleAvatarClick = (userId: number) => {

  // TODO: 可以跳转到用户主页或显示用户信息弹窗
}

//  显示右键菜单
const showContextMenu = (event: MouseEvent) => {
  event.preventDefault()
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuVisible.value = true
}

//  隐藏右键菜单
const hideContextMenu = () => {
  contextMenuVisible.value = false
}

//  处理菜单选择
const handleMenuSelect = (key: string) => {
  hideContextMenu()

  switch (key) {
    case 'recall':
      handleRecall()
      break
    case 'delete':
      handleDelete()
      break
    case 'copy':
      handleCopy()
      break
  }
}

//  撤回消息
const handleRecall = () => {  if (!props.message.id) {
    console.warn('⚠️ [MessageBubble] 消息 ID 不存在，无法撤回')

    return
  }  //  通过 WebSocket 发送撤回请求
  const ws = getWebSocket()

//     exists: !!ws,
//     isConnected: ws?.isConnected(),
//     client: ws ? '存在' : '不存在'
//   })

  if (ws && ws.isConnected()) {

    ws.recallMessage(props.message.id)

  } else {
    console.error('❌ [MessageBubble.handleRecall] WebSocket 未连接，无法发送撤回请求')


  }

  emit('recall', props.message.id)


}

//  删除消息
const handleDelete = () => {  if (!props.message.id) {
    console.warn('⚠️ [MessageBubble] 消息 ID 不存在，无法删除')

    return
  }  //  通过 WebSocket 发送删除请求
  const ws = getWebSocket()

//     exists: !!ws,
//     isConnected: ws?.isConnected(),
//     client: ws ? '存在' : '不存在'
//   })

  if (ws && ws.isConnected()) {

    ws.deleteMessage(props.message.id)

  } else {
    console.error('❌ [MessageBubble.handleDelete] WebSocket 未连接，无法发送删除请求')


  }

  emit('delete', props.message.id)


}

//  复制消息内容
const handleCopy = () => {
  const content = props.message.content
  if (navigator.clipboard) {
    navigator.clipboard.writeText(content).then(() => {

    }).catch(err => {
      console.error('❌ [MessageBubble] 复制失败:', err)
    })
  } else {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = content
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)

  }

  emit('copy', content)
}
</script>

<style scoped lang="scss">
.message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 100%;
  margin-bottom: 16px;
  position: relative;

  &.own {
    flex-direction: row-reverse;  //  自己的消息：反向排列（头像在右）
    
    .message-bubble {
      background: #18a058;  //  绿色背景（自己）
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
  flex-shrink: 0;  //  头像不压缩
}

.message-bubble {
  max-width: 70%;  //  气泡最大宽度 70%
  padding: 12px 16px;
  background: #f0f0f0;  //  灰色背景（对方）
  border-radius: 12px;
  position: relative;
  word-wrap: break-word;  //  长文本换行
  cursor: context-menu;  //  显示右键菜单光标
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

.message-context-menu {
  position: fixed;
  z-index: 9999;
}
</style>
