<template>
  <div class="chat-detail">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="chat-header-left">
        <n-avatar :src="store.currentConversation?.avatar" round size="medium" />
        <div class="chat-info">
          <div class="chat-name">{{ store.currentConversation?.username }}</div>
          <div :class="['chat-status', { online: isOtherUserOnline }]">
            {{ isOtherUserOnline ? '在线' : lastSeenAtText }}
          </div>
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
import { MessageStatus, isUnreadMessage } from '@/types/message'
import { debounce } from '@/utils/function'
import type { WsUserOnlineStatus } from '@/types/message'

const appContext = useGlobalProperties()
const store = useChatStore()
const messageListRef = ref<HTMLElement | null>(null)
const wsUnsubscribeFunctions = ref<(() => void)[]>([])
const isProcessingRecall = ref(false)
const lastReadTimestamp = ref<Map<number, number>>(new Map())
const pendingReadReceipts = ref<Set<number>>(new Set())
const isUserScrolling = ref(false)
const isAtBottom = ref(true)
const lastMessageLength = ref(0)

// 在线状态相关
const isOtherUserOnline = ref(false)
const lastSeenAt = ref<string | undefined>(undefined)
let unsubscribeOnlineStatus: (() => void) | null = null

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
  
  try {
    //  1. 乐观添加：立即显示在界面上，标记为"发送中"

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

const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const debouncedSendReadReceipt = debounce((userId: number, reason: string = '用户操作') => {
    const ws = getWebSocket()
    if (ws && ws.isConnected()) {
        ws.sendReadReceipt(userId)

        const userInfo = appContext?.$toolUtil?.storageGet('UserInfo')
        const currentUserId = userInfo ? JSON.parse(userInfo).id : null

        const messagesFromUser = store.messages.filter((msg: Message) =>
            msg.fromUserId === userId &&
            msg.toUserId === currentUserId &&
            isUnreadMessage(msg.status) &&
            !msg.isSelf
        )

        if (messagesFromUser.length > 0) {
            const lastMessage = messagesFromUser[messagesFromUser.length - 1]
            lastReadTimestamp.value.set(userId, Date.now())

            // console.log('✅ [已读回执] 已发送给用户:', userId,
            //     '- 原因:', reason,
            //     '- 消息数:', messagesFromUser.length,
            //     '- 最后消息 ID:', lastMessage.id)
        }
    } else {
        pendingReadReceipts.value.add(userId)
        console.warn('⚠️ [sendReadReceipt] WebSocket 未连接，已加入待发送队列:', userId)
    }
}, 300)

const sendReadReceiptIfNeed = (userId: number, reason: string = '检查未读消息') => {
    if (!userId) {
        return
    }

    const userInfo = appContext?.$toolUtil?.storageGet('UserInfo')
    const currentUserId = userInfo ? JSON.parse(userInfo).id : null

    if (!currentUserId) {
        console.warn('⚠️ [sendReadReceiptIfNeed] 未获取到当前用户 ID')
        return
    }

    const hasUnread = store.messages.some((msg: Message) =>
        msg.toUserId === currentUserId &&
        msg.fromUserId !== currentUserId &&
        isUnreadMessage(msg.status) &&
        !msg.isSelf
    )

    if (hasUnread) {
        debouncedSendReadReceipt(userId, reason)
    }
    // else {
    //     console.log('ℹ️ [sendReadReceiptIfNeed] 无未读消息，跳过:', userId, '- 原因:', reason)
    // }
}

const retryPendingReadReceipts = () => {
    const ws = getWebSocket()
    if (!ws || !ws.isConnected()) {
        return
    }

    pendingReadReceipts.value.forEach(userId => {
        // console.log('🔄 [重试] 补发已读回执给用户:', userId)
        debouncedSendReadReceipt(userId, 'WebSocket 重连后补发')
    })

    pendingReadReceipts.value.clear()
}

onMounted(async () => {
  //  如果是通过路由参数打开的会话，不需要恢复
  const routeUserId = store.currentConversation?.userId
  if (routeUserId || !store.currentConversation) {
    if (routeUserId) {
      await store.loadMessages(routeUserId, true)
      await nextTick()
      await nextTick()
      processRecalledMessages()
      await nextTick()
      store.messages.forEach((msg: any, index: number) => {
        const isOwn = isOwnMessage(msg)
      })
      await nextTick()
      scrollToBottom()

      setTimeout(() => {
        sendReadReceiptIfNeed(routeUserId, '页面加载完成')
      }, 300)
    }
  } else {
    const savedSession = store.restoreSessionFromStorage()

    if (savedSession) {
        const conversation = {
            userId: savedSession.userId,
            username: savedSession.username,
            avatar: savedSession.avatar,
            lastMessage: '',
            lastTime: new Date().toISOString(),
            unreadCount: 0
        }

        await store.switchConversation(conversation as any)

        ElMessage.success(`已恢复到与 ${savedSession.username} 的聊天`)

        setTimeout(() => {
            sendReadReceiptIfNeed(savedSession.userId, '会话恢复')
        }, 300)
    }
  }

  registerWebSocketHandlers()

  const ws = getWebSocket()
  if (ws) {
      const unsubscribeConnection = ws.onStateChange((state) => {
          if (state === 1) {
              setTimeout(() => {
                  retryPendingReadReceipts()
              }, 500)
          }
      })
      wsUnsubscribeFunctions.value.push(unsubscribeConnection)
  }

  setupOnlineStatusListener()
})

onUnmounted(() => {

  //  清理 WebSocket 处理器
  cleanupWebSocketHandlers()

  unsubscribeOnlineStatus?.()
})


//  检测用户是否在底部
const checkIsAtBottom = () => {
    const messageListEl = messageListRef.value
    if (!messageListEl) {
        // console.log('⚠️ [checkIsAtBottom] messageListEl 为 null')
        return false
    }

    // 距离底部 50px 以内都算在底部
    const threshold = 50
    const scrollTop = messageListEl.scrollTop
    const scrollHeight = messageListEl.scrollHeight
    const clientHeight = messageListEl.clientHeight

    const distanceToBottom = scrollHeight - scrollTop - clientHeight

    // console.log('📍 [checkIsAtBottom]',
    //     '- scrollTop:', scrollTop,
    //     '- scrollHeight:', scrollHeight,
    //     '- clientHeight:', clientHeight,
    //     '- 距离底部:', distanceToBottom,
    //     '- 阈值:', threshold,
    //     '- 结果:', distanceToBottom <= threshold)

    return distanceToBottom <= threshold
}

//  监听用户滚动行为
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement

  // 更新是否在底部的状态
  const wasAtBottom = isAtBottom.value
  isAtBottom.value = checkIsAtBottom()

  // 检测用户是否正在往上翻
  if (!isAtBottom.value) {
    isUserScrolling.value = true
  } else {
    isUserScrolling.value = false
  }

  // console.log('📜 [handleScroll]',
  //     '- 之前在底部:', wasAtBottom,
  //     '- 现在在底部:', isAtBottom.value,
  //     '- 是否滚动中:', isUserScrolling.value)

  // 加载更多历史消息
  if (target.scrollTop === 0 && !store.loading && store.hasMore) {
    store.loadMoreMessages()
  }
}

watch(() => store.messages.length, async (newLen: number, oldLen: number) => {
    const addedCount = newLen - oldLen

    // console.log('👀 [Watch] 消息长度变化:',
    //     '- 旧值:', oldLen,
    //     '- 新值:', newLen,
    //     '- 增加:', addedCount,
    //     '- isAtBottom:', isAtBottom.value,
    //     '- isUserScrolling:', isUserScrolling.value)

    //  如果是加载历史消息（一次性添加≥15 条）
    if (addedCount >= 15) {
        console.log('📜 [Watch] 检测到加载历史消息，保持滚动位置')

        // 关键修复：保存当前滚动位置和高度
        const messageListEl = messageListRef.value
        if (messageListEl) {
            // 临时禁用平滑滚动
            messageListEl.style.scrollBehavior = 'auto'

            const currentScrollTop = messageListEl.scrollTop
            const currentScrollHeight = messageListEl.scrollHeight

            await nextTick()

            // 计算新增消息导致的高度变化
            const newScrollHeight = messageListEl.scrollHeight
            const heightDiff = newScrollHeight - currentScrollHeight

            // 调整滚动条，补偿新增的高度，保持用户视野不变
            messageListEl.scrollTop = currentScrollTop + heightDiff

            // console.log('📏 [Watch] 已保持滚动位置:',
            //     '- 原 scrollTop:', currentScrollTop,
            //     '- 原 scrollHeight:', currentScrollHeight,
            //     '- 新 scrollHeight:', newScrollHeight,
            //     '- 高度差:', heightDiff,
            //     '- 新 scrollTop:', messageListEl.scrollTop)

            // 恢复平滑滚动
            setTimeout(() => {
                messageListEl.style.scrollBehavior = 'smooth'
            }, 50)
        }

        lastMessageLength.value = newLen
        return
    }

    //  如果是收到新消息（添加 1-2 条）
    if (addedCount > 0 && addedCount < 15) {
        const messageListEl = messageListRef.value
        if (messageListEl) {
            // 临时禁用平滑滚动
            messageListEl.style.scrollBehavior = 'auto'

            // 关键修复：保存当前滚动位置和可视区域
            const currentScrollTop = messageListEl.scrollTop
            const currentScrollHeight = messageListEl.scrollHeight

            await nextTick()

            // 等待 DOM 更新后，计算新增消息导致的高度变化
            const newScrollHeight = messageListEl.scrollHeight
            const heightDiff = newScrollHeight - currentScrollHeight

            // 调整滚动条，补偿新增的高度
            messageListEl.scrollTop = currentScrollTop + heightDiff

            // console.log('📏 [Watch] 调整滚动条以保持视野:',
            //     '- 原 scrollTop:', currentScrollTop,
            //     '- 原 scrollHeight:', currentScrollHeight,
            //     '- 新 scrollHeight:', newScrollHeight,
            //     '- 高度差:', heightDiff,
            //     '- 新 scrollTop:', messageListEl.scrollTop)

            // 恢复平滑滚动
            setTimeout(() => {
                messageListEl.style.scrollBehavior = 'smooth'
            }, 50)
        } else {
            await nextTick()
        }

        processRecalledMessages()

        // 只有在底部时才滚动到最新
        if (isAtBottom.value && !isUserScrolling.value) {
            scrollToBottom()
        }

        const ws = getWebSocket()
        if (ws && ws.isConnected() && store.currentConversation?.userId) {
            const newMessages = store.messages.slice(oldLen)
            const hasUnreadFromCurrentConv = newMessages.some((msg: Message) =>
                msg.fromUserId === store.currentConversation?.userId &&
                isUnreadMessage(msg.status) &&
                !msg.isSelf
            )

            if (hasUnreadFromCurrentConv) {
                // console.log('📨 [Watch] 收到当前会话的新消息，触发已读回执')
                debouncedSendReadReceipt(store.currentConversation.userId, '收到新消息')
            }
        }
    }

    lastMessageLength.value = newLen
    isUserScrolling.value = false
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

// 监听在线状态更新
const setupOnlineStatusListener = () => {
  const ws = getWebSocket()
  if (!ws || !ws.isConnected()) {
    console.warn('⚠️ [OnlineStatus] WebSocket 未连接')
    return
  }

  const otherUserId = store.currentConversation?.userId
  console.log('🔵 [OnlineStatus] 开始订阅，当前聊天对象 ID:', otherUserId)

  // 订阅用户在线状态
  const handler = (data: any) => {
    console.log('📥 [OnlineStatus] 收到原始数据:', data)

    // 兼容处理：后端可能返回数组或单个对象
    let statusData
    if (Array.isArray(data)) {
      // 如果是数组，取第一个元素
      statusData = data[0]
      console.log('📥 [OnlineStatus] 检测到数组格式，提取第一个元素:', statusData)
    } else {
      statusData = data
    }

    if (!otherUserId) {
      console.warn('⚠️ [OnlineStatus] 当前聊天对象 ID 为空')
      return
    }

    // 兼容字段名：isOnline 或 online
    const userId = statusData.userId
    const isOnline = statusData.isOnline ?? statusData.online
    const lastSeenAtValue = statusData.lastSeenAt

    console.log('🔍 [OnlineStatus] 解析后的数据:',
      '- userId:', userId,
      '- isOnline:', isOnline,
      '- lastSeenAt:', lastSeenAtValue)

    if (userId === otherUserId) {
      isOtherUserOnline.value = isOnline
      lastSeenAt.value = lastSeenAtValue

      console.log('🟢 [OnlineStatus] 状态更新成功:',
        '- 用户 ID:', otherUserId,
        '- 在线:', isOnline,
        '- 最后时间:', lastSeenAtValue)
    } else {
      console.log('⚠️ [OnlineStatus] 用户 ID 不匹配，跳过:',
        '- 期望:', otherUserId,
        '- 实际:', userId)
    }
  }

  unsubscribeOnlineStatus = ws.on('USER_ONLINE_STATUS', handler)

  console.log('✅ [OnlineStatus] 已注册处理器，等待后端推送...')

  // 主动查询当前聊天对象的在线状态
  if (store.currentConversation?.userId) {
    console.log('🔍 [OnlineStatus] 主动查询用户在线状态:', store.currentConversation.userId)
    ws.queryUserOnlineStatus([store.currentConversation.userId])
  } else {
    console.warn('⚠️ [OnlineStatus] 当前会话为空，无法查询')
  }
}

// 计算最后在线时间文本
const lastSeenAtText = computed(() => {
  if (!lastSeenAt.value) return '离线'

  const now = Date.now()
  const lastSeen = new Date(lastSeenAt.value).getTime()
  const diff = now - lastSeen

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) {
    return '刚刚离线'
  } else if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`
  } else if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  } else {
    return `${Math.floor(diff / day)}天前`
  }
})

// 监听当前会话变化，重新订阅在线状态
watch(() => store.currentConversation?.userId, async (newUserId, oldUserId) => {
  if (newUserId && newUserId !== oldUserId) {
    // 取消旧的订阅
    unsubscribeOnlineStatus?.()

    // 重置状态
    isOtherUserOnline.value = false
    lastSeenAt.value = undefined

    // 等待一下确保 WebSocket 已就绪
    await nextTick()
    setupOnlineStatusListener()
  }
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
