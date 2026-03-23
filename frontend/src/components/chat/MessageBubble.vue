<template>
  <div
    class="message-wrapper"
    :class="{ 'own': isOwn }"
  >
    <!--  使用独立的头像组件 -->
    <ChatAvatar
      :user-id="message.fromUserId"
      :avatar="message.fromUser?.avatar"
      size="small"
      round
      clickable
      @click="handleAvatarClick"
    />

    <div
      class="message-bubble"
      :class="{ 'own': isOwn }"
      @contextmenu.prevent="showContextMenu"
    >
      <div class="message-content">
        <!-- 检查是否为纯文件消息 -->
        <template v-if="isFileMessage">
          <div
            class="file-card"
            @click="downloadFile"
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
              <div class="file-size">
                {{ fileInfo.size }}
              </div>
            </div>
            <div class="file-action">
              <n-button
                size="small"
                type="primary"
                @click.stop="downloadFile"
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
        </template>
        <template v-else>
          <!-- 统一使用 TipTap 渲染所有消息（包括文本、图片、富文本） -->
          <!-- 关键修复：只有在 editor 初始化完成后才渲染 EditorContent -->
          <EditorContent
            v-if="editorReady && readonlyEditor"
            :editor="readonlyEditor"
            class="tiptap-readonly"
          />
          <!-- 加载中提示 -->
          <div
            v-else
            class="message-loading"
          >
            <n-spin size="small" />
          </div>
        </template>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenuVisible"
      class="message-context-menu"
      :style="contextMenuStyle"
    >
      <n-dropdown
        :options="menuOptions"
        :x="contextMenuX"
        :y="contextMenuY"
        :show="contextMenuVisible"
        :on-clickoutside="hideContextMenu"
        @select="handleMenuSelect"
      />
    </div>

    <div class="message-meta">
      <span class="message-time">{{ formatTime(message.createTime) }}</span>
      <span
        v-if="isOwn"
        class="message-status"
      >
        <!--  显示发送中状态 -->
        <template v-if="message._sending || message.status === 'SENDING'">
          <n-spin
            size="small"
            :stroke-width="3"
          />
        </template>
        <template v-else-if="message.status === 1 || message.status === 'READ'">
          已读
        </template>
        <template v-else-if="message.status === 'SENT' || message.status === 'DELIVERED'">
          已发送
        </template>
        <template v-else-if="message.status === 'FAILED'">
          失败
        </template>
        <template v-else>
          已发送
        </template>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, toRaw } from 'vue'
import type { Message } from '@/types/message'
import dayjs from 'dayjs'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NAvatar, NImage, NIcon, NButton, NDropdown, useMessage } from 'naive-ui'
import ChatAvatar from '@/components/chat/ChatAvatar.vue'
import { getWebSocket } from '@/utils/websocket'

import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { ShareCardNodeExtension } from '@/utils/tiptap-share-card-node'
import { FileNodeExtension } from '@/utils/tiptap-file-node'

const appContext = useGlobalProperties()
const message = useMessage()

interface Props {
  message: Message
  isOwn: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'recall': [messageId: number]
  'delete': [messageId: number]
  'copy': [content: string]
}>()

// 新增：判断是否为文件消息
const isFileMessage = computed(() => {
  if (!props.message.content) {return false}

  let content: any
  try {
    content = typeof props.message.content === 'string'
      ? JSON.parse(props.message.content)
      : toRaw(props.message.content)
  } catch {
    return false
  }

  // 检查是否包含 fileNode 类型的节点
  if (content?.type === 'doc' && Array.isArray(content.content)) {
    return content.content.some((node: any) => node?.type === 'fileNode')
  }

  return false
})

// 新增：判断是否为分享卡片消息
const isShareCardMessage = computed(() => {
  if (!props.message.content) {return false}

  let content: any
  try {
    content = typeof props.message.content === 'string'
      ? JSON.parse(props.message.content)
      : toRaw(props.message.content)
  } catch {
    return false
  }

  if (content?.type === 'doc' && Array.isArray(content.content)) {
    return content.content.some((node: any) => node?.type === 'shareCardNode')
  }

  return false
})

// 新增：解析文件信息
const fileInfo = computed(() => {
  if (!props.message.content) {return { name: '未知文件', size: '', url: '', type: '' }}

  let content: any
  try {
    content = typeof props.message.content === 'string'
      ? JSON.parse(props.message.content)
      : toRaw(props.message.content)
  } catch {
    return { name: '未知文件', size: '', url: '', type: '' }
  }

  // 解析 fileNode 节点
  if (content?.type === 'doc' && Array.isArray(content.content)) {
    const fileNode = content.content.find((node: any) => node?.type === 'fileNode')
    if (fileNode?.attrs) {
      const attrs = fileNode.attrs

      // 格式化文件大小
      const formatFileSize = (bytes: number): string => {
        if (!bytes || bytes === 0) {return '0 B'}
        const k = 1024
        const sizes = ['B', 'KB', 'MB', 'GB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
      }

      // 获取文件类型
      const getFileType = (extension: string, mimeType: string): string => {
        const ext = extension.toLowerCase()
        const mime = mimeType.toLowerCase()
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

      return {
        name: attrs.name || '未知文件',
        size: formatFileSize(attrs.size),
        url: attrs.src || '',
        type: getFileType(attrs.extension, attrs.mimeType)
      }
    }
  }

  return { name: '未知文件', size: '', url: '', type: '' }
})

// 新增：根据文件类型返回图标颜色
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

// 为每条富文本消息创建一个只读的 TipTap 编辑器实例
let readonlyEditor: Editor | null = null
// 添加一个标记，用于控制 EditorContent 的渲染
const editorReady = ref(false)

// 在组件挂载后初始化只读编辑器
onMounted(() => {

  try {
    // 关键修复：使用 toRaw 去除响应式包装，确保使用纯 JSON 对象
    let contentJson: any

    if (typeof props.message.content === 'string') {

      contentJson = JSON.parse(props.message.content)
    } else {

      // 使用 toRaw 获取原始对象，避免 Vue 的 Proxy 包装导致的问题
      contentJson = toRaw(props.message.content)
    }

    // 创建编辑器实例（不使用 ref）
    readonlyEditor = new Editor({
      extensions: [
        StarterKit,
        Image,
        ShareCardNodeExtension,  // 添加分享卡片节点支持
        FileNodeExtension        // 添加文件节点支持
      ],
      content: contentJson,
      editable: false, // 关键：设置为不可编辑
      editorProps: {
        attributes: {
          class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl focus:outline-none'
        }
      },
      onCreate: () => {

        // 标记编辑器已就绪，触发 EditorContent 渲染
        editorReady.value = true

        // 强制刷新一次内容，确保内容被正确渲染
        setTimeout(() => {
          if (readonlyEditor && !readonlyEditor.isDestroyed) {

            readonlyEditor.commands.setContent(contentJson, false)
          }
        }, 0)
      }
    })

  } catch (error) {
    console.error('❌ [MessageBubble] Failed to create TipTap editor:', error)
    console.error('  - 错误类型:', error instanceof Error ? error.name : 'Unknown')
    console.error('  - 错误信息:', error instanceof Error ? error.message : error)
    console.error('  - 原始内容:', props.message.content)
    // 如果创建失败，则不渲染编辑器
    readonlyEditor = null
    editorReady.value = false
  }
})

// 在组件销毁前，销毁编辑器以释放资源
onBeforeUnmount(() => {
  if (readonlyEditor) {

    try {
      readonlyEditor.destroy()
    } catch (error) {
      console.error('⚠️ [MessageBubble] 销毁编辑器时出错:', error)
    }
    readonlyEditor = null
    editorReady.value = false
  }
})

//  右键菜单相关状态
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)

//  菜单选项
const menuOptions = computed(() => {
  const options = [
    {
      label: '复制',
      key: 'copy',
      icon: () => '📋'
    },
    {
      label: '删除',
      key: 'delete',
      icon: () => '🗑️'
    }
  ]

  if (props.isOwn) {
    options.unshift(
      {
        label: '撤回',
        key: 'recall',
        icon: () => '↩️'
      }
    )
  }

  return options
})

const contextMenuStyle = computed(() => ({
  position: 'fixed',
  left: `${contextMenuX.value}px`,
  top: `${contextMenuY.value}px`
}))

const avatarUrl = computed(() => {

  //  始终使用 fromUser 的头像（发送者的头像）
  if (props.message.fromUser?.avatar) {
    return props.message.fromUser.avatar
  }

  // 降级方案：从本地存储获取
  const avatar = appContext?.$toolUtil?.storageGet('avatar')
  if (avatar) {
    const baseUrl = appContext?.$config?.url || 'http://localhost:8080'
    return avatar.startsWith('http') ? avatar : `${baseUrl}/${avatar}`
  }
  return ''
})

const handleRecallMessage = (messageId: number) => {
  emit('recall', messageId)
}

const handleDeleteMessage = (messageId: number) => {
  emit('delete', messageId)
}

const handleCopyMessage = async (content: any) => {
  try {
    // 关键修复：提取纯文本，而不是复制 JSON
    let plainText = ''

    if (typeof content === 'string') {
      try {
        const json = JSON.parse(content)
        plainText = extractPlainText(json)
      } catch {
        plainText = content
      }
    } else if (typeof content === 'object') {
      plainText = extractPlainText(content)
    } else {
      plainText = String(content)
    }

    // 复制到剪贴板
    await navigator.clipboard.writeText(plainText)
    message.success('复制成功')
    emit('copy', plainText)
  } catch (error) {
    console.error('❌ [MessageBubble] 复制失败:', error)
    message.error('复制失败')
  }
}

// 新增：从 TipTap JSON 中提取纯文本
const extractPlainText = (node: any): string => {
  if (!node) {return ''}

  if (typeof node === 'string') {
    return node
  }

  if (Array.isArray(node)) {
    return node.map(extractPlainText).join('')
  }

  // 图片标记
  if (node.type === 'image') {
    return '[图片]'
  }

  // 文件链接标记
  if (node.marks?.some((m: any) => m.type === 'link')) {
    return node.text || '[文件]'
  }

  // 递归提取子节点
  if (node.content) {
    return extractPlainText(node.content)
  }

  if (node.text) {
    return node.text
  }

  return ''
}

//  显示右键菜单
const showContextMenu = (event: MouseEvent) => {
  event.preventDefault()
  contextMenuVisible.value = true
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
}

const hideContextMenu = () => {
  contextMenuVisible.value = false
}

const handleMenuSelect = (key: string) => {
  hideContextMenu()

  if (key === 'recall') {
    handleRecallMessage(props.message.id)
  } else if (key === 'delete') {
    handleDeleteMessage(props.message.id)
  } else if (key === 'copy') {
    // 复制纯文本内容
    handleCopyMessage(props.message.content)
  }
}

const formatTime = (time: string | number) => {
  return dayjs(time).format('HH:mm')
}

const getFileName = (url: string) => {
  try {
    return url.split('/').pop() || '文件'
  } catch {
    return '文件'
  }
}

const downloadFile = () => {
  const url = fileInfo.value.url
  if (!url) {
    message.warning('文件链接无效')
    return
  }

  // 创建临时链接触发下载
  const link = document.createElement('a')
  link.href = url
  link.download = fileInfo.value.name
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  message.success('开始下载文件')
}

const handleAvatarClick = () => {
  if (props.message.fromUserId) {
    // TODO: 跳转到用户主页

  }
}

</script>

<style scoped lang="scss">
.message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 100%;
  margin-bottom: 16px;
  position: relative;

  &.own {
    flex-direction: row-reverse;  //  自己的消息：反向排列（头像在右）
    
    .message-bubble {
      background: #18a058;  //  绿色背景（自己）
      color: #fff;
      
      .message-meta {
        .message-time,
        .message-status {
          color: rgba(255, 255, 255, 0.8);
        }
      }
    }
  }
}

.message-avatar {
  flex-shrink: 0;  //  头像不压缩
}

.message-bubble {
  max-width: 70%;  //  气泡最大宽度 70%
  padding: 12px 16px;
  background: #f0f0f0;  //  灰色背景（对方）
  border-radius: 12px;
  position: relative;
  word-wrap: break-word;  //  长文本换行
  cursor: context-menu;  //  显示右键菜单光标
}

.message-content {
  font-size: 14px;
  line-height: 1.4;  // 调整行高从 1.6 到 1.5
  word-wrap: break-word;
  word-break: break-all;

  // 关键修复：移除 TipTap 产生的空段落间距
  :deep(.ProseMirror) {
    p {
      margin: 0;  // 移除段落默认边距
      min-height: auto;  // 移除最小高度限制

      // 如果是空段落，不显示高度
      &:empty {
        display: none;
      }

      // 只保留最后一个段落的正常显示（即使是空的）
      &:last-child {
        margin-bottom: 0;
      }
    }

    // 移除编辑器底部的额外间距
    > *:last-child {
      margin-bottom: 0;
    }
  }

  // 只读编辑器的特殊样式
  :deep(.tiptap-readonly) {
    .ProseMirror {
      padding: 0;  // 移除内边距
      background: transparent;  // 透明背景
      border: none;  // 无边框

      p {
        margin: 0;

        &:empty {
          display: none;
        }
      }
    }
  }

  // 新增：文件卡片样式
  .file-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
    max-width: 320px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

    &:hover {
      background: rgba(255, 255, 255, 1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
      transform: translateY(-2px);
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

      .file-size {
        font-size: 12px;
        color: #999;
      }
    }

    .file-action {
      flex-shrink: 0;
    }
  }
}

.message-image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 8px;
  cursor: pointer;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .file-name {
    flex: 1;
    font-size: 13px;
  }
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 6px;
  font-size: 12px;
  
  .message-time,
  .message-status {
    color: #999;
  }
}

.message-context-menu {
  position: fixed;
  z-index: 9999;
}

// 专门为只读的TipTap编辑器添加样式
.tiptap-readonly {
  :deep(.ProseMirror) {
    padding: 0;
    background: none;
    border: none;

    * {
      cursor: default;
    }

    p.is-empty::before {
      display: none;
    }

    img {
      max-width: 100%;
      max-height: 200px;
      border-radius: 4px;
      margin: 4px 0;
    }

    a {
      color: #18a058;
      text-decoration: underline;
    }
  }
}
</style>
