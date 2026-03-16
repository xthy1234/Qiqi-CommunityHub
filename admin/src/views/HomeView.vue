NEW_FILE_CODE
<template>
  <div class="home-view">
    <!-- 欢迎卡片 -->
    <NCard class="welcome-card" size="large">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎使用社区管理平台</h1>
        <p class="welcome-subtitle">高效、便捷、专业的社区运营管理工具</p>
      </div>
    </NCard>

    <!-- 数据统计卡片 -->
    <div class="stats-grid">
      <NCard class="stat-card" size="large">
        <div class="stat-icon user">
          <Icon icon="ri:user-smile-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">12,458</div>
          <div class="stat-label">总用户数</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large">
        <div class="stat-icon article">
          <Icon icon="ri:article-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">8,326</div>
          <div class="stat-label">帖子总数</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large">
        <div class="stat-icon comment">
          <Icon icon="ri:message-3-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">45,892</div>
          <div class="stat-label">评论总数</div>
        </div>
      </NCard>

      <NCard class="stat-card" size="large">
        <div class="stat-icon report">
          <Icon icon="ri:alert-line" :width="32" :height="32" />
        </div>
        <div class="stat-info">
          <div class="stat-value">23</div>
          <div class="stat-label">待处理举报</div>
        </div>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { NCard } from 'naive-ui'
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

    &.article {
      background: linear-gradient(135deg, #3498db 0%, #5dade2 100%);
      color: #fff;
    }

    &.comment {
      background: linear-gradient(135deg, #9b59b6 0%, #af7ac5 100%);
      color: #fff;
    }

    &.report {
      background: linear-gradient(135deg, #e74c3c 0%, #ec7063 100%);
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
    }

    .stat-label {
      font-size: 14px;
      color: #85888e;
    }
  }
}
</style>
