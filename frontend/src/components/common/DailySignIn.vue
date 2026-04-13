
<template>
  <n-card
    :bordered="false"
    class="daily-signin-card"
    @click="handleCardClick"
  >
    <div class="signin-content">
      <!-- 左侧：签到状态 -->
      <div class="signin-status">
        <div class="icon-wrapper">
          <Icon
            :icon="signedInToday ? 'ri:checkbox-circle-fill' : 'ri:calendar-check-line'"
            :size="56"
            :class="['status-icon', signedInToday ? 'signed' : 'unsigned']"
          />
        </div>
      </div>

      <!-- 右侧：信息展示 -->
      <div class="signin-info">
        <div class="header">
          <h3 class="title">
            {{ signedInToday ? '今日已签到' : '每日签到' }}
          </h3>
          <n-tag
            :type="signedInToday ? 'success' : 'warning'"
            size="small"
            round
          >
            {{ signedInToday ? '明日再来' : '立即领取' }}
          </n-tag>
        </div>

        <div class="points-display">
          <div class="points-item">
            <span class="label">当前积分</span>
            <span class="value">{{ points }}</span>
          </div>
          <div class="divider" />
          <div class="points-item">
            <span class="label">连续签到</span>
            <span class="value streak">{{ streak }}<small>天</small></span>
          </div>
        </div>

        <div class="bonus-rules">
          <div class="rule-item">
            <Icon
              icon="ri:coin-line"
              :size="14"
            />
            <span>基础奖励 <strong>10 积分</strong></span>
          </div>
          <div class="rule-item">
            <Icon
              icon="ri:medal-line"
              :size="14"
            />
            <span>连续 3 天额外 <strong>+10 积分</strong></span>
          </div>
          <div class="rule-item">
            <Icon
              icon="ri:crown-line"
              :size="14"
            />
            <span>连续 7 天额外 <strong>+20 积分</strong></span>
          </div>
        </div>

        <div class="action">
          <n-button
            v-if="!signedInToday"
            type="primary"
            size="medium"
            :loading="signingIn"
            class="signin-btn"
            @click.stop="handleSignIn"
          >
            <template #icon>
              <Icon icon="ri:gift-2-line" />
            </template>
            立即签到
          </n-button>
          <n-button
            v-else
            type="success"
            size="medium"
            disabled
            class="signin-btn"
          >
            <template #icon>
              <Icon icon="ri:checkbox-circle-line" />
            </template>
            已签到
          </n-button>
          
          <n-button
            quaternary
            class="detail-btn"
            @click.stop="navigateToDetail"
          >
            查看详情
            <template #icon>
              <Icon icon="ri:arrow-right-line" />
            </template>
          </n-button>
        </div>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, NCard, NButton, NTag } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { usePointsStore } from '@/stores/points'
import signInService from '@/api/signIn'

const router = useRouter()
const message = useMessage()
const pointsStore = usePointsStore()

// 状态数据
const points = ref<number>(0)
const streak = ref<number>(0)
const signedInToday = ref<boolean>(false)
const signingIn = ref<boolean>(false)
const isLoading = ref<boolean>(false)

/**
 * 加载积分信息 - 优先使用 /sign-in/points 接口
 */
const loadPointsInfo = async () => {
  try {
    isLoading.value = true

    // 并行调用两个接口
    const [pointsData, statusData] = await Promise.all([
      signInService.getPointsInfo(),
      signInService.getSignInStatus()
    ])

    points.value = pointsData.points
    streak.value = pointsData.streak
    signedInToday.value = statusData.signedIn  // ✅ 使用正确的字段名

    // 更新 Store
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
    } catch (retryError) {
      console.error('备用方案也失败了:', retryError)
    }
  } finally {
    isLoading.value = false
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
    
    message.success(
      `签到成功！获得 ${result.pointsEarned} 积分，连续签到 ${result.streak} 天`,
      { duration: 3000 }
    )
    
    // 更新显示数据
    points.value = result.balance
    streak.value = result.streak
    signedInToday.value = true
    
    // 触发刷新事件
    emit('refresh')
  } catch (error: any) {
    message.error(error.message || '签到失败')
  } finally {
    signingIn.value = false
  }
}

/**
 * 跳转到详情页
 */
const navigateToDetail = () => {
  router.push('/index/user/signin')
}

/**
 * 卡片点击事件
 */
const handleCardClick = () => {
  // 如果点击的不是按钮区域，也跳转到详情页
  navigateToDetail()
}

/**
 * 刷新数据（供外部调用）
 */
const refresh = () => {
  loadPointsInfo()
}

// 暴露方法给父组件
defineExpose({
  refresh
})

// 定义事件
const emit = defineEmits<{
  (e: 'refresh'): void
}>()

onMounted(() => {

  loadPointsInfo()
})
</script>

<style scoped lang="scss">
.daily-signin-card {
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
  
  :deep(.n-card__content) {
    padding: 0;
  }
  
  .signin-content {
    display: flex;
    padding: 20px;
  }
  
  .signin-status {
    display: flex;
    align-items: center;
    justify-content: center;
    padding-right: 20px;
    
    .icon-wrapper {
      position: relative;
      
      .status-icon {
        &.signed {
          color: #18a058;
          filter: drop-shadow(0 2px 8px rgba(24, 160, 88, 0.3));
        }
        
        &.unsigned {
          color: #f0a020;
          animation: pulse 2s infinite;
        }
      }
    }
  }
  
  .signin-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
    
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .title {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        margin: 0;
      }
    }
    
    .points-display {
      display: flex;
      align-items: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 16px;
      border-radius: 8px;
      
      .points-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        
        .label {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.8);
        }
        
        .value {
          font-size: 24px;
          font-weight: bold;
          color: #fff;
          
          &.streak {
            small {
              font-size: 12px;
              margin-left: 2px;
            }
          }
        }
      }
      
      .divider {
        width: 1px;
        height: 40px;
        background: rgba(255, 255, 255, 0.3);
        margin: 0 24px;
      }
    }
    
    .bonus-rules {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      
      .rule-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #606266;
        background: #f5f7fa;
        padding: 6px 10px;
        border-radius: 4px;
        
        strong {
          color: #f0a020;
          font-weight: 600;
        }
      }
    }
    
    .action {
      display: flex;
      gap: 12px;
      align-items: center;
      
      .signin-btn {
        height: 40px;
        padding: 0 24px;
        border-radius: 20px;
        font-weight: 600;
      }
      
      .detail-btn {
        margin-left: auto;
        color: #909399;
        
        &:hover {
          color: #18a058;
        }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.05);
  }
}

@media (max-width: 768px) {
  .signin-content {
    flex-direction: column;
    padding: 16px;
  }
  
  .signin-status {
    padding-right: 0;
    margin-bottom: 16px;
  }
  
  .points-display {
    .points-item {
      .value {
        font-size: 20px;
      }
    }
    
    .divider {
      margin: 0 16px;
    }
  }
  
  .bonus-rules {
    .rule-item {
      font-size: 11px;
      padding: 4px 8px;
    }
  }
  
  .action {
    flex-wrap: wrap;
    
    .signin-btn {
      flex: 1;
      min-width: 120px;
    }
    
    .detail-btn {
      margin-left: 0;
      width: 100%;
    }
  }
}
</style>
