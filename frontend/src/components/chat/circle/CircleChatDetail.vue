<!-- src/components/circle-chat/CircleChatDetail.vue -->
<template>
  <div class="circle-chat-detail">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="chat-header-left">
        <n-avatar
          :src="store.currentCircle?.avatar"
          round
          size="medium"
        />
        <div class="chat-info">
          <div class="chat-name">
            {{ store.currentCircle?.name }}
          </div>
          <div class="chat-description">
            {{ store.currentCircle?.memberCount }} 人 · 
            <span
              v-if="onlineCount > 0"
              class="online-count"
            >
              {{ onlineCount }} 人在线
            </span>
          </div>
        </div>
      </div>
      
      <div class="chat-header-right">
        <n-dropdown
          :options="menuOptions"
          trigger="click"
          @select="handleMenuClick"
        >
          <n-button text>
            <template #icon>
              <n-icon size="20">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                >
                  <path
                    fill="currentColor"
                    d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2s-2 .9-2 2s.9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2s2-.9 2-2s-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2s2-.9 2-2s-.9-2-2-2z"
                  />
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
        <!-- 加载更多触发器 -->
        <div
          v-if="store.hasMore && store.messages.length > 0"
          class="load-more-trigger"
        >
          <n-text depth="3">
            加载中...
          </n-text>
        </div>
        
        <!-- 空状态 -->
        <div
          v-if="store.messages.length === 0"
          class="empty-messages"
        >
          <div class="empty-state">
            <n-icon
              size="80"
              color="#dcdfe6"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="80"
                height="80"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"
                />
              </svg>
            </n-icon>
            <div class="empty-message-title">
              暂无聊天记录
            </div>
            <div class="empty-message-description">
              发送第一条消息，开启你们的讨论吧~
            </div>
          </div>
        </div>
        
        <!-- 消息列表 -->
        <div
          v-else
          class="messages-wrapper"
        >
          <template
            v-for="msg in store.messages"
            :key="msg.id || msg._tempId"
          >
            <!-- 系统提示消息 -->
            <SystemMessageTip
              v-if="msg._isSystemTip"
              :type="msg._tipType"
              :username="msg._tipUsername"
              :timestamp="msg.createTime"
            />

            <!-- 普通消息气泡 -->
            <MessageBubble
              v-else
              :message="convertToMessage(msg)"
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
import { useCircleChatStore } from '@/stores/circleChat'
import { useMessage, useDialog } from 'naive-ui'
import type {Circle, CircleMessage} from '@/types/circleChat'
import MessageBubble from '../common/MessageBubble.vue'
import ChatInput from '../common/ChatInput.vue'
import SystemMessageTip from '../common/SystemMessageTip.vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import {NIcon, NDropdown, NAvatar, NSpin, NText} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { getWebSocket } from '@/utils/websocket'
import chatService from '@/api/chat'
import { getCurrentInstance } from 'vue'
import { circleApi } from '@/api/circle'
import { circleMemberApi } from '@/api/circleMember'
import { circleChatApi } from '@/api/circleChat'
import { circleWebSocket } from '@/api/circleWebSocket'
import {Message} from "@/types/message";


// 添加 emit 声明
const emit = defineEmits(['show-members', 'show-pending-applications'])

const appContext = useGlobalProperties()
const store = useCircleChatStore()
const messageListRef = ref<HTMLElement | null>(null)
const isUserScrolling = ref(false)
const isAtBottom = ref(true)
const instance = getCurrentInstance()
const message = useMessage()
const dialog = useDialog()
let unsubscribeCircleMessage: (() => void) | null = null
const unsubscribeCircleDelete: (() => void) | null = null

// 在线人数统计
const onlineCount = computed(() => {
  return Array.from(store.onlineMembers.values()).filter(v => v).length
})

// 获取当前用户 ID
const currentUserId = computed(() => {
  const useridStr = localStorage.getItem('userid')
  if (useridStr) {
    return parseInt(useridStr)
  }
  return null
})

// 判断是否是自己发送的消息（前端判断）
const isOwnMessage = (msg: CircleMessage) => {
  return msg.isSelf === true || msg.senderId === currentUserId.value
}

// 判断是否是圈主或管理员
const isOwnerOrAdmin = computed(() => {
  if (!store.currentCircle || !currentUserId.value) {return false}

  // 从成员列表中查找当前用户的角色
  const currentMember = store.members.find(m => m.userId === currentUserId.value)
  return currentMember?.role === 'OWNER' || currentMember?.role === 'ADMIN'
})

// 菜单选项（动态生成）
const menuOptions = computed(() => {
  const options = [
    {
      label: '查看成员',
      key: 'view-members',
      icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:user-group-line' }) })
    }
  ]

  // 只有圈主和管理员才能看到待审核申请
  if (isOwnerOrAdmin.value) {
    options.push({
      label: '待审核申请',
      key: 'pending-applications',
      icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:file-list-line' }) })
    })
  }

  options.push(
    {
      label: '圈子设置',
      key: 'circle-settings',
      icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:settings-3-line' }) })
    },
    {
      label: '退出圈子',
      key: 'leave-circle',
      icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:logout-box-line' }) })
    }
  )

  return options
})

const handleMenuClick = async (key: string) => {
  if (key === 'view-members') {
    emit('show-members')
  } else if (key === 'pending-applications') {
    emit('show-pending-applications')
  } else if (key === 'circle-settings') {
    // TODO: 打开圈子设置
    message.info('圈子设置功能开发中...')
  } else if (key === 'leave-circle') {
    // 退出圈子
    if (!store.currentCircle) return

    dialog.warning({
      title: '确认退出',
      content: `确定要退出「${store.currentCircle.name}」吗？`,
      positiveText: '确定退出',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          await circleMemberApi.leaveCircle(store.currentCircle!.id)
          message.success('已退出圈子')

          // 立即刷新页面
          window.location.reload()
        } catch (error: any) {
          console.error('[圈子聊天] 退出圈子失败:', error)
          message.error(error.response?.data?.msg || '退出失败')
        }
      }
    })
  }
}

// 类型转换：CircleMessage -> Message (适配 MessageBubble 组件)
const convertToMessage = (circleMsg: CircleMessage): Message => {

  const isSelf = circleMsg.senderId === currentUserId.value

  return {
    id: circleMsg.id,
    fromUserId: circleMsg.senderId,
    toUserId: circleMsg.circleId,  // 借用 toUserId 存储 circleId
    content: circleMsg.content,
    msgType: circleMsg.msgType,
    status: circleMsg.status,
    createTime: circleMsg.createTime,
    isSelf: isSelf,  // 前端计算，不依赖后端
    isRecalled: circleMsg.isRecalled,
    fromUser: circleMsg.sender,
    _isSystemTip: circleMsg._isSystemTip,
    _tipType: circleMsg._tipType,
    _tipUsername: circleMsg._tipUsername,
    _sending: circleMsg._sending,
    _tempId: circleMsg._tempId
  } as any
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    const container = messageListRef.value
    container.scrollTop = container.scrollHeight
  }
}

// 检测用户是否在底部
const checkIsAtBottom = () => {
  const messageListEl = messageListRef.value
  if (!messageListEl) {
    return false
  }

  const threshold = 50
  const scrollTop = messageListEl.scrollTop
  const scrollHeight = messageListEl.scrollHeight
  const clientHeight = messageListEl.clientHeight
  const distanceToBottom = scrollHeight - scrollTop - clientHeight

  return distanceToBottom <= threshold
}

// 处理滚动事件
const handleScroll = async (e: Event) => {
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

  // 滚动到顶部时加载更多历史消息
  if (target.scrollTop === 0 && store.hasMore && !store.loading) {
    await loadMoreMessages()
  }
}

/**
 * 加载更多历史消息
 */
const loadMoreMessages = async () => {
  if (!store.currentCircle || store.loading) {return}

  try {
    store.loading = true

    // 记录当前滚动高度
    const container = messageListRef.value
    const oldScrollHeight = container?.scrollHeight || 0

    // 调用 API 加载下一页
    const result = await circleChatApi.getChatHistory(store.currentCircle.id, {
      page: store.messagePage,
      limit: 20
    })

    // 将新消息添加到前面（保持时间顺序）
    if (result.list.length > 0) {
      // reverse 让旧消息在前面，新消息在后面
      const reversedMessages = result.list.reverse()

      // 使用 store.setMessages 追加到前面
      store.setMessages(reversedMessages, false)

      // 处理撤回和删除消息
      store.processRecalledMessages(store.messages)
      store.processDeletedMessages(store.messages)

      // 保持滚动位置（避免跳动）
      await nextTick()
      if (container) {
        const newScrollHeight = container.scrollHeight
        const heightDiff = newScrollHeight - oldScrollHeight
        container.scrollTop = heightDiff
      }
    }

  } catch (error: any) {
    console.error('[圈子聊天] 加载更多消息失败:', error)
    message.error('加载失败')
  } finally {
    store.loading = false
  }
}

/**
 * 发送消息（完整实现）
 */
const handleSendMessage = async (content: any, msgType = 0) => {
  if (!store.currentCircle) {return}
  
  try {
    // content 是 TipTap JSON 对象（从 ChatInput 直接传来）

    // 1. 乐观添加：立即显示在界面上，标记为"发送中"
    // store 需要存储字符串形式，所以如果 content 是对象则 stringify
    const contentString = typeof content === 'string' ? content : JSON.stringify(content)
    const tempMessage = store.addSendingMessage(contentString, store.currentCircle.id)
    await nextTick()
    scrollToBottom()

    // 2. 构建消息对象（content 保持为对象）
    const chatMessage = {
      circleId: store.currentCircle.id,
      content: content,  // TipTap JSON 对象（不要 stringify）
      msgType: 0,  // 固定为 0，后端会根据 JSON 内容判断
      extra: {}
    }

    // 3. 通过 WebSocket 发送到后端
    // 使用 circleWebSocket 发送消息到 /app/circle-message
    circleWebSocket.sendCircleMessage(store.currentCircle.id, chatMessage)

    // 4. 等待后端推送确认（监听 CIRCLE_CHAT_MESSAGE 事件）
    // store.confirmSentMessage() 会在收到推送时自动调用

  } catch (error: any) {
    console.error('发送消息失败:', error)
    message.error('消息发送失败')
  }
}

// 撤回消息
const handleRecallMessage = (messageId: number) => {
  if (!store.currentCircle) {return}

  // 乐观更新 UI
  store.recallMessageOptimistic(messageId)

  // 调用 WebSocket 撤回接口
  circleWebSocket.recallMessage(messageId, '用户主动撤回')
}

// 删除消息
const handleDeleteMessage = async (messageId: number) => {
  if (!store.currentCircle) {return}

  try {
    // TODO: 调用 HTTP 删除接口
    // 等后端接口完成后实现
    // await circleChatApi.deleteMessage(store.currentCircle.id, messageId)

    // 临时方案：通过 WebSocket 发送删除请求
    const ws = getWebSocket()
    if (ws && ws.isConnected()) {
      const client = (ws as any).client
      if (client && client.connected) {
        const deleteRequest = {
          messageId: messageId,
          circleId: store.currentCircle.id
        }

        client.publish({
          destination: '/app/circle-delete-message',
          body: JSON.stringify(deleteRequest)
        })

      }
    }
  } catch (error: any) {
    console.error('删除消息失败:', error)
    message.error('删除消息失败')
  }
}

// 复制消息
const handleCopyMessage = (content: string) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(content)
  }
}

// 监听消息长度变化，自动滚动
watch(() => store.messages.length, async (newLen: number, oldLen: number) => {
  const addedCount = newLen - oldLen

  if (addedCount > 0) {
    await nextTick()

    // 只有在底部时才滚动到最新
    if (isAtBottom.value && !isUserScrolling.value) {
      scrollToBottom()
    }
  }
}, { immediate: true })

// 监听当前圈子变化
watch(() => store.currentCircle, async (newCircle : Circle | null) => {
  if (newCircle) {
    store.saveCurrentCircleToStorage()

    nextTick(() => {
      scrollToBottom()


      if (newCircle.id) {
        // 1. 调用后端 API 标记为已读（更新 last_read_time）
        store.markAsReadByAPI(newCircle.id)

        // 2. 发送 WebSocket 已读回执（通知其他成员）
        store.markCircleAsRead(newCircle.id)
      }
    })
  }
}, { deep: true, immediate: true })

// 清理 WebSocket 处理器
const cleanupWebSocketHandlers = () => {
  if (unsubscribeCircleMessage) {
    unsubscribeCircleMessage()
    unsubscribeCircleMessage = null
  }
}

// 注册 WebSocket 消息处理器
const registerWebSocketHandlers = () => {
  const ws = getWebSocket()
  if (!ws) {
    console.warn(' [圈子聊天] WebSocket 未初始化，无法注册处理器')
    return
  }

  // 注册圈子消息处理器（统一处理 SEND、RECALL、DELETE）
  unsubscribeCircleMessage = ws.on('CIRCLE_CHAT_MESSAGE', (data: any) => {

    // 1. 删除消息处理
    if (data.action === 'DELETE' || data.deletedByAdmin === true) {

      const deleterNickname = data.deleter?.nickname || '管理员'
      store.handleDeleteMessageNotification(
        data.id,
        data.deleter?.id,
        deleterNickname
      )
      return
    }

    // 2. 撤回消息处理
    if (data.action === 'RECALL') {
      // 撤回消息
      store.recallMessageOptimistic(data.id, data.reason)
      return
    }

    // 3. 普通消息处理 - 前端判断是否是自己发送的
    const isSelf = data.senderId === currentUserId.value

    if (isSelf) {
      // 自己发送的消息，后端推送回来确认
      // 确保消息对象包含正确的 isSelf 字段
      const messageWithSelfFlag = {
        ...data,
        isSelf: true
      }
      store.confirmSentMessage(messageWithSelfFlag)

    } else {
      // 其他成员发送的消息
      const messageWithSelfFlag = {
        ...data,
        isSelf: false
      }
      store.receiveMessage(messageWithSelfFlag)


      if (store.currentCircle && data.circleId === store.currentCircle.id) {
        // 延迟一下，确保消息已经添加到列表
        setTimeout(() => {
          store.markAsReadByAPI(data.circleId)
          store.markCircleAsRead(data.circleId)
        }, 100)
      }
    }
  })

}

onMounted(() => {
  // 恢复上次浏览的圈子
  const saved = store.restoreCircleFromStorage()
  if (saved) {

  }

  // 注册 WebSocket 监听器
  registerWebSocketHandlers()
})

onUnmounted(() => {
  store.clearSavedCircle()
  cleanupWebSocketHandlers()
})
</script>

<style scoped lang="scss">
.circle-chat-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.chat-header {
  height: 64px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;

  .chat-header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .chat-info {
      .chat-name {
        font-size: 16px;
        font-weight: 600;
        color: #333;
      }

      .chat-description {
        font-size: 12px;
        color: #999;
        margin-top: 2px;

        .online-count {
          color: #18a058;
        }
      }
    }
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;

  .load-more-trigger {
    text-align: center;
    padding: 12px;
  }

  .empty-messages {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;

    .empty-state {
      text-align: center;

      .empty-message-title {
        margin-top: 24px;
        font-size: 16px;
        color: #666;
      }

      .empty-message-description {
        margin-top: 8px;
        font-size: 13px;
        color: #999;
      }
    }
  }

  .messages-wrapper {
    display: flex;
    flex-direction: column;
  }
}
</style>
