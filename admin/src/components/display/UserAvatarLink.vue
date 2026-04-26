<template>
  <div class="user-avatar-link" @click.stop="handleClick">
    <n-avatar
      v-if="avatarUrl"
      :src="avatarUrl"
      :size="size"
      round
      class="user-avatar"
      fallback-src="/default-avatar.png"
    />
    <n-avatar
      v-else
      :size="size"
      round
      class="user-avatar-placeholder"
    >
      <span class="avatar-initial">{{ getInitial() }}</span>
    </n-avatar>
    
    <span v-if="showName && nickname" class="user-nickname">
      {{ nickname }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NAvatar } from 'naive-ui'
import { getAvatarUrl } from '@/utils/userUtils'

interface Props {
  userId: number | string
  nickname?: string
  avatar?: string
  size?: number | 'small' | 'medium' | 'large'
  showName?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  showName: false
})

const router = useRouter()

const avatarUrl = computed(() => {
  return props.avatar ? getAvatarUrl(props.avatar) : ''
})

const getInitial = () => {
  const name = props.nickname || '用户'
  return name.charAt(0).toUpperCase()
}

const handleClick = (event: MouseEvent) => {
  event.stopPropagation()
  if (props.userId) {
    router.push(`/user/${props.userId}`)
  }
}
</script>

<style lang="scss" scoped>
.user-avatar-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    opacity: 0.8;
    
    .user-nickname {
      color: #18a058;
    }
  }

  .user-avatar,
  .user-avatar-placeholder {
    flex-shrink: 0;
    object-fit: cover;
    border: 2px solid #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .user-avatar-placeholder {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;

    .avatar-initial {
      font-size: inherit;
      font-weight: bold;
      color: #fff;
    }
  }

  .user-nickname {
    font-size: 14px;
    color: #303133;
    transition: all 0.3s ease;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
