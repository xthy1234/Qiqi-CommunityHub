<template>
  <div class="user-card">
    <div class="user-info">
      <div class="avatar-wrapper">
        <n-avatar
          :src="getAvatarUrl(user.avatar)"
          :size="60"
          round
          class="avatar"
        />
        <span
          v-if="isOnline"
          class="online-dot"
        />
      </div>
      
      <div class="user-details">
        <div class="user-header">
          <span
            class="nickname"
            @click="goToUserProfile"
          >{{ user.username || user.nickname }}</span>
          <n-tag
            v-if="isFriend"
            type="success"
            size="small"
            class="friend-tag"
          >
            互相关注
          </n-tag>
        </div>
        
        <p class="signature">
          {{ user.signature || '暂无签名' }}
        </p>
        
        <div class="actions">
          <n-button
            v-if="showFollowBtn"
            :type="isFollowing ? 'default' : 'primary'"
            size="small"
            :loading="actionLoading"
            @click="handleFollowToggle"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </n-button>
          
          <n-button
            v-if="showSendMessageBtn"
            type="primary"
            size="small"
            secondary
            @click="handleSendMessage"
          >
            发消息
          </n-button>
        </div>
      </div>
    </div>
    
    <div class="follow-time">
      <Icon
        icon="ri:time-line"
        :size="14"
      />
      <span>{{ formatFollowTime(user.followTime) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NAvatar, NButton, NTag } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { getAvatarUrl } from '@/utils/userUtils'
import followApi from '@/api/follow'
import { getWebSocket } from '@/utils/websocket'
import type { WsUserOnlineStatus } from '@/types/message'

interface UserCardProps {
  user: {
    id?: number
    userId?: number
    username?: string
    nickname?: string
    avatar?: string
    signature?: string
    gender?: number
    followTime: string
  }
  // 新增：场景类型 - 'following'(关注列表) | 'followers'(粉丝列表) | 'search'(搜索结果)
  sceneType?: 'following' | 'followers' | 'search'
  currentUserId?: number
}

const props = withDefaults(defineProps<UserCardProps>(), {
  sceneType: 'following' // 默认为关注列表场景
})

const router = useRouter()
const appContext = computed(() => (window as any).appContext)
const $http = appContext.value?.$http

const isFollowing = ref(false)
const isFriend = ref(false)
const actionLoading = ref(false)
const isOnline = ref(false)
let unsubscribeOnlineStatus: (() => void) | null = null

const isCurrentUser = computed(() => {  // 优先使用 userId，如果没有则使用 id
  const actualUserId = props.user.userId || props.user.id
  return props.currentUserId === actualUserId
})

// 是否显示关注按钮 - 仅在搜索场景或特定场景显示
const showFollowBtn = computed(() => {
  // 如果是自己，不显示
  if (isCurrentUser.value) {return false}

  // 根据场景决定是否显示关注按钮
  switch (props.sceneType) {
    case 'search':
      // 搜索结果：显示关注按钮
      return true
    case 'following':
    case 'followers':
      // 关注列表/粉丝列表：不显示关注按钮（因为已经关注了）
      return false
    default:
      return false
  }
})

// 是否显示发消息按钮 - 在关注列表和粉丝列表中显示
const showSendMessageBtn = computed(() => {
  // 如果是自己，不显示

  return  !isCurrentUser.value

  // 在关注列表或粉丝列表中显示
  // return props.sceneType === 'following' || props.sceneType === 'followers'
})

// 获取用户真实 ID 的辅助函数
const getUserActualId = (): number => {
  return props.user.userId || props.user.id || 0
}

const handleSendMessage = () => {
  // 直接跳转到聊天页面，后端会在发送第一条消息时自动创建会话
  router.push(`/chat/${getUserActualId()}`)
}

const goToUserProfile = () => {
  router.push(`/user/${getUserActualId()}`)
}

// 初始化时查询关注状态
onMounted(async () => {
  if (props.showFollowBtn && !isCurrentUser.value) {
    try {
      const status = await followApi.getFollowStatus([getUserActualId()])
      isFollowing.value = status[getUserActualId()] || false
      
      if (isFollowing.value) {
        isFriend.value = await followApi.isFriend(getUserActualId())
      }
    } catch (error) {
      console.error('查询关注状态失败:', error)
    }
  }

  // 订阅在线状态
  setupOnlineStatusListener()
})

onUnmounted(() => {
  unsubscribeOnlineStatus?.()
})

// 订阅用户在线状态
const setupOnlineStatusListener = () => {
  const userId = getUserActualId()
  if (!userId) {return}

  const ws = getWebSocket()
  if (!ws || !ws.isConnected()) {
    console.warn('⚠️ [UserCard] WebSocket 未连接')
    return
  }

  const handler = (data: WsUserOnlineStatus['data']) => {
    if (data.userId === userId) {
      isOnline.value = data.online

    }
  }

  unsubscribeOnlineStatus = ws.on('USER_ONLINE_STATUS', handler)

  // 主动查询
  ws.queryUserOnlineStatus([userId])
}

const handleFollowToggle = async () => {
  if (actionLoading.value) {return}
  
  actionLoading.value = true
  try {
    await followApi.followOrUnfollow({
      userId: getUserActualId(),
      action: isFollowing.value ? 'unfollow' : 'follow'
    })
    
    isFollowing.value = !isFollowing.value
    
    // 如果刚关注，检查是否互关
    if (isFollowing.value) {
      isFriend.value = await followApi.isFriend(getUserActualId())
    }
    
    appContext.value?.$toolUtil.message(
      isFollowing.value ? '关注成功' : '已取消关注',
      'success'
    )
    
    // 触发事件通知父组件
    emit('follow-change', { userId: getUserActualId(), isFollowing: isFollowing.value })
  } catch (error: any) {
    console.error('关注操作失败:', error)
    appContext.value?.$toolUtil.message(error.message || '操作失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const formatFollowTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`
  } else if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  } else if (diff < 7 * day) {
    return `${Math.floor(diff / day)}天前`
  } else {
    return date.toLocaleDateString()
  }
}

const emit = defineEmits<{
  (e: 'follow-change', data: { userId: number; isFollowing: boolean }): void
}>()
</script>

<style lang="scss" scoped>
.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #52c41a;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(82, 196, 26, 0.3);
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  
  .user-info {
    display: flex;
    gap: 16px;
    flex: 1;
    
    .avatar {
      cursor: pointer;
      transition: transform 0.3s;
      
      &:hover {
        transform: scale(1.1);
      }
    }
    
    .user-details {
      flex: 1;
      min-width: 0;
      
      .user-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;
        
        .nickname {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          cursor: pointer;
          
          &:hover {
            color: #18a058;
          }
        }
        
        .friend-tag {
          font-size: 12px;
        }
      }
      
      .signature {
        font-size: 13px;
        color: #909399;
        margin: 0 0 8px 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .actions {
        display: flex;
        gap: 8px;
      }
    }
  }
  
  .follow-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #c0c4cc;
    flex-shrink: 0;
    margin-left: 16px;
  }
}
</style>
