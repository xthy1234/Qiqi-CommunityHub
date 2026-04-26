<template>
  <div class="chat-input-container">
    <div class="input-toolbar">
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button
            text
            @click="handleUpload"
          >
            <template #icon>
              <n-icon size="20">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                >
                  <path
                    fill="currentColor"
                    d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"
                  />
                </svg>
              </n-icon>
            </template>
          </n-button>
        </template>
        发送文件
      </n-tooltip>
      
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button
            text
            @click="openVideoUpload"
          >
            <template #icon>
              <n-icon size="20">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                >
                  <path
                    fill="currentColor"
                    d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"
                  />
                </svg>
              </n-icon>
            </template>
          </n-button>
        </template>
        发送视频
      </n-tooltip>

      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button
            text
            @click="insertEmoji"
          >
            <template #icon>
              <n-icon size="20">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                >
                  <path
                    fill="currentColor"
                    d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8s8 3.58 8 8s-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8S14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8S7 8.67 7 9.5S7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"
                  />
                </svg>
              </n-icon>
            </template>
          </n-button>
        </template>
        表情
      </n-tooltip>
    </div>
    
    <div class="input-wrapper">
      <EditorContent :editor="editor" />
    </div>
    
    <div class="input-footer">
      <n-button 
        type="primary" 
        :disabled="!canSend"
        @click="handleSend"
      >
        发送
      </n-button>
    </div>
    
    <!-- 文件上传 -->
    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      @change="handleFileChange"
    />

    <!-- 视频上传 -->
    <input
      ref="videoInputRef"
      type="file"
      accept="video/*"
      style="display: none"
      @change="handleVideoInputChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { NButton, NIcon, NTooltip, useMessage } from 'naive-ui'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Placeholder from "@tiptap/extension-placeholder";
import {uploadAPI} from "@/api/upload";
import { FileNodeExtension } from '@/utils/tiptap-file-node'
import { ShareCardNodeExtension } from '@/utils/tiptap-share-card-node'
import { VideoNodeExtension } from '@/utils/tiptap-video-node'
import { normalizeFileUrl } from '@/utils/fileUrl'

const message = useMessage()
const fileInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)

// 最大字符数限制
const MAX_CHAR_COUNT = 500

// 判断是否为文档类型文件
const isDocumentType = (mimeType: string): boolean => {
  if (!mimeType) {return false}

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

  return documentTypes.includes(mimeType) || mimeType.startsWith('application/') || mimeType.startsWith('text/')
}

// 创建轻量化的 TipTap 编辑器实例
const editor = useEditor({
  extensions: [
    StarterKit,
    Image,
    FileNodeExtension,
    ShareCardNodeExtension,
    VideoNodeExtension,
    Placeholder.configure({
      placeholder: '输入消息... (Enter 发送，Shift+Enter 换行)'
    })
  ],
  content: '',
  editorProps: {
    attributes: {
      class: 'editor-content',
      placeholder: '输入消息... (Enter 发送，Shift+Enter 换行)'
    },
    handleDOMEvents: {
      paste: (view, event) => {
        const items = event.clipboardData?.items
        if (!items) {return false}

        for (let i = 0; i < items.length; i++) {
          const item = items[i]

          if (item.type.startsWith('image/')) {
            const file = item.getAsFile()
            if (file) {
              event.preventDefault()
              handleImagePaste(file)
              return true
            }
          }

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
      }
    }
  },
  autofocus: 'end',
  onUpdate: ({ editor }) => {
    const text = editor.state.doc.textContent
    if (text.length > MAX_CHAR_COUNT) {
      message.warning(`消息内容超过 ${MAX_CHAR_COUNT} 字限制`)
      const pos = editor.state.selection.from
      editor.commands.setTextSelection(Math.min(pos, MAX_CHAR_COUNT))
      editor.commands.deleteSelection()
    }
  }
})

// 计算属性：判断是否可以发送消息
const canSend = computed(() => {
  if (!editor.value) {return false}
  const json = editor.value.getJSON()
  return !(json.content?.length === 1 && json.content[0].type === 'paragraph' && !json.content[0].content)
})

const emit = defineEmits<{
  send: [content: string, msgType: number]
}>()

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!editor.value || !canSend.value) {return}

  // 获取 TipTap 的 JSON 对象（不要 stringify，直接传递对象）
  const contentJson = editor.value.getJSON()

  // 直接发送 JSON 对象给父组件
  emit('send', contentJson, 0)
  editor.value.commands.clearContent()
}

const handleUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {return}

  try {
    const imageUrl = await uploadAPI.uploadImage(file)

    editor.value.chain().focus().setImage({ src: normalizeFileUrl(imageUrl) }).run()
    message.success('图片上传成功')
  } catch (error: any) {
    message.error(error.message || '图片上传失败')
  } finally {
    if (target) {
      target.value = ''
    }
  }
}

const handleImagePaste = async (file: File) => {
  try {
    const imageUrl = await uploadAPI.uploadImage(file)

    editor.value.chain().focus().setImage({ src: normalizeFileUrl(imageUrl) }).run()
    message.success('图片粘贴成功')
  } catch (error: any) {
    message.error(error.message || '图片粘贴失败')
  }
}

const openVideoUpload = () => {
  videoInputRef.value?.click()
}

const handleVideoInputChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {return}

  let loadingMessage: any = null
  try {
    loadingMessage = message.loading('视频上传中...', { duration: 0 })
    const videoUrl = await uploadAPI.uploadVideo(file)

    editor.value.commands.setVideo({
      src: videoUrl,
      title: file.name,
      duration: 0,
      annotations: []
    })

    message.destroyAll()
    message.success('视频上传成功')
  } catch (error: any) {
    message.destroyAll()
    message.error(error.message || '视频上传失败')
  } finally {
    if (target) {
      target.value = ''
    }
  }
}

const handleVideoPaste = async (file: File) => {
  try {
    message.loading('视频上传中...', { duration: 0 })
    const response = await uploadAPI.uploadVideo(file)

    if (!response) {
      throw new Error('视频上传失败')
    }

    editor.value.commands.setVideo({
      src: response,
      title: file.name,
      duration: 0,
      annotations: []
    })

    message.destroyAll()
    message.success('视频粘贴成功')
  } catch (error: any) {
    console.error('[ChatInput] 粘贴视频失败:', error)
    message.destroyAll()
    message.error('视频粘贴失败')
  }
}

// 处理文档类文件粘贴
const handleDocumentPaste = async (file: File) => {
  try {
    message.loading('文件上传中...', { duration: 0 })
    const fileUrl = await uploadAPI.uploadAnyFile(file)

    editor.value.commands.setFileNode({
      src: fileUrl,
      name: file.name,
      size: file.size,
      mimeType: file.type,
      extension: file.name.split('.').pop() || ''
    })

    message.destroyAll()
    message.success('文件粘贴成功')
  } catch (error: any) {
    message.destroyAll()
    message.error(error.message || '文件粘贴失败')
  }
}

const insertEmoji = () => {
  // TODO: 实现表情选择器
}

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.destroy()
  }
})
</script>

<style scoped lang="scss">
.chat-input-container {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.input-wrapper {
  :deep(.ProseMirror) {
    min-height: 80px;
    max-height: 200px;
    background: aliceblue;
    overflow-y: auto;
    padding: 8px 12px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    font-size: 14px;
    line-height: 1;

    &:hover {
      border-color: #c0c4cc;
    }

    &:focus {
      border-color: #18a058;
      box-shadow: 0 0 0 2px rgba(24, 160, 88, 0.2);
    }
  }
  :deep(.ProseMirror) {
    img {
      max-width: 100%;
      max-height: 200px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      margin: 8px 0;
      cursor: pointer;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
      &:hover {
        transform: scale(1.02);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
    }
  }
  :deep(.ProseMirror) {
    &::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }
    &::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }
    &::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 3px;
      &:hover {
        background: #a8a8a8;
      }
    }
  }

}

.input-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
