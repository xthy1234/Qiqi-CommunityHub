<template>
  <div
    class="notification-item"
    :class="{ 'is-unread': !notification.isRead }"
    @click="handleClick"
  >
    <!-- 头像 -->
    <div class="notification-avatar">
      <n-avatar
        v-if="displayAvatar"
        :src="displayAvatar"
        :size="48"
        round
        class="avatar"
      />
      <n-avatar
        v-else
        :size="48"
        round
        class="avatar system-avatar"
      >
        系
      </n-avatar>
    </div>

    <!-- 内容 -->
    <div class="notification-content">
      <div class="notification-header">
        <span class="notification-title">{{ displayTitle }}</span>
        <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
      </div>

      <!-- 通知主体：支持富文本解析 -->
      <div class="notification-body">
        <!-- 如果是 JSON 格式的富文本内容（兼容旧数据） -->
        <template v-if="isRichTextContent">
          <EditorContent
            v-if="editorReady && readonlyEditor"
            :editor="readonlyEditor"
            class="tiptap-readonly notification-tiptap"
          />
          <div
            v-else
            class="loading-editor"
          >
            <n-spin size="small" />
          </div>
        </template>

        <!-- 普通文本内容（新格式） -->
        <template v-else>
          <div class="plain-text">{{ displayContent }}</div>
        </template>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="notification-actions">
      <n-button
        v-if="!notification.isRead"
        type="primary"
        size="small"
        text
        @click.stop="handleMarkAsRead"
      >
        标记已读
      </n-button>
      <n-button
        type="error"
        size="small"
        text
        @click.stop="handleDelete"
      >
        删除
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, toRaw } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { NotificationType } from '@/types/notification'
import type { Notification } from '@/types/notification'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import { useMessage } from 'naive-ui'
import { getNotificationTitle, getNotificationDescription } from '@/utils/notificationText'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const messageApi = useMessage()

const props = defineProps<{
  notification: Notification
}>()

const emit = defineEmits<{
  (e: 'mark-read', ids: number[]): void
  (e: 'delete', id: number): void
  (e: 'navigate', notification: Notification): void
}>()

// 获取显示的头像（优先从 extra 中获取）
const displayAvatar = computed(() => {
  // 1. 优先使用 fromUserAvatar
  if (props.notification.fromUserAvatar) {
    return props.notification.fromUserAvatar
  }

  // 2. 从 extra 中提取头像
  const safeExtra = props.notification.extra || {}

  // 检查不同类型通知的头像来源
  const user = (safeExtra as any)?.liker
    || (safeExtra as any)?.follower
    || (safeExtra as any)?.commenter
    || (safeExtra as any)?.replier
    || (safeExtra as any)?.inviter
    || (safeExtra as any)?.operator
    || (safeExtra as any)?.applicant
    || (safeExtra as any)?.newMember
    || (safeExtra as any)?.quitter
    || (safeExtra as any)?.removedUser
    || (safeExtra as any)?.proposer

  if (user?.avatar) {
    return user.avatar
  }

  return ''
})

// 获取通知标题
const displayTitle = computed(() => {
  return getNotificationTitle(props.notification)
})

// 获取通知内容
const displayContent = computed(() => {
  return getNotificationDescription(props.notification)
})

// 判断是否为富文本内容（JSON 格式，用于兼容旧数据）
const isRichTextContent = computed(() => {
  if (!props.notification.content) return false

  try {
    const content = typeof props.notification.content === 'string'
      ? JSON.parse(props.notification.content)
      : props.notification.content

    return content?.type === 'doc' && Array.isArray(content.content)
  } catch {
    return false
  }
})

// 只读编辑器实例
let readonlyEditor: Editor | null = null
const editorReady = ref(false)

// 初始化编辑器
onMounted(async () => {
  if (!isRichTextContent.value) return

  try {
    let contentJson: any

    if (typeof props.notification.content === 'string') {
      contentJson = JSON.parse(props.notification.content)
    } else {
      contentJson = toRaw(props.notification.content)
    }

    // 创建只读编辑器
    readonlyEditor = new Editor({
      extensions: [
        StarterKit,
        Image,
        Link.configure({
          openOnClick: false,
          HTMLAttributes: {
            target: '_blank',
            rel: 'noopener noreferrer'
          }
        })
      ],
      content: contentJson,
      editable: false,
      editorProps: {
        attributes: {
          class: 'prose prose-sm focus:outline-none'
        }
      },
      onCreate: () => {
        editorReady.value = true

        // 强制刷新内容
        setTimeout(() => {
          if (readonlyEditor && !readonlyEditor.isDestroyed) {
            readonlyEditor.commands.setContent(contentJson)
          }
        }, 0)
      }
    })
  } catch (error) {
    console.error('❌ [NotificationItem] TipTap 编辑器创建失败:', error)
    readonlyEditor = null
    editorReady.value = false
  }
})

// 销毁编辑器
onBeforeUnmount(() => {
  if (readonlyEditor && !readonlyEditor.isDestroyed) {
    try {
      readonlyEditor.destroy()
    } catch (error) {
      console.error('⚠️ [NotificationItem] 销毁编辑器失败:', error)
    }
    readonlyEditor = null
    editorReady.value = false
  }
})

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).fromNow()
}

// 点击跳转
const handleClick = () => {
  emit('navigate', props.notification)
}

// 标记已读
const handleMarkAsRead = () => {
  emit('mark-read', [props.notification.id])
}

// 删除
const handleDelete = () => {
  emit('delete', props.notification.id)
}
</script>

<style scoped lang="scss">
.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f7fa;
  }

  &.is-unread {
    background-color: #f0f9ff;

    .notification-title {
      font-weight: bold;
    }
  }
}

.notification-avatar {
  margin-right: 12px;
  flex-shrink: 0;

  .avatar {
    :deep(img) {
      width: 48px;
      height: 48px;
      object-fit: cover;
    }
  }

  .system-avatar {
    background-color: #e5e7eb !important;
    color: #6b7280 !important;
    font-size: 18px;
  }
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-title {
  font-size: 14px;
  color: #1f2937;
}

.notification-time {
  font-size: 12px;
  color: #9ca3af;
  flex-shrink: 0;
  margin-left: 12px;
}

.notification-body {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;

  .plain-text {
    word-break: break-all;
  }

  // TipTap 编辑器样式
  :deep(.notification-tiptap) {
    .ProseMirror {
      padding: 0;
      background: transparent;
      border: none;

      p {
        margin: 0;
        min-height: auto;

        &:empty {
          display: none;
        }

        &:last-child {
          margin-bottom: 0;
        }
      }

      img {
        max-width: 200px;
        max-height: 150px;
        border-radius: 4px;
        margin: 4px 0;
        cursor: pointer;

        &:hover {
          opacity: 0.9;
        }
      }

      a {
        color: #18a058;
        text-decoration: underline;

        &:hover {
          text-decoration: none;
        }
      }
    }
  }

  .loading-editor {
    display: inline-flex;
    align-items: center;
  }
}

.notification-actions {
  display: flex;
  gap: 8px;
  margin-left: 12px;
  flex-shrink: 0;
}
</style>
