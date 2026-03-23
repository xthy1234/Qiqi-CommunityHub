<template>
  <div class="user-avatar-dropdown">
    <n-dropdown
      :options="dropdownOptions"
      placement="bottom-end"
      @select="handleSelect"
    >
      <div class="avatar-wrapper">
        <n-avatar
          v-if="fullAvatarUrl"
          :src="fullAvatarUrl"
          :size="40"
          round
        />
        <n-avatar
          v-else
          :size="40"
          round
        >
          {{ userNickname?.charAt(0) || '用' }}
        </n-avatar>
        <Icon
          class="dropdown-icon"
          icon="ri:arrow-down-s-line"
        />
      </div>
    </n-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { getFullUrl } from '@/utils/userUtils'

const appContext = useGlobalProperties()
const router = useRouter()

const userAvatar = ref<string>('')
const userNickname = ref<string>('')
const fullAvatarUrl = ref<string>('')

const dropdownOptions = computed(() => [
  {
    label: '个人中心',
    key: 'center',
    icon: () => h(Icon, { icon: 'ri:user-line', width: '18' })
  },
  {
    type: 'divider',
    key: 'd1'
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(Icon, { icon: 'ri:logout-box-r-line', width: '18' }),
    props: {
      style: { color: '#ff6b6b' }
    }
  }
])

const handleSelect = (key: string) => {
  if (key === 'center') {
    router.push('/index/userCenter')
  } else if (key === 'logout') {
    handleLogout()
  }
}

const handleLogout = () => {
  appContext?.$toolUtil?.storageClear()
  router.push('/login')
}

const loadUserInfo = () => {
  const avatarPath = appContext?.$toolUtil?.storageGet('avatar') || ''
  const baseUrl = appContext?.$config?.url || 'http://localhost:8080'

  if (avatarPath) {
    fullAvatarUrl.value = getFullUrl(avatarPath, baseUrl)
  } else {
    fullAvatarUrl.value = ''
  }

  userNickname.value = appContext?.$toolUtil?.storageGet('nickname') || ''
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style lang="scss" scoped>
.user-avatar-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;

  .avatar-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #fff;
    padding: 4px;
    border-radius: 4px;
    transition: background 0.3s;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    .dropdown-icon {
      font-size: 16px;
      margin-left: 4px;
    }
  }
}
</style>
