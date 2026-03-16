<template>
  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <div class="conversation-panel">
      <div class="conversation-header">
        <h3>消息</h3>
        <n-badge :value="store.unreadCount" :show="store.unreadCount > 0" :max="99">
          <n-button text size="small">
            <template #icon>
              <n-icon size="18">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8s8 3.59 8 8s-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
                </svg>
              </n-icon>
            </template>
          </n-button>
        </n-badge>
      </div>
      
      <div class="conversation-list">
        <n-spin :show="store.loading">
          <div v-if="store.conversations.length === 0" class="empty-conversations">
            <div class="empty-state">
              <n-icon size="80" color="#dcdfe6">
                <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
                </svg>
              </n-icon>
              <div class="empty-title">暂无会话</div>
              <div class="empty-description">
                还没有开始任何聊天，<br />去发现页面找找感兴趣的内容吧~
              </div>
              <n-button type="primary" @click="goToDiscover">
                去发现
              </n-button>
            </div>
          </div>
          
          <n-list v-else hoverable clickable>
            <n-list-item
              v-for="conv in store.conversations"
              :key="conv.userId"
              @click="handleSelectConversation(conv)"
              :class="{ 'active': store.currentConversation?.userId === conv.userId }"
            >
              <template #prefix>
                <n-badge :value="conv.unreadCount" :show="conv.unreadCount > 0" :dot-style="{ background: '#18a058' }">
                  <n-avatar :src="conv.avatar" round size="large" />
                </n-badge>
              </template>
              
              <div class="conversation-item-content">
                <div class="conversation-top">
                  <span class="conversation-name">{{ conv.username }}</span>
                  <span class="conversation-time">{{ formatTime(conv.lastTime) }}</span>
                </div>
                <div class="conversation-bottom">
                  <n-ellipsis class="conversation-message" :tooltip="false">
                    {{ conv.lastMessage }}
                  </n-ellipsis>
                </div>
              </div>
            </n-list-item>
          </n-list>
        </n-spin>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-panel">
      <ChatDetail v-if="store.currentConversation" />
      <div v-else class="empty-chat">
        <div class="empty-state">
          <n-icon size="100" color="#dcdfe6">
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 24 24">
              <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
            </svg>
          </n-icon>
          <div class="empty-title-large">选择一个会话开始聊天</div>
          <div class="empty-description-large">
            左侧列表选择联系人，或等待新消息<br />
            与好友分享生活点滴，畅聊有趣话题
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import ChatDetail from './ChatDetail.vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { NBadge, NButton, NIcon, NSpin, NEmpty, NList, NListItem, NAvatar, NEllipsis } from 'naive-ui'
import { useRouter } from 'vue-router'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const store = useChatStore()
const router = useRouter()

onMounted(() => {
  store.loadConversations()
})

const handleSelectConversation = async (conv: any) => {
  await store.switchConversation(conv)
}

const goToDiscover = () => {
  router.push('/index/articleList')
}

const formatTime = (time: string) => {
  const now = dayjs()
  const target = dayjs(time)
  const diff = now.diff(target, 'day')
  
  if (diff === 0) {
    return target.format('HH:mm')
  } else if (diff === 1) {
    return '昨天'
  } else if (diff < 7) {
    return target.format('dddd')
  } else {
    return target.format('YYYY-MM-DD')
  }
}
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.conversation-panel {
  width: 360px;
  min-width: 360px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.conversation-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
    
    &:hover {
      background: #c0c4cc;
    }
  }
}

.empty-conversations {
  padding: 60px 20px;
  text-align: center;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;

    .empty-title {
      margin-top: 24px;
      font-size: 16px;
      font-weight: 600;
      color: #606266;
    }

    .empty-description {
      margin-top: 12px;
      font-size: 13px;
      color: #909399;
      line-height: 1.8;
      text-align: center;
    }

    .n-button {
      margin-top: 24px;
      padding-left: 32px;
      padding-right: 32px;
    }
  }
}

.conversation-item-content {
  flex: 1;
  min-width: 0;
}

.conversation-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conversation-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.conversation-time {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.conversation-bottom {
  display: flex;
  align-items: center;
}

.conversation-message {
  font-size: 13px;
  color: #999;
  max-width: 200px;
}

.active {
  background-color: #f0f8ff !important;
}

.chat-panel {
  flex: 1;
  min-width: 0;
  background: #f5f5f5;
  position: relative;
}

.empty-chat {
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

    .empty-title-large {
      margin-top: 24px;
      font-size: 18px;
      font-weight: 600;
      color: #606266;
    }

    .empty-description-large {
      margin-top: 16px;
      font-size: 14px;
      color: #909399;
      line-height: 1.8;
      max-width: 400px;
    }
  }
}
</style>
