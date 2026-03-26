<template>
  <PageContainer
    header-title="修改建议审核"
    :show-back="true"
    @back="goBack"
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
        description="暂无修改建议"
        size="large"
      >
        <template #extra>
          <n-space>
            <n-button
              type="primary"
              @click="goBack"
            >
              返回首页
            </n-button>
            <n-button
              @click="loadSuggestions"
            >
              刷新列表
            </n-button>
          </n-space>
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
            name="0"
            tab="待审核"
          >
            <n-badge
              :value="pendingCount"
              :show="pendingCount > 0"
              :max="99"
            >
              <span></span>
            </n-badge>
          </n-tab-pane>
          <n-tab-pane
            name="1"
            tab="已通过"
          />
          <n-tab-pane
            name="2"
            tab="已拒绝"
          />
          <n-tab-pane
            name="all"
            tab="全部"
          />
        </n-tabs>

        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索文章标题或提议者"
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
            
            <div class="proposer-info">
              <UserAvatarLink
                :user-id="item.proposerId"
                :nickname="item.proposerName || '匿名用户'"
                :avatar="item.proposerAvatar"
                :size="32"
              />
              <span class="proposer-name">{{ item.proposerName || '匿名用户' }}</span>
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
                {{ formatDate(item.createTime) }}
              </span>
              <span
                v-if="item.reviewTime"
                class="meta-item"
              >
                <Icon
                  icon="ri:shield-check-line"
                  width="14"
                />
                {{ formatDate(item.reviewTime) }}
              </span>
            </div>
          </div>

          <div class="card-actions">
            <n-button
              v-if="item.status === 0"
              type="primary"
              size="small"
              @click.stop="openReviewDialog(item)"
            >
              <template #icon>
                <Icon icon="ri:checkbox-circle-line" />
              </template>
              审核
            </n-button>
            <n-button
              size="small"
              @click.stop="viewDetail(item)"
            >
              <template #icon>
                <Icon icon="ri:eye-line" />
              </template>
              详情
            </n-button>
            <n-button
              v-if="item.status === 0"
              type="error"
              size="small"
              @click.stop="confirmDelete(item)"
            >
              <template #icon>
                <Icon icon="ri:delete-bin-line" />
              </template>
              删除
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

    <!-- 审核对话框 -->
    <n-modal
      v-model:show="reviewModalVisible"
      preset="dialog"
      :title="`审核修改建议`"
      :style="{ width: '1000px', maxWidth: '95vw' }"
      :closable="true"
    >
      <div class="review-dialog-content">
        <!-- 基本信息 -->
        <div class="suggestion-info">
          <n-descriptions
            bordered
            :column="2"
            size="small"
          >
            <n-descriptions-item label="文章标题">
              {{ currentSuggestion?.articleTitle || `文章 ID: ${currentSuggestion?.articleId}` }}
            </n-descriptions-item>
            <n-descriptions-item label="提议者">
              {{ currentSuggestion?.proposerName || '匿名用户' }}
            </n-descriptions-item>
            <n-descriptions-item label="提交时间">
              {{ formatDate(currentSuggestion?.createTime) }}
            </n-descriptions-item>
            <n-descriptions-item label="修改说明">
              {{ currentSuggestion?.changeSummary || '无' }}
            </n-descriptions-item>
          </n-descriptions>
        </div>

        <!-- 差异对比区域 -->
        <div class="diff-viewer-wrapper">
          <DiffViewer
            :source-content="currentArticleContent"
            :target-content="currentSuggestionContent"
            source-title="当前文章内容"
            target-title="修改建议内容"
            source-icon="ri:file-text-line"
            target-icon="ri:edit-line"
            :show-stats="true"
          />
        </div>

        <!-- 审核操作 -->
        <n-form
          :model="reviewForm"
          label-placement="left"
          label-width="80px"
          style="margin-top: 20px;"
        >
          <n-form-item label="审核结果">
            <n-radio-group v-model:value="reviewForm.status">
              <n-space>
                <n-radio :value="true">
                  <n-tag type="success" size="small">通过</n-tag>
                </n-radio>
                <n-radio :value="false">
                  <n-tag type="error" size="small">拒绝</n-tag>
                </n-radio>
              </n-space>
            </n-radio-group>
          </n-form-item>
          
          <n-form-item
            v-if="!reviewForm.status"
            label="拒绝理由"
          >
            <n-input
              v-model:value="reviewForm.reason"
              type="textarea"
              placeholder="请输入拒绝理由（选填）"
              :rows="3"
              maxlength="500"
              show-count
            />
          </n-form-item>
        </n-form>
      </div>
      
      <template #action>
        <n-space justify="end">
          <n-button @click="reviewModalVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="reviewing"
            @click="submitReview"
          >
            提交审核
          </n-button>
        </n-space>
      </template>
    </n-modal>

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
            <n-descriptions-item label="提议者">
              <UserAvatarLink
                :user-id="currentSuggestion?.proposerId"
                :nickname="currentSuggestion?.proposerName || '匿名用户'"
                :avatar="currentSuggestion?.proposerAvatar"
                :size="24"
              />
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
          </n-descriptions>
        </div>

        <div class="detail-body">
          <h4 class="section-title">内容差异对比</h4>
          <DiffViewer
            :source-content="currentArticleContent"
            :target-content="currentSuggestionContent"
            source-title="当前文章内容"
            target-title="修改建议内容"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import DiffViewer from '@/components/common/DiffViewer.vue'
import { articleSuggestionAPI, type ArticleEditSuggestion } from '@/api/articleSuggestion'
import { articleAPI } from '@/api/article'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()

// 响应式数据
const loading = ref(false)
const suggestions = ref<ArticleEditSuggestion[]>([])
const currentStatus = ref<string>('0')
const pendingCount = ref(0)
const searchKeyword = ref('')
const pagination = reactive({
  page: 1,
  limit: 20,
  total: 0
})

// 模态框相关
const reviewModalVisible = ref(false)
const detailModalVisible = ref(false)
const currentSuggestion = ref<ArticleEditSuggestion | null>(null)
const reviewing = ref(false)

// 审核表单
const reviewForm = reactive({
  status: true as boolean,
  reason: ''
})

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
      limit: pagination.limit,
      status: 0,
      keyword: ''
    }
    
    if (currentStatus.value !== 'all') {
      params.status = parseInt(currentStatus.value)
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    // 调用获取"我收到的建议"接口
    const response = await articleSuggestionAPI.getMyReceivedSuggestions(params)
    const data = response.data.data
    
    suggestions.value = data.list || []
    pagination.total = data.total || 0
    
    // 统计待审核数量
    if (currentStatus.value === '0') {
      pendingCount.value = pagination.total
    } else {
      // 单独查询待审核数量
      const pendingResponse = await articleSuggestionAPI.getMyReceivedSuggestions({ 
        status: 0, 
        page: 1, 
        limit: 1 
      })
      pendingCount.value = pendingResponse.data.data?.total || 0
    }
  } catch (error) {
    console.error('加载建议列表失败:', error)
    message.error('加载建议列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 打开审核对话框
 */
const openReviewDialog = async (item: ArticleEditSuggestion) => {
  currentSuggestion.value = item
  reviewForm.status = true
  reviewForm.reason = ''
  
  try {
    // 获取建议详情
    const detailResponse = await articleSuggestionAPI.getById(item.id)
    const suggestionData = detailResponse.data.data
    
    // 获取当前文章内容
    const articleResponse = await articleAPI.getById(item.articleId)
    const articleData = articleResponse.data.data
    
    // 保存内容用于对比
    currentArticleContent.value = articleData.content || {}
    currentSuggestionContent.value = suggestionData.content || {}
    
    reviewModalVisible.value = true
  } catch (error) {
    console.error('加载建议详情失败:', error)
    message.error('加载建议详情失败')
  }
}

/**
 * 提交审核
 */
const submitReview = async () => {
  if (!currentSuggestion.value) return

  reviewing.value = true
  try {
    await articleSuggestionAPI.review(currentSuggestion.value.id, {
      approved: reviewForm.status,
      reason: reviewForm.status ? undefined : reviewForm.reason
    })

    message.success(reviewForm.status ? '已通过建议' : '已拒绝建议')
    reviewModalVisible.value = false
    loadSuggestions()
  } catch (error) {
    console.error('审核失败:', error)
    message.error('审核失败，请重试')
  } finally {
    reviewing.value = false
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
 * 确认删除
 */
const confirmDelete = (item: ArticleEditSuggestion) => {
  dialog.warning({
    title: '删除建议',
    content: '确定要删除这条修改建议吗？此操作不可恢复。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await articleSuggestionAPI.delete(item.id)
        message.success('删除成功')
        loadSuggestions()
      } catch (error) {
        console.error('删除失败:', error)
        message.error('删除失败')
      }
    }
  })
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
const getStatusType = (status: number) => {
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
const getStatusText = (status: number) => {
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
 * 返回文章详情页或上一页
 */
const goBack = () => {
  const articleId = route.query.articleId as string

  if (articleId) {
    // 优先返回文章详情页
    router.push({
      path: '/index/articleDetail',
      query: { id: articleId }
    })
  } else {
    // 如果没有文章 ID，返回上一页（如从侧边栏菜单进入）
    router.back()
  }
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
    grid-template-columns: repeat(auto-fill, minmax(650px, 1fr));
    
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

      .proposer-info {
        display: flex;
        align-items: center;
        gap: 8px;
        
        .proposer-name {
          font-size: 14px;
          color: #666;
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
        gap: 16px;
        font-size: 13px;
        color: #999;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
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

.review-dialog-content {
  .suggestion-info {
    margin-bottom: 20px;
  }

  .diff-viewer-wrapper {
    margin: 20px 0;
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
