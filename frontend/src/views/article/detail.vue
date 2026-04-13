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
      class="article-layout"
    >
      <!-- 主内容区 -->
      <div class="article-main">
        <div class="article-content">
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
              <div class="meta-item">
                <Icon
                  icon="ri:version-line"
                  width="16"
                />
                <span>版本 {{ article.majorVersion || 1 }}.{{ article.minorVersion || 0 }}</span>
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

          <!-- 文章内容 - 使用只读编辑器 -->
          <div
            v-if="article?.content"
            class="article-content-body"
          >
            <EditorContent
              v-if="editor"
              :editor="editor"
            />
          </div>

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
      </div>

      <!-- 右侧边栏组件 -->
      <ArticleSidebar
        :is-author="isCurrentUser"
        :article="article"
        :edit-mode="editMode"
        :pending-suggestions-count="pendingSuggestionsCount"
        :contributors="contributors"
        @edit="editArticle"
        @suggest="submitSuggestion"
        @versions="viewVersionHistory"
        @review-suggestions="reviewSuggestions"
        @update:editMode="handleEditModeUpdate"
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import {useRoute, useRouter} from 'vue-router'
import { useBackNavigation } from '@/utils/backNavigation'
import { useMessage, useDialog } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ArticleInteractionBar from '@/components/article/ArticleInteractionBar.vue'
import ArticleSidebar from '@/components/article/ArticleSidebar.vue'
import CommentSection from '@/components/comment/CommentSection.vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import { articleAPI, type Article } from '@/api/article'
import { interactionAPI } from '@/api/interaction'
import { getAvatarUrl } from '@/utils/userUtils'
import { useGlobalProperties } from '@/utils/globalProperties'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from "@tiptap/starter-kit"
import Image from "@tiptap/extension-image"
import Link from "@tiptap/extension-link"
import { FileNodeExtension } from '@/utils/tiptap-file-node'
import { ShareCardNodeExtension } from '@/utils/tiptap-share-card-node'
import { VideoNodeExtension } from '@/utils/tiptap-video-node'
import { VideoAnnotationRefExtension } from '@/utils/tiptap-video-annotation-ref'
import { articleContributorAPI } from '@/api/contributor'
import { useVisitedStore } from '@/stores/visited'
import { normalizeFileUrl } from '@/utils/fileUrl'

const appContext = useGlobalProperties()
const router = useRouter()
const route = useRoute()
const { navigateWithBackUrl, goBack: backNavigation } = useBackNavigation()
const dialog = useDialog()
const message = useMessage()
const visitedStore = useVisitedStore()

// 编辑器扩展配置 (避免重复)
const extensions = [
  StarterKit.configure({ link: false }),
  Image,
  Link.configure({
    openOnClick: false
  }),
  FileNodeExtension,
  ShareCardNodeExtension,
  VideoNodeExtension,
  VideoAnnotationRefExtension
]

// 创建只读编辑器
const editor = useEditor({
  extensions,
  editable: false,
  content: ''
})

/**
 * 格式化日期
 */
const formatDate = (dateString: string | null): string => {
  if (!dateString) {return ''}
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 响应式数据
const loading = ref<boolean>(true)
const article = ref<Article | null>(null)
const isCurrentUser = ref<boolean>(false)
const currentUserAvatar = ref<string>('')
const currentUserId = ref<string | number>('')
const isAdmin = ref<boolean>(false)
const editMode = ref<number>(0)
const pendingSuggestionsCount = ref<number>(0)
const contributors = ref([])
const totalContributors = ref(0)

// 计算属性
const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')

/**
 * 获取封面图片 URL
 */
const getCoverImageUrl = (): string => {
  if (!article.value?.coverUrl) {return ''}

  const coverUrl = article.value.coverUrl.split(',')[0]

  // 使用统一的 URL 处理工具
  return normalizeFileUrl(coverUrl, baseUrl.value)
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

    // 获取当前版本号（后端已正常返回）

    // 设置编辑模式
    editMode.value = article.value?.editMode || 0

    // 设置编辑器内容
    if (article.value?.content && editor.value) {
      editor.value.commands.setContent(article.value.content)
    }

    // 设置全局变量，供分享组件使用
    if (article.value) {
      window.detailArticleData = {
        title: article.value.title,
        coverUrl: getCoverImageUrl(),
        authorNickname: article.value.authorNickname,
        publishTime: article.value.publishTime,
        id: article.value.id,
        version: `${article.value.majorVersion || 1}.${article.value.minorVersion || 0}`
      }

    }

    // 检查是否为当前用户
    const userId = appContext?.$toolUtil?.storageGet('userid')
    isCurrentUser.value = article.value ? String(article.value.authorId) === String(userId) : false

    // 加载待审核建议数量（仅作者）
    if (isCurrentUser.value) {
      loadPendingSuggestionsCount()
    }

    // 加载贡献者列表
    loadContributors()

  } catch (error) {
    console.error('❌ [加载文章失败] error:', error)
    message.error('加载文章失败')
    article.value = null
    // 清除全局变量
    window.detailArticleData = undefined
  } finally {
    loading.value = false
  }
}

/**
 * 加载待审核建议数量
 */
const loadPendingSuggestionsCount = async () => {
  try {
    // TODO: 调用 API 获取该文章的待审核建议数量
    // const response = await articleSuggestionAPI.getList(article.value!.id, { status: 0, page: 1, limit: 1 })
    // pendingSuggestionsCount.value = response.data.data?.total || 0
    pendingSuggestionsCount.value = 0 // 临时值
  } catch (error) {
    console.error('加载待审核数量失败:', error)
  }
}

/**
 * 加载贡献者列表
 */
const loadContributors = async () => {
  if (!article.value?.id) {
    return
  }
  try {
    const response = await articleContributorAPI.getList(article.value?.id, {
      limit: 5,
      orderBy: 'score'
    })

    if (response.data.code === 0) {
      contributors.value = response.data.data.list || response.data.data
      totalContributors.value = response.data.data.total || contributors.value.length
    }
  } catch (error) {
    console.error('加载贡献者列表失败:', error)
  }
}

/**
 * 处理编辑模式更新
 */
const handleEditModeUpdate = async (key: number) => {
  try {
    // 调用 API 更新编辑模式
    await articleAPI.updateEditMode(article.value!.id, key)

    editMode.value = key
    message.success('编辑模式已更新')

    // 刷新页面
    setTimeout(() => {
      location.reload()
    }, 500)
  } catch (error) {
    console.error('更新编辑模式失败:', error)
    message.error('更新编辑模式失败')
  }
}

/**
 * 查看版本历史
 */
const viewVersionHistory = () => {
  if (!article.value?.id) {return}

  navigateWithBackUrl({
    path: '/index/article/versions',
    query: { articleId: article.value.id }
  })
}

/**
 * 审核建议
 */
const reviewSuggestions = () => {
  if (!article.value?.id) {return}
  navigateWithBackUrl(`/index/article/suggestions?articleId=${article.value.id}&status=0`)
}

/**
 * 提交修改建议
 */
const submitSuggestion = () => {
  if (!article.value?.id) {return}

  navigateWithBackUrl(`/index/article/${article.value.id}/suggest`)
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

  message.success('开始下载附件')
}

/**
 * 编辑文章
 */
const editArticle = () => {
  if (!article.value?.id) {return}

  navigateWithBackUrl({
    path: '/index/article/editor',
    query: { id: article.value.id }
  })
}

/**
 * 返回上一页
 */
const goBack = () => {
  backNavigation()
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
  loadContributors()
  incrementViewCount()
})

/**
 * 组件卸载时
 */
onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.destroy()
  }
})

/**
 * 增加文章浏览量
 */
const incrementViewCount = () => {
  const id = route.query.id as string
  if (!id) {return}

  // 前端去重：检查是否已经计数
  if (!visitedStore.hasViewed(id)) {
    articleAPI.incrementViewCount(id)
      .then(() => {
        // 标记为已访问
        visitedStore.markViewed(id)
        // 可选：更新本地浏览量显示
        if (article.value) {
          article.value.viewCount = (article.value.viewCount || 0) + 1
        }
      })
      .catch((error) => {
        console.error('增加浏览量失败:', error)
      })
  }
}
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

.article-layout {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.article-main {
  min-width: 0;
}

.article-content {
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

  :deep(.ProseMirror) {
    padding: 0;

    p {
      margin-bottom: 16px;
    }

    h1, h2, h3, h4, h5, h6 {
      margin-top: 24px;
      margin-bottom: 16px;
      font-weight: 600;
    }

    ul, ol {
      padding-left: 20px;
      margin-bottom: 16px;
    }

    blockquote {
      margin: 16px 0;
      padding: 10px 20px;
      background: #f5f7fa;
      border-left: 4px solid #409EFF;
      border-radius: 4px;
    }

    code {
      padding: 2px 6px;
      background: #f5f7fa;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
    }

    pre {
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

    table {
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

    img {
      max-width: 100%;
      height: auto;
      margin: 10px 0;
      border-radius: 4px;
    }
  }

  // 视频节点样式
  :deep(.video-node-wrapper) {
    margin: 16px 0;

    .video-container {
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    }
  }

  // 文件节点样式
  :deep(.file-node) {
    display: inline-flex;
    align-items: center;
    padding: 8px 12px;
    background: #f5f7fa;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    margin: 8px 0;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: #ecf5ff;
      border-color: #409eff;
    }

    .file-icon {
      font-size: 24px;
      margin-right: 8px;
      color: #409eff;
    }

    .file-info {
      flex: 1;
      overflow: hidden;

      .file-name {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .file-size {
        font-size: 12px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }

  // 分享卡片节点样式
  :deep(.share-card-node) {
    display: block;
    margin: 16px 0;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .share-card-cover {
      width: 100%;
      height: 200px;
      object-fit: cover;
    }

    .share-card-content {
      padding: 16px;

      .share-card-title {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
      }

      .share-card-description {
        font-size: 14px;
        color: #666;
        line-height: 1.6;
      }
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
