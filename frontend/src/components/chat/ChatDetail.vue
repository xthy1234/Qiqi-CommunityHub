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
          <template v-for="msg in store.messages" :key="msg.id">
            <!-- 系统提示消息 -->
            <SystemMessageTip
              v-if="msg._isSystemTip"
              :type="msg._tipType"
              :username="msg._tipUsername"
              :is-own="msg.isSelf"
              :timestamp="msg.createTime"
            />

            <!-- 普通消息气泡 -->
            <MessageBubble
              v-else
              :message="msg"
              :is-own="isOwnMessage(msg)"
              @recall="handleRecallMessage"
              @delete="handleDeleteMessage"
              @copy="handleCopyMessage"
            />
          </template>
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
import MessageBubble from './MessageBubble.vue'
import ChatInput from './ChatInput.vue'
import SystemMessageTip from './SystemMessageTip.vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import type { Message } from '@/types/message'
import type { ConversationVO } from '@/types/message'
import messageService from '@/api/message'
import chatService from '@/api/chat'
import { NIcon, NDropdown, NAvatar, NSpin, NEmpty, NText } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { getWebSocket } from '@/utils/websocket'
import { ElMessage } from 'element-plus'

const appContext = useGlobalProperties()
const store = useChatStore()
const messageListRef = ref<HTMLElement | null>(null)
const wsUnsubscribeFunctions = ref<(() => void)[]>([])
const isProcessingRecall = ref(false) //  防止重复处理的标志位

//  获取当前登录用户 ID（用于发送消息）
const currentUserId = computed(() => {
  //  尝试从 UserInfo 读取（登录时存储的 key）
  const userInfoStr = appContext?.$toolUtil?.storageGet('UserInfo')
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      const userId = userInfo?.id
      if (userId) {

        return userId
      }
    } catch (error) {
      console.error('❌ [currentUserId] 解析 UserInfo 失败:', error)
    }
  }

  // 降级方案：尝试从 userid 读取
  const userIdStr = appContext?.$toolUtil?.storageGet('userid')
  if (userIdStr) {
    const userId = parseInt(userIdStr)

    return userId
  }

  console.warn('⚠️ [currentUserId] 未获取到用户 ID')
  return null
})

//  判断是否是自己发送的消息（使用后端返回的 isSelf 字段）
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

//  监听当前会话变化，自动保存
watch(() => store.currentConversation, (newConv: ConversationVO | null) => {
  if (newConv && newConv.userId) {

    store.saveCurrentSessionToStorage()
  }
}, { deep: true })

const handleSendMessage = async (content: string) => {
  if (!store.currentConversation) return
  
  try {    //  1. 乐观添加：立即显示在界面上，标记为"发送中"

    const tempMessage = store.addSendingMessage(content, store.currentConversation.userId)
    await nextTick()
    scrollToBottom()


    //  2. 发送到后端（HTTP API 确保持久化）

    //实际上，不需要再通过旧的接口向数据库中插入数据了。。。吧
    // const response = await messageService.sendMessage({
    //   toUserId: store.currentConversation.userId,
    //   content,
    //   msgType: 0
    // })    //  3. 通过 WebSocket 推送给对方（同时也会推送给自己）
    if (chatService.isConnected()) {

      const ws = chatService.getWebSocket()
      if (ws && store.currentConversation) {

        ws.sendPrivateMessage(
          store.currentConversation.userId,
          content,
          0
        )


      }
    } else {
      console.warn('⚠️ [步骤 3] WebSocket 未连接，无法推送')
      //  如果 WebSocket 未连接，降级使用 HTTP 响应


      try {
        const response = await messageService.sendMessage({
          toUserId: store.currentConversation.userId,
          content,
          msgType: 0
        })

        const fallbackMessage: Message = {
          id: response.messageId || 0,
          _tempId: tempMessage._tempId,
          fromUserId: currentUserId.value || 0,
          toUserId: store.currentConversation.userId,
          content,
          msgType: 0,
          status: 'SENT',
          createTime: response.createTime || new Date().toISOString(),
          isSelf: true
        }
        store.confirmSentMessage(fallbackMessage)
      } catch (httpError) {
        console.error('❌ [降级处理] HTTP 发送消息失败:', httpError)
        ElMessage.error('消息发送失败，请检查网络连接')
      }
    }

    await nextTick()
    scrollToBottom()


  } catch (error) {
    console.error('❌ [ChatDetail.sendMessage] 发送消息失败:', error)
    console.error('  - 错误堆栈:', error)
    //  TODO: 消息发送失败处理（显示重试按钮）
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


  //  如果是通过路由参数打开的会话，不需要恢复
  const routeUserId = store.currentConversation?.userId

//       hasRouteUserId: !!routeUserId,
//       hasCurrentConversation: !!store.currentConversation,
//       routeUserId
//     })

  if (routeUserId || !store.currentConversation) {
    // 已经有当前会话对象，直接加载消息
    if (routeUserId) {
      await store.loadMessages(routeUserId, true)


      //  等待 DOM tick 确保响应式系统完全更新
      await nextTick()
      await nextTick()

      //  处理撤回消息：将 isRecalled=true 的消息转换为系统提示
      processRecalledMessages()

      //  再次等待，确保转换后的消息也触发视图更新
      await nextTick()

      //  打印每条消息的方向判断
      store.messages.forEach((msg: any, index: number) => {
        const isOwn = isOwnMessage(msg)

        //   id: msg.id,
        //   content: msg.content.substring(0, 30),
        //   fromUserId: msg.fromUserId,
        //   isSelf: msg.isSelf,
        //   isOwn: isOwn,
        //   direction: isOwn ? '➡️ 右边（我发送的）' : '⬅️ 左边（收到的）'
        // })
      })
      await nextTick()
      scrollToBottom()

      //  发送已读回执（延迟发送，等待页面渲染完成）
      setTimeout(() => {
        sendReadReceiptIfNeed(routeUserId)
      }, 500)
    }
  } else {
    //  没有当前会话，尝试从 sessionStorage 恢复

    const savedSession = store.restoreSessionFromStorage()

    if (savedSession) {


      // 构造会话对象
      const conversation = {
        userId: savedSession.userId,
        username: savedSession.username,
        avatar: savedSession.avatar,
        lastMessage: '',
        lastTime: new Date().toISOString(),
        unreadCount: 0
      }

      // 切换到该会话
      await store.switchConversation(conversation as any)


      ElMessage.success(`已恢复到与 ${savedSession.username} 的聊天`)

      //  发送已读回执
      setTimeout(() => {
        sendReadReceiptIfNeed(savedSession.userId)
      }, 500)
    } else {

    }
  }

  //  注册 WebSocket 消息处理器
  registerWebSocketHandlers()
})

onUnmounted(() => {

  //  清理 WebSocket 处理器
  cleanupWebSocketHandlers()
})

//  只保留一个 watch：监听消息列表长度变化
watch(() => store.messages.length, async (newLen: number, oldLen: number) => {
  //  只在消息数量增加时才处理（新消息到达）
  if (newLen !== oldLen) {
    await nextTick()
    scrollToBottom()

    //  检查新消息中是否有撤回消息
    processRecalledMessages()
  }
}, { immediate: true })

//  处理消息撤回
const handleRecallMessage = (messageId: number) => {  //  1. 乐观更新：立即在界面上显示撤回状态

  store.recallMessageOptimistic(messageId)


  //  2. 通过 WebSocket 发送撤回请求到后端
  const ws = getWebSocket()
  if (ws && ws.isConnected()) {

    ws.recallMessage(messageId)

  } else {
    console.error('❌ [步骤 2] WebSocket 未连接，无法发送撤回请求')
    ElMessage.error('网络连接异常，撤回失败')
  }

}

//  处理消息删除（乐观更新版本）
const handleDeleteMessage = (messageId: number) => {  //  1. 乐观删除：立即从界面移除（仅对自己可见）

  const deletedMsg = store.deleteMessageOptimistic(messageId)

  if (deletedMsg) {

  } else {
    console.warn('⚠️ [步骤 1] 未找到要删除的消息')

    return
  }

  //  2. 通过 WebSocket 发送删除请求到后端
  const ws = getWebSocket()
  if (ws && ws.isConnected()) {

    ws.deleteMessage(messageId)

  } else {
    console.error('❌ [步骤 2] WebSocket 未连接，无法发送删除请求')
    ElMessage.error('网络连接异常，删除失败')

    //  如果 WebSocket 未连接，可以选择回滚或删除失败提示
    // 这里选择回滚（重新添加消息）
    if (deletedMsg) {

      store.messages.push(deletedMsg)
    }
  }

}

//  处理复制消息
const handleCopyMessage = (content: string) => {

  // TODO: 可以显示一个提示消息
}

//  注册 WebSocket 消息处理器
const registerWebSocketHandlers = () => {
  const ws = getWebSocket()
  if (!ws) {
    console.warn('⚠️ [ChatDetail] WebSocket 未初始化，无法注册处理器')
    return
  }


  //  注册撤回消息处理器 - 使用 store 的方法
  const unsubscribeRecall = ws.on('MESSAGE_RECALL', (data: any) => {

    if (data.messageId) {
      //  直接调用 store 的方法处理
      store.receiveRecallNotification(data)

    }
  })
  wsUnsubscribeFunctions.value.push(unsubscribeRecall)


  //  注册删除消息处理器 - 使用 store 的方法
  const unsubscribeDelete = ws.on('MESSAGE_DELETE', (data: any) => {

    if (data.messageId) {
      //  直接调用 store 的方法处理（确认删除）
      store.receiveDeleteNotification(data)

    }
  })
  wsUnsubscribeFunctions.value.push(unsubscribeDelete)


}

//  清理 WebSocket 处理器
const cleanupWebSocketHandlers = () => {

  wsUnsubscribeFunctions.value.forEach(unsubscribe => {
    try {
      unsubscribe()
    } catch (error) {
      console.error('❌ [ChatDetail] 清理处理器失败:', error)
    }
  })
  wsUnsubscribeFunctions.value = []

}

//  处理加载的消息中的撤回消息
const processRecalledMessages = () => {
  //  防止重复处理
  if (isProcessingRecall.value) {

    return
  }


  let processedCount = 0

  store.messages.forEach((msg: any, index: number) => {
    if (msg.isRecalled === true && !msg._isSystemTip) {


      //  关键：创建新对象并替换，确保触发响应式更新
      const newMsg = {
        ...msg,
        content: 'recall',
        _isSystemTip: true,
        _tipType: 'recall',
        _tipUsername: msg.fromUser?.username || msg.fromUser?.nickname || ''
      }

      //  使用 splice 替换元素，确保触发响应式
      store.messages.splice(index, 1, newMsg as any)

      processedCount++
    }
  })

  if (processedCount > 0) {


    //  强制触发下一次 tick 的更新
    nextTick(() => {

      isProcessingRecall.value = false //  重置标志位
    })

    isProcessingRecall.value = true //  立即设置标志位，防止 watch 再次触发
  } else {
    isProcessingRecall.value = false //  没有处理也重置
  }
}

//  发送已读回执（如果需要）
const sendReadReceiptIfNeed = (userId: number) => {//     targetUserId: userId,
//     currentUserId: currentUserId.value,
//     totalMessages: store.messages.length
//   })

  //  详细打印每条消息的状态

  store.messages.forEach((msg: Message, index: number) => {
    //  检查是否匹配条件
    const isToMe = msg.toUserId === currentUserId.value
    const isFromOtherUser = msg.fromUserId !== currentUserId.value
    //  只检测对方发来的、未读的消息（排除自己发的）
    //  修复：删除 'UNREAD'，因为 Message 类型中没有这个值
    const isUnread = (msg.status === 0 || msg.status === 'SENT' || msg.status === 'DELIVERED') && !msg.isSelf
    const shouldMarkAsRead = isToMe && isFromOtherUser && isUnread  })


  // 检查是否有未读消息
  const hasUnread = store.messages.some((msg: Message) =>
    msg.toUserId === currentUserId.value &&
    msg.fromUserId !== currentUserId.value &&
    (msg.status === 0 || msg.status === 'SENT' || msg.status === 'DELIVERED') &&
    !msg.isSelf
  )

//     hasUnread,
//     userId,
//     currentUserId: currentUserId.value,
//     totalMessages: store.messages.length,
//     conditions: {
//       'toUserId 匹配': `msg.toUserId === ${currentUserId.value}`,
//       'fromUserId 不匹配': `msg.fromUserId !== ${currentUserId.value}`,
//       'status 是未读': '0 (数据库), SENT, 或 DELIVERED',
//       '不是自己的消息': '!msg.isSelf'
//     }
//   })

  if (hasUnread) {


    const ws = getWebSocket()
    if (ws && ws.isConnected()) {
      ws.sendReadReceipt(userId)


      // 本地更新消息状态
      updateLocalMessageStatus(userId, 'READ')
    } else {
      console.warn('⚠️ [sendReadReceiptIfNeed] WebSocket 未连接，无法发送已读回执')
    }
  } else {    //  额外诊断：检查是否有对方发来的消息
    const messagesFromOther = store.messages.filter((msg: Message)=>
      msg.fromUserId !== currentUserId.value && msg.toUserId === currentUserId.value
    )


  }

}

//  本地更新消息状态
const updateLocalMessageStatus = (userId: number, status: 'READ' | 'UNREAD') => {


  store.messages.forEach((msg: any) => {
    if (msg.toUserId === currentUserId.value && msg.fromUserId === userId) {
      msg.status = status === 'READ' ? 'READ' : 'UNREAD'
    }
  })

}

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
