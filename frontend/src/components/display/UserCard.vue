<!-- src/components/discover/UserListItem.vue -->
<template>
  <div
    class="user-card"
    :class="{ 'self-card': user.isSelf }"
    @click="handleUserClick"
  >
    <!-- 自身标识 -->
    <div
      v-if="user.isSelf"
      class="self-badge"
    >
      <n-tag
        type="success"
        size="small"
        round
      >
        <template #icon>
          <n-icon size="12">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="12"
              height="12"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5l1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
              />
            </svg>
          </n-icon>
        </template>
        我
      </n-tag>
    </div>

    <div class="user-avatar-wrapper">
      <n-avatar
        v-if="user.avatar"
        :src="getAvatarUrl(user.avatar)"
        round
        size="large"
      />
      <n-avatar
        v-else
        round
        size="large"
      >
        {{ getInitials(user.nickname) }}
      </n-avatar>
      
      <!-- 在线状态 -->
      <span
        v-if="user.isOnline"
        class="online-indicator"
      />
    </div>
    
    <div class="user-info">
      <div class="user-name">
        {{ user.nickname }}
      </div>
      <div class="user-signature">
        <n-ellipsis :tooltip="false">
          {{ user.signature || '这个人很懒，什么都没写' }}
        </n-ellipsis>
      </div>
      <div class="user-stats">
        <span class="stat-item">
          <n-icon size="14">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="14"
              height="14"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3s-3-1.34-3-3s1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22c.03-1.99 4-3.08 6-3.08c1.99 0 5.97 1.09 6 3.08C16.71 18.92 14.5 20.2 12 20.2z"
              />
            </svg>
          </n-icon>
          {{ user.followerCount || 0 }}
        </span>
        <span class="stat-item">
          <n-icon size="14">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="14"
              height="14"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05c1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"
              />
            </svg>
          </n-icon>
          {{ user.followingCount || 0 }}
        </span>
        <span class="stat-item">
          <n-icon size="14">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="14"
              height="14"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"
              />
            </svg>
          </n-icon>
          {{ user.articleCount || 0 }}
        </span>
      </div>
    </div>
    
    <div class="user-actions">
      <div class="action-buttons">
        <n-button
          v-if="!user.isSelf"
          :type="user.isFollowing ? 'default' : 'primary'"
          size="small"
          round
          @click.stop="handleFollow"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </n-button>

        <n-button
          v-if="!user.isSelf"
          size="small"
          round
          @click.stop="handleMessage"
        >
          发消息
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NAvatar, NEllipsis, NIcon, NButton, NTag } from 'naive-ui'
import { getAvatarUrl } from '@/utils/userUtils'
import type { UserInfo } from '@/types/discover'

interface Props {
  user: UserInfo
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', user: UserInfo): void
  (e: 'follow', user: UserInfo): void
  (e: 'message', user: UserInfo): void
}>()

/**
 * 获取昵称首字母
 */
const getInitials = (nickname: string) => {
  if (!nickname) {return ''}
  return nickname.charAt(0).toUpperCase()
}

/**
 * 处理点击
 */
const handleUserClick = () => {
  emit('click', props.user)
}

/**
 * 处理关注
 */
const handleFollow = () => {
  emit('follow', props.user)
}

/**
 * 处理发消息
 */
const handleMessage = () => {
  emit('message', props.user)
}
</script>

<style scoped lang="scss">
.user-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;

  &.self-card {
    border: 2px solid #18a058;
    background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%);
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
  }

  .self-badge {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 1;
  }

  .user-avatar-wrapper {
    position: relative;
    display: inline-block;
    margin-bottom: 16px;

    .online-indicator {
      position: absolute;
      bottom: 4px;
      right: 4px;
      width: 12px;
      height: 12px;
      background: #18a058;
      border: 2px solid #fff;
      border-radius: 50%;
    }
  }

  .user-info {
    flex: 1;
    width: 100%;
    margin-bottom: 16px;
    text-align: center;

    .user-name {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
    }

    .user-signature {
      font-size: 13px;
      color: #999;
      margin-bottom: 12px;
      line-height: 1.6;
      min-height: 40px;
    }

    .user-stats {
      display: flex;
      justify-content: center;
      gap: 16px;
      font-size: 13px;
      color: #666;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .user-actions {
    width: 100%;
    text-align: center;

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 8px;

      .n-button {
        flex: 0 0 auto;
      }
    }
  }
}
</style>
