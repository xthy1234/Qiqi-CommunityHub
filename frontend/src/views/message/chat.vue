<template>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import ChatDetail from './ChatDetail.vue'
import ConversationPanel from '@/components/ConversationPanel.vue'
import EmptyChat from '@/components/EmptyChat.vue'
import { useRouter } from 'vue-router'

const store = useChatStore()
const router = useRouter()
const route = useRoute()

let isInitialized = false

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
    console.log('⚠️ 没有会话，创建临时会话（仅用于右侧聊天窗口）')
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

// 组件挂载时加载会话列表
onMounted(async () => {
  console.log('🟢 [onMounted] 组件已挂载')
  console.log('🟢 [onMounted] 开始加载会话列表')

  try {
    await store.loadConversations()
    console.log('📋 [onMounted] 会话列表加载完成，数量:', store.conversations.length)
  } catch (error) {
    console.error('❌ [onMounted] 加载会话列表失败:', error)
  }

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
}

.chat-panel {
  flex: 1;
  min-width: 0;
  background: #f5f5f5;
  position: relative;
}
</style>
