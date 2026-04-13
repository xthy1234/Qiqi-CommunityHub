// src/views/user/UserSignIn.vue
<template>
  <PageContainer
    header-title="积分中心"
    :show-back="false"
  >
    <div class="sign-in-container">
      <!-- 签到卡片 -->
      <n-card class="sign-in-card">
        <div class="sign-in-header">
          <h2 class="card-title">
            每日签到
          </h2>
          <n-tag
            :type="signedInToday ? 'success' : 'primary'"
            size="large"
          >
            {{ signedInToday ? '今日已签到' : '快去签到' }}
          </n-tag>
        </div>

        <div class="sign-in-content">
          <!-- 积分展示 -->
          <div class="points-display">
            <div class="points-item">
              <div class="points-label">
                当前积分
              </div>
              <div class="points-value">
                {{ points }}
              </div>
            </div>
            <div class="divider" />
            <div class="points-item">
              <div class="points-label">
                连续签到
              </div>
              <div class="points-value">
                {{ streak }}<span class="days-unit">天</span>
              </div>
            </div>
          </div>

          <!-- 签到按钮 -->
          <div class="sign-in-action">
            <n-button
              v-if="!signedInToday"
              type="primary"
              size="large"
              :loading="signingIn"
              class="sign-in-btn"
              @click="handleSignIn"
            >
              <template #icon>
                <Icon icon="ri:calendar-check-line" />
              </template>
              立即签到
            </n-button>
            <n-button
              v-else
              type="success"
              size="large"
              disabled
              class="sign-in-btn"
            >
              <template #icon>
                <Icon icon="ri:checkbox-circle-line" />
              </template>
              明日再来
            </n-button>
          </div>

          <!-- 签到规则说明 -->
          <div class="sign-in-rules">
            <h4 class="rules-title">
              签到规则
            </h4>
            <ul class="rules-list">
              <li>每日签到可获得 <strong>10 积分</strong></li>
              <li>连续签到 3 天额外奖励 <strong>10 积分</strong></li>
              <li>连续签到 7 天额外奖励 <strong>20 积分</strong></li>
              <li>积分可用于兑换礼品、下载资源等</li>
            </ul>
          </div>
        </div>
      </n-card>

      <!-- 积分流水 -->
      <n-card class="transactions-card">
        <template #header>
          <div class="card-header">
            <h3 class="card-title">
              积分明细
            </h3>
            <div class="header-actions">
              <n-select
                v-model:value="transactionFilter.source"
                :options="sourceOptions"
                placeholder="来源类型"
                clearable
                style="width: 150px"
                @update:value="handleFilterChange"
              />
              <n-date-picker
                v-model:value="dateRange"
                type="daterange"
                placeholder="选择日期范围"
                clearable
                style="width: 240px"
                @update:value="handleFilterChange"
              />
            </div>
          </div>
        </template>

        <n-data-table
          :columns="columns"
          :data="transactionList"
          :loading="transactionsLoading"
          :pagination="false"
          :scroll-x="1000"
        />

        <div class="pagination-wrapper">
          <n-pagination
            v-model:page="pagination.page"
            :item-count="transactionTotal"
            :page-size="pagination.limit"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          >
            <template #prefix="{ itemCount }">
              共 {{ itemCount }} 条
            </template>
          </n-pagination>
        </div>
      </n-card>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, NButton, NTag, NCard, NDataTable, NPagination, NSelect, NDatePicker } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { usePointsStore } from '@/stores/points'
import signInService from '@/api/signIn'
import type { PointsTransaction } from '@/api/signIn'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const pointsStore = usePointsStore()

// 签到相关状态
const points = ref<number>(0)
const streak = ref<number>(0)
const signedInToday = ref<boolean>(false)
const signingIn = ref<boolean>(false)
const isLoadingPoints = ref<boolean>(false)

// 积分流水相关状态
const transactionList = ref<PointsTransaction[]>([])
const transactionTotal = ref<number>(0)
const transactionsLoading = ref<boolean>(false)

// 筛选条件
const transactionFilter = reactive({
  source: '',
  startDate: '',
  endDate: ''
})

const dateRange = ref<[number, number] | null>(null)

// 分页
const pagination = ref({
  page: 1,
  limit: 10
})

// 数据来源选项
const sourceOptions = [
  { label: '每日签到', value: 'sign_in' },
  { label: '发布文章', value: 'post_article' },
  { label: '文章被点赞', value: 'like_received' },
  { label: '发表评论', value: 'post_comment' },
  { label: '评论被点赞', value: 'comment_like_received' },
  { label: '分享文章', value: 'share_article' },
  { label: '关注用户', value: 'follow_user' },
  { label: '邀请新用户', value: 'invite_user' }
]

// 表格列定义
const columns = [
  {
    title: '日期',
    key: 'createdAt',
    width: 180,
    render: (row: PointsTransaction) => {
      return h('span', {}, dayjs(row.createdAt).format('YYYY-MM-DD HH:mm:ss'))
    }
  },
  {
    title: '来源',
    key: 'source',
    width: 150,
    render: (row: PointsTransaction) => {
      const sourceMap: Record<string, string> = {
        sign_in: '每日签到',
        post_article: '发布文章',
        like_received: '文章被点赞',
        post_comment: '发表评论',
        comment_like_received: '评论被点赞',
        share_article: '分享文章',
        follow_user: '关注用户',
        invite_user: '邀请新用户'
      }
      return h(NTag, { type: 'info' }, { default: () => sourceMap[row.source] || row.source })
    }
  },
  {
    title: '变动积分',
    key: 'amount',
    width: 120,
    render: (row: PointsTransaction) => {
      const color = row.amount > 0 ? '#18a058' : '#d03050'
      const sign = row.amount > 0 ? '+' : ''
      return h('span', { style: { color, fontWeight: 'bold' } }, `${sign}${row.amount}`)
    }
  },
  {
    title: '当前余额',
    key: 'balance',
    width: 120,
    render: (row: PointsTransaction) => {
      return h('span', { style: { color: '#f0a020', fontWeight: 'bold' } }, row.balance)
    }
  },
  {
    title: '描述',
    key: 'description',
    minWidth: 200,
    ellipsis: { tooltip: true }
  }
]

/**
 * 加载积分和签到信息 - 修复版
 */
const loadPointsInfo = async () => {
  try {
    isLoadingPoints.value = true

    // 并行调用两个接口
    const [pointsData, statusData] = await Promise.all([
      signInService.getPointsInfo(),
      signInService.getSignInStatus()
    ])

    // 更新本地状态
    points.value = pointsData.points
    streak.value = pointsData.streak
    signedInToday.value = statusData.signedIn  // ✅ 使用正确的字段名

    // 同时更新 Store
    pointsStore.points = pointsData.points
    pointsStore.streak = pointsData.streak
    pointsStore.signedInToday = statusData.signedIn  // ✅ 使用正确的字段名

  } catch (error: any) {
    console.error('加载积分信息失败:', error)

    // 降级方案
    try {
      const pointsData = await signInService.getPointsInfo()
      points.value = pointsData.points
      streak.value = pointsData.streak
      signedInToday.value = false

      console.warn('使用降级方案，签到状态设为 false')
    } catch (fallbackError) {
      message.error('加载积分信息失败：' + (error.message || '未知错误'))
    }
  } finally {
    isLoadingPoints.value = false
  }
}

/**
 * 处理签到
 */
const handleSignIn = async () => {
  if (signedInToday.value) {
    message.warning('今日已签到')
    return
  }

  try {
    signingIn.value = true
    const result = await pointsStore.signIn()
    
    message.success(`签到成功！获得 ${result.pointsEarned} 积分，连续签到 ${result.streak} 天`)
    
    // 更新显示数据
    points.value = result.balance
    streak.value = result.streak
    signedInToday.value = true
    
    // 刷新积分流水
    loadTransactions()
  } catch (error: any) {
    message.error(error.message || '签到失败')
  } finally {
    signingIn.value = false
  }
}

/**
 * 加载积分流水
 */
const loadTransactions = async () => {
  try {
    transactionsLoading.value = true
    
    const params: any = {
      page: pagination.value.page,
      limit: pagination.value.limit
    }
    
    if (transactionFilter.source) {
      params.source = transactionFilter.source
    }
    
    if (transactionFilter.startDate) {
      params.startDate = transactionFilter.startDate
    }
    
    if (transactionFilter.endDate) {
      params.endDate = transactionFilter.endDate
    }
    
    const result = await pointsStore.loadTransactions(params)
    transactionList.value = result.list
    transactionTotal.value = result.total
  } catch (error) {
    console.error('加载积分流水失败:', error)
    message.error('加载积分流水失败')
  } finally {
    transactionsLoading.value = false
  }
}

/**
 * 处理分页变化
 */
const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadTransactions()
}

/**
 * 处理每页数量变化
 */
const handlePageSizeChange = (pageSize: number) => {
  pagination.value.limit = pageSize
  pagination.value.page = 1
  loadTransactions()
}

/**
 * 处理筛选条件变化
 */
const handleFilterChange = () => {
  if (dateRange.value) {
    transactionFilter.startDate = dayjs(dateRange.value[0]).format('YYYY-MM-DD')
    transactionFilter.endDate = dayjs(dateRange.value[1]).format('YYYY-MM-DD')
  } else {
    transactionFilter.startDate = ''
    transactionFilter.endDate = ''
  }
  
  pagination.value.page = 1
  loadTransactions()
}

onMounted(() => {

  loadPointsInfo()
  loadTransactions()
})
</script>

<style scoped lang="scss">
.sign-in-container {
  max-width: 1200px;
  margin: 0 auto;
}

.sign-in-card {
  margin-bottom: 20px;
  
  .sign-in-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    .card-title {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }
  }
  
  .sign-in-content {
    .points-display {
      display: flex;
      justify-content: space-around;
      align-items: center;
      padding: 30px 0;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 12px;
      margin-bottom: 30px;
      
      .points-item {
        text-align: center;
        
        .points-label {
          color: rgba(255, 255, 255, 0.8);
          font-size: 14px;
          margin-bottom: 8px;
        }
        
        .points-value {
          color: #fff;
          font-size: 36px;
          font-weight: bold;
          
          .days-unit {
            font-size: 16px;
            margin-left: 4px;
          }
        }
      }
      
      .divider {
        width: 1px;
        height: 60px;
        background: rgba(255, 255, 255, 0.3);
      }
    }
    
    .sign-in-action {
      text-align: center;
      margin-bottom: 30px;
      
      .sign-in-btn {
        width: 200px;
        height: 50px;
        font-size: 18px;
      }
    }
    
    .sign-in-rules {
      background: #f5f7fa;
      padding: 20px;
      border-radius: 8px;
      
      .rules-title {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 12px;
        color: #303133;
      }
      
      .rules-list {
        list-style: none;
        padding: 0;
        margin: 0;
        
        li {
          padding: 8px 0;
          color: #606266;
          font-size: 14px;
          
          strong {
            color: #f0a020;
          }
        }
      }
    }
  }
}

.transactions-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .card-title {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
  
  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
