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
  unsubscribeMessage = chatService.onNewMessage((message: Message) => {    //  关键：使用 isSelf 字段判断
    if (message.isSelf) {


      store.confirmSentMessage(message)

    } else {


      store.receiveMessage(message)

    }

  })

  // 监听消息状态更新
  unsubscribeStatus = chatService.onMessageStatusUpdate((data) => {

    //  传递整个数据对象，而不是解构
    store.updateMessageStatus(data)
  })

  // 监听连接状态变化
  unsubscribeConnection = chatService.onConnectionChange((state) => {

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

    await chatService.connect()

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

    return
  }

  if (!userId) {

    return
  }


  //  检查是否已有该会话（从后端加载的）
  const existingConv = store.conversations.find((c: any) => c.userId === userId)


  if (existingConv) {

    await store.switchConversation(existingConv)
  } else {

    // 创建临时会话（仅用于首次聊天，不添加到左侧列表）
    const tempConv = {
      userId,
      username: '未知用户',
      avatar: '',
      lastMessage: '',
      lastTime: new Date().toISOString(),
      unreadCount: 0
    }

    //  关键：只设置为当前会话，不添加到 conversations 列表
    store.currentConversation = tempConv
    store.messages = []

    // 获取用户信息并加载消息

    await store.switchConversation(tempConv)

  }

  isInitialized = true

}

// 监听路由参数变化
watch(() => route.params.userId, async (newUserId: string | undefined, oldUserId: string | undefined) => {


  // 如果是首次初始化（undefined -> undefined），跳过
  if (!newUserId || newUserId === oldUserId) {

    return
  }

  const userId = Number(newUserId)


  if (!isNaN(userId)) {

    try {
      await initializeChatWithUser(userId)

    } catch (error) {
      console.error('❌ initializeChatWithUser 执行失败:', error)
    }
  }
}, { immediate: true })

// 组件挂载时加载会话列表和连接 WebSocket
onMounted(async () => {  try {
    await store.loadConversations()

  } catch (error) {
    console.error('❌ [onMounted] 加载会话列表失败:', error)
  }

  //  连接 WebSocket
  await initializeWebSocket()

  //  如果有路由参数，优先使用路由参数
  if (route.params.userId && !isInitialized) {
    const userId = Number(route.params.userId)


    if (!isNaN(userId)) {

      setTimeout(async () => {

        try {
          await initializeChatWithUser(userId)

        } catch (error) {
          console.error('❌ [setTimeout] 初始化聊天失败:', error)
        }
      }, 100)
    }
  }
  //  如果没有路由参数，尝试从 sessionStorage 恢复会话
  else if (!store.currentConversation) {


    const savedSession = store.restoreSessionFromStorage()

    if (savedSession) {


      // 检查是否已有该会话（从后端加载的）
      const existingConv = store.conversations.find((c: any) => c.userId === savedSession.userId)

      if (existingConv) {

        await store.switchConversation(existingConv)
      } else {

        // 创建临时会话
        const tempConv = {
          userId: savedSession.userId,
          username: savedSession.username,
          avatar: savedSession.avatar,
          lastMessage: '',
          lastTime: new Date().toISOString(),
          unreadCount: 0
        }

        await store.switchConversation(tempConv as any)
      }

    } else {

    }
  } else {

  }
})

// 组件卸载时断开 WebSocket
onUnmounted(() => {

  disconnectWebSocket()

  //  清理会话保存状态
  store.clearSavedSession()

})

const handleSelectConversation = async (conv: any) => {

  await store.switchConversation(conv)

  //  手动保存一次（确保立即保存）

  store.saveCurrentSessionToStorage()
}

</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: calc(100vh - 30px);
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
