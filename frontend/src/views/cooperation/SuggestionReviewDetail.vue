<template>
  <PageContainer
    :header-title="isReviewMode ? '审核修改建议' : '建议详情'"
    :show-back="true"
    @back="goBack"
  >
    <div class="review-detail-wrapper">
      <!-- 加载状态 -->
      <div
        v-if="loading"
        class="loading-container"
      >
        <n-skeleton
          text
          :repeat="10"
        />
      </div>

      <!-- 内容区域 -->
      <div
        v-else
        class="review-content"
      >
        <!-- 基本信息卡片 -->
        <n-card
          title="建议信息"
          size="large"
          class="info-card"
        >
          <n-descriptions
            bordered
            :column="2"
            size="large"
          >
            <n-descriptions-item label="文章标题">
              {{ suggestionData?.articleTitle || `文章 ID: ${suggestionData?.articleId}` }}
            </n-descriptions-item>
            <n-descriptions-item label="提议者">
              <UserAvatarLink
                v-if="suggestionData?.proposerId"
                :user-id="suggestionData.proposerId"
                :nickname="suggestionData?.proposer?.nickname || '匿名用户'"
                :avatar="suggestionData?.proposer?.avatar"
                :size="32"
              />
              <span v-else>匿名用户</span>
            </n-descriptions-item>
            <n-descriptions-item label="提交时间">
              {{ formatDate(suggestionData?.createTime) }}
            </n-descriptions-item>
            <n-descriptions-item
              v-if="suggestionData?.reviewTime"
              label="审核时间"
            >
              {{ formatDate(suggestionData?.reviewTime) }}
            </n-descriptions-item>
            <n-descriptions-item label="当前状态">
              <n-tag
                :type="getStatusType(suggestionData?.status)"
                size="medium"
                round
              >
                {{ getStatusText(suggestionData?.status) }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="修改说明">
              {{ suggestionData?.changeSummary || '无' }}
            </n-descriptions-item>
            <n-descriptions-item
              v-if="suggestionData?.rejectReason"
              label="拒绝理由"
              :span="2"
            >
              {{ suggestionData?.rejectReason }}
            </n-descriptions-item>
          </n-descriptions>
        </n-card>

        <!-- 差异对比区域 -->
        <n-card
          title="内容差异对比"
          size="large"
          class="diff-card"
        >
          <TextDiffViewer
            :source="originalContent"
            :target="modifiedContent"
            source-label="当前文章内容"
            target-label="修改建议内容"
            source-icon="ri:file-text-line"
            target-icon="ri:edit-line"
            :show-stats="true"
          />
        </n-card>

        <!-- 审核操作卡片（仅待审核状态且为审核模式时显示） -->
        <n-card
          v-if="isReviewMode && suggestionData?.status === 0"
          title="审核操作"
          size="large"
          class="action-card"
        >
          <n-form
            :model="reviewForm"
            label-placement="left"
            label-width="100px"
          >
            <n-form-item label="审核结果">
              <n-radio-group v-model:value="reviewForm.status">
                <n-space>
                  <n-radio :value="true">
                    <n-tag type="success" size="medium">通过</n-tag>
                  </n-radio>
                  <n-radio :value="false">
                    <n-tag type="error" size="medium">拒绝</n-tag>
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
                :rows="4"
                maxlength="500"
                show-count
              />
            </n-form-item>

            <n-form-item>
              <n-space>
                <n-button
                  type="primary"
                  :loading="reviewing"
                  @click="submitReview"
                >
                  <template #icon>
                    <Icon icon="ri:checkbox-circle-line" />
                  </template>
                  提交审核
                </n-button>
                <n-button @click="goBack">
                  返回
                </n-button>
              </n-space>
            </n-form-item>
          </n-form>
        </n-card>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import TextDiffViewer from '@/components/common/TextDiffViewer.vue'
import { articleSuggestionAPI, type ArticleEditSuggestion } from '@/api/articleSuggestion'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// 响应式数据
const loading = ref(false)
const suggestionData = ref<ArticleEditSuggestion | null>(null)
const originalContent = ref<object>({})
const modifiedContent = ref<object>({})
const reviewing = ref(false)

// 审核表单
const reviewForm = reactive({
  status: true as boolean,
  reason: ''
})

// 计算是否为审核模式（从审核列表进入）
const isReviewMode = ref(false)

/**
 * 加载建议详情
 */
const loadDetail = async () => {
  const suggestionId = route.params.id as string
  
  if (!suggestionId) {
    message.error('缺少建议 ID 参数')
    goBack()
    return
  }

  loading.value = true
  try {
    const response = await articleSuggestionAPI.getById(suggestionId)
    const data = response.data.data
    
    suggestionData.value = data
    originalContent.value = data.originalContent || {}
    modifiedContent.value = data.content || {}
    
    // 判断是否为审核模式
    // mode=0 或不传：审核模式（从审核列表进入）
    // mode=1：查看模式（从我的建议进入）
    const modeParam = route.query.mode as string
    isReviewMode.value = modeParam !== '1'
  } catch (error) {
    console.error('加载建议详情失败:', error)
    message.error('加载建议详情失败')
  } finally {
    loading.value = false
  }
}

/**
 * 提交审核
 */
const submitReview = async () => {
  if (!suggestionData.value) return

  reviewing.value = true
  try {
    await articleSuggestionAPI.review(suggestionData.value.id, {
      approved: reviewForm.status,
      reason: reviewForm.status ? undefined : reviewForm.reason
    })

    message.success(reviewForm.status ? '已通过建议' : '已拒绝建议')
    
    // 审核后返回列表页
    goBack()
  } catch (error) {
    console.error('审核失败:', error)
    message.error('审核失败，请重试')
  } finally {
    reviewing.value = false
  }
}

/**
 * 返回上一页或文章详情页
 */
const goBack = () => {
  const fromArticle = route.query.from as string
  
  if (fromArticle === 'article') {
    // 从文章详情页进入的，返回文章详情页
    const articleId = route.query.articleId as string
    if (articleId) {
      router.push({
        path: '/index/articleDetail',
        query: { id: articleId }
      })
      return
    }
  }
  
  // 默认返回上一页
  router.back()
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

onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.review-detail-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

.loading-container {
  padding: 20px;
}

.review-content {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .info-card,
  .diff-card,
  .action-card {
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .diff-card {
    :deep(.text-diff-viewer) {
      margin-top: 16px;
    }
  }

  .action-card {
    background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  }
}
</style>
