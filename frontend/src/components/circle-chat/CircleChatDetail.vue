<!-- src/components/circle-chat/CircleChatDetail.vue -->
<template>
  <div class="circle-chat-detail">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <div class="chat-header-left">
        <n-avatar :src="store.currentCircle?.avatar" round size="medium" />
        <div class="chat-info">
          <div class="chat-name">{{ store.currentCircle?.name }}</div>
          <div class="chat-description">
            {{ store.currentCircle?.memberCount }} 人 · 
            <span v-if="onlineCount > 0" class="online-count">
              {{ onlineCount }} 人在线
            </span>
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
        <!-- 加载更多触发器 -->
        <div v-if="store.hasMore && store.messages.length > 0" class="load-more-trigger">
          <n-text depth="3">加载中...</n-text>
        </div>
        
        <!-- 空状态 -->
        <div v-if="store.messages.length === 0" class="empty-messages">
          <div class="empty-state">
            <n-icon size="80" color="#dcdfe6">
              <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24">
                <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
              </svg>
            </n-icon>
            <div class="empty-message-title">暂无聊天记录</div>
            <div class="empty-message-description">
              发送第一条消息，开启你们的讨论吧~
            </div>
          </div>
        </div>
        
        <!-- 消息列表 -->
        <div v-else class="messages-wrapper">
          <template v-for="msg in store.messages" :key="msg.id || msg._tempId">
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
import type { CircleMessage, Message } from '@/types/message'
import MessageBubble from '../chat/MessageBubble.vue'
import ChatInput from '../chat/ChatInput.vue'
import SystemMessageTip from '../chat/SystemMessageTip.vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NIcon, NDropdown, NAvatar, NSpin, NText } from 'naive-ui'
import { Icon } from '@iconify/vue'

const appContext = useGlobalProperties()
const store = useCircleChatStore()
const messageListRef = ref<HTMLElement | null>(null)

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

// 判断是否是自己发送的消息
const isOwnMessage = (msg: CircleMessage) => {
  return msg.isSelf === true || msg.senderId === currentUserId.value
}

// 菜单选项
const menuOptions = [
  {
    label: '查看成员',
    key: 'view-members',
    icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'ri:user-group-line' }) })
  },
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
]

const handleMenuClick = async (key: string) => {
  if (key === 'view-members') {
    // TODO: 打开成员列表侧边栏
    console.log('查看成员')
  } else if (key === 'circle-settings') {
    // TODO: 打开圈子设置
    console.log('圈子设置')
  } else if (key === 'leave-circle') {
    // TODO: 退出圈子
    console.log('退出圈子')
  }
}

// 类型转换：CircleMessage -> Message (适配 MessageBubble 组件)
const convertToMessage = (circleMsg: CircleMessage): Message => {
  return {
    id: circleMsg.id,
    fromUserId: circleMsg.senderId,
    toUserId: circleMsg.circleId,  // 借用 toUserId 存储 circleId
    content: circleMsg.content,
    msgType: circleMsg.msgType,
    status: circleMsg.status,
    createTime: circleMsg.createTime,
    isSelf: circleMsg.isSelf,
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

// 处理滚动事件
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  if (target.scrollTop === 0 && store.hasMore) {
    // TODO: 加载更多历史消息
    console.log('加载更多消息')
  }
}

// 发送消息
const handleSendMessage = async (content: string) => {
  if (!store.currentCircle) return
  
  try {
    // 1. 乐观添加：立即显示在界面上，标记为"发送中"
    const tempMessage = store.addSendingMessage(content, store.currentCircle.id)
    await nextTick()
    scrollToBottom()

    // 2. 通过 WebSocket 发送
    // TODO: 实现 WebSocket 发送逻辑
    console.log('📤 发送消息:', content, 'to circle:', store.currentCircle.id)
    
    // 模拟发送成功（实际应该等 WebSocket 确认）
    setTimeout(() => {
      store.confirmSentMessage({
        ...tempMessage,
        id: Date.now(),
        _sending: false,
        createTime: new Date().toISOString()
      } as CircleMessage)
    }, 300)

  } catch (error: any) {
    console.error('发送消息失败:', error)
  }
}

// 撤回消息
const handleRecallMessage = (messageId: number) => {
  // TODO: 调用撤回接口
  console.log('撤回消息:', messageId)
  store.recallMessageOptimistic(messageId)
}

// 删除消息
const handleDeleteMessage = (messageId: number) => {
  // TODO: 调用删除接口
  console.log('删除消息:', messageId)
}

// 复制消息
const handleCopyMessage = (content: string) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(content)
  }
}

// 监听当前圈子变化
watch(() => store.currentCircle, (newCircle) => {
  if (newCircle) {
    store.saveCurrentCircleToStorage()
    nextTick(() => {
      scrollToBottom()
    })
  }
}, { deep: true })

onMounted(() => {
  // 恢复上次浏览的圈子
  const saved = store.restoreCircleFromStorage()
  if (saved) {
    console.log('恢复上次的圈子:', saved)
  }
})

onUnmounted(() => {
  store.clearSavedCircle()
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
