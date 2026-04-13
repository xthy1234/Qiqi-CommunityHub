<template>
  <div class="editor-wrapper">
    <!-- 工具栏 -->
    <div
      v-if="editor"
      class="toolbar"
    >
      <!-- 撤销/重做 -->
      <n-button
        size="tiny"
        :disabled="!editor.can().undo()"
        quaternary
        title="撤销 (Ctrl+Z)"
        @click="editor.chain().focus().undo().run()"
      >
        <template #icon>
          <Icon
            icon="material-symbols:undo"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :disabled="!editor.can().redo()"
        quaternary
        title="重做 (Ctrl+Y)"
        @click="editor.chain().focus().redo().run()"
      >
        <template #icon>
          <Icon
            icon="material-symbols:redo"
            width="16"
          />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 标题选择器 -->
      <n-select
        v-model:value="currentHeading"
        :options="headingOptions"
        placeholder="正文"
        size="tiny"
        style="width: 100px"
        @update:value="setHeading"
      />

      <n-divider vertical />

      <!-- 基础样式 -->
      <n-button
        size="tiny"
        :type="editor.isActive('bold') ? 'primary' : ''"
        quaternary
        title="加粗 (Ctrl+B)"
        @click="toggleBold"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-bold"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('italic') ? 'primary' : ''"
        quaternary
        title="斜体 (Ctrl+I)"
        @click="toggleItalic"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-italic"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('underline') ? 'primary' : ''"
        quaternary
        title="下划线 (Ctrl+U)"
        @click="toggleUnderline"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-underlined"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('strike') ? 'primary' : ''"
        quaternary
        title="删除线"
        @click="toggleStrike"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-strikethrough"
            width="16"
          />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 列表 -->
      <n-button
        size="tiny"
        :type="editor.isActive('bulletList') ? 'primary' : ''"
        quaternary
        title="无序列表"
        @click="toggleBulletList"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-list-bulleted"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('orderedList') ? 'primary' : ''"
        quaternary
        title="有序列表"
        @click="toggleOrderedList"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-list-numbered"
            width="16"
          />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 引用和代码 -->
      <n-button
        size="tiny"
        :type="editor.isActive('blockquote') ? 'primary' : ''"
        quaternary
        title="引用块"
        @click="toggleBlockquote"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-quote"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('code') ? 'primary' : ''"
        quaternary
        title="行内代码"
        @click="toggleCode"
      >
        <template #icon>
          <Icon
            icon="material-symbols:code"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('codeBlock') ? 'primary' : ''"
        quaternary
        title="代码块"
        @click="toggleCodeBlock"
      >
        <template #icon>
          <Icon
            icon="material-symbols:code-blocks"
            width="16"
          />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 插入元素 -->
      <n-button
        size="tiny"
        quaternary
        title="插入链接 (Ctrl+K)"
        @click="openLinkDialog"
      >
        <template #icon>
          <Icon
            icon="material-symbols:link"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        quaternary
        title="插入图片"
        @click="openImageDialog"
      >
        <template #icon>
          <Icon
            icon="material-symbols:image"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        quaternary
        title="上传文件"
        @click="triggerFileUpload"
      >
        <template #icon>
          <Icon
            icon="material-symbols:upload-file"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        quaternary
        title="插入我的文章卡片"
        @click="openArticleSelector"
      >
        <template #icon>
          <Icon
            icon="material-symbols:article"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        quaternary
        title="插入视频"
        @click="openVideoDialog"
      >
        <template #icon>
          <Icon
            icon="material-symbols:video-file"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        quaternary
        title="上传视频"
        @click="triggerVideoUpload"
      >
        <template #icon>
          <Icon
            icon="material-symbols:upload"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        quaternary
        title="插入视频注释引用"
        @click="openAnnotationRefDialog"
      >
        <template #icon>
          <Icon
            icon="material-symbols:bookmark"
            width="16"
          />
        </template>
      </n-button>
      <input
        ref="fileInputRef"
        type="file"
        style="display: none"
        @change="handleFileInputChange"
      />
      <input
        ref="videoInputRef"
        type="file"
        accept="video/*"
        style="display: none"
        @change="handleVideoInputChange"
      />

      <n-divider vertical />

      <!-- 对齐方式 -->
      <n-button
        size="tiny"
        :type="editor.isActive({ textAlign: 'left' }) ? 'primary' : ''"
        quaternary
        title="左对齐"
        @click="editor.chain().focus().setTextAlign('left').run()"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-align-left"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive({ textAlign: 'center' }) ? 'primary' : ''"
        quaternary
        title="居中"
        @click="editor.chain().focus().setTextAlign('center').run()"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-align-center"
            width="16"
          />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive({ textAlign: 'right' }) ? 'primary' : ''"
        quaternary
        title="右对齐"
        @click="editor.chain().focus().setTextAlign('right').run()"
      >
        <template #icon>
          <Icon
            icon="material-symbols:format-align-right"
            width="16"
          />
        </template>
      </n-button>
    </div>

    <!-- 编辑器内容区 -->
    <editor-content
      :editor="editor"
      class="tiptap"
    />

    <!-- 链接插入对话框 -->
    <n-modal
      v-model:show="linkDialogVisible"
      preset="dialog"
      title="插入链接"
      :style="{ width: '400px' }"
    >
      <n-form
        :model="linkForm"
        label-placement="left"
        label-width="80px"
      >
        <n-form-item label="链接文本">
          <n-input
            v-model:value="linkForm.text"
            placeholder="选中的文本将作为链接文字"
            :disabled="!!selectedText"
          />
        </n-form-item>
        <n-form-item
          label="链接地址"
          required
        >
          <n-input
            v-model:value="linkForm.url"
            placeholder="https://example.com"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="linkDialogVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            @click="insertLink"
          >
            确定
          </n-button>
          <n-button
            v-if="editor?.isActive('link')"
            type="error"
            @click="removeLink"
          >
            移除链接
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 图片插入对话框 -->
    <n-modal
      v-model:show="imageDialogVisible"
      preset="dialog"
      title="插入图片"
      :style="{ width: '400px' }"
    >
      <n-form
        :model="imageForm"
        label-placement="left"
        label-width="80px"
      >
        <n-form-item
          label="图片 URL"
          required
        >
          <n-input
            v-model:value="imageForm.url"
            placeholder="https://example.com/image.jpg"
          />
        </n-form-item>
        <n-form-item label="替代文本">
          <n-input
            v-model:value="imageForm.alt"
            placeholder="图片描述（可选）"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="imageDialogVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            @click="insertImage"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分享卡片插入对话框 -->
    <n-modal
      v-model:show="shareCardDialogVisible"
      preset="dialog"
      title="选择要分享的文章"
      :style="{ width: '700px' }"
    >
      <div class="article-selector">
        <!-- 搜索框 -->
        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索文章标题..."
          clearable
          style="margin-bottom: 16px;"
        >
          <template #prefix>
            <Icon icon="ri:search-line" />
          </template>
        </n-input>

        <!-- 文章列表 -->
        <div class="article-list">
          <n-spin :show="loadingArticles">
            <n-empty
              v-if="!loadingArticles && filteredArticles.length === 0"
              description="暂无文章"
            />

            <div
              v-else
              class="article-grid"
            >
              <n-card
                v-for="article in filteredArticles"
                :key="article.id"
                class="article-item"
                size="small"
                :bordered="selectedArticle?.id === article.id"
                @click="selectArticle(article)"
              >
                <div class="article-card-content">
                  <!-- 封面图 -->
                  <div
                    v-if="article.coverUrl"
                    class="article-cover"
                  >
                    <n-image
                      :src="getArticleCoverUrl(article.coverUrl)"
                      object-fit="cover"
                      class="cover-img"
                      :preview-disabled="true"
                    />
                  </div>

                  <!-- 文章信息 -->
                  <div class="article-info">
                    <h3 class="article-title">
                      {{ article.title }}
                    </h3>
                    <p class="article-summary">
                      {{ article.summary || '暂无摘要' }}
                    </p>
                    <div class="article-meta">
                      <span class="meta-item">
                        <Icon icon="ri:eye-line" />
                        {{ article.viewCount || 0 }}
                      </span>
                      <span class="meta-item">
                        <Icon icon="ri:star-line" />
                        {{ article.favoriteCount || 0 }}
                      </span>
                      <span class="meta-item">
                        <Icon icon="ri:time-line" />
                        {{ formatDate(article.createTime) }}
                      </span>
                    </div>
                  </div>
                </div>
              </n-card>
            </div>
          </n-spin>
        </div>

        <!-- 分页 -->
        <div
          v-if="totalPages > 1"
          class="article-pagination"
        >
          <n-pagination
            v-model:page="currentPage"
            :page-count="totalPages"
            :page-size="pageSize"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update-page-size="handlePageSizeChange"
          />
        </div>
      </div>

      <template #action>
        <n-space justify="end">
          <n-button @click="shareCardDialogVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :disabled="!selectedArticle"
            @click="insertSelectedArticle"
          >
            插入文章卡片
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 视频插入对话框 -->
    <n-modal
      v-model:show="videoDialogVisible"
      preset="dialog"
      title="插入视频"
      :style="{ width: '500px' }"
    >
      <n-form
        :model="videoForm"
        label-placement="left"
        label-width="80px"
      >
        <n-form-item
          label="视频 URL"
          required
        >
          <n-input
            v-model:value="videoForm.src"
            placeholder="https://example.com/video.mp4"
          />
        </n-form-item>
        <n-form-item label="封面图">
          <n-input
            v-model:value="videoForm.poster"
            placeholder="https://example.com/poster.jpg（可选）"
          />
        </n-form-item>
        <n-form-item label="视频标题">
          <n-input
            v-model:value="videoForm.title"
            placeholder="视频标题（可选）"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="videoDialogVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            @click="insertVideo"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 视频注释引用选择器 -->
    <n-modal
      v-model:show="annotationRefDialogVisible"
      preset="dialog"
      title="选择视频注释引用"
      :style="{ width: '600px' }"
    >
      <div class="annotation-ref-selector">
        <n-alert
          type="info"
          :show-icon="false"
          style="margin-bottom: 16px;"
        >
          从当前文档的视频中选择要引用的注释
        </n-alert>

        <n-spin :show="loadingVideos">
          <n-empty
            v-if="!loadingVideos && availableAnnotations.length === 0"
            description="当前文档中没有可用的视频注释"
          />

          <div
            v-else
            class="annotation-list"
          >
            <n-card
              v-for="(item, index) in availableAnnotations"
              :key="`${item.videoNodeId}-${item.annotation.id}`"
              class="annotation-item"
              size="small"
              :bordered="selectedAnnotationRef?.annotation.id === item.annotation.id"
              @click="selectAnnotationRef(item)"
            >
              <div class="annotation-item-content">
                <div class="annotation-video-title">
                  <Icon
                    icon="material-symbols:video-file"
                    width="16"
                  />
                  <span>{{ item.videoTitle }}</span>
                </div>
                <div class="annotation-time">
                  {{ formatTimeRange(item.annotation.startTime, item.annotation.endTime) }}
                </div>
                <div class="annotation-title">
                  {{ item.annotation.title }}
                </div>
                <div
                  v-if="item.annotation.content"
                  class="annotation-content"
                >
                  {{ item.annotation.content }}
                </div>
              </div>
            </n-card>
          </div>
        </n-spin>
      </div>

      <template #action>
        <n-space justify="end">
          <n-button @click="annotationRefDialogVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :disabled="!selectedAnnotationRef"
            @click="insertAnnotationRef"
          >
            插入引用
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onBeforeUnmount, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { useMessage } from 'naive-ui'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Placeholder from '@tiptap/extension-placeholder'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import type { SelectOption } from 'naive-ui'
import { all, createLowlight } from 'lowlight'
import { FileNodeExtension } from '@/utils/tiptap-file-node'
import { ShareCardNodeExtension } from '@/utils/tiptap-share-card-node'
import { VideoNodeExtension } from '@/utils/tiptap-video-node'
import { VideoAnnotationRefExtension } from '@/utils/tiptap-video-annotation-ref'
import { uploadAPI } from '@/api/upload'
import { articleAPI, type Article } from '@/api/article'

const message = useMessage()

const props = defineProps<{
  modelValue?: object
}>()

const emit = defineEmits<{
  'update:modelValue': [value: object]
}>()

const currentHeading = ref('paragraph')
const linkDialogVisible = ref(false)
const imageDialogVisible = ref(false)
const shareCardDialogVisible = ref(false)
const videoDialogVisible = ref(false)
const annotationRefDialogVisible = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const userArticles = ref<Article[]>([])
const selectedArticle = ref<Article | null>(null)
const loadingArticles = ref(false)
const loadingVideos = ref(false)

interface AnnotationItem {
  id: string
  startTime: number
  endTime?: number
  title: string
  content: string
}

interface VideoWithAnnotations {
  videoNodeId: string
  videoTitle: string
  annotation: AnnotationItem
}

const availableAnnotations = ref<VideoWithAnnotations[]>([])
const selectedAnnotationRef = ref<VideoWithAnnotations | null>(null)

const linkForm = reactive({
  text: '',
  url: ''
})

const imageForm = reactive({
  url: '',
  alt: ''
})

const videoForm = reactive({
  src: '',
  poster: '',
  title: ''
})

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const isSettingContent = ref(false)

const filteredArticles = computed(() => {
  if (!searchKeyword.value) {
    return userArticles.value
  }
  return userArticles.value.filter(article =>
    article.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const selectedText = ref('')

const headingOptions: SelectOption[] = [
  { label: '标题 1', value: 'h1' },
  { label: '标题 2', value: 'h2' },
  { label: '标题 3', value: 'h3' },
  { label: '正文', value: 'paragraph' }
]

const lowlight = createLowlight(all)

/**
 * 格式化日期
 */
const formatDate = (dateString: string | null): string => {
  if (!dateString) {return ''}
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 获取文章封面 URL
 */
const getArticleCoverUrl = (coverUrl: string): string => {
  if (!coverUrl) {return ''}

  // 如果已经是完整 URL，直接返回
  if (coverUrl.startsWith('http://') || coverUrl.startsWith('https://')) {
    return coverUrl
  }

  // 拼接完整 URL
  const baseUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
  return `${baseUrl}/${coverUrl}`
}

/**
 * 格式化时间范围
 */
const formatTimeRange = (startTime: number, endTime?: number): string => {
  if (endTime && endTime > startTime) {
    return `${formatTime(startTime)}-${formatTime(endTime)}`
  }
  return formatTime(startTime)
}

/**
 * 格式化时间
 */
const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: { levels: [2, 3] },
      codeBlock: false,
      link: false,
      underline: false
    }),
    Image,
    Link,
    TextAlign,
    Underline,
    Color,
    Highlight,
    Placeholder.configure({
      placeholder: '开始编辑文章内容...'
    }),
    CodeBlockLowlight.configure({
      lowlight: createLowlight(all)
    }),
    FileNodeExtension,
    ShareCardNodeExtension,
    VideoNodeExtension,
    VideoAnnotationRefExtension
  ],
  content: props.modelValue || '',
  editorProps: {
    handleDOMEvents: {
      paste: (view, event) => {
        const items = event.clipboardData?.items
        if (!items) {return false}

        for (let i = 0; i < items.length; i++) {
          const item = items[i]

          // 处理图片粘贴
          if (item.type.startsWith('image/')) {
            const file = item.getAsFile()
            if (file) {
              event.preventDefault()
              handleImagePaste(file)
              return true
            }
          }

          // 处理视频粘贴
          if (item.type.startsWith('video/')) {
            const file = item.getAsFile()
            if (file) {
              event.preventDefault()
              handleVideoPaste(file)
              return true
            }
          }

          // 处理文档类文件粘贴（PDF、Word、Excel 等）
          if (isDocumentType(item.type)) {
            const file = item.getAsFile()
            if (file) {
              event.preventDefault()
              handleDocumentPaste(file)
              return true
            }
          }
        }
        return false
      },
      // 正确处理中文输入法的 composition 事件
      compositionstart: () => {
        if (editor.value) {
          editor.value.isComposing = true
        }
        return false
      },
      compositionend: () => {
        if (editor.value) {
          editor.value.isComposing = false
        }
        return false
      }
    }
  },
  onUpdate: ({ editor }) => {
    // 如果是 setContent 触发的更新，跳过 emit
    if (isSettingContent.value) {return}

    const json = editor.getJSON()
    emit('update:modelValue', json)

    if (editor.isActive('heading', { level: 2 })) {
      currentHeading.value = 'h2'
    } else if (editor.isActive('heading', { level: 3 })) {
      currentHeading.value = 'h3'
    } else {
      currentHeading.value = 'paragraph'
    }
  },
  onSelectionUpdate: ({ editor }) => {
    const { from, to } = editor.state.selection
    selectedText.value = editor.state.doc.textBetween(from, to)

    if (editor.isActive('link')) {
      const { href } = editor.getAttributes('link')
      linkForm.url = href || ''
      linkForm.text = selectedText.value
    }
  }
})

/**
 * 打开文章选择器并加载用户文章
 */
const openArticleSelector = async () => {
  shareCardDialogVisible.value = true
  selectedArticle.value = null
  searchKeyword.value = ''
  currentPage.value = 1

  await loadUserArticles()
}

/**
 * 加载用户文章列表
 */
const loadUserArticles = async () => {
  loadingArticles.value = true
  try {
    const userId = localStorage.getItem('userid')
    if (!userId) {
      message.warning('请先登录')
      shareCardDialogVisible.value = false
      return
    }

    // 调用 API 获取用户文章列表
    const response = await articleAPI.getList({
      page: currentPage.value,
      limit: pageSize.value,
      authorId: userId,
      orderBy: 'createTime',
      sortOrder: 'desc'
    })

    if (response.data.code === 0) {
      userArticles.value = response.data.data.list || []
      totalPages.value = response.data.data.totalPages || 1
    } else {
      message.error('加载文章列表失败')
    }
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 加载用户文章失败:', error)
    message.error('加载文章列表失败')
  } finally {
    loadingArticles.value = false
  }
}

/**
 * 选择文章
 */
const selectArticle = (article: Article) => {
  selectedArticle.value = article
}

/**
 * 插入选中的文章卡片
 */
const insertSelectedArticle = () => {
  if (!editor.value || !selectedArticle.value) {
    message.warning('请先选择一篇文章')
    return
  }

  // 使用选中文章的信息创建分享卡片
  editor.value.commands.setShareCard({
    title: selectedArticle.value.title,
    summary: selectedArticle.value.summary || '',
    cover: selectedArticle.value.coverUrl ? getArticleCoverUrl(selectedArticle.value.coverUrl) : '',
    url: `${window.location.origin}/index/articleDetail?id=${selectedArticle.value.id}`,
    author: selectedArticle.value.authorNickname || '匿名用户',
    publishTime: formatDate(selectedArticle.value.publishTime || selectedArticle.value.createTime)
  })

  shareCardDialogVisible.value = false
  message.success('文章卡片插入成功')
}

/**
 * 处理分页大小变化
 */
const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadUserArticles()
}

// 监听页码变化
watch(currentPage, () => {
  loadUserArticles()
})

watch(() => props.modelValue, (newVal: object) => {
  if (!editor.value || isSettingContent.value) {return}

  const currentJson = editor.value.getJSON()

  // 只有当内容真正不同时才更新
  if (JSON.stringify(newVal) !== JSON.stringify(currentJson)) {
    // 保存当前光标位置
    const { from, to } = editor.value.state.selection

    isSettingContent.value = true
    // 第二个参数 false 表示不自动聚焦，避免光标跳转
    editor.value.commands.setContent(newVal, false)

    // 恢复光标位置（如果位置仍然有效）
    setTimeout(() => {
      try {
        const docSize = editor.value.state.doc.content.size
        const validFrom = Math.min(from, docSize - 1)
        const validTo = Math.min(to, docSize - 1)
        editor.value.commands.setTextSelection({ from: validFrom, to: validTo })
      } catch (e) {
        // 如果光标位置无效，保持当前位置
      }
      isSettingContent.value = false
    }, 0)
  }
}, { deep: true })

const setHeading = (level: string) => {
  if (!editor.value) {return}

  if (level === 'paragraph') {
    editor.value.chain().focus().setParagraph().run()
  } else if (level === 'h2') {
    editor.value.chain().focus().toggleHeading({ level: 2 }).run()
  } else if (level === 'h3') {
    editor.value.chain().focus().toggleHeading({ level: 3 }).run()
  }
}

const toggleBold = () => {
  editor.value?.chain().focus().toggleBold().run()
}

const toggleItalic = () => {
  editor.value?.chain().focus().toggleItalic().run()
}

const toggleUnderline = () => {
  editor.value?.chain().focus().toggleUnderline().run()
}

const toggleStrike = () => {
  editor.value?.chain().focus().toggleStrike().run()
}

const toggleBulletList = () => {
  editor.value?.chain().focus().toggleBulletList().run()
}

const toggleOrderedList = () => {
  editor.value?.chain().focus().toggleOrderedList().run()
}

const toggleBlockquote = () => {
  editor.value?.chain().focus().toggleBlockquote().run()
}

const toggleCode = () => {
  editor.value?.chain().focus().toggleCode().run()
}

const toggleCodeBlock = () => {
  if (!editor.value) {return}
  editor.value.chain().focus().toggleCodeBlock().run()
}

/**
 * 判断是否为文档类型文件
 */
const isDocumentType = (mimeType: string): boolean => {
  const documentTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'text/plain',
    'application/zip',
    'application/x-rar-compressed',
    'application/x-7z-compressed'
  ]

  return documentTypes.includes(mimeType) || mimeType.startsWith('application/')
}

/**
 * 处理文档类文件粘贴
 */
const handleDocumentPaste = async (file: File) => {
  try {
    message.loading('文件上传中...', { duration: 0 })
    const response = await uploadAPI.uploadAnyFile(file)

    if (!response) {
      throw new Error('文件上传失败')
    }

    // 插入文件节点，response 是 downloadUrl (/api/files/{id}/download)
    editor.value.commands.setFileNode({
      src: response,
      name: file.name,
      size: file.size,
      mimeType: file.type,
      extension: file.name.split('.').pop() || ''
    })

    message.destroyAll()
    message.success('文件粘贴成功')
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 粘贴文件失败:', error)
    message.destroyAll()
    message.error('文件粘贴失败')
  }
}

const handleImagePaste = async (file: File) => {
  try {
    message.loading('图片上传中...', { duration: 0 })
    const response = await uploadAPI.uploadImage(file)

    if (!response) {
      throw new Error('图片上传失败')
    }

    // 新版 API 返回 viewUrl (/api/files/123/view)
    editor.value.chain().focus().setImage({ src: response }).run()
    message.destroyAll()
    message.success('图片粘贴成功')
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 粘贴图片失败:', error)
    message.destroyAll()
    message.error('图片粘贴失败')
  }
}

/**
 * 处理视频粘贴
 */
const handleVideoPaste = async (file: File) => {
  try {
    message.loading('视频上传中...', { duration: 0 })
    const response = await uploadAPI.uploadVideo(file)

    if (!response) {
      throw new Error('视频上传失败')
    }

    // 插入视频节点，response 是 viewUrl (/api/files/123/view)
    editor.value.commands.setVideo({
      src: response,
      title: file.name,
      duration: 0,
      annotations: []
    })

    message.destroyAll()
    message.success('视频粘贴成功')
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 粘贴视频失败:', error)
    message.destroyAll()
    message.error('视频粘贴失败')
  }
}

const openLinkDialog = () => {
  if (!editor.value) {return}

  if (!selectedText.value && !editor.value.isActive('link')) {
    message.warning('请先选中要添加链接的文字')
    return
  }

  linkForm.text = selectedText.value
  linkForm.url = ''

  if (editor.value.isActive('link')) {
    const { href } = editor.value.getAttributes('link')
    linkForm.url = href || ''
  }

  linkDialogVisible.value = true
}

const insertLink = () => {
  if (!editor.value || !linkForm.url) {return}

  if (!selectedText.value && !linkForm.text) {
    linkForm.text = linkForm.url
  }

  editor.value
    .chain()
    .focus()
    .extendMarkRange('link')
    .insertContent({
      type: 'text',
      text: linkForm.text,
      marks: [
        {
          type: 'link',
          attrs: {
            href: linkForm.url
          }
        }
      ]
    })
    .setLink({ href: linkForm.url })
    .run()

  linkDialogVisible.value = false
}

const removeLink = () => {
  if (!editor.value) {return}
  editor.value.chain().focus().unsetLink().run()
  linkDialogVisible.value = false
}

const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

const handleFileInputChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {return}

  try {
    const response = await uploadAPI.uploadAnyFile(file)

    if (!response) {
      throw new Error('文件上传失败')
    }

    // 插入文件节点
    editor.value.commands.setFileNode({
      src: response,
      name: file.name,
      size: Number(file.size),
      mimeType: String(file.type),
      extension: file.name.split('.').pop() || ''
    })
    message.success('文件上传成功')
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 文件上传失败:', error)
    message.error(`文件上传失败：${error.message}`)
  } finally {
    if (target) {
      target.value = ''
    }
  }
}

const openImageDialog = () => {
  imageForm.url = ''
  imageForm.alt = ''
  imageDialogVisible.value = true
}

const insertImage = () => {
  if (!editor.value || !imageForm.url) {return}

  editor.value
    .chain()
    .focus()
    .setImage({
      src: imageForm.url,
      alt: imageForm.alt
    })
    .run()

  imageDialogVisible.value = false
}

const handleUpload = async (file: File) => {
  if (!editor.value) {return}

  try {
    const response = await uploadAPI.uploadAnyFile(file)

    if (!response) {
      throw new Error('文件上传失败')
    }

    // 【修复】使用正确的命令名 setFileNode
    editor.value.commands.setFileNode({
      src: response,
      name: file.name,
      size: file.size,
      mimeType: file.type,
      extension: file.name.split('.').pop() || ''
    })

    message.success('文件上传成功')
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 文件上传失败:', error)
    message.error(`文件上传失败：${error.message}`)
  }
}

const openShareCardDialog = () => {

}

const insertShareCard = () => {

}

const openVideoDialog = () => {
  if (!editor.value) {return}

  videoForm.src = ''
  videoForm.poster = ''
  videoForm.title = ''
  videoDialogVisible.value = true
}

const insertVideo = async () => {
  if (!editor.value) {return}

  if (!videoForm.src) {
    message.error('请输入视频地址')
    return
  }

  // 如果是本地上传的视频，videoForm.src 已经是 /api/files/{id} 格式
  // 如果是在线视频链接，直接使用
  editor.value.commands.setVideo({
    src: videoForm.src,
    poster: videoForm.poster || undefined,
    title: videoForm.title || '',
    duration: 0,
    annotations: []
  })

  videoDialogVisible.value = false
  message.success('视频插入成功')
}

const triggerVideoUpload = () => {
  videoInputRef.value?.click()
}

const handleVideoInputChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {return}

  try {
    message.loading('视频上传中...', { duration: 0 })
    const response = await uploadAPI.uploadVideo(file)

    if (!response) {
      throw new Error('视频上传失败')
    }

    // 插入视频节点
    editor.value.commands.setVideo({
      src: response,
      title: file.name,
      duration: 0,
      annotations: []
    })

    message.destroyAll()
    message.success('视频上传成功')
  } catch (error: any) {
    console.error('❌ [RichTextEditor] 视频上传失败:', error)
    message.destroyAll()
    message.error(`视频上传失败：${error.message}`)
  } finally {
    if (target) {
      target.value = ''
    }
  }
}

/**
 * 打开视频注释引用选择器
 */
const openAnnotationRefDialog = () => {
  if (!editor.value) {return}

  // 扫描文档中的所有视频节点及其注释
  scanVideoAnnotations()

  if (availableAnnotations.value.length === 0) {
    message.warning('当前文档中没有可用的视频注释')
    return
  }

  selectedAnnotationRef.value = null
  annotationRefDialogVisible.value = true
}

/**
 * 扫描文档中的视频注释
 */
const scanVideoAnnotations = () => {
  if (!editor.value) {return}

  loadingVideos.value = true
  availableAnnotations.value = []

  const doc = editor.value.state.doc

  doc.descendants((node: any) => {
    if (node.type.name === 'videoNode') {
      const annotations = node.attrs.annotations || []
      const videoTitle = node.attrs.title || '未命名视频'
      const videoNodeId = node.attrs.id || `video_${Date.now()}`

      annotations.forEach((annotation: AnnotationItem) => {
        availableAnnotations.value.push({
          videoNodeId,
          videoTitle,
          annotation
        })
      })
    }
  })

  loadingVideos.value = false
}

/**
 * 选择要引用的注释
 */
const selectAnnotationRef = (item: VideoWithAnnotations) => {
  selectedAnnotationRef.value = item
}

/**
 * 插入视频注释引用
 */
const insertAnnotationRef = () => {
  if (!editor.value || !selectedAnnotationRef.value) {
    message.warning('请先选择一个注释')
    return
  }

  const { videoNodeId, annotation } = selectedAnnotationRef.value

  editor.value.commands.setVideoAnnotationRef({
    videoNodeId,
    annotationId: annotation.id,
    displayText: ''
  })

  annotationRefDialogVisible.value = false
  message.success('视频注释引用插入成功')
}

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.destroy()
  }
})
</script>

<style lang="scss" scoped>
.editor-wrapper {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    padding: 8px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;

    :deep(.n-button:not([type="primary"])) {
      color: #909399;
      background-color: transparent;

      &:hover {
        background-color: #ecf5ff;
        color: #409eff;
      }

      &.n-button--type-primary {
        color: #fff;
        background-color: #18a058;

        &:hover {
          background-color: #16955b;
        }
      }

      &[disabled] {
        color: #c0c4cc;
        cursor: not-allowed;
      }
    }
  }

  .tiptap {
    min-height: 480px;
    max-height: 600px;
    overflow-y: auto;
    resize: vertical;
    padding: 16px;
    outline: none;
    font-size: 14px;
    line-height: 1.8;
    background: #fafafa;
    border: 1px solid #e4e7ed;
    transition: background-color 0.3s;

    &:focus-within {
      background-color: #fff;
      border-color: #18a058;
    }

    // 所有需要穿透的选择器都加上 :deep()
    :deep(p.is-editor-empty:first-child::before) {
      color: #adb5bd;
      content: attr(data-placeholder);
      float: left;
      height: 0;
      pointer-events: none;
    }

    :deep(h2), :deep(h3) {
      margin-top: 24px;
      margin-bottom: 16px;
      font-weight: 600;
      line-height: 1.25;
    }

    :deep(h1) {
      font-size: 2em;
      border-bottom: 2px solid #eaecef;
      padding-bottom: 0.3em;
      margin-top: 32px;
      margin-bottom: 16px;
      font-weight: 600;
      line-height: 1.25;
    }

    :deep(h2) {
      font-size: 1.5em;
      border-bottom: 1px solid #eaecef;
      padding-bottom: 0.3em;
    }

    :deep(h3) {
      font-size: 1.25em;
    }

    :deep(p) {
      margin: 0 0 1em 0;
    }

    :deep(ul), :deep(ol) {
      padding-left: 2em;
      margin: 0 0 1em 0;
    }

    :deep(li) {
      margin: 0.5em 0;
    }

    :deep(blockquote) {
      margin: 0;
      padding: 0 1em;
      color: #6a737d;
      border-left: 0.25em solid #dfe2e5;
    }

    :deep(code) {
      padding: 0.2em 0.4em;
      margin: 0;
      font-size: 85%;
      background-color: rgba(27, 31, 35, 0.05);
      border-radius: 3px;
      font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
      color: #e83e8c;
    }

    :deep(pre) {
      padding: 16px;
      overflow: auto;
      font-size: 85%;
      line-height: 1.45;
      background-color: #f6f8fa;
      border: 1px solid #e1e4e8;
      border-radius: 3px;
      margin: 1em 0;
      color: #24292e;

      code {
        padding: 0;
        margin: 0;
        background: none;
        font-size: 100%;
        color: inherit;
      }
    }

    :deep(img) {
      max-width: 100%;
      box-sizing: border-box;
      border-radius: 4px;
      margin: 8px 0;
      cursor: pointer;
    }

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

    .article-selector {
      max-height: 500px;

      .article-list {
        max-height: 400px;
        overflow-y: auto;

        .article-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          gap: 12px;

          .article-item {
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
            }

            &.n-card--bordered {
              border-color: #18a058;
              background-color: #f0f9eb;
            }

            .article-card-content {
              .article-cover {
                width: 100%;
                height: 140px;
                overflow: hidden;
                border-radius: 4px;
                margin-bottom: 12px;

                .cover-img {
                  width: 100%;
                  height: 100%;
                  object-fit: cover;
                  max-width: 100%;
                  max-height: 140px;
                }
              }

              .article-info {
                .article-title {
                  font-size: 14px;
                  font-weight: 600;
                  color: #333;
                  margin: 0 0 8px 0;
                  overflow: hidden;
                  text-overflow: ellipsis;
                  display: -webkit-box;
                  -webkit-line-clamp: 2;
                  -webkit-box-orient: vertical;
                }

                .article-summary {
                  font-size: 12px;
                  color: #666;
                  margin: 0 0 8px 0;
                  overflow: hidden;
                  text-overflow: ellipsis;
                  display: -webkit-box;
                  -webkit-line-clamp: 2;
                  -webkit-box-orient: vertical;
                  line-height: 1.5;
                }

                .article-meta {
                  display: flex;
                  gap: 12px;
                  font-size: 12px;
                  color: #999;

                  .meta-item {
                    display: flex;
                    align-items: center;
                    gap: 4px;
                  }
                }
              }
            }
          }
        }
      }

      .article-pagination {
        margin-top: 16px;
        display: flex;
        justify-content: center;
      }
    }

    // 视频注释引用选择器样式
    .annotation-ref-selector {
      max-height: 500px;

      .annotation-list {
        max-height: 400px;
        overflow-y: auto;

        .annotation-item {
          cursor: pointer;
          transition: all 0.3s;
          margin-bottom: 8px;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          }

          &.n-card--bordered {
            border-color: #18a058;
            background-color: #f0f9eb;
          }

          .annotation-item-content {
            .annotation-video-title {
              display: flex;
              align-items: center;
              gap: 6px;
              font-size: 13px;
              font-weight: 600;
              color: #2080f0;
              margin-bottom: 6px;
            }

            .annotation-time {
              font-size: 12px;
              color: #999;
              font-family: 'Courier New', monospace;
              margin-bottom: 4px;
            }

            .annotation-title {
              font-size: 14px;
              font-weight: 600;
              color: #333;
              margin-bottom: 4px;
            }

            .annotation-content {
              font-size: 12px;
              color: #666;
              line-height: 1.5;
            }
          }
        }
      }
    }

    :deep(a) {
      color: #18a058;
      text-decoration: none;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }

    :deep(mark) {
      background-color: #faf089;
      border-radius: 0.4em;
      padding: 0.1em 0.2em;
    }

    :deep(u) {
      text-decoration: underline;
    }

    :deep(s) {
      text-decoration: line-through;
    }
  }
}
</style>
