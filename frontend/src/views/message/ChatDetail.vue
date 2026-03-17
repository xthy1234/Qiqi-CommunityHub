<template>
  <div class="chat-detail">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="chat-header-left">
        <n-avatar :src="store.currentConversation?.avatar" round size="medium" />
        <div class="chat-info">
          <div class="chat-name">{{ store.currentConversation?.username }}</div>
          <div class="chat-status">在线</div>
        </div>
      </div>
      
      <div class="chat-header-right">
        <n-dropdown :options="menuOptions" trigger="click" @select="handleMenuClick">
          <n-button text>
            <template #icon>
              <n-icon size="20">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2s-2 .9-2 2s.9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2s2-.9 2-2s-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2s2-.9 2-2s-.9-2-2-2z"/>
                </svg>
              </n-icon>
            </template>
          </n-button>
        </n-dropdown>
      </div>
    </div>

    <!-- 消息列表 -->
    <div 
      ref="messageListRef"
      class="message-list"
      @scroll="handleScroll"
    >
      <n-spin :show="store.loading">
        <div v-if="store.hasMore && store.messages.length > 0" class="load-more-trigger">
          <n-text depth="3">加载中...</n-text>
        </div>
        
        <div v-if="store.messages.length === 0" class="empty-messages">
          <div class="empty-state">
            <n-icon size="80" color="#dcdfe6">
              <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24">
                <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
              </svg>
            </n-icon>
            <div class="empty-message-title">暂无聊天记录</div>
            <div class="empty-message-description">
              发送第一条消息，开启你们的对话吧~
            </div>
          </div>
        </div>
        
        <div v-else class="messages-wrapper">
          <MessageBubble
            v-for="msg in store.messages"
            :key="msg.id"
            :message="msg"
            :is-own="isOwnMessage(msg)"
          />
        </div>
      </n-spin>
    </div>

    <!-- 输入区域 -->
    <ChatInput @send="handleSendMessage" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch, h } from 'vue'
import { useChatStore } from '@/stores/chat'
import MessageBubble from '../../components/MessageBubble.vue'
import ChatInput from '../../components/ChatInput.vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import type { Message } from '@/types/message'
import messageService from '@/api/message'
import { NIcon, NDropdown, NAvatar, NSpin, NEmpty, NText } from 'naive-ui'
import { Icon } from '@iconify/vue'

const appContext = useGlobalProperties()
const store = useChatStore()
const messageListRef = ref<HTMLElement | null>(null)

// 🔥 简化：直接使用消息中的 isSelf 字段
const isOwnMessage = (msg: Message) => {
  return msg.isSelf === true
}

const menuOptions = [
  {
    label: '查看资料',
    key: 'view-profile',
    icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:user-line' }) })
  },
  {
    label: '清空聊天',
    key: 'clear-chat',
    icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:delete-bin-line' }) })
  }
]

const handleMenuClick = async (key: string) => {
  if (key === 'view-profile') {
    if (store.currentConversation?.userId) {
      window.open(`/#/user/${store.currentConversation.userId}`, '_blank')
    }
  } else if (key === 'clear-chat') {
    if (store.currentConversation) {
      await store.clearChatHistory(store.currentConversation.userId)
    }
  }
}

const handleSendMessage = async (content: string) => {
  if (!store.currentConversation) return
  
  try {
    const response = await messageService.sendMessage({
      toUserId: store.currentConversation.userId,
      content,
      msgType: 0
    })

    const message: Message = {
      id: response.messageId,
      fromUserId: currentUserId.value,
      toUserId: store.currentConversation.userId,
      content,
      msgType: 0,
      status: 1,
      createTime: response.createTime
    }
    
    store.addSentMessage(message)
    
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
  }
}

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  if (target.scrollTop === 0 && !store.loading && store.hasMore) {
    store.loadMoreMessages()
  }
}

const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(async () => {
  console.log('🟢 [onMounted] 开始加载消息')
  if (store.currentConversation && store.currentConversation.userId) {
    await store.loadMessages(store.currentConversation.userId, true)
    console.log('🟢 [onMounted] 消息加载完成，消息数量:', store.messages.length)
    // 🔥 打印每条消息的方向判断
    store.messages.forEach((msg: any, index: number) => {
      const isOwn = isOwnMessage(msg)
      console.log(`📝 [消息${index}]`, {
        id: msg.id,
        content: msg.content.substring(0, 30),
        fromUserId: msg.fromUserId,
        isSelf: msg.isSelf,
        isOwn: isOwn,
        direction: isOwn ? '➡️ 右边（我发送的）' : '⬅️ 左边（收到的）'
      })
    })
    await nextTick()
    scrollToBottom()
  }
})

watch(() => store.messages.length, async () => {
  await nextTick()
  scrollToBottom()
}, { immediate: true })
</script>

<style scoped lang="scss">
.chat-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-info {
  display: flex;
  flex-direction: column;
}

.chat-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chat-status {
  font-size: 12px;
  color: #18a058;
  margin-top: 2px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  scroll-behavior: smooth;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
  }
}

.load-more-trigger {
  text-align: center;
  padding: 12px;
  margin-bottom: 10px;
}

.empty-messages {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    text-align: center;

    .empty-message-title {
      margin-top: 24px;
      font-size: 16px;
      font-weight: 600;
      color: #606266;
    }

    .empty-message-description {
      margin-top: 12px;
      font-size: 13px;
      color: #909399;
      line-height: 1.6;
    }
  }
}

.messages-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
