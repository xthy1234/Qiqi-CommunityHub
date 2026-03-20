<!-- src/components/circle-chat/CircleMemberList.vue -->
<template>
  <div class="circle-member-list">
    <div class="member-list-header">
      <div class="header-title">
        <n-icon size="20" @click="handleBack">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
            <path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
          </svg>
        </n-icon>
        <span>成员列表</span>
        <span class="member-count">{{ members.length }}人</span>
      </div>
      
      <div class="header-actions">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button text @click="handleInvite">
              <template #icon>
                <n-icon size="20">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
                    <path fill="currentColor" d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                  </svg>
                </n-icon>
              </template>
            </n-button>
          </template>
          邀请成员
        </n-tooltip>
      </div>
    </div>

    <div class="member-list-content">
      <n-spin :show="loading">
        <!-- 管理员分组 -->
        <div v-if="admins.length > 0" class="member-section">
          <div class="section-title">管理员</div>
          <n-list hoverable clickable>
            <n-list-item
              v-for="member in admins"
              :key="member.userId"
              @click="handleMemberClick(member)"
            >
              <template #prefix>
                <n-badge :dot="member.isOnline" :show="member.isOnline">
                  <n-avatar :src="member.avatar" round size="medium" />
                </n-badge>
              </template>
              
              <div class="member-item-content">
                <div class="member-name">{{ member.nickname }}</div>
                <div class="member-role">
                  <n-tag v-if="member.role === 'OWNER'" type="error" size="small" bordered round>
                    圈主
                  </n-tag>
                  <n-tag v-else-if="member.role === 'ADMIN'" type="warning" size="small" bordered round>
                    管理员
                  </n-tag>
                </div>
              </div>
            </n-list-item>
          </n-list>
        </div>

        <!-- 普通成员分组 -->
        <div v-if="members.length > 0" class="member-section">
          <div class="section-title">普通成员</div>
          <n-list hoverable clickable>
            <n-list-item
              v-for="member in members"
              :key="member.userId"
              @click="handleMemberClick(member)"
            >
              <template #prefix>
                <n-badge :dot="member.isOnline" :show="member.isOnline">
                  <n-avatar :src="member.avatar" round size="medium" />
                </n-badge>
              </template>
              
              <div class="member-item-content">
                <div class="member-name">{{ member.nickname }}</div>
                <div class="member-join-time">
                  {{ formatJoinTime(member.joinTime) }}
                </div>
              </div>
            </n-list-item>
          </n-list>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-members">
          <n-empty description="暂无成员" />
        </div>
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon, NSpin, NList, NListItem, NAvatar, NBadge, NTag, NTooltip, NButton, NEmpty } from 'naive-ui'
import type { CircleMember } from '@/types/circleChat'
import dayjs from 'dayjs'

interface Props {
  members: CircleMember[]
  loading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  members: [],
  loading: false
})

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'invite'): void
  (e: 'member-click', member: CircleMember): void
}>()

// 按角色分组
const admins = computed(() => {
  return props.members.filter(m => m.role === 'OWNER' || m.role === 'ADMIN')
})

const members = computed(() => {
  return props.members.filter(m => m.role === 'MEMBER')
})

const formatJoinTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD')
}

const handleBack = () => {
  emit('back')
}

const handleInvite = () => {
  emit('invite')
}

const handleMemberClick = (member: CircleMember) => {
  emit('member-click', member)
}
</script>

<style scoped lang="scss">
.circle-member-list {
  width: 320px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}

.member-list-header {
  height: 64px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;

  .header-title {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 16px;
    font-weight: 600;
    color: #333;

    .n-icon {
      cursor: pointer;
      color: #666;
      
      &:hover {
        color: #333;
      }
    }

    .member-count {
      font-size: 13px;
      font-weight: 400;
      color: #999;
      margin-left: 4px;
    }
  }
}

.member-list-content {
  flex: 1;
  overflow-y: auto;
}

.member-section {
  .section-title {
    padding: 16px 20px 8px;
    font-size: 13px;
    font-weight: 600;
    color: #999;
    background: #fafafa;
  }
}

.member-item-content {
  flex: 1;
  min-width: 0;
  
  .member-name {
    font-size: 14px;
    color: #333;
    margin-bottom: 4px;
  }

  .member-role {
    display: flex;
    gap: 4px;
  }

  .member-join-time {
    font-size: 12px;
    color: #999;
  }
}

.empty-members {
  padding: 60px 20px;
  text-align: center;
}
</style>
