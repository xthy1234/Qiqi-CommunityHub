<template>
  <div class="article-grid-list">
    <!-- 加载中 - 骨架屏 -->
    <n-grid v-if="loading" :cols="responsiveCols.cols" :x-gap="20" :y-gap="20">
      <n-grid-item v-for="i in loadingCount" :key="i">
        <n-card>
          <div class="skeleton-article-card">
            <n-skeleton text style="height: 150px; margin-bottom: 12px;" />
            <n-skeleton text style="width: 80%" />
            <n-skeleton text :repeat="2" />
            <div class="skeleton-meta">
              <n-skeleton circular size="small" />
              <n-skeleton text style="width: 100px" />
            </div>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>

    <!-- 空状态 -->
    <n-empty
      v-else-if="!loading && articles.length === 0"
      :description="emptyText || '暂无文章'"
      :style="{ padding: emptyText ? '60px' : '0' }"
    >
      <template #extra>
        <slot name="empty-extra">
          <n-button 
            v-if="showEmptyAction" 
            type="primary" 
            @click="$emit('empty-action')"
          >
            {{ emptyActionText || '去逛逛' }}
          </n-button>
        </slot>
      </template>
    </n-empty>

    <!-- 文章列表 -->
    <n-grid
      v-else
      :cols="responsiveCols.cols"
      :x-gap="20"
      :y-gap="20"
      class="article-grid"
    >
      <n-grid-item
        v-for="article in articles"
        :key="article.id"
      >
        <ArticleCard :article="article" />
      </n-grid-item>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NGrid, NGridItem, NCard, NSkeleton, NEmpty, NButton } from 'naive-ui'
import ArticleCard from '@/components/ArticleCard.vue'

interface Article {
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
  favoriteCount?: number
  shareCount?: number
  viewCount?: number
  publishTime?: string
  createTime?: string
  [key: string]: any
}

interface ResponsiveCols {
  cols: number
}

const props = withDefaults(defineProps<{
  // 文章列表数据
  articles: Article[]
  // 是否正在加载
  loading?: boolean
  // 加载时显示的骨架屏数量
  loadingCount?: number
  // 空状态提示文字
  emptyText?: string
  // 是否显示空状态操作按钮
  showEmptyAction?: boolean
  // 空状态操作按钮文字
  emptyActionText?: string
  // 列数配置（可选，默认响应式）
  cols?: number
}>(), {
  loading: false,
  loadingCount: 6,
  emptyText: '暂无文章',
  showEmptyAction: false,
  emptyActionText: '去逛逛',
  cols: 3
})

// 定义事件
const emit = defineEmits<{
  (e: 'empty-action'): void
}>()

// 计算响应式列数
const responsiveCols = computed<ResponsiveCols>(() => {
  if (props.cols) {
    return { cols: props.cols }
  }
  
  // 默认响应式逻辑
  const width = window.innerWidth
  
  if (width >= 1200) {
    return { cols: 3 }
  } else if (width >= 768) {
    return { cols: 2 }
  } else {
    return { cols: 1 }
  }
})
</script>

<style lang="scss" scoped>
.article-grid-list {
  width: 100%;
}

.skeleton-article-card {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .skeleton-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
  }
}

.article-grid {
  display: grid;
  gap: 20px;
  
  @media (min-width: 1200px) {
    grid-template-columns: repeat(v-bind('responsiveCols.cols'), 1fr);
  }
  
  @media (max-width: 1199px) and (min-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (max-width: 767px) {
    grid-template-columns: 1fr;
  }
}
</style>
