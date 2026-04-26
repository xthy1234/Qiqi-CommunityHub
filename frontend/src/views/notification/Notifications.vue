<template>
  <page-container
    :header-title="'通知'"
  >
    <template #headerExtra>
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
    </template>

    <!-- 筛选标签 -->
    <div class="filter-tabs">
      <n-tabs
        v-model:value="activeTab"
        type="line"
        animated
        @update:value="handleTabChange"
      >
        <n-tab-pane
          name="all"
          tab="全部通知"
        />
        <n-tab-pane
          :name="'unread'"
          :tab="`未读通知 (${unreadCount})`"
        />
        <n-tab-pane
          name="read"
          tab="已读通知"
        />
      </n-tabs>
    </div>

    <!-- 通知列表 -->
    <NotificationList />
  </page-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {useMessage, useDialog, NButton} from 'naive-ui'
import PageContainer from '@/components/layout/PageContainer.vue'
import NotificationList from '@/components/notification/NotificationList.vue'
import { useNotificationStore } from '@/stores/notification'
import {storeToRefs} from "pinia";
import {Icon} from "@iconify/vue";

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
    // 刷新当前标签页的通知列表
    await handleTabChange(activeTab.value)
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


  // 初始化 WebSocket 订阅
  notificationStore.initWebSocketSubscription()

  // 加载未读数
  try {
    await notificationStore.loadUnreadCount()

  } catch (error) {
    console.error('[Notifications] 加载未读数量失败:', error)
  }
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
