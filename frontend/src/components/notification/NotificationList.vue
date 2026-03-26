<template>
  <div class="notification-list">
    <!-- 空状态 -->
    <n-empty v-if="!isLoading && notificationList.length === 0" description="暂无通知" />

    <!-- 列表 -->
    <div v-else class="list-container">
      <NotificationItem
        v-for="notification in notificationList"
        :key="notification.id"
        :notification="processNotificationContent(notification)"
        @mark-read="handleMarkRead"
        @delete="handleDelete"
        @navigate="handleNavigate"
      />

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <n-button
          :loading="isLoading"
          text
          type="primary"
          @click="handleLoadMore"
        >
          加载更多
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import NotificationItem from '@/components/notification/NotificationItem.vue'
import { useNotificationStore } from '@/stores/notification'
import { NotificationType } from '@/types/notification'
import type { Notification } from '@/types/notification'
import {storeToRefs} from "pinia"

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const notificationStore = useNotificationStore()

const { notificationList, isLoading } = storeToRefs(notificationStore)

// 分页参数
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

// 是否还有更多
const hasMore = computed(() => {
  return notificationList.value.length < total.value
})

// 加载通知列表
const loadNotifications = async () => {
  try {
    const res = await notificationStore.loadNotifications({
      page: currentPage.value,
      limit: pageSize,
    })
    if (res?.pagination) {
      total.value = res.pagination.total
    }
  } catch (error) {
    message.error('加载通知失败')
  }
}

// 加载更多
const handleLoadMore = async () => {
  currentPage.value++
  await loadNotifications()
}

// 标记已读
const handleMarkRead = async (ids: number[]) => {
  try {
    await notificationStore.markAsRead(ids)
    message.success('标记成功')
  } catch (error) {
    message.error('标记失败')
  }
}

// 删除通知
const handleDelete = async (id: number) => {
  try {
    await dialog.confirm({
      title: '提示',
      content: '确定要删除这条通知吗？',
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        await notificationStore.removeNotification(id)
        message.success('删除成功')
      }
    })
  } catch (error: any) {
    if (error !== 'cancel') {
      message.error('删除失败')
    }
  }
}

// 跳转处理
const handleNavigate = async (notification: Notification) => {
  // 先标记为已读
  if (!notification.isRead) {
    await handleMarkRead([notification.id])
  }

  // 根据类型跳转
  const routePath = getNotificationRoute(notification)
  if (routePath) {
    router.push(routePath)
  }
}

// 处理通知内容（确保是对象格式）
const processNotificationContent = (notification: Notification): Notification => {
  if (!notification.content) return notification

  try {
    // 如果 content 是字符串，尝试解析为 JSON
    if (typeof notification.content === 'string') {
      const parsed = JSON.parse(notification.content)
      return {
        ...notification,
        content: parsed
      }
    }
    // 如果已经是对象，直接返回
    return notification
  } catch {
    // 解析失败，保持原样（可能是普通文本）
    return notification
  }
}

// 获取跳转路由
const getNotificationRoute = (notification: Notification) => {
  const { type, extra } = notification

  switch (type) {
    case 1: // COMMENT
    case 2: // LIKE
    case 4: // REPLY
      const articleId = (extra as any)?.articleId || notification.sourceId
      const commentId = (extra as any)?.commentId
      // 使用 query 参数方式传递 articleId
      return articleId ? `/index/articleDetail?id=${articleId}` : '/'

    case 3: // FOLLOW
      const followerId = (extra as any)?.follower?.id || notification.fromUserId || notification.sourceId
      // 用户首页路由：/user/{userId}
      return followerId ? `/user/${followerId}` : '/'

    case 5: // ARTICLE_AUDIT
      const auditArticleId = (extra as any)?.articleId || notification.sourceId
      return auditArticleId ? `/index/articleDetail?id=${auditArticleId}` : '/article/my-articles'

    case 6: // CIRCLE_INVITE
    case 7: // CIRCLE_REMOVED
    case 8: // CIRCLE_JOIN
    case 9: // MEMBER_JOIN
    case 10: // MEMBER_QUIT
    case 11: // MEMBER_REMOVED
      const circleId = (extra as any)?.circleId || notification.sourceId
      return circleId ? `/circle/${circleId}` : '/circle'

    case 12: // SUGGESTION_SUBMIT
    case 13: // SUGGESTION_REVIEW
      const suggestionId = (extra as any)?.suggestionId
      return suggestionId ? `/suggestion/${suggestionId}` : '/article/my-suggestions'

    case 14: // SYSTEM_MESSAGE
      const link = (extra as any)?.link
      return link || null

    default:
      return null
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped lang="scss">
.notification-list {
  .list-container {
    display: flex;
    flex-direction: column;
  }

  .load-more {
    display: flex;
    justify-content: center;
    padding: 20px;
  }
}
</style>
