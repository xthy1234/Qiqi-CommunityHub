<template>
  <div class="home-view">
    <!-- 欢迎卡片 -->
    <NCard class="welcome-card" size="large">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎使用社区管理平台</h1>
        <p class="welcome-subtitle">高效、便捷、专业的社区运营管理工具</p>
      </div>
    </NCard>

    <!-- 核心指标卡片 -->
    <div class="stats-grid">
      <NCard class="stat-card" size="large" @click="handleStatClick('totalUsers')">
        <div class="stat-icon user">
          <Icon icon="ri:user-smile-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(dashboardStats.totalUsers) }}</div>
          <div class="stat-label">总用户数</div>
          <div v-if="dashboardStats.userGrowthRate !== undefined && dashboardStats.userGrowthRate !== null" class="stat-growth">
            <NTag
              :type="dashboardStats.userGrowthRate >= 0 ? 'success' : 'error'"
              size="small"
            >
              {{ dashboardStats.userGrowthRate >= 0 ? '↑' : '↓' }}
              {{ Math.abs(dashboardStats.userGrowthRate).toFixed(2) }}%
            </NTag>
            <span class="growth-hint">较昨日</span>
          </div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large" @click="handleStatClick('todayNewUsers')">
        <div class="stat-icon new-user">
          <Icon icon="ri:user-add-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(dashboardStats.todayNewUsers) }}</div>
          <div class="stat-label">今日新增用户</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large" @click="handleStatClick('totalArticles')">
        <div class="stat-icon article">
          <Icon icon="ri:article-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(dashboardStats.totalArticles) }}</div>
          <div class="stat-label">文章总数</div>
          <div v-if="dashboardStats.articleGrowthRate !== undefined && dashboardStats.articleGrowthRate !== null" class="stat-growth">
            <NTag
              :type="dashboardStats.articleGrowthRate >= 0 ? 'success' : 'error'"
              size="small"
            >
              {{ dashboardStats.articleGrowthRate >= 0 ? '↑' : '↓' }}
              {{ Math.abs(dashboardStats.articleGrowthRate).toFixed(2) }}%
            </NTag>
            <span class="growth-hint">较昨日</span>
          </div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large" @click="handleStatClick('todayNewArticles')">
        <div class="stat-icon new-article">
          <Icon icon="ri:add-circle-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(dashboardStats.todayNewArticles) }}</div>
          <div class="stat-label">今日新增文章</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large" @click="handleStatClick('pendingAuditArticles')">
        <div class="stat-icon pending">
          <Icon icon="ri:file-warning-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value warning">{{ formatNumber(dashboardStats.pendingAuditArticles) }}</div>
          <div class="stat-label">待审核文章</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large" @click="handleStatClick('pendingReports')">
        <div class="stat-icon report">
          <Icon icon="ri:alert-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value danger">{{ formatNumber(dashboardStats.pendingReports) }}</div>
          <div class="stat-label">待处理举报</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large" @click="handleStatClick('totalComments')">
        <div class="stat-icon comment">
          <Icon icon="ri:message-3-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(dashboardStats.totalComments) }}</div>
          <div class="stat-label">评论总数</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large">
        <div class="stat-icon active">
          <Icon icon="ri:fire-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(dashboardStats.todayActiveUsers) }}</div>
          <div class="stat-label">今日活跃用户</div>
        </div>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { NCard, NTag, useMessage } from 'naive-ui'
import statsApi, { type DashboardStats } from '@/api/stats'

const router = useRouter()
const message = useMessage()

const dashboardStats = ref<DashboardStats>({
  totalUsers: 0,
  todayNewUsers: 0,
  yesterdayNewUsers: 0,
  userGrowthRate: 0,
  totalArticles: 0,
  todayNewArticles: 0,
  yesterdayNewArticles: 0,
  articleGrowthRate: 0,
  pendingAuditArticles: 0,
  pendingReports: 0,
  totalComments: 0,
  todayActiveUsers: 0
})

const loading = ref(false)

const fetchDashboardStats = async () => {
  loading.value = true
  try {
    const res = await statsApi.getDashboardStats()

    if (res.code === 0 || res.code === 200) {
      dashboardStats.value = res.data
    } else {
      message.error(res.msg || '加载统计数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    message.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const formatNumber = (num: number): string => {
  return num.toLocaleString('zh-CN')
}

const handleStatClick = (key: string) => {
  const routeMap: Record<string, string> = {
    totalUsers: '/users',
    todayNewUsers: '/users',
    totalArticles: '/articles',
    todayNewArticles: '/articles',
    pendingAuditArticles: '/articles/audit',
    pendingReports: '/admin/reports',
    totalComments: '/comments'
  }

  const route = routeMap[key]
  if (route) {
    router.push(route)
  }
}

onMounted(() => {
  fetchDashboardStats()
})
</script>

<style lang="scss" scoped>
.home-view {
  padding: 0;
}

.welcome-card {
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .welcome-content {
    padding: 20px 0;

    .welcome-title {
      margin: 0 0 8px 0;
      font-size: 28px;
      font-weight: 600;
      color: #18a058;
    }

    .welcome-subtitle {
      margin: 0;
      font-size: 14px;
      color: #85888e;
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  .stat-icon {
    width: 64px;
    height: 64px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;

    &.user {
      background: linear-gradient(135deg, #18a058 0%, #2ecc71 100%);
      color: #fff;
    }

    &.new-user {
      background: linear-gradient(135deg, #3498db 0%, #5dade2 100%);
      color: #fff;
    }

    &.article {
      background: linear-gradient(135deg, #9b59b6 0%, #af7ac5 100%);
      color: #fff;
    }

    &.new-article {
      background: linear-gradient(135deg, #f39c12 0%, #f5b041 100%);
      color: #fff;
    }

    &.pending {
      background: linear-gradient(135deg, #e67e22 0%, #eb984e 100%);
      color: #fff;
    }

    &.report {
      background: linear-gradient(135deg, #e74c3c 0%, #ec7063 100%);
      color: #fff;
    }

    &.comment {
      background: linear-gradient(135deg, #1abc9c 0%, #48c9b0 100%);
      color: #fff;
    }

    &.active {
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
      color: #fff;
    }
  }

  .stat-info {
    flex: 1;

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: #2c3e50;
      margin-bottom: 4px;

      &.warning {
        color: #e67e22;
      }

      &.danger {
        color: #e74c3c;
      }
    }

    .stat-label {
      font-size: 14px;
      color: #85888e;
      margin-bottom: 4px;
    }

    .stat-growth {
      display: flex;
      align-items: center;
      gap: 6px;

      .growth-hint {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
