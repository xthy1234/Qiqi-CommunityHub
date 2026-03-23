<!-- src/components/discover/UserCard.vue -->
<template>
  <div
    class="user-card"
    @click="handleUserClick"
  >
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
                d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
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
          :type="user.isFollowing ? 'default' : 'primary'"
          size="small"
          round
          @click.stop="handleFollow"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </n-button>

        <n-button
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
import { NAvatar, NEllipsis, NIcon, NButton } from 'naive-ui'
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

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
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
