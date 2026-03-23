<template>
  <div class="article-detail-container">
    <PageHeader
      title="文章详情"
      @back="goBack"
    />

    <!-- 加载状态 - 使用骨架屏 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <div class="skeleton-header">
        <n-skeleton
          text
          :repeat="1"
        />
        <n-skeleton
          text
          style="width: 60%"
        />
      </div>

      <div class="skeleton-meta">
        <n-skeleton
          circular
          size="small"
        />
        <n-skeleton
          text
          style="width: 150px"
        />
        <n-skeleton
          text
          style="width: 100px"
        />
      </div>

      <div class="skeleton-cover">
        <n-skeleton text />
      </div>

      <div class="skeleton-content">
        <n-skeleton
          text
          :repeat="3"
        />
        <n-skeleton
          text
          :repeat="2"
        />
        <n-skeleton
          text
          :repeat="4"
        />
      </div>

      <div class="skeleton-actions">
        <n-skeleton
          text
          :repeat="5"
        />
      </div>
    </div>

    <!-- 文章内容 -->
    <div
      v-else-if="article"
      class="article-content"
    >
      <!-- 文章头部 -->
      <div class="article-header">
        <h1 class="article-title">
          {{ article.title }}
        </h1>

        <div class="article-meta">
          <div class="meta-item">
            <UserAvatarLink
              :user-id="article.authorId"
              :nickname="article.authorNickname || '匿名用户'"
              :avatar="article.authorAvatar"
              :size="40"
            />
          </div>
          <div class="meta-item">
            <Icon
              icon="ri:time-line"
              width="16"
            />
            <span>{{ formatDate(article.publishTime || article.createTime) }}</span>
          </div>
          <div class="meta-item">
            <Icon
              icon="ri:eye-line"
              width="16"
            />
            <span>{{ article.viewCount || 0 }} 阅读</span>
          </div>
          <div class="meta-item">
            <Icon
              icon="ri:star-line"
              width="16"
            />
            <span>{{ article.favoriteCount || 0 }} 收藏</span>
          </div>
        </div>
      </div>

      <!-- 封面图 -->
      <div
        v-if="article.coverUrl"
        class="cover-image-wrapper"
      >
        <n-image
          :src="getCoverImageUrl()"
          object-fit="cover"
          class="cover-image"
          @error="handleCoverError"
        />
      </div>

      <!-- 分类标签 -->
      <div class="category-tag">
        <n-tag type="info">
          {{ article.categoryName || article.categoryStrName || '未分类' }}
        </n-tag>
      </div>

      <!-- 编辑按钮（仅作者可见） -->
      <div
        v-if="isCurrentUser"
        class="edit-button-wrapper"
      >
        <n-button
          type="primary"
          @click="editArticle"
        >
          <template #icon>
            <Icon
              icon="ri:edit-line"
              width="18"
            />
          </template>
          编辑此文章
        </n-button>
      </div>

      <!-- 文章内容 -->
      <div
        class="article-content-body"
        v-html="article.content"
      />

      <!-- 附件 -->
      <div
        v-if="article.attachment"
        class="attachment-section"
      >
        <div class="attachment-title">
          <Icon
            icon="ri:file-line"
            width="18"
          />
          <span>附件</span>
        </div>
        <n-button
          type="primary"
          text
          @click="downloadAttachment"
        >
          下载附件
        </n-button>
      </div>

      <!-- 互动操作区 -->
      <ArticleInteractionBar
        v-if="article?.id"
        :article-id="article.id"
        :like-count="article.likeCount"
        :favorite-count="article.favoriteCount"
        @update="handleInteractionUpdate"
      />

      <!-- 审核状态（仅管理员可见） -->
      <div
        v-if="article.auditStatus && article.auditStatus !== '1'"
        class="audit-status"
      >
        <n-alert
          :type="article.auditStatus === '2' ? 'error' : 'warning'"
          :closable="false"
        >
          {{ getAuditStatusText(article.auditStatus) }}
        </n-alert>
        <div
          v-if="article.auditReply"
          class="audit-reply"
        >
          <strong>审核回复：</strong>{{ article.auditReply }}
        </div>
      </div>

      <!-- 评论区 -->
      <CommentSection
        v-if="article?.id"
        :article-id="article.id"
        :current-user-id="currentUserId"
        :user-avatar="currentUserAvatar"
        :is-admin="isAdmin"
        :article-author-id="article.authorId"
        @update="handleCommentUpdate"
      />
    </div>

    <!-- 空状态 -->
    <div
      v-else
      class="empty-container"
    >
      <n-empty description="文章不存在或已被删除" />
      <n-button
        type="primary"
        @click="goBack"
      >
        返回首页
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import { Icon } from '@iconify/vue'
import { getAvatarUrl, formatDate, getAuditStatusText } from '@/utils/userUtils'
import {Article, articleAPI} from '@/api/article'
import { useGlobalProperties } from '@/utils/globalProperties'
import ArticleInteractionBar from '@/components/article/ArticleInteractionBar.vue'
import CommentSection from '@/components/comment/CommentSection.vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import { NSkeleton, NImage, NTag, NButton, NAlert, NEmpty } from 'naive-ui'
import PageHeader from '@/components/common/PageHeader.vue'

// 获取全局上下文
const appContext = useGlobalProperties()
const route = useRoute()
const router = useRouter()
const instance = getCurrentInstance()
const $message = instance?.appContext.config.globalProperties.$message
// 响应式数据
const loading = ref<boolean>(true)
const article = ref<Article | null>(null)
const isCurrentUser = ref<boolean>(false)
const currentUserAvatar = ref<string>('')
const currentUserId = ref<string | number>('')
const isAdmin = ref<boolean>(false)

// 计算属性
const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')

/**
 * 获取封面图片 URL
 */
const getCoverImageUrl = (): string => {
  if (!article.value?.coverUrl) {return ''}

  const coverUrl = article.value.coverUrl.split(',')[0]

  // 如果已经是完整 URL，直接返回
  if (coverUrl.startsWith('http://') || coverUrl.startsWith('https://')) {
   return coverUrl
  }

  // 拼接完整 URL
  return `${baseUrl.value}/${coverUrl}`
}

/**
 * 处理封面图加载失败
 */
const handleCoverError = () => {
// console.error('封面图加载失败')
}

/**
 * 加载文章详情
 */
const loadArticleDetail = async () => {
  loading.value = true
  try {
    const id = route.query.id as string
    if (!id) {
      throw new Error('缺少文章 ID')
    }

    const response = await articleAPI.getById(id)
    article.value = response.data.data

    // 设置全局变量，供分享组件使用
    if (article.value) {
      window.detailArticleData = {
        title: article.value.title,
        coverUrl: getCoverImageUrl(),
        authorNickname: article.value.authorNickname,
        publishTime: article.value.publishTime,
        id: article.value.id
      }

    }

    // 检查是否为当前用户
    const userId = appContext?.$toolUtil?.storageGet('userid')
    isCurrentUser.value = article.value ? String(article.value.authorId) === String(userId) : false

  } catch (error) {
    console.error('加载文章失败:', error)
    $message?.error('加载文章失败')
    article.value = null
    // 清除全局变量
    window.detailArticleData = undefined
  } finally {
    loading.value = false
  }
}

/**
 * 处理互动状态更新
 */
const handleInteractionUpdate = (data: {
  isLiked: boolean
  isDisliked: boolean
  isFavorited: boolean
  likeCount?: number
  dislikeCount?: number
  favoriteCount?: number
}) => {

  // 更新文章的点赞数和收藏数
  if (article.value) {
    if (data.likeCount !== undefined) {
      article.value.likeCount = data.likeCount
    }
    if (data.dislikeCount !== undefined) {
      article.value.dislikeCount = data.dislikeCount
    }
    if (data.favoriteCount !== undefined) {
      article.value.favoriteCount = data.favoriteCount
    }
  }
}

/**
 * 处理评论数量更新
 */
const handleCommentUpdate = (data: { count: number }) => {

}

/**
 * 下载附件
 */
const downloadAttachment = () => {
  if (!article.value?.attachment) {return}

  const attachment = article.value.attachment
  const fileName = attachment.split('/').pop() || 'attachment'

  const link = document.createElement('a')
  link.href = baseUrl.value + attachment
  link.download = fileName
  link.click()

  $message?.success('开始下载附件')
}

/**
 * 编辑文章
 */
const editArticle = () => {
  if (!article.value?.id) {return}
  router.push(`/index/article/editor?id=${article.value.id}`)
}

/**
 * 返回上一页
 */
const goBack = () => {
  router.back()
}

/**
 * 获取当前用户头像
 */
const getCurrentUserAvatar = () => {
  const avatar = appContext?.$toolUtil?.storageGet('avatar')
  const userId = appContext?.$toolUtil?.storageGet('userid')
  const userRole = appContext?.$toolUtil?.storageGet('role')
  currentUserAvatar.value = avatar ? getAvatarUrl(avatar) : '/default-avatar.png'
  currentUserId.value = userId || ''
  // 假设角色为 1 的是管理员，根据实际情况调整
  isAdmin.value = userRole === '2' || userRole === 2 || false
}

/**
 * 组件挂载时
 */
onMounted(() => {
  loadArticleDetail()
  getCurrentUserAvatar()
})
</script>

<style lang="scss" scoped>
.loading-container {
  padding: 40px;
  background: #fff;
  border-radius: 12px;

  .skeleton-header {
    margin-bottom: 20px;
  }

  .skeleton-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 30px;
  }

  .skeleton-cover {
    margin-bottom: 30px;
    height: 400px;
  }

  .skeleton-content {
    margin-bottom: 30px;
  }

  .skeleton-actions {
    display: flex;
    gap: 20px;
    justify-content: center;
  }
}

.article-detail-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 20px;
}

.article-content {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.article-header {
  margin-bottom: 30px;

  .article-title {
    font-size: 28px;
    font-weight: 600;
    color: #333;
    margin-bottom: 20px;
    line-height: 1.4;
  }

  .article-meta {
    display: flex;
    align-items: center;
    gap: 20px;
    flex-wrap: wrap;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #666;
      font-size: 14px;

    }
  }
}

.cover-image-wrapper {
  width: 100%;
  height: 400px;
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;

  .cover-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.category-tag {
  margin-bottom: 30px;
}

.article-content-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  min-height: 300px;

  :deep(img) {
    max-width: 100%;
    height: auto;
    margin: 10px 0;
    border-radius: 4px;
  }

  :deep(p) {
    margin-bottom: 16px;
  }

  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin-top: 24px;
    margin-bottom: 16px;
    font-weight: 600;
  }

  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin-bottom: 16px;
  }

  :deep(blockquote) {
    margin: 16px 0;
    padding: 10px 20px;
    background: #f5f7fa;
    border-left: 4px solid #409EFF;
    border-radius: 4px;
  }

  :deep(code) {
    padding: 2px 6px;
    background: #f5f7fa;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
  }

  :deep(pre) {
    padding: 16px;
    background: #282c34;
    color: #abb2bf;
    border-radius: 6px;
    overflow-x: auto;
    margin: 16px 0;

    code {
      background: transparent;
      padding: 0;
      color: inherit;
    }
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;

    th, td {
      border: 1px solid #dcdfe6;
      padding: 12px;
      text-align: left;
    }

    th {
      background: #f5f7fa;
      font-weight: 600;
    }
  }
}

.attachment-section {
  margin: 30px 0;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;

  .attachment-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
    color: #666;
  }
}

.interaction-bar {
  display: flex;
  gap: 30px;
  padding: 20px 0;
  margin-top: 30px;
  border-top: 1px solid #eee;
  justify-content: center;

  .interaction-item {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 10px 20px;
    border-radius: 8px;
    transition: all 0.3s;
    color: #666;
    font-size: 14px;

    &:hover {
      background: #f5f7fa;
      color: #409EFF;
    }

    &.active {
      color: #67C23A;
      background: #f0f9eb;
    }

    .icon-active {
      color: #E6A23C;
    }

    .star-filled {
      fill: #E6A23C;
      color: #E6A23C;
    }
  }
}

.audit-status {
  margin-top: 30px;

  .audit-reply {
    margin-top: 10px;
    padding: 10px;
    background: #fef0f0;
    border-radius: 4px;
    color: #666;
  }
}

.edit-button-wrapper {
  margin: 20px 0;
  text-align: right;
}

@media (max-width: 768px) {
  .article-container {
    padding: 20px;
  }

  .article-title {
    font-size: 22px !important;
  }

  .cover-image-wrapper {
    height: 250px !important;
  }

  .article-content-body {
    font-size: 14px !important;
  }

  .interaction-bar {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
