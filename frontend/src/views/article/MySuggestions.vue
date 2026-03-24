<template>
  <PageContainer
    header-title="我的建议"
    :show-back="true"
  >
    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <n-skeleton
        text
        :repeat="5"
      />
    </div>

    <!-- 空状态 -->
    <div
      v-else-if="suggestions.length === 0"
      class="empty-container"
    >
      <n-empty 
        description="您还没有提交过修改建议"
        size="large"
      >
        <template #extra>
          <n-button
            type="primary"
            @click="goToArticleList"
          >
            去浏览文章
          </n-button>
        </template>
      </n-empty>
    </div>

    <!-- 建议列表 -->
    <div
      v-else
      class="suggestion-list"
    >
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <n-tabs
          v-model:value="currentStatus"
          type="line"
          animated
          @update:value="handleStatusChange"
        >
          <n-tab-pane
            name="all"
            tab="全部"
          />
          <n-tab-pane
            name="0"
            tab="待审核"
          />
          <n-tab-pane
            name="1"
            tab="已通过"
          />
          <n-tab-pane
            name="2"
            tab="已拒绝"
          />
        </n-tabs>

        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索文章标题"
          clearable
          style="width: 300px;"
          @input="handleSearch"
        >
          <template #prefix>
            <Icon icon="ri:search-line" />
          </template>
        </n-input>
      </div>

      <!-- 建议卡片列表 -->
      <div class="suggestion-cards">
        <n-card
          v-for="item in suggestions"
          :key="item.id"
          class="suggestion-card"
          hoverable
          @click="viewDetail(item)"
        >
          <div class="card-header">
            <div class="title-section">
              <h3 class="card-title">{{ item.articleTitle || `文章 ID: ${item.articleId}` }}</h3>
              <n-tag
                :type="getStatusType(item.status)"
                size="small"
                round
                bordered
              >
                {{ getStatusText(item.status) }}
              </n-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="change-summary">
              <Icon
                icon="ri:edit-circle-line"
                width="16"
              />
              <span>{{ item.changeSummary || '未填写修改说明' }}</span>
            </div>
            
            <div class="meta-info">
              <span class="meta-item">
                <Icon
                  icon="ri:time-line"
                  width="14"
                />
                提交于 {{ formatDate(item.createTime) }}
              </span>
              <span
                v-if="item.reviewTime"
                class="meta-item"
              >
                <Icon
                  icon="ri:shield-check-line"
                  width="14"
                />
                {{ item.status === 1 ? '已通过' : '已拒绝' }}
              </span>
              <span
                v-if="item.rejectReason && item.status === 2"
                class="meta-item reject-reason"
              >
                理由：{{ item.rejectReason }}
              </span>
            </div>
          </div>

          <div class="card-actions">
            <n-button
              size="small"
              @click.stop="viewDetail(item)"
            >
              <template #icon>
                <Icon icon="ri:eye-line" />
              </template>
              详情
            </n-button>
          </div>
        </n-card>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.limit"
          :item-count="pagination.total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update-page="handlePageChange"
          @update-page-size="handlePageSizeChange"
        >
          <template #prefix="{ itemCount }">
            共 {{ itemCount }} 条
          </template>
        </n-pagination>
      </div>
    </div>

    <!-- 详情查看对话框 -->
    <n-modal
      v-model:show="detailModalVisible"
      preset="dialog"
      :title="currentSuggestion?.title || '建议详情'"
      :style="{ width: '1000px', maxWidth: '95vw' }"
      :closable="true"
    >
      <div class="detail-content">
        <div class="detail-header">
          <n-descriptions
            bordered
            :column="2"
          >
            <n-descriptions-item label="文章标题">
              {{ currentSuggestion?.articleTitle || `文章 ID: ${currentSuggestion?.articleId}` }}
            </n-descriptions-item>
            <n-descriptions-item label="提交时间">
              {{ formatDate(currentSuggestion?.createTime) }}
            </n-descriptions-item>
            <n-descriptions-item
              v-if="currentSuggestion?.reviewTime"
              label="审核时间"
            >
              {{ formatDate(currentSuggestion?.reviewTime) }}
            </n-descriptions-item>
            <n-descriptions-item label="修改说明">
              {{ currentSuggestion?.changeSummary || '无' }}
            </n-descriptions-item>
            <n-descriptions-item
              v-if="currentSuggestion?.rejectReason"
              label="拒绝理由"
            >
              {{ currentSuggestion?.rejectReason }}
            </n-descriptions-item>
            <n-descriptions-item label="审核结果">
              <n-tag
                :type="getStatusType(currentSuggestion?.status)"
                size="small"
              >
                {{ getStatusText(currentSuggestion?.status) }}
              </n-tag>
            </n-descriptions-item>
          </n-descriptions>
        </div>

        <div class="detail-body">
          <h4 class="section-title">内容差异对比</h4>
          <DiffViewer
            :source-content="currentArticleContent"
            :target-content="currentSuggestionContent"
            source-title="原文内容"
            target-title="您的修改建议"
            :show-stats="true"
          />
        </div>
      </div>
      
      <template #action>
        <n-button @click="detailModalVisible = false">
          关闭
        </n-button>
      </template>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import DiffViewer from '@/components/common/DiffViewer.vue'
import { articleSuggestionAPI, type ArticleEditSuggestion } from '@/api/articleSuggestion'
import { articleAPI } from '@/api/article'

const router = useRouter()
const message = useMessage()

// 响应式数据
const loading = ref(false)
const suggestions = ref<ArticleEditSuggestion[]>([])
const currentStatus = ref<string>('all')
const searchKeyword = ref('')
const pagination = reactive({
  page: 1,
  limit: 20,
  total: 0
})

// 模态框相关
const detailModalVisible = ref(false)
const currentSuggestion = ref<ArticleEditSuggestion | null>(null)

// 内容数据
const currentArticleContent = ref<object>({})
const currentSuggestionContent = ref<object>({})

/**
 * 加载建议列表
 */
const loadSuggestions = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.limit
    }
    
    if (currentStatus.value !== 'all') {
      params.status = parseInt(currentStatus.value)
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    // 调用获取"我提交的建议"接口
    const response = await articleSuggestionAPI.getMySuggestions(params)
    const data = response.data.data
    
    suggestions.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('加载建议列表失败:', error)
    message.error('加载建议列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查看详情
 */
const viewDetail = async (item: ArticleEditSuggestion) => {
  currentSuggestion.value = item
  
  try {
    const detailResponse = await articleSuggestionAPI.getById(item.id)
    const suggestionData = detailResponse.data.data
    
    // 获取文章内容用于对比
    const articleResponse = await articleAPI.getById(item.articleId)
    const articleData = articleResponse.data.data
    
    // 保存内容用于对比
    currentArticleContent.value = articleData.content || {}
    currentSuggestionContent.value = suggestionData.content || {}
    
    detailModalVisible.value = true
  } catch (error) {
    console.error('加载详情失败:', error)
    message.error('加载详情失败')
  }
}

/**
 * 处理状态切换
 */
const handleStatusChange = (value: string) => {
  pagination.page = 1
  loadSuggestions()
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  pagination.page = 1
  loadSuggestions()
}

/**
 * 处理分页
 */
const handlePageChange = (page: number) => {
  pagination.page = page
  loadSuggestions()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.limit = pageSize
  pagination.page = 1
  loadSuggestions()
}

/**
 * 获取状态类型
 */
const getStatusType = (status?: number) => {
  if (status === undefined) return 'default'
  const map = {
    0: 'warning',
    1: 'success',
    2: 'error'
  }
  return map[status] || 'default'
}

/**
 * 获取状态文本
 */
const getStatusText = (status?: number) => {
  if (status === undefined) return '未知'
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

/**
 * 格式化日期
 */
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 返回上一页
 */
const goBack = () => {
  router.back()
}

/**
 * 前往文章列表
 */
const goToArticleList = () => {
  router.push('/index/articleList')
}

onMounted(() => {
  loadSuggestions()
})
</script>

<style lang="scss" scoped>
.loading-container {
  padding: 20px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 500px;
}

.suggestion-list {
  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  .suggestion-cards {
    display: grid;
    gap: 20px;
    grid-template-columns: repeat(auto-fill, minmax(600px, 1fr));
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }

  .suggestion-card {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .title-section {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;

        .card-title {
          font-size: 16px;
          font-weight: 600;
          color: #333;
          margin: 0;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 400px;
        }
      }
    }

    .card-body {
      margin-bottom: 16px;

      .change-summary {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: #666;
        margin-bottom: 12px;
        padding: 8px;
        background: #f5f7fa;
        border-radius: 6px;
      }

      .meta-info {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        font-size: 13px;
        color: #999;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
          
          &.reject-reason {
            color: #f00;
            width: 100%;
            margin-top: 8px;
          }
        }
      }
    }

    .card-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
      padding-top: 12px;
      border-top: 1px solid #f0f0f0;
    }
  }

  .pagination-wrapper {
    margin-top: 30px;
    display: flex;
    justify-content: center;
  }
}

.detail-content {
  .detail-header {
    margin-bottom: 20px;
  }

  .detail-body {
    .section-title {
      margin: 20px 0 12px 0;
      font-size: 16px;
      font-weight: 600;
      color: #606266;
    }
  }
}
</style>
