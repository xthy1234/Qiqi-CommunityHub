<!-- src/components/circle-chat/CircleInfoCard.vue -->
<template>
  <div class="circle-info-card">
    <div class="card-header">
      <n-avatar
        :src="circle?.avatar"
        round
        size="large"
      />
      <div class="card-title">
        {{ circle?.name }}
      </div>
    </div>

    <div class="card-content">
      <!-- 圈子描述 -->
      <div class="info-section">
        <div class="section-label">
          <n-icon size="18">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
              />
            </svg>
          </n-icon>
          <span>简介</span>
        </div>
        <div class="section-value">
          {{ circle?.description || '暂无简介' }}
        </div>
      </div>

      <!-- 圈子统计 -->
      <div class="info-section">
        <div class="section-label">
          <n-icon size="18">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"
              />
            </svg>
          </n-icon>
          <span>成员</span>
        </div>
        <div class="section-value">
          {{ circle?.memberCount }} 人
        </div>
      </div>

      <!-- 我的角色 -->
      <div class="info-section">
        <div class="section-label">
          <n-icon size="18">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12c5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"
              />
            </svg>
          </n-icon>
          <span>我的身份</span>
        </div>
        <div class="section-value">
          <n-tag
            v-if="circle?.myRole === 'OWNER'"
            type="error"
            size="medium"
            bordered
            round
          >
            圈主
          </n-tag>
          <n-tag
            v-else-if="circle?.myRole === 'ADMIN'"
            type="warning"
            size="medium"
            bordered
            round
          >
            管理员
          </n-tag>
          <n-tag
            v-else
            type="default"
            size="medium"
            bordered
            round
          >
            成员
          </n-tag>
        </div>
      </div>

      <!-- 创建时间 -->
      <div class="info-section">
        <div class="section-label">
          <n-icon size="18">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
            >
              <path
                fill="currentColor"
                d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8s8 3.58 8 8s-3.58 8-8 8zm.5-13H11v6l5.25 3.15l.75-1.23l-4.5-2.67z"
              />
            </svg>
          </n-icon>
          <span>创建于</span>
        </div>
        <div class="section-value">
          {{ formatCreateTime(circle?.createTime) }}
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="card-actions">
      <n-button
        v-if="canManage"
        type="primary"
        block
        @click="handleSettings"
      >
        圈子设置
      </n-button>
      <n-button
        v-else
        type="default"
        block
        secondary
        @click="handleLeave"
      >
        退出圈子
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon, NAvatar, NTag, NButton } from 'naive-ui'
import type { Circle } from '@/types/circleChat'
import dayjs from 'dayjs'

interface Props {
  circle: Circle | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'settings'): void
  (e: 'leave'): void
}>()

// 是否可以管理圈子
const canManage = computed(() => {
  return props.circle?.myRole === 'OWNER' || props.circle?.myRole === 'ADMIN'
})

const formatCreateTime = (time?: string) => {
  if (!time) {return '未知'}
  return dayjs(time).format('YYYY-MM-DD')
}

const handleSettings = () => {
  emit('settings')
}

const handleLeave = () => {
  emit('leave')
}
</script>

<style scoped lang="scss">
.circle-info-card {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;

  .n-avatar {
    margin-bottom: 12px;
    border: 3px solid rgba(255, 255, 255, 0.3);
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    text-align: center;
  }
}

.card-content {
  .info-section {
    margin-bottom: 20px;

    .section-label {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      font-size: 13px;
      opacity: 0.9;
    }

    .section-value {
      font-size: 14px;
      padding-left: 26px;
      line-height: 1.6;
    }
  }
}

.card-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
</style>
