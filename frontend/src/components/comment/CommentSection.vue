<!-- src/components/CommentSection.vue -->
<template>
  <div class="comment-section">
    <div class="comment-header">
      <h3>评论</h3>
      <span class="comment-count">{{ totalComments }} 条评论</span>
    </div>

    <!-- 发表评论输入框 -->
    <div class="comment-input-wrapper">
      <div class="current-user-avatar">
        <n-avatar
          :size="40"
          :src="userAvatar"
        />
      </div>
      <div class="comment-input-box">
        <n-input
          v-model:value="newComment.content"
          type="textarea"
          :rows="3"
          :placeholder="editingComment ? '编辑评论...' : (replyingToComment ? `回复 @${getReplyTargetNickname()}...` : '写下你的评论...')"
          maxlength="1000"
          show-count
        />
        <div class="comment-actions">
          <n-button
            v-if="editingComment"
            type="primary"
            :loading="submitting"
            :disabled="!newComment.content.trim()"
            @click="submitEdit"
          >
            保存修改
          </n-button>
          <n-button
            v-else
            type="primary"
            :loading="submitting"
            :disabled="!newComment.content.trim()"
            @click="submitComment"
          >
            {{ replyingToComment ? '回复评论' : '发表评论' }}
          </n-button>
          <n-button
            v-if="replyingToComment || editingComment"
            @click="cancelAction"
          >
            取消
          </n-button>
        </div>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list-wrapper">
      <div
        v-if="commentsLoading"
        class="comments-loading"
      >
        <Icon
          icon="ri:loader-4-line"
          class="is-loading"
          width="24"
        />
        <span>加载评论...</span>
      </div>

      <div
        v-else-if="commentList.length === 0"
        class="empty-comments"
      >
        <Icon
          icon="ri:message-3-line"
          width="48"
        />
        <span>暂无评论，快来抢沙发吧~</span>
      </div>

      <div
        v-else
        class="comment-list"
      >
        <div
          v-for="comment in commentList"
          :key="comment.id"
          class="main-comment-wrapper"
        >
          <!-- 主评论卡片 -->
          <CommentCard
            :comment="comment"
            :current-user-id="currentUserId"
            :is-admin="isAdmin"
            :article-author-id="articleAuthorId"
            @reply="handleReply"
            @like="handleLike"
            @dislike="handleDislike"
            @edit="handleEdit"
            @delete="handleDelete"
          />

          <!-- 子评论区域 -->
          <div
            v-if="comment.topReplies && comment.topReplies.length > 0"
            class="replies-container"
          >
            <CommentCard
              v-for="reply in sortRepliesByTime(comment.topReplies)"
              :key="reply.id"
              :comment="reply"
              :is-reply="true"
              :current-user-id="currentUserId"
              :is-admin="isAdmin"
              :article-author-id="articleAuthorId"
              @reply="handleReply"
              @like="handleLike"
              @dislike="handleDislike"
              @edit="handleEdit"
              @delete="handleDelete"
            />

            <!-- 查看更多回复按钮 -->
            <div
              v-if="comment.replyCount && comment.replyCount > (comment.topReplies?.length || 0)"
              class="view-more-replies"
              @click="loadMoreReplies(comment.id!)"
            >
              <span>查看全部 {{ comment.replyCount }} 条回复</span>
              <Icon
                icon="ri:arrow-down-s-line"
                width="16"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多主评论 -->
      <div
        v-if="hasMore && !loadingMore"
        class="load-more-trigger"
        @click="loadMoreComments"
      >
        <span>加载更多评论</span>
      </div>
      <div
        v-if="loadingMore"
        class="loading-more"
      >
        <Icon
          icon="ri:loader-4-line"
          class="is-loading"
          width="20"
        />
        <span>加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { commentAPI, type Comment } from '@/api/comment'
import { formatDate } from '@/utils/userUtils'
import { useGlobalProperties } from '@/utils/globalProperties'
import CommentCard from './CommentCard.vue'
import { NAvatar, NInput, NButton, useDialog, useMessage } from 'naive-ui'

interface Props {
  articleId: number | string
  currentUserId?: string | number
  userAvatar: string
  isAdmin?: boolean
  articleAuthorId?: string | number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [value: { count: number }]
}>()

const appContext = useGlobalProperties()
const message = useMessage()
const dialog = useDialog()

const commentList = ref<Comment[]>([])
const totalComments = ref<number>(0)
const commentsLoading = ref<boolean>(false)
const loadingMore = ref<boolean>(false)
const submitting = ref<boolean>(false)
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const hasMore = ref<boolean>(true)
const newComment = ref({ content: '' })
const replyingToComment = ref<Comment | null>(null)
const editingComment = ref<Comment | null>(null)

// 获取回复目标的昵称
const getReplyTargetNickname = (): string => {
  if (!replyingToComment.value) return ''
  return replyingToComment.value.userNickname || replyingToComment.value.user?.nickname || '用户'
}

// 对子评论按创建时间升序排序（仅用于展示，不响应式更新）
const sortRepliesByTime = (replies: Comment[]): Comment[] => {
  return [...replies].sort((a, b) => {
    const timeA = new Date(a.createTime || '').getTime()
    const timeB = new Date(b.createTime || '').getTime()
    return timeA - timeB
  })
}

// 加载评论列表
const loadComments = async (page: number = 1, append: boolean = false) => {
  if (!props.articleId) {return}

  if (page === 1) {
    commentsLoading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const response = await commentAPI.getArticleComments(props.articleId, 'list', page, pageSize.value)

    if (response.data.code === 0 && response.data.data) {
      const { list, totalCount, currPage, totalPage } = response.data.data

      // 后端已按点赞数降序返回，前端直接使用
      if (append) {
        commentList.value = [...commentList.value, ...list]
      } else {
        commentList.value = list
      }

      totalComments.value = totalCount
      currentPage.value = currPage
      hasMore.value = currPage < totalPage

      emit('update', { count: totalComments.value })
    }
  } catch (error) {
    message.error('加载评论失败')
  } finally {
    commentsLoading.value = false
    loadingMore.value = false
  }
}

// 加载更多主评论
const loadMoreComments = async () => {
  const nextPage = currentPage.value + 1
  await loadComments(nextPage, true)
}

// 加载更多子评论
const loadMoreReplies = async (parentCommentId: number) => {
  try {
    const response = await commentAPI.getReplies(parentCommentId, 1, 50)

    if (response.data.code === 0 && response.data.data) {
      const replies = response.data.data.list

      // 找到对应的主评论并替换 topReplies
      const parentComment = commentList.value.find(c => c.id === parentCommentId)
      if (parentComment) {
        parentComment.topReplies = replies
      }
    }
  } catch (error) {
    message.error('加载回复失败')
  }
}

// 处理回复
const handleReply = (comment: Comment) => {
  if (replyingToComment.value?.id === comment.id) {
    cancelAction()
  } else {
    replyingToComment.value = comment
    editingComment.value = null
  }
}

// 处理点赞
const handleLike = async (comment: Comment) => {
  if (!comment.id) return

  try {
    if (comment.isLiked) {
      await commentAPI.cancelLike(comment.id)
      comment.isLiked = false
      comment.likeCount = (comment.likeCount || 0) - 1
    } else {
      await commentAPI.likeComment(comment.id)
      comment.isLiked = true
      comment.likeCount = (comment.likeCount || 0) + 1
      // 点赞时自动取消点踩
      if (comment.isDisliked) {
        comment.isDisliked = false
        comment.dislikeCount = (comment.dislikeCount || 0) - 1
      }
    }
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
  }
}

// 处理点踩
const handleDislike = async (comment: Comment) => {
  if (!comment.id) return

  try {
    if (comment.isDisliked) {
      await commentAPI.cancelDislike(comment.id)
      comment.isDisliked = false
      comment.dislikeCount = (comment.dislikeCount || 0) - 1
    } else {
      await commentAPI.dislikeComment(comment.id)
      comment.isDisliked = true
      comment.dislikeCount = (comment.dislikeCount || 0) + 1
      // 点踩时自动取消点赞
      if (comment.isLiked) {
        comment.isLiked = false
        comment.likeCount = (comment.likeCount || 0) - 1
      }
    }
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
  }
}

// 处理编辑
const handleEdit = (comment: Comment) => {
  editingComment.value = comment
  newComment.value.content = comment.content
  replyingToComment.value = null
}

// 处理删除
const handleDelete = async (comment: Comment) => {
  if (!comment.id) return

  try {
    await new Promise<void>((resolve, reject) => {
      dialog.warning({
        title: '提示',
        content: '确定要删除这条评论吗？',
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: () => resolve(),
        onNegativeClick: () => reject('cancel'),
        onClose: () => reject('cancel')
      })
    })

    const response = await commentAPI.deleteComment(comment.id)

    if (response.data.code === 0) {
      message.success('删除成功')
      await loadComments(1, false)
    } else {
      message.error(response.data.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      message.error('删除失败')
    }
  }
}

// 提交评论
const submitComment = async () => {
  if (!props.articleId || !newComment.value.content.trim()) return

  const token = appContext?.$toolUtil?.storageGet('Token')
  if (!token) {
    message.warning('请先登录')
    return
  }

  submitting.value = true
  try {
    if (replyingToComment.value) {
      // 调试日志
      console.log('[CommentSection] 准备回复评论:', {
        replyingTo: {
          id: replyingToComment.value.id,
          parentId: replyingToComment.value.parentId,
          userId: replyingToComment.value.userId || replyingToComment.value.user?.id,
          nickname: replyingToComment.value.userNickname || replyingToComment.value.user?.nickname
        }
      })

      await commentAPI.replyComment(replyingToComment.value.id!, {
        contentId: props.articleId,
        replyContent: newComment.value.content,
        replyId: replyingToComment.value.userId || replyingToComment.value.user?.id
      })
      message.success('回复成功')
    } else {
      await commentAPI.createComment({
        contentId: props.articleId,
        content: newComment.value.content,
        parentId: null
      })
      message.success('评论成功')
    }

    newComment.value.content = ''
    replyingToComment.value = null
    await loadComments(1, false)
  } catch (error: any) {
    console.error('[CommentSection] 评论失败:', error)
    message.error(error.response?.data?.msg || '评论失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 提交编辑
const submitEdit = async () => {
  if (!editingComment.value || !newComment.value.content.trim()) return

  submitting.value = true
  try {
    await commentAPI.patchComment(editingComment.value.id!, newComment.value.content)
    message.success('修改成功')

    editingComment.value.content = newComment.value.content
    editingComment.value.isEdited = true
    editingComment.value.updateTime = new Date().toISOString()

    cancelAction()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '修改失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 取消操作
const cancelAction = () => {
  replyingToComment.value = null
  editingComment.value = null
  newComment.value.content = ''
}

onMounted(() => {
  loadComments(1, false)
})

defineExpose({
  refresh: () => loadComments(1, false)
})
</script>

<style lang="scss" scoped>
.comment-section {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 2px solid #f0f0f0;

  .comment-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #333;
      margin: 0;
    }

    .comment-count {
      font-size: 14px;
      color: #999;
    }
  }

  .comment-input-wrapper {
    display: flex;
    gap: 15px;
    margin-bottom: 30px;

    .current-user-avatar {
      flex-shrink: 0;
    }

    .comment-input-box {
      flex: 1;

      .comment-actions {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        margin-top: 10px;
      }
    }
  }

  .comment-list-wrapper {
    .comments-loading,
    .empty-comments {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px 0;
      color: #999;
      gap: 10px;
    }

    .comment-list {
      .main-comment-wrapper {
        border-bottom: 1px solid #f0f0f0;

        &:last-child {
          border-bottom: none;
        }
      }

      .replies-container {
        margin-top: 15px;
        padding-left: 55px;
        border-left: 2px solid #f0f0f0;

        .view-more-replies {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 5px;
          padding: 10px;
          margin-top: 10px;
          color: #409EFF;
          font-size: 13px;
          cursor: pointer;
          transition: background 0.3s;
          border-radius: 4px;

          &:hover {
            background: #f0f7ff;
          }
        }
      }
    }

    .load-more-trigger {
      text-align: center;
      padding: 15px;
      color: #409EFF;
      cursor: pointer;
      font-size: 14px;
      transition: background 0.3s;
      border-radius: 4px;

      &:hover {
        background: #f5f7fa;
      }
    }

    .loading-more {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 8px;
      padding: 20px;
      color: #999;
      font-size: 14px;
    }
  }
}
</style>
