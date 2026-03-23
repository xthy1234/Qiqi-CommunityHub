<template>
  <div class="chat-input-container">
    <div class="input-toolbar">
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button text @click="handleUpload">
            <template #icon>
              <n-icon size="20">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/>
                </svg>
              </n-icon>
            </template>
          </n-button>
        </template>
        发送文件
      </n-tooltip>
      
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button text @click="insertEmoji">
            <template #icon>
              <n-icon size="20">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8s8 3.58 8 8s-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8S14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8S7 8.67 7 9.5S7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NButton, NIcon, NTooltip, NInput } from 'naive-ui'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Placeholder from "@tiptap/extension-placeholder";
import {uploadAPI} from "@/api/upload";
import { FileNodeExtension } from '@/utils/tiptap-file-node'
import { ShareCardNodeExtension } from '@/utils/tiptap-share-card-node'

const appContext = useGlobalProperties()
const fileInputRef = ref<HTMLInputElement | null>(null)

// 最大字符数限制
const MAX_CHAR_COUNT = 500

// 创建轻量化的 TipTap 编辑器实例
const editor = useEditor({
  extensions: [
    StarterKit,
    Image,
    FileNodeExtension,
    ShareCardNodeExtension, // 添加 ShareCard 扩展
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
    // 关键修复：粘贴图片时自动压缩
    handleDOMEvents: {
      paste: (view, event) => {
        const items = event.clipboardData?.items
        if (!items) return false

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
        }
        return false
      }
    }
  },
  autofocus: 'end',
  onUpdate: ({ editor }) => {
    // 检查字符数限制
    const text = editor.state.doc.textContent
    if (text.length > MAX_CHAR_COUNT) {
      appContext?.$message.warning(`消息内容超过 ${MAX_CHAR_COUNT} 字限制`)
      // 截断多余内容
      const pos = editor.state.selection.from
      editor.commands.setTextSelection(Math.min(pos, MAX_CHAR_COUNT))
      editor.commands.deleteSelection()
    }
  }
})

// 计算属性：判断是否可以发送消息
const canSend = computed(() => {
  if (!editor.value) return false
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
  if (!editor.value || !canSend.value) return

  // 获取 TipTap 的 JSON 对象（不要 stringify，直接传递对象）
  const contentJson = editor.value.getJSON()



  // 直接发送 JSON 对象给父组件
  emit('send', contentJson, 0)
  editor.value.commands.clearContent()
}

const handleUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !editor.value) return

  try {


    const response = await uploadAPI.uploadFile(file)


    // 关键修复：正确解析响应数据并转换为完整 URL
    const fileData = response.code === 0 ? response : response.data
    let fileUrl = fileData.url

    if (!fileUrl) {
      throw new Error('上传响应中缺少 url 字段')
    }

    // 关键修复：如果是相对路径，转换为完整 URL
    if (fileUrl.startsWith('/')) {
      const baseUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
      fileUrl = `${baseUrl}${fileUrl}`
    }


    if (file.type.startsWith('image/')) {
      // 图片：插入到编辑器
      editor.value.chain().focus().setImage({ src: fileUrl }).run()
      appContext?.$message.success('图片上传成功')
    } else {
      // 文件：使用新的 File 节点插入
      const extension = file.name.split('.').pop()


      // 关键修复：确保所有属性都是正确的类型
      editor.value.commands.setFile({
        src: fileUrl,
        name: String(file.name),
        size: Number(file.size),
        mimeType: String(file.type),
        extension: extension ? String(extension) : ''
      })

      appContext?.$message.success('文件上传成功')
    }
  } catch (error: any) {
    console.error('❌ [ChatInput] 文件上传失败:', error)
    appContext?.$message.error(`文件上传失败：${error.message}`)
  }

  target.value = ''
}

// 新增：处理粘贴的图片
const handleImagePaste = async (file: File) => {
  try {


    const response = await uploadAPI.uploadFile(file)
    const fileData = response.code === 0 ? response : response.data
    let fileUrl = fileData.url

    if (!fileUrl) {
      throw new Error('上传响应中缺少 url 字段')
    }

    // 关键修复：如果是相对路径，转换为完整 URL
    if (fileUrl.startsWith('/')) {
      const baseUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
      fileUrl = `${baseUrl}${fileUrl}`
    }

    // 插入图片到编辑器
    editor.value.chain().focus().setImage({ src: fileUrl }).run()
    appContext?.$message.success('图片粘贴成功')
  } catch (error: any) {
    console.error('❌ [ChatInput] 粘贴图片失败:', error)
    appContext?.$message.error('图片粘贴失败')
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
