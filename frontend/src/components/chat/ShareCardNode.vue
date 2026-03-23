<!-- src/components/chat/ShareCardNode.vue -->
<template>
  <node-view-wrapper class="share-card-wrapper">
    <div class="share-card" @click="handleClick">
      <div v-if="coverUrl" class="share-card-cover">
        <img :src="coverUrl" :alt="title" @error="handleCoverError" />
      </div>
      <div class="share-card-content">
        <div class="share-card-title">{{ title }}</div>
        <div v-if="summary" class="share-card-summary">{{ summary }}</div>
        <div class="share-card-meta">
          <span v-if="author" class="share-card-author">{{ author }}</span>
          <span v-if="publishTime" class="share-card-time">{{ formatTime(publishTime) }}</span>
        </div>
        <div class="share-card-action">
          <n-button size="small" type="primary" @click.stop="handleOpen">
            <template #icon>
              <n-icon>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/>
                </svg>
              </n-icon>
            </template>
            查看详情
          </n-button>
        </div>
      </div>
    </div>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon, NButton } from 'naive-ui'
import { NodeViewWrapper } from '@tiptap/vue-3'
import { useGlobalProperties } from '@/utils/globalProperties'

interface Props {
  editor: any
  node: {
    attrs: {
      title?: string
      summary?: string
      cover?: string
      url?: string
      author?: string
      publishTime?: string
    }
  }
  decorations: any[]
}

const props = withDefaults(defineProps<Props>(), {
  editor: null,
  node: () => ({ attrs: {} }),
  decorations: () => []
})

const appContext = useGlobalProperties()

// 从 node.attrs 读取属性
const title = computed(() => props.node?.attrs?.title || '分享卡片')
const summary = computed(() => props.node?.attrs?.summary || '')
const coverUrl = computed(() => props.node?.attrs?.cover || '')
const url = computed(() => props.node?.attrs?.url || '')
const author = computed(() => props.node?.attrs?.author || '')
const publishTime = computed(() => props.node?.attrs?.publishTime || '')

// 格式化时间
const formatTime = (time: string): string => {
  if (!time) return ''
  try {
    const date = new Date(time)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch {
    return time
  }
}

// 封面图加载失败
const handleCoverError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = '/placeholder.svg' // 使用占位图
}

// 点击卡片
const handleClick = () => {
  // 可以在这里添加点击逻辑
}

// 打开链接
const handleOpen = () => {
  if (!url.value) {
    appContext?.$message.warning('链接无效')
    return
  }
  
  window.open(url.value, '_blank')
  appContext?.$message.success('正在打开文章...')
}
</script>

<style scoped lang="scss">
.share-card-wrapper {
  margin: 12px 0;
}

.share-card {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  max-width: 400px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  
  &:hover {
    background: rgba(255, 255, 255, 1);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
  
  .share-card-cover {
    width: 100%;
    height: 200px;
    overflow: hidden;
    background: #f5f5f5;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }
    
    &:hover img {
      transform: scale(1.05);
    }
  }
  
  .share-card-content {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .share-card-title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      line-height: 1.5;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
    
    .share-card-summary {
      font-size: 14px;
      color: #666;
      line-height: 1.6;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
    }
    
    .share-card-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: #999;
      
      .share-card-author {
        font-weight: 500;
      }
    }
    
    .share-card-action {
      margin-top: 8px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
