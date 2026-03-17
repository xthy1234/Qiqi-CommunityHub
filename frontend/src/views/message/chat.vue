<template>
<!--  <PageContainer header-title="聊天" :show-back="false">-->

  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <ConversationPanel
      :conversations="store.conversations"
      :loading="store.loading"
      :unread-count="store.unreadCount"
      :active-user-id="store.currentConversation?.userId"
      @select-conversation="handleSelectConversation"
    />

    <!-- 右侧聊天区域 -->
    <div class="chat-panel">
      <ChatDetail v-if="store.currentConversation" />
      <EmptyChat v-else />
    </div>

    <!-- WebSocket 连接状态提示 -->
    <div v-if="!isConnected" class="connection-toast">
      <n-alert title="WebSocket 未连接" type="warning" :bordered="false" closable>
        正在尝试重新连接...
      </n-alert>
    </div>
  </div>
<!--  </PageContainer>-->
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, watch, ref} from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import ChatDetail from '../../components/chat/ChatDetail.vue'
import ConversationPanel from '@/components/chat/ConversationPanel.vue'
import EmptyChat from '@/components/chat/EmptyChat.vue'
import { useRouter } from 'vue-router'
import PageContainer from "@/components/common/PageContainer.vue";
import chatService from '@/api/chat'
import type { Message } from '@/types/message'
import { NAlert } from 'naive-ui'

const store = useChatStore()
const router = useRouter()
const route = useRoute()

const isConnected = ref(true)
let unsubscribeMessage: (() => void) | null = null
let unsubscribeStatus: (() => void) | null = null
let unsubscribeConnection: (() => void) | null = null

let isInitialized = false

/** 设置 WebSocket 消息监听 */
const setupWebSocketListeners = () => {
  // 监听新消息
  unsubscribeMessage = chatService.onNewMessage((message: Message) => {
    console.log('📩 [WebSocket] 收到新消息:', message)
    store.receiveMessage(message)
  })

  // 监听消息状态更新
  unsubscribeStatus = chatService.onMessageStatusUpdate(({ messageId, status }) => {
    console.log('📊 [WebSocket] 消息状态更新:', messageId, status)
    store.updateMessageStatus(messageId, status)
  })

  // 监听连接状态变化
  unsubscribeConnection = chatService.onConnectionChange((state) => {
    console.log('🔌 [WebSocket] 连接状态变化:', state)
    isConnected.value = state === 'connected'
  })
}

/** 清理 WebSocket 监听 */
const cleanupWebSocketListeners = () => {
  unsubscribeMessage?.()
  unsubscribeStatus?.()
  unsubscribeConnection?.()
}

/** 初始化 WebSocket 连接 */
const initializeWebSocket = async () => {
  try {
    console.log('🟢 开始连接 WebSocket...')
    await chatService.connect()
    console.log('✅ WebSocket 连接成功')
    isConnected.value = true

    setupWebSocketListeners()
  } catch (error) {
    console.error('❌ WebSocket 连接失败:', error)
    isConnected.value = false
  }
}

/** 断开 WebSocket 连接 */
const disconnectWebSocket = () => {
  cleanupWebSocketListeners()
  chatService.disconnect()
  isConnected.value = false
}

/** 初始化与指定用户的聊天 */
const initializeChatWithUser = async (userId: number) => {
  // 防止重复初始化同一个用户
  if (isInitialized && store.currentConversation?.userId === userId) {
    console.log('⚠️ 已经初始化过该用户，跳过')
    return
  }

  if (!userId) {
    console.log('❌ userId 为空，跳过初始化')
    return
  }

  console.log('🟢 开始初始化与用户', userId, '的聊天')

  // 🔥 检查是否已有该会话（从后端加载的）
  const existingConv = store.conversations.find((c: any) => c.userId === userId)
  console.log('🔍 是否已有会话:', !!existingConv)

  if (existingConv) {
    console.log('✅ 已有会话，直接切换')
    await store.switchConversation(existingConv)
  } else {
    console.log('⚠️ 没有会话，创建临时会话（仅用于右侧聊天）')
    // 创建临时会话（仅用于首次聊天，不添加到左侧列表）
    const tempConv = {
      userId,
      username: '未知用户',
      avatar: '',
      lastMessage: '',
      lastTime: new Date().toISOString(),
      unreadCount: 0
    }

    // 🔥 关键：只设置为当前会话，不添加到 conversations 列表
    store.currentConversation = tempConv
    store.messages = []

    // 获取用户信息并加载消息
    console.log('🔵 调用 switchConversation 来获取用户信息和加载消息')
    await store.switchConversation(tempConv)

    console.log('✅ 临时会话创建完成，用户可以在右侧聊天窗口发消息了')
  }

  isInitialized = true
  console.log('✅ 初始化完成，isInitialized = true')
}

// 监听路由参数变化
watch(() => route.params.userId, async (newUserId: string | undefined, oldUserId: string | undefined) => {
  console.log('🟡 路由参数变化:', { old: oldUserId, new: newUserId })

  // 如果是首次初始化（undefined -> undefined），跳过
  if (!newUserId || newUserId === oldUserId) {
    console.log('⚠️ userId 无变化或为空，跳过')
    return
  }

  const userId = Number(newUserId)
  console.log('🟡 userId 转换结果:', userId, 'isNaN:', isNaN(userId))

  if (!isNaN(userId)) {
    console.log('🟡 开始调用 initializeChatWithUser')
    try {
      await initializeChatWithUser(userId)
      console.log('🟡 initializeChatWithUser 执行完成')
    } catch (error) {
      console.error('❌ initializeChatWithUser 执行失败:', error)
    }
  }
}, { immediate: true })

// 组件挂载时加载会话列表和连接 WebSocket
onMounted(async () => {
  console.log('🟢 [onMounted] 组件已挂载')
  console.log('🟢 [onMounted] 开始加载会话列表')

  try {
    await store.loadConversations()
    console.log('📋 [onMounted] 会话列表加载完成，数量:', store.conversations.length)
  } catch (error) {
    console.error('❌ [onMounted] 加载会话列表失败:', error)
  }

  // 🔥 连接 WebSocket
  await initializeWebSocket()

  // 🔥 如果有路由参数且未被 watch 处理过，需要延迟初始化
  // （因为 watch 的 immediate 可能先于 onMounted 执行）
  if (route.params.userId && !isInitialized) {
    const userId = Number(route.params.userId)
    console.log('🟢 [onMounted] 检测到路由参数 userId:', userId)

    if (!isNaN(userId)) {
      console.log('🟢 [onMounted] 等待会话列表加载完成后初始化聊天')
      setTimeout(async () => {
        console.log('🟢 [setTimeout] 开始初始化聊天')
        try {
          await initializeChatWithUser(userId)
          console.log('🟢 [setTimeout] 初始化聊天完成')
        } catch (error) {
          console.error('❌ [setTimeout] 初始化聊天失败:', error)
        }
      }, 100)
    }
  } else {
    console.log('⚠️ [onMounted] 已经初始化过或没有路由参数，跳过')
  }
})

// 组件卸载时断开 WebSocket
onUnmounted(() => {
  console.log('🔴 [onUnmounted] 断开 WebSocket 连接')
  disconnectWebSocket()
})

const handleSelectConversation = async (conv: any) => {
  console.log('🔵 [handleSelectConversation] 选择会话:', conv)
  await store.switchConversation(conv)
}
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: calc(100vh - 10px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
}

.chat-panel {
  flex: 1;
  min-width: 0;
  background: #f5f5f5;
  position: relative;
}

.connection-toast {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  width: auto;
  max-width: 400px;

  :deep(.n-alert) {
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}
</style>
