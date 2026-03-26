<template>
  <page-container
      :header-title="'通知'"
  >
    <template #header>
      <div class="page-header">
        <h2>通知中心</h2>
        <div class="header-actions">
          <n-space>
            <n-button
              v-if="hasUnread"
              type="primary"
              size="small"
              @click="handleMarkAllRead"
            >
              全部已读
            </n-button>
            <n-button
              type="error"
              size="small"
              @click="handleClear"
            >
              清空通知
            </n-button>
          </n-space>
        </div>
      </div>
    </template>

    <!-- 筛选标签 -->
    <div class="filter-tabs">
      <n-tabs v-model:value="activeTab" type="line" animated @update:value="handleTabChange">
        <n-tab-pane name="all" tab="全部通知" />
        <n-tab-pane :name="'unread'" :tab="`未读通知 (${unreadCount})`" />
        <n-tab-pane name="read" tab="已读通知" />
      </n-tabs>
    </div>

    <!-- 通知列表 -->
    <NotificationList />
  </page-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import NotificationList from '@/components/notification/NotificationList.vue'
import { useNotificationStore } from '@/stores/notification'
import {storeToRefs} from "pinia";

const message = useMessage()
const dialog = useDialog()
const notificationStore = useNotificationStore()
const { unreadCount, hasUnread } = storeToRefs(notificationStore)

const activeTab = ref('all')

// 切换标签
const handleTabChange = async (tab: string) => {
  let isRead = null
  if (tab === 'unread') {
    isRead = false
  } else if (tab === 'read') {
    isRead = true
  }

  try {
    await notificationStore.loadNotifications({ isRead })
  } catch (error) {
    message.error('加载失败')
  }
}

// 全部已读
const handleMarkAllRead = async () => {
  try {
    await notificationStore.markAllAsRead()
    message.success('已全部标记为已读')
  } catch (error) {
    message.error('操作失败')
  }
}

// 清空通知
const handleClear = async () => {
  try {
    await dialog.warning({
      title: '警告',
      content: '确定要清空所有通知吗？此操作不可恢复',
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        await notificationStore.clearAll()
        message.success('清空成功')
      }
    })
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error('操作失败')
    }
  }
}

onMounted(async () => {
  // 加载未读数
  await notificationStore.loadUnreadCount()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #1f2937;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.filter-tabs {
  margin-bottom: 16px;
}
</style>
