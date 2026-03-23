<template>
  <node-view-wrapper class="file-node-wrapper">
    <div
      class="file-card"
      @click="handleDownload"
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
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon, NButton } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'
import { useGlobalProperties } from '@/utils/globalProperties'

// 关键修复：接收完整的 NodeView props（包括 node 对象）
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
}

const props = withDefaults(defineProps<Props>(), {
  editor: null,
  node: () => ({ attrs: {} }),
  decorations: () => []
})

// 关键新增：添加日志检查 node.attrs
console.log('🔍 [FileNode] node.attrs:', props.node?.attrs)

const appContext = useGlobalProperties()

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (!bytes || bytes === 0) {return '0 B'}

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 获取文件类型（关键修复：添加类型转换）
const getFileType = (extension?: string | null, mimeType?: string | null): string => {
  // 关键修复：强制转换为字符串并处理空值
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

// 计算文件信息（关键修改：从 node.attrs 读取）
const fileInfo = computed(() => ({
  name: props.node?.attrs?.name || '未知文件',
  size: formatFileSize(props.node?.attrs?.size || 0),
  type: getFileType(props.node?.attrs?.extension, props.node?.attrs?.mimeType)
}))

// 处理下载
const handleDownload = () => {
  const src = props.node?.attrs?.src
  const name = props.node?.attrs?.name || 'download'

  if (!src) {
    appContext?.$message.warning('文件链接无效')
    return
  }

  const link = document.createElement('a')
  link.href = src
  link.download = name
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  appContext?.$message.success('开始下载文件')
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
</style>
