<template>
  <n-card
    class="article-card-component"
    hoverable
    :title="article.title"
    @click="handleClick"
  >
    <template #cover>
      <img
        v-if="article.coverUrl"
        :src="getCoverImageUrl(article.coverUrl)"
        alt="封面"
        class="article-cover-img"
        @error="handleImageError"
      />
      <div v-else class="cover-placeholder">
        <Icon icon="ri:image-line" width="40" />
      </div>
    </template>
    
    <template #header-extra>
      <n-tag 
        v-if="article.categoryName" 
        size="small" 
        type="info"
        round
      >
        {{ article.categoryName }}
      </n-tag>
    </template>
    
    <div class="article-meta">
      <UserAvatarLink
        :userId="article.authorId || ''"
        :nickname="article.authorNickname"
        :avatar="article.authorAvatar"
        :size="32"
        :showName="true"
      />
      <div class="publish-time" v-if="article.publishTime">
        <Icon icon="ri:calendar-line" width="14" />
        {{ formatDate(article.publishTime) }}
      </div>
    </div>
    
    <div class="article-stats">
      <div class="stat-item">
        <Icon icon="ri:eye-line" width="16" />
        <span>{{ formatNumber(article.viewCount || 0) }}</span>
      </div>
      <div class="stat-item">
        <Icon icon="ri:thumb-up-line" width="16" />
        <span>{{ formatNumber(article.likeCount || 0) }}</span>
      </div>
      <div class="stat-item">
        <Icon icon="ri:chat-1-line" width="16" />
        <span>{{ formatNumber(article.commentCount || 0) }}</span>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NTag } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handleImageError } from '@/utils/userUtils'
import {useGlobalProperties} from "@/utils/globalProperties";
import UserAvatarLink from '@/components/common/UserAvatarLink.vue'

interface ArticleProps {
  id: number | string
  title: string
  coverUrl?: string | null
  categoryId?: number | string
  categoryName?: string
  authorId?: number | string
  authorNickname?: string
  authorAvatar?: string | null
  likeCount?: number
  dislikeCount?: number
  viewCount?: number
  commentCount?: number
  publishTime?: string
  auditStatus?: string
  createTime?: string
  [key: string]: any
}

const props = defineProps<{
  article: ArticleProps
}>()

const router = useRouter()
const appContext = useGlobalProperties()
const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')

const isHttpUrl = (url: string): boolean => {
  if (!url) return false
  return url.startsWith('http')
}

const getCoverImageUrl = (coverUrl: string): string => {
  if (!coverUrl || coverUrl === 'null') return '/placeholder.svg'
  return isHttpUrl(coverUrl) ? coverUrl : `${baseUrl.value}/${coverUrl}`
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return String(num)
}

const handleClick = () => {
  router.push(`/index/articleDetail?id=${props.article.id}`)
}
</script>

<style lang="scss" scoped>
.article-card-component {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s;

  .article-cover-img {
    width: 100%;
    height: 200px;
    object-fit: cover;
  }

  .cover-placeholder {
    width: 100%;
    height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
  }

  .article-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .publish-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #999;
  }

  .article-stats {
    display: flex;
    justify-content: space-around;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #666;

      &:hover {
        color: #18a058;
      }
    }
  }

  &:hover {
    transform: translateY(-5px);

    :deep(.n-card__cover img) {
      transform: scale(1.05);
    }
  }

  :deep(.n-card-header__main) {
    font-size: 14px;
    font-weight: 500;

    .n-card-header__title {
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
  }
}
</style>
