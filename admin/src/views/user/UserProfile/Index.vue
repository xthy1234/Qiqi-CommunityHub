<template>
  <PageContainer
    header-title="个人信息"
    :show-back="false"
  >
    <div class="profile-content">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar-display">
          <n-avatar
            v-if="userInfo.avatar"
            :src="getAvatarUrl(userInfo.avatar)"
            class="avatar-image"
            :size="150"
            round
          />
          <n-avatar
            v-else
            class="avatar-placeholder"
            :size="150"
            round
          >
            {{ getAvatarInitials() }}
          </n-avatar>
        </div>
      </div>

      <!-- 信息列表 -->
      <div class="info-list">
        <div class="info-item">
          <span class="info-label">用户账号：</span>
          <span class="info-value">{{ userInfo.account || '-' }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">用户姓名：</span>
          <span class="info-value">{{ userInfo.nickname || '-' }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">性别：</span>
          <span class="info-value">{{ getGenderText(userInfo.gender) }}</span>
        </div>

        <div
          v-if="userInfo.phone"
          class="info-item"
        >
          <span class="info-label">手机号码：</span>
          <span class="info-value">{{ userInfo.phone }}</span>
        </div>

        <div
          v-if="userInfo.email"
          class="info-item"
        >
          <span class="info-label">邮箱：</span>
          <span class="info-value">{{ userInfo.email }}</span>
        </div>

        <div
          v-if="userInfo.birthday"
          class="info-item"
        >
          <span class="info-label">生日：</span>
          <span class="info-value">{{ userInfo.birthday }}</span>
        </div>

        <div
          v-if="userInfo.signature"
          class="info-item"
        >
          <span class="info-label">个性签名：</span>
          <span class="info-value signature-text">{{ userInfo.signature }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">注册时间：</span>
          <span class="info-value">{{ formatDateTime(userInfo.createTime) }}</span>
        </div>

        <div
          v-if="userInfo.lastLoginTime"
          class="info-item"
        >
          <span class="info-label">最后登录：</span>
          <span class="info-value">{{ formatDateTime(userInfo.lastLoginTime) }}</span>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGlobalProperties } from '@/utils/globalProperties'
import { useMessage } from 'naive-ui'
import { getAvatarUrl, getGenderText, formatDateTime } from '@/utils/userUtils'
import PageContainer from '@/components/layout/PageContainer.vue'
import { userApi } from '@/api/user'

interface UserInfo {
  id?: number
  account: string
  nickname?: string
  avatar?: string
  gender?: number
  phone?: string
  email?: string
  birthday?: string
  signature?: string
  createTime?: string
  lastLoginTime?: string
  lastLoginIp?: string
}

const router = useRouter()
const appContext = useGlobalProperties()
const message = useMessage()

const userInfo = ref<UserInfo>({
  account: '',
  nickname: '',
  avatar: '',
  gender: 0,
  phone: '',
  email: '',
  birthday: undefined,
  signature: ''
})

const fetchUserInfo = async (): Promise<void> => {
  try {
    const currentUserResponse = await userApi.getCurrentUser()

    let currentUser = null

    if (currentUserResponse.data?.data) {
      currentUser = currentUserResponse.data.data
    } else if (currentUserResponse.data) {
      currentUser = currentUserResponse.data
    }

    if (!currentUser?.id) {
      message.error('请先登录')
      router.push('/login')
      return
    }

    const response = await userApi.getUserById(currentUser.id)

    const userData = response.data?.data || response.data
    if (userData) {
      userInfo.value = {
        id: userData.id,
        account: userData.account || '-',
        nickname: userData.nickname || userData.username || '-',
        avatar: userData.avatar || '',
        gender: userData.gender ?? 0,
        phone: userData.phone || '',
        email: userData.email || '',
        birthday: userData.birthday || null,
        signature: userData.signature || '',
        createTime: userData.createTime,
        lastLoginTime: userData.lastLoginTime,
        lastLoginIp: userData.lastLoginIp
      }

      appContext?.$toolUtil.storageSet('userid', userData.id)
      appContext?.$toolUtil.storageSet('nickname', userData.account)
      appContext?.$toolUtil.storageSet('avatar', userData.avatar)
    }
  } catch (error: any) {
    console.error('[UserProfile] 获取用户信息失败:', error)

    if (error.response?.status === 401) {
      message.error('请先登录')
      router.push('/login')
    } else {
      message.error('获取用户信息失败')
    }
  }
}

const getAvatarInitials = (): string => {
  const name = userInfo.value.nickname || userInfo.value.account || '用户'
  return name.charAt(0).toUpperCase()
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.profile-content {
  .avatar-section {
    display: flex;
    justify-content: center;
    margin-bottom: 40px;

    .avatar-display {
      .avatar-image,
      .avatar-placeholder {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .avatar-placeholder {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        font-size: 60px;
        font-weight: bold;
        color: #fff;
      }
    }
  }

  .info-list {
    margin-bottom: 40px;

    .info-item {
      display: flex;
      padding: 16px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .info-label {
        width: 120px;
        font-weight: 500;
        color: #666;
        flex-shrink: 0;
      }

      .info-value {
        flex: 1;
        color: #333;

        &.signature-text {
          white-space: pre-wrap;
          word-break: break-all;
        }
      }
    }
  }
}
</style>
