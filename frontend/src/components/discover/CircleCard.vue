<!-- src/components/discover/CircleCard.vue -->
<template>
  <div class="circle-card" @click="handleCircleClick">
    <div class="circle-header">
      <n-avatar
        v-if="circle.avatar"
        :src="getAvatarUrl(circle.avatar)"
        round
        size="large"
      />
      <n-avatar v-else round size="large">
        {{ getInitials(circle.name) }}
      </n-avatar>
      
      <div class="circle-basic-info">
        <div class="circle-name">{{ circle.name }}</div>
        <div class="circle-type">
          <n-tag :type="circle.type === 1 ? 'success' : 'warning'" size="small" bordered round>
            {{ circle.type === 1 ? '公开' : '私密' }}
          </n-tag>
        </div>
      </div>
    </div>
    
    <div class="circle-description">
      <n-ellipsis :tooltip="false" :line-clamp="2">
        {{ circle.description || '暂无描述' }}
      </n-ellipsis>
    </div>
    
    <div class="circle-stats">
      <span class="stat-item">
        <n-icon size="14">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24">
            <path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
          </svg>
        </n-icon>
        {{ circle.memberCount || 0 }} 成员
      </span>
      <span class="stat-item">
        <n-icon size="14">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24">
            <path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93c0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41c0 2.08-.8 3.97-2.1 5.39z"/>
          </svg>
        </n-icon>
        创建于 {{ formatCreateTime(circle.createTime) }}
      </span>
    </div>
    
    <div class="circle-actions">
      <n-button
        :type="circle.isJoined ? 'default' : 'primary'"
        size="small"
        round
        @click.stop="handleJoinCircle"
      >
        {{ circle.isJoined ? '已加入' : '加入' }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NAvatar, NEllipsis, NIcon, NButton, NTag } from 'naive-ui'
import { getAvatarUrl } from '@/utils/userUtils'
import dayjs from 'dayjs'
import type { CircleInfo } from '@/types/discover'

interface Props {
  circle: CircleInfo
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', circle: CircleInfo): void
  (e: 'join', circle: CircleInfo): void
}>()

/**
 * 获取名称首字母
 */
const getInitials = (name: string) => {
  if (!name) return ''
  return name.charAt(0).toUpperCase()
}

/**
 * 格式化创建时间
 */
const formatCreateTime = (time?: string) => {
  if (!time) return '未知'
  return dayjs(time).format('YYYY-MM-DD')
}

/**
 * 处理点击
 */
const handleCircleClick = () => {
  emit('click', props.circle)
}

/**
 * 处理加入
 */
const handleJoinCircle = () => {
  emit('join', props.circle)
}
</script>

<style scoped lang="scss">
.circle-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
  }

  .circle-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .circle-basic-info {
      flex: 1;
      min-width: 0;

      .circle-name {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-bottom: 6px;
      }
    }
  }

  .circle-description {
    font-size: 14px;
    color: #666;
    margin-bottom: 16px;
    line-height: 1.6;
    flex: 1;
    min-height: 44px;
  }

  .circle-stats {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #999;
    margin-bottom: 16px;
    flex-wrap: wrap;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .circle-actions {
    width: 100%;
    text-align: center;
  }
}
</style>
