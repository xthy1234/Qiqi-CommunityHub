NEW_FILE_CODE
<template>
  <n-avatar
    :src="avatarUrl"
    :size="size"
    :round="round"
    :class="['chat-avatar', { 'clickable': clickable }]"
    @click="handleClick"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NAvatar } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'

interface Props {
  userId?: number
  avatar?: string
  size?: number | 'small' | 'medium' | 'large'
  round?: boolean
  clickable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  userId: undefined,
  avatar: '',
  size: 'small',
  round: true,
  clickable: false
})

const appContext = useGlobalProperties()

const emit = defineEmits<{
  click: [userId: number]
}>()

const avatarUrl = computed(() => {
  if (props.avatar) {

    const baseUrl = appContext?.$config?.url || 'http://localhost:8080'

    return props.avatar.startsWith('http') ? props.avatar : `${baseUrl}/${props.avatar}`
  }

  return ''
})

const handleClick = () => {
  if (props.clickable && props.userId) {
    emit('click', props.userId)
  }
}
</script>

<style scoped lang="scss">
.chat-avatar {
  flex-shrink: 0;
  cursor: default;
  transition: transform 0.2s;
  
  &.clickable {
    cursor: pointer;
    
    &:hover {
      transform: scale(1.1);
    }
  }
}
</style>
