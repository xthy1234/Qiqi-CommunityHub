<template>
  <node-view-wrapper class="file-node-wrapper">
    <div
      class="file-card"
      :class="{ 'is-editable': isEditable, 'is-chat-context': isInChatContext }"
      @click="handleCardClick"
    >
      <div class="file-icon">
        <n-icon
          size="40"
          :color="getFileIconColor(fileInfo.type)"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="40"
            height="40"
            viewBox="0 0 24 24"
          >
            <path
              fill="currentColor"
              d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"
            />
          </svg>
        </n-icon>
      </div>
      <div class="file-info">
        <div class="file-name">
          {{ fileInfo.name }}
        </div>
        <div class="file-meta">
          <span class="file-size">{{ fileInfo.size }}</span>
          <span
            v-if="fileInfo.extension"
            class="file-extension"
          >· {{ fileInfo.extension.toUpperCase() }}</span>
        </div>
      </div>

      <!-- 编辑模式：显示操作按钮 -->
      <div
        v-if="isEditable"
        class="file-actions"
      >
        <n-button
          size="small"
          type="error"
          secondary
          @click.stop="handleDelete"
        >
          <template #icon>
            <n-icon>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"
                />
              </svg>
            </n-icon>
          </template>
          删除
        </n-button>
        <n-button
          size="small"
          type="primary"
          secondary
          @click.stop="handleReplace"
        >
          <template #icon>
            <n-icon>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M9 16h6v-6h4l-7-7l-7 7h4v6zm-4 2h14v2H5v-2z"
                />
              </svg>
            </n-icon>
          </template>
          替换
        </n-button>
      </div>

      <!-- 只读模式：显示下载按钮 -->
      <div
        v-else
        class="file-action"
      >
        <n-button
          size="small"
          type="primary"
          @click.stop="handleDownload"
        >
          <template #icon>
            <n-icon>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7l7-7z"
                />
              </svg>
            </n-icon>
          </template>
          下载
        </n-button>
      </div>
    </div>

    <!-- 隐藏的文件选择器（用于替换） -->
    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      @change="handleFileInputChange"
    />
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { NIcon, NButton, useMessage } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'
import { normalizeFileUrl } from '@/utils/fileUrl'
import { uploadAPI } from '@/api/upload'

const message = useMessage()

interface Props {
  editor: any
  node: {
    attrs: {
      src?: string | null
      name?: string
      size?: number
      mimeType?: string
      extension?: string
    }
  }
  decorations: any[]
  selected: boolean
  updateAttributes: (attrs: Record<string, any>) => void
}

const props = withDefaults(defineProps<Props>(), {
  editor: null,
  node: () => ({ attrs: {} }),
  decorations: () => [],
  selected: false,
  updateAttributes: () => {}
})

const fileInputRef = ref<HTMLInputElement | null>(null)

// 判断是否为可编辑模式
const isEditable = computed(() => {
  return props.editor?.isEditable === true
})

// 判断是否在聊天场景中
const isInChatContext = computed(() => {
  const path = window.location.pathname
  const hash = window.location.hash
  return path.includes('/chat') || hash.includes('chat')
})

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (!bytes || bytes === 0) {return '0 B'}

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 获取文件类型
const getFileType = (extension?: string | null, mimeType?: string | null): string => {
  const ext = String(extension || '').toLowerCase()
  const mime = String(mimeType || '').toLowerCase()

  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
  const videoExts = ['mp4', 'avi', 'mov', 'wmv']
  const audioExts = ['mp3', 'wav', 'ogg']
  const docExts = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
  const zipExts = ['zip', 'rar', '7z']

  if (imageExts.includes(ext) || mime.startsWith('image/')) {return 'image'}
  if (videoExts.includes(ext) || mime.startsWith('video/')) {return 'video'}
  if (audioExts.includes(ext) || mime.startsWith('audio/')) {return 'audio'}
  if (docExts.includes(ext)) {return 'document'}
  if (zipExts.includes(ext)) {return 'archive'}
  return 'file'
}

// 根据文件类型返回图标颜色
const getFileIconColor = (type: string): string => {
  const colors: Record<string, string> = {
    'image': '#18a058',
    'video': '#f0a020',
    'audio': '#9856ff',
    'document': '#2080f0',
    'archive': '#d03050',
    'file': '#666666'
  }
  return colors[type] || '#666666'
}

// 计算文件信息
const fileInfo = computed(() => ({
  name: props.node?.attrs?.name || '未知文件',
  size: formatFileSize(props.node?.attrs?.size || 0),
  extension: props.node?.attrs?.extension || '',
  type: getFileType(props.node?.attrs?.extension, props.node?.attrs?.mimeType)
}))

// 处理卡片点击（只读模式下触发下载）
const handleCardClick = () => {
  if (!isEditable.value) {
    handleDownload()
  }
}

// 处理下载
const handleDownload = () => {
  const rawSrc = props.node?.attrs?.src
  const name = props.node?.attrs?.name || 'download'

  if (!rawSrc) {
    message.warning('文件链接无效')
    return
  }

  const src = normalizeFileUrl(rawSrc)

  const link = document.createElement('a')
  link.href = src
  link.download = name
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  message.success('开始下载文件')
}

// 处理删除
const handleDelete = () => {
  if (!props.editor) {return}

  // 获取当前节点的位置并删除
  const { state } = props.editor
  const { selection } = state
  const { from, to } = selection

  props.editor.chain().focus().deleteRange({ from, to }).run()
  message.success('文件已删除')
}

// 处理替换文件
const handleReplace = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileInputChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {return}

  try {
    message.loading('文件上传中...', { duration: 0 })
    const fileUrl = await uploadAPI.uploadAnyFile(file)

    props.updateAttributes({
      src: fileUrl,
      name: file.name,
      size: file.size,
      mimeType: file.type,
      extension: file.name.split('.').pop() || ''
    })

    message.destroyAll()
    message.success('文件替换成功')
  } catch (error: any) {
    message.destroyAll()
    message.error(error.message || '文件上传失败')
  } finally {
    if (target) {
      target.value = ''
    }
  }
}

</script>

<style scoped lang="scss">
.file-node-wrapper {
  margin: 8px 0;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  transition: all 0.3s ease;
  max-width: 400px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  &.is-editable {
    cursor: default;
  }

  &:not(.is-editable) {
    cursor: pointer;

    &:hover {
      background: rgba(255, 255, 255, 1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
      transform: translateY(-2px);
    }
  }

  .file-icon {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    background: #f5f5f5;
    border-radius: 8px;
  }

  .file-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .file-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .file-meta {
      font-size: 12px;
      color: #999;
      display: flex;
      align-items: center;
      gap: 4px;

      .file-extension {
        text-transform: uppercase;
      }
    }
  }

  .file-actions {
    flex-shrink: 0;
    display: flex;
    gap: 8px;
  }

  .file-action {
    flex-shrink: 0;
  }
}
</style>
