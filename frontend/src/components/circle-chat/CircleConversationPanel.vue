<!-- src/components/circle-chat/CircleConversationPanel.vue -->
<template>
  <CollapsibleAvatarList
    title="圈子"
    :default-collapsed="false"
    @collapse-change="handleCollapseChange"
  >
    <!-- 头部操作区域 -->
    <template #header-actions>
      <n-badge
        :value="store.totalUnreadCount"
        :show="store.totalUnreadCount > 0"
        :max="99"
      >
        <n-button
          text
          size="small"
          @click="handleRefresh"
        >
          <template #icon>
            <n-icon size="18">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="18"
                height="18"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"
                />
              </svg>
            </n-icon>
          </template>
        </n-button>
      </n-badge>
    </template>
    
    <!-- 展开模式：完整会话列表 -->
    <template #list-content>
      <n-spin :show="loading">
        <div
          v-if="conversations.length === 0"
          class="empty-conversations"
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
                  d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 9h-2V7h-2v5H6v2h2v5h2v-5h2v-2z"
                />
              </svg>
            </n-icon>
            <div class="empty-title">
              暂时还没有加入任何圈子
            </div>
            <div class="empty-description">
              去 <n-button
                text
                type="primary"
                @click.stop="handleGoToDiscover"
              >
                发现
              </n-button>
              加入感兴趣的圈子吧
            </div>
          </div>
        </div>
        
        <n-list
          v-else
          hoverable
          clickable
        >
          <n-list-item
            v-for="conv in conversations"
            :key="conv.circleId"
            :class="{ 'active': activeCircleId === conv.circleId }"
            @click="handleSelectConversation(conv)"
          >
            <template #prefix>
              <n-badge
                :value="conv.unreadCount"
                :show="conv.unreadCount > 0"
                :dot-style="{ background: '#18a058' }"
              >
                <n-avatar
                  :src="conv.circleAvatar"
                  round
                  size="large"
                />
              </n-badge>
            </template>
            
            <div class="conversation-item-content">
              <div class="conversation-top">
                <span class="conversation-name">{{ conv.circleName }}</span>
                <span class="conversation-time">{{ formatTime(conv.lastMessageTime) }}</span>
              </div>
              <div class="conversation-bottom">
                <n-ellipsis
                  class="conversation-message"
                  :tooltip="false"
                >
                  <span
                    v-if="conv.lastMessageSenderNickname"
                    class="sender-name"
                  >
                    {{ conv.lastMessageSenderNickname }}:
                  </span>
                  {{ conv.lastMessageContent || '暂无消息' }}
                </n-ellipsis>
              </div>
            </div>
          </n-list-item>
        </n-list>
      </n-spin>
    </template>

    <!-- 折叠模式：只显示头像 -->
    <template #collapsed-content>
      <n-spin :show="loading">
        <div
          v-for="conv in conversations"
          :key="conv.circleId"
          class="collapsed-avatar-item"
          :class="{ 'active': activeCircleId === conv.circleId }"
          @click="handleSelectConversation(conv)"
        >
          <n-badge
            :value="conv.unreadCount"
            :show="conv.unreadCount > 0"
            :dot-style="{ background: '#18a058' }"
          >
            <n-avatar
              :src="conv.circleAvatar"
              round
              size="large"
              style="cursor: pointer;"
            />
          </n-badge>
        </div>
      </n-spin>
    </template>
  </CollapsibleAvatarList>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NBadge, NButton, NIcon, NSpin, NList, NListItem, NAvatar, NEllipsis } from 'naive-ui'
import type { CircleConversation } from '@/types/circleChat'
import dayjs from 'dayjs'
import CollapsibleAvatarList from '../chat/CollapsibleAvatarList.vue'
import { useCircleChatStore } from '@/stores/circleChat'

interface Props {
  conversations: CircleConversation[]
  loading: boolean
  activeCircleId?: number
}

const props = withDefaults(defineProps<Props>(), {
  activeCircleId: 0
})

const emit = defineEmits<{
  (e: 'select-circle', conversation: CircleConversation): void
  (e: 'refresh'): void
}>()

const router = useRouter()
const store = useCircleChatStore()

const handleCollapseChange = (collapsed: boolean) => {
  // 处理折叠状态变化
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

const handleSelectConversation = (conv: CircleConversation) => {
  emit('select-circle', conv)
}

const handleRefresh = () => {
  emit('refresh')
}

const handleGoToDiscover = () => {
  router.push('/index/circle/discover')
}
</script>

<style scoped lang="scss">
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
  
  .sender-name {
    color: #666;
    margin-right: 4px;
  }
}

.active {
  background-color: #f0f8ff !important;
}

.collapsed-avatar-item {
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
  padding: 8px;
  border-radius: 50%;

  &:hover {
    background-color: #f5f5f5;
    transform: scale(1.1);
  }

  &.active {
    .n-avatar {
      box-shadow: 0 0 0 2px #18a058;
    }
  }
}
</style>
