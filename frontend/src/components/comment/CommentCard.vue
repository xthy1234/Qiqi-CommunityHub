<!-- src/components/comment/CommentCard.vue -->
<template>
  <div class="comment-card" :class="{ 'is-reply': isReply }">
    <div class="comment-avatar">
      <UserAvatarLink
          :user-id="comment.userId || comment.user?.id"
          :nickname="comment.userNickname || comment.user?.nickname"
          :avatar="comment.userAvatar || comment.user?.avatar"
          :size="isReply ? 32 : 40"
      />
    </div>
    <div class="comment-body">
      <div class="comment-header">
        <span class="comment-author">{{ comment.userNickname || comment.user?.nickname }}</span>
        <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
        <span
            v-if="comment.isEdited && comment.updateTime"
            class="edited-tag"
            :class="{ small: isReply }"
            :title="'最后编辑时间：' + formatDate(comment.updateTime)"
        >
          已编辑
        </span>
      </div>

      <div class="comment-content">
        <span
            v-if="comment.replyToUser"
            class="reply-to-prefix"
        >
          回复 @{{ comment.replyToUser.nickname }} 
        </span>
        {{ comment.content }}
      </div>

      <div class="comment-actions">
        <div
            class="action-item"
            :class="{ small: isReply }"
            @click="$emit('reply', comment)"
        >
          <Icon icon="ri:chat-1-line" :width="isReply ? 14 : 16" />
          <span>回复</span>
        </div>

        <div
            class="action-item"
            :class="{ small: isReply, active: comment.isLiked }"
            @click="$emit('like', comment)"
        >
          <Icon
              :icon="comment.isLiked ? 'ri:thumb-up-fill' : 'ri:thumb-up-line'"
              :width="isReply ? 14 : 16"
          />
          <span>{{ comment.likeCount || 0 }}</span>
        </div>

        <div
            class="action-item"
            :class="{ small: isReply, active: comment.isDisliked }"
            @click="$emit('dislike', comment)"
        >
          <Icon
              :icon="comment.isDisliked ? 'ri:thumb-down-fill' : 'ri:thumb-down-line'"
              :width="isReply ? 14 : 16"
          />
          <span>{{ comment.dislikeCount || 0 }}</span>
        </div>

        <div
            v-if="canEdit"
            class="action-item"
            :class="{ small: isReply }"
            @click="$emit('edit', comment)"
        >
          <Icon icon="ri:pencil-line" :width="isReply ? 14 : 16" />
          <span>编辑</span>
        </div>

        <div
            v-if="canDelete"
            class="action-item delete"
            :class="{ small: isReply }"
            @click="$emit('delete', comment)"
        >
          <Icon icon="ri:delete-bin-line" :width="isReply ? 14 : 16" />
          <span>删除</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@iconify/vue'
import { type Comment } from '@/api/comment'
import { formatDate } from '@/utils/userUtils'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'

interface Props {
  comment: Comment
  isReply?: boolean
  currentUserId?: string | number
  isAdmin?: boolean
  articleAuthorId?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  isReply: false
})

defineEmits<{
  reply: [comment: Comment]
  like: [comment: Comment]
  dislike: [comment: Comment]
  edit: [comment: Comment]
  delete: [comment: Comment]
}>()

const canEdit = computed(() => {
  if (!props.currentUserId) return false
  const commentUserId = props.comment.userId || props.comment.user?.id
  return String(commentUserId) === String(props.currentUserId)
})

const canDelete = computed(() => {
  if (!props.currentUserId) return false
  if (props.isAdmin) return true
  if (props.articleAuthorId && String(props.articleAuthorId) === String(props.currentUserId)) return true
  const commentUserId = props.comment.userId || props.comment.user?.id
  return String(commentUserId) === String(props.currentUserId)
})
</script>

<style lang="scss" scoped>
.comment-card {
  display: flex;
  gap: 15px;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;

  &.is-reply {
    padding: 12px 0;

    &:not(:last-child) {
      border-bottom: 1px solid #f5f5f5;
    }
  }

  &:last-child:not(.is-reply) {
    border-bottom: none;
  }

  .comment-avatar {
    flex-shrink: 0;
  }

  .comment-body {
    flex: 1;

    .comment-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .comment-author {
        font-weight: 500;
        color: #409EFF;
      }

      .comment-time {
        font-size: 12px;
        color: #999;
      }

      .edited-tag {
        font-size: 12px;
        color: #999;
        cursor: help;

        &.small {
          font-size: 11px;
        }
      }
    }

    .comment-content {
      font-size: 14px;
      line-height: 1.6;
      color: #333;
      margin-bottom: 10px;
      word-break: break-word;

      .reply-to-prefix {
        color: #409EFF;
        font-weight: 500;
        margin-right: 4px;
      }
    }

    .comment-actions {
      display: flex;
      gap: 20px;

      .action-item {
        display: flex;
        align-items: center;
        gap: 4px;
        cursor: pointer;
        font-size: 13px;
        color: #999;
        transition: all 0.3s;

        &:hover {
          color: #409EFF;
        }

        &.active {
          color: #67C23A;
        }

        &.delete:hover {
          color: #F56C6C;
        }

        &.small {
          font-size: 12px;
        }
      }
    }
  }
}
</style>
