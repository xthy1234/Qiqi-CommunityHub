<template>
  <n-card
    class="article-select-card"
    size="small"
    :bordered="isSelected"
    @click="$emit('select', article)"
  >
    <div class="card-content">
      <!-- 封面图 -->
      <div
        v-if="article.coverUrl"
        class="cover-wrapper"
      >
        <img
          :src="getCoverUrl(article.coverUrl)"
          alt="封面"
          class="cover-img"
          @error="handleImageError"
        />
      </div>

      <!-- 文章信息 -->
      <div class="article-info">
        <h3 class="article-title">
          {{ article.title }}
        </h3>
        <p
          v-if="article.summary"
          class="article-summary"
        >
          {{ article.summary }}
        </p>
        <div class="article-meta">
          <span class="meta-item">
            <Icon icon="ri:eye-line" width="14" />
            {{ formatNumber(article.viewCount || 0) }}
          </span>
          <span class="meta-item">
            <Icon icon="ri:star-line" width="14" />
            {{ formatNumber(article.favoriteCount || 0) }}
          </span>
          <span class="meta-item">
            <Icon icon="ri:time-line" width="14" />
            {{ formatDate(article.createTime) }}
          </span>
        </div>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NCard } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { normalizeFileUrl } from '@/utils/fileUrl'
import type { Article } from '@/api/article'

const props = defineProps<{
  article: Article
  isSelected: boolean
}>()

defineEmits<{
  (e: 'select', article: Article): void
}>()

const getCoverUrl = (coverUrl: string): string => {
  return normalizeFileUrl(coverUrl)
}

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = '/placeholder.svg'
}

const formatDate = (dateString: string | null): string => {
  if (!dateString) {return ''}
  const date = new Date(dateString)
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
</script>

<style lang="scss" scoped>
.article-select-card {
  cursor: pointer;
  transition: all 0.3s;
  height: 100%;

  &:hover {
    border-color: #18a058 !important;
    box-shadow: 0 2px 12px rgba(24, 160, 88, 0.2);
    transform: translateY(-2px);
  }

  &.is-selected {
    border-color: #18a058 !important;
    box-shadow: 0 0 0 2px rgba(24, 160, 88, 0.2);
  }

  .card-content {
    .cover-wrapper {
      width: 100%;
      height: 150px;
      margin-bottom: 12px;
      border-radius: 4px;
      overflow: hidden;
      background: #f5f5f5;

      .cover-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s;
      }

      &:hover .cover-img {
        transform: scale(1.05);
      }
    }

    .article-info {
      .article-title {
        font-size: 14px;
        font-weight: 600;
        color: #333;
        margin: 0 0 8px 0;
        line-height: 1.5;
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
        line-height: 1.6;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }

      .article-meta {
        display: flex;
        gap: 12px;
        font-size: 12px;
        color: #999;
        margin-top: 8px;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }
}
</style>
