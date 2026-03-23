<!-- src/components/CommentSection.vue -->
<template>
  <div class="comment-section">
    <div class="comment-header">
      <h3>评论</h3>
      <span class="comment-count">{{ commentCount }} 条评论</span>
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
          placeholder="写下你的评论..."
          maxlength="500"
          show-count
        />
        <div class="comment-actions">
          <n-button
            type="primary"
            :loading="submitting"
            :disabled="!newComment.content.trim()"
            @click="submitComment"
          >
            {{ replyingToComment ? '回复评论' : '发表评论' }}
          </n-button>
          <n-button
            v-if="replyingToComment"
            @click="cancelReply"
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
          class="comment-item"
        >
          <div class="comment-avatar">
            <UserAvatarLink
              :user-id="comment.userId"
              :nickname="comment.userNickname"
              :avatar="comment.userAvatar"
              :size="40"
            />
          </div>
          <div class="comment-content">
            <div class="comment-info">
              <span class="comment-author">{{ comment.userNickname }}</span>
              <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
            </div>
            <div class="comment-text">
              {{ comment.content }}
            </div>
            <div class="comment-actions-bar">
              <div
                class="action-item"
                @click="toggleReply(comment)"
              >
                <Icon
                  icon="ri:chat-1-line"
                  width="16"
                />
                <span>回复</span>
              </div>
              <div
                class="action-item"
                :class="{ active: comment.isLiked }"
                @click="() => likeComment(comment, 2)"
              >
                <Icon
                  :icon="comment.isLiked ? 'ri:thumb-up-fill' : 'ri:thumb-up-line'"
                  width="16"
                />
                <span>{{ comment.likeCount || 0 }}</span>
              </div>
              <div
                class="action-item"
                :class="{ active: comment.isDisliked }"
                @click="() => likeComment(comment, 3)"
              >
                <Icon
                  :icon="comment.isDisliked ? 'ri:thumb-down-fill' : 'ri:thumb-down-line'"
                  width="16"
                />
                <span>踩</span>
              </div>
              <div
                v-if="canDeleteComment(comment)"
                class="action-item delete"
                @click="deleteComment(comment)"
              >
                <Icon
                  icon="ri:delete-bin-line"
                  width="16"
                />
                <span>删除</span>
              </div>
            </div>

            <!-- 子评论（回复） -->
            <div
              v-if="comment.children && comment.children.length > 0"
              class="comment-replies"
            >
              <div
                v-for="reply in comment.children"
                :key="reply.id"
                class="reply-item"
              >
                <div class="reply-avatar">
                  <UserAvatarLink
                    :user-id="reply.userId"
                    :nickname="reply.userNickname"
                    :avatar="reply.userAvatar"
                    :size="32"
                  />
                </div>
                <div class="reply-content">
                  <div class="reply-info">
                    <span class="reply-author">{{ reply.userNickname }}</span>
                    <span class="reply-time">{{ formatDate(reply.createTime) }}</span>
                  </div>
                  <div class="reply-text">
                    {{ reply.content }}
                  </div>
                  <div class="reply-actions-bar">
                    <div
                      class="action-item small"
                      @click="toggleReply(reply)"
                    >
                      <Icon
                        icon="ri:chat-1-line"
                        width="14"
                      />
                      <span>回复</span>
                    </div>
                    <div
                      class="action-item small"
                      :class="{ active: reply.isLiked }"
                      @click="() => likeComment(reply, 2)"
                    >
                      <Icon
                        :icon="reply.isLiked ? 'ri:thumb-up-fill' : 'ri:thumb-up-line'"
                        width="14"
                      />
                      <span>{{ reply.likeCount || 0 }}</span>
                    </div>
                    <div
                      class="action-item small"
                      :class="{ active: reply.isDisliked }"
                      @click="() => likeComment(reply, 3)"
                    >
                      <Icon
                        :icon="reply.isDisliked ? 'ri:thumb-down-fill' : 'ri:thumb-down-line'"
                        width="14"
                      />
                      <span>踩</span>
                    </div>
                    <div
                      v-if="canDeleteComment(reply)"
                      class="action-item small delete"
                      @click="deleteComment(reply)"
                    >
                      <Icon
                        icon="ri:delete-bin-line"
                        width="14"
                      />
                      <span>删除</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { getCurrentInstance } from 'vue'
import { commentAPI, type Comment } from '@/api/comment'
import { interactionAPI } from '@/api/interaction'
import { getAvatarUrl, formatDate } from '@/utils/userUtils'
import { useGlobalProperties } from '@/utils/globalProperties'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
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
const commentCount = ref<number>(0)
const commentsLoading = ref<boolean>(false)
const submitting = ref<boolean>(false)
const newComment = ref({ content: '' })
const replyingToComment = ref<Comment | null>(null)

// 加载评论列表
const loadComments = async () => {
  if (!props.articleId) {return}

  commentsLoading.value = true
  try {
    const response = await commentAPI.getTree(props.articleId)
    if (response.data.data && Array.isArray(response.data.data)) {
      commentList.value = response.data.data

    } else {
      commentList.value = []
    }

    const countResponse = await commentAPI.getCount(props.articleId)
    commentCount.value = countResponse.data.data || 0

    emit('update', { count: commentCount.value })
  } catch (error) {
// console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

// 提交评论
const submitComment = async () => {
  if (!props.articleId || !newComment.value.content.trim()) {return}

  const token = appContext?.$toolUtil?.storageGet('Token')
  if (!token) {
    message.warning('请先登录')
    return
  }

  submitting.value = true
  try {
    if (replyingToComment.value) {
      await commentAPI.reply({
        contentId: props.articleId,
        parentId: replyingToComment.value.id!,
        replyContent: newComment.value.content
      })
      message.success('回复成功')
    } else {
      await commentAPI.create({
        contentId: props.articleId,
        content: newComment.value.content
      })
      message.success('评论成功')
    }

    newComment.value.content = ''
    replyingToComment.value = null
    await loadComments()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '评论失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 切换回复状态
const toggleReply = (comment: Comment) => {
  if (replyingToComment.value?.id === comment.id) {
    cancelReply()
  } else {
    replyingToComment.value = comment
  }
}

// 取消回复
const cancelReply = () => {
  replyingToComment.value = null
  newComment.value.content = ''
}

// 点赞/点踩评论
const likeComment = async (comment: Comment, actionTy: 2 | 3) => {
  if (!comment.id) {return}

  try {
    const isLike = actionType === 2
    const isDislike = actionType === 3

    if (isLike && comment.isLiked) {
      await interactionAPI.cancelLike({
        contentId: comment.id,
        actionType: 2,
        tableName: 'comment'
      })
      comment.isLiked = false
      comment.likeCount = (comment.likeCount || 0) - 1
    } else if (isDislike && comment.isDisliked) {
      await interactionAPI.cancelLike({
        contentId: comment.id,
        actionType: 3,
        tableName: 'comment'
      })
      comment.isDisliked = false
    } else if (isLike) {
      await interactionAPI.like({
        contentId: comment.id,
        actionType: 2,
        tableName: 'comment'
      })
      comment.isLiked = true
      comment.likeCount = (comment.likeCount || 0) + 1
      if (comment.isDisliked) {
        comment.isDisliked = false
      }
    } else if (isDislike) {
      await interactionAPI.like({
        contentId: comment.id,
        actionType: 3,
        tableName: 'comment'
      })
      comment.isDisliked = true
      if (comment.isLiked) {
        comment.isLiked = false
        comment.likeCount = (comment.likeCount || 0) - 1
      }
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 删除评论
const deleteComment = async (comment: Comment) => {
  if (!comment.id) {return}

  try {
    await new Promise<void>((resolve, reject) => {
      dialog.warning({
        title: '提示',
        content: '确定要删除这条评论吗？',
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: () => {
          resolve()
        },
        onNegativeClick: () => {
          reject('cancel')
        },
        onClose: () => {
          reject('cancel')
        }
      })
    })

    const response = await commentAPI.delete(comment.id)

    if (response.data.code === 0 || response.data.success) {
      message.success('删除成功')
      setTimeout(async () => {
        await loadComments()
      }, 300)
    } else {
      message.error(response.data.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      message.error('删除失败')
    }
  }
}

// 判断是否有权限删除评论
const canDeleteComment = (comment: Comment): boolean => {
  if (!props.currentUserId) {return false}
  if (props.isAdmin) {return true}
  if (props.articleAuthorId && String(props.articleAuthorId) === String(props.currentUserId)) {return true}
  if (String(comment.userId) === String(props.currentUserId)) {return true}
  return false
}

onMounted(() => {
  loadComments()
})

defineExpose({
  refresh: loadComments
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
    .comments-loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px 0;
      color: #999;
      gap: 10px;
    }

    .empty-comments {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px 0;
      color: #999;
      gap: 10px;
    }

    .comment-list {
      .comment-item {
        display: flex;
        gap: 15px;
        padding: 20px 0;
        border-bottom: 1px solid #f0f0f0;

        &:last-child {
          border-bottom: none;
        }

        .comment-avatar {
          flex-shrink: 0;
        }

        .comment-content {
          flex: 1;

          .comment-info {
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
          }

          .comment-text {
            font-size: 14px;
            line-height: 1.6;
            color: #333;
            margin-bottom: 10px;
            word-break: break-word;
          }

          .comment-actions-bar {
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

          .comment-replies {
            margin-top: 15px;
            padding-left: 15px;
            border-left: 2px solid #f0f0f0;

            .reply-item {
              display: flex;
              gap: 10px;
              padding: 12px 0;

              &:not(:last-child) {
                border-bottom: 1px solid #f5f5f5;
              }

              .reply-avatar {
                flex-shrink: 0;
              }

              .reply-content {
                flex: 1;

                .reply-info {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  margin-bottom: 6px;

                  .reply-author {
                    font-weight: 500;
                    color: #409EFF;
                    font-size: 13px;
                  }

                  .reply-time {
                    font-size: 11px;
                    color: #999;
                  }
                }

                .reply-text {
                  font-size: 13px;
                  line-height: 1.5;
                  color: #333;
                  margin-bottom: 8px;
                }

                .reply-actions-bar {
                  display: flex;
                  gap: 15px;
                }
              }
            }
          }
        }
      }
    }
  }
}
</style>
