<template>
  <div class="conversation-panel">
    <div class="conversation-header">
      <h3>消息</h3>
      <n-badge :value="unreadCount" :show="unreadCount > 0" :max="99">
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
      <n-spin :show="loading">
        <div v-if="conversations.length === 0" class="empty-conversations">
          <div class="empty-state">
            <n-icon size="80" color="#dcdfe6">
              <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24">
                <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
              </svg>
            </n-icon>
            <div class="empty-title">暂时还没有私信会话</div>
            <div class="empty-description">
              去 <n-button text type="primary" @click="handleGoToFollowing">关注列表</n-button>
              或 <n-button text type="primary" @click="handleGoToSearch">搜索</n-button> 找人聊天吧
            </div>
          </div>
        </div>
        
        <n-list v-else hoverable clickable>
          <n-list-item
            v-for="conv in conversations"
            :key="conv.userId"
            @click="handleSelectConversation(conv)"
            :class="{ 'active': activeUserId === conv.userId }"
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
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NBadge, NButton, NIcon, NSpin, NList, NListItem, NAvatar, NEllipsis } from 'naive-ui'
import type { ConversationVO } from '@/types/message'
import dayjs from "dayjs";

interface Props {
  conversations: ConversationVO[]
  loading: boolean
  unreadCount: number
  activeUserId?: number
}

const props = withDefaults(defineProps<Props>(), {
  activeUserId: 0
})

const emit = defineEmits<{
  (e: 'select-conversation', conversation: ConversationVO): void
}>()

const router = useRouter()

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

const handleSelectConversation = (conv: ConversationVO) => {
  emit('select-conversation', conv)
}

const handleGoToFollowing = () => {
  const userId = getCurrentUserId()
  if (userId) {
    router.push(`/user/${userId}/following`)
  } else {
    router.push('/index/articleList')
  }
}

const handleGoToSearch = () => {
  router.push('/index/articleList')
}

const getCurrentUserId = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    return user?.id
  }
  return null
}
</script>

<style scoped lang="scss">
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

      .n-button {
        margin-top: 24px;
        padding-left: 32px;
        padding-right: 32px;
      }
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
</style>
