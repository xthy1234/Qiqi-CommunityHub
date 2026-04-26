<template>
  <n-modal
    v-model:show="modalVisible"
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
            <ArticleSelectCard
              v-for="article in filteredArticles"
              :key="article.id"
              :article="article"
              :is-selected="selectedArticle?.id === article.id"
              @select="selectArticle"
            />
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
        <n-button @click="handleCancel">
          取消
        </n-button>
        <n-button
          type="primary"
          :disabled="!selectedArticle"
          @click="handleConfirm"
        >
          插入文章卡片
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useMessage } from 'naive-ui'
import { articleAPI, type Article } from '@/api/article'
import ArticleSelectCard from '../article/ArticleSelectCard.vue'

const message = useMessage()

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'confirm', article: Article): void
}>()

const modalVisible = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const loadingArticles = ref(false)
const userArticles = ref<Article[]>([])
const selectedArticle = ref<Article | null>(null)

const filteredArticles = computed(() => {
  if (!searchKeyword.value) {
    return userArticles.value
  }
  return userArticles.value.filter(article =>
    article.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

watch(() => props.show, (newVal : boolean) => {
  modalVisible.value = newVal
  if (newVal) {
    resetState()
    loadUserArticles()
  }
}, { immediate: true })

watch(modalVisible, (newVal: boolean) => {
  if (!newVal) {
    emit('update:show', false)
  }
})

const resetState = () => {
  selectedArticle.value = null
  searchKeyword.value = ''
  currentPage.value = 1
  userArticles.value = []
}

const loadUserArticles = async () => {
  loadingArticles.value = true
  try {
    const userId = localStorage.getItem('userid')
    if (!userId) {
      message.warning('请先登录')
      modalVisible.value = false
      return
    }

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
    console.error('[ArticleSelector] 加载用户文章失败:', error)
    message.error('加载文章列表失败')
  } finally {
    loadingArticles.value = false
  }
}

const selectArticle = (article: Article) => {
  selectedArticle.value = article
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadUserArticles()
}

watch(currentPage, () => {
  loadUserArticles()
})

const handleCancel = () => {
  modalVisible.value = false
}

const handleConfirm = () => {
  if (!selectedArticle.value) {
    message.warning('请先选择一篇文章')
    return
  }
  emit('confirm', selectedArticle.value)
  modalVisible.value = false
}
</script>

<style lang="scss" scoped>
.article-selector {
  max-height: 600px;
  overflow-y: auto;

  .article-list {
    margin-bottom: 16px;

    .article-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 12px;
    }
  }

  .article-pagination {
    display: flex;
    justify-content: center;
    padding-top: 16px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
