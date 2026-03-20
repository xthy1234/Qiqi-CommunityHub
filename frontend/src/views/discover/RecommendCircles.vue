<!-- src/views/discover/RecommendCircles.vue -->
<template>
  <PageContainer header-title="推荐圈子" :show-back="false">
    <template #headerExtra>
      <n-button type="primary" @click="navigateToCreate">
        <Icon icon="ri:add-circle-line" style="margin-right: 4px;" />
        创建圈子
      </n-button>
    </template>

    <div class="recommend-circles-page">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索感兴趣的圈子..."
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <n-icon size="18">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24">
                <path fill="currentColor" d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5C16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
              </svg>
            </n-icon>
          </template>
        </n-input>
        
        <div class="search-bar-right">
          <n-radio-group v-model:value="circleType" @change="handleTypeChange">
            <n-space>
              <n-radio :value="null">全部</n-radio>
              <n-radio :value="1">公开</n-radio>
              <n-radio :value="0">私密</n-radio>
            </n-space>
          </n-radio-group>
        </div>
      </div>

      <!-- 圈子列表 -->
      <div class="circles-grid">
        <n-spin :show="loading">
          <div v-if="circles.length === 0 && !loading" class="empty-state">
            <n-empty description="暂无推荐圈子" />
          </div>

          <div v-else class="circles-content">
            <CircleCard
              v-for="circle in circles"
              :key="circle.id"
              :circle="circle"
              @click="handleCircleClick"
              @join="handleJoinCircle"
            />
          </div>

          <!-- 分页 -->
          <n-pagination
            v-if="total > 0"
            v-model:page="pagination.page"
            :item-count="total"
            :page-size="pagination.limit"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
            class="pagination"
          >
            <template #prefix="{ itemCount }">
              共 {{ itemCount }} 条
            </template>
          </n-pagination>
        </n-spin>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, NInput, NSpin, NEmpty, NPagination, NRadioGroup, NRadio, NSpace, NButton, NIcon } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import CircleCard from '@/components/discover/CircleCard.vue'
import { getAvatarUrl } from '@/utils/userUtils'
import dayjs from 'dayjs'
import type { CircleInfo } from '@/types/discover'
import { circleMemberApi } from '@/api/circle'

const router = useRouter()
const message = useMessage()

const circles = ref<CircleInfo[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const circleType = ref<number | null>(null)
const total = ref(0)
const pagination = ref({
  page: 1,
  limit: 20,
  totalPages: 0
})

// 状态控制
const showCreateModal = ref(false)
const creatingLoading = ref(false)

/**
 * 加载推荐圈子列表
 */
const loadCircles = async () => {
  try {
    loading.value = true
    
    const result = await circleMemberApi.getRecommendedCircles({
      page: pagination.value.page,
      limit: pagination.value.limit,
      keyword: searchKeyword.value
    })
    
    circles.value = result.list.map((item: any) => ({
      id: item.id,
      name: item.name,
      avatar: item.avatar,
      description: item.description,
      type: item.type,
      memberCount: item.memberCount,
      ownerId: item.ownerId,
      ownerNickname: item.ownerNickname,
      isJoined: item.isJoined,
      createTime: item.createTime
    }))
    total.value = result.total
    pagination.value.totalPages = result.total
    
  } catch (error: any) {
    console.error('加载推荐圈子失败:', error)
    message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索圈子
 */
const handleSearch = () => {
  pagination.value.page = 1
  loadCircles()
}

/**
 * 类型切换
 */
const handleTypeChange = () => {
  pagination.value.page = 1
  loadCircles()
}

/**
 * 点击圈子卡片
 */
const handleCircleClick = (circle: CircleInfo) => {
  if (circle.isJoined) {
    // 已加入，跳转到圈子聊天
    router.push('/index/circle-chat')
  } else {
    // 未加入，显示详情或提示加入
    message.info('请先加入圈子')
  }
}

/**
 * 加入/退出圈子
 */
const handleJoinCircle = async (circle: CircleInfo) => {
  if (circle.isJoined) {
    message.warning('您已加入该圈子')
    return
  }

  try {
    await circleMemberApi.joinCircle(circle.id)
    
    message.success('加入成功')
    circle.isJoined = true
    circle.memberCount = (circle.memberCount || 0) + 1

    // 刷新列表
    await loadCircles()
    
  } catch (error: any) {
    console.error('加入圈子失败:', error)
    message.error(error.message || '加入失败')
  }
}

/**
 * 页码变化
 */
const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadCircles()
}

/**
 * 每页数量变化
 */
const handlePageSizeChange = (pageSize: number) => {
  pagination.value.limit = pageSize
  pagination.value.page = 1
  loadCircles()
}

/**
 * 获取名称首字母
 */
const getInitials = (name: string) => {
  if (!name) return ''
  return name.charAt(0).toUpperCase()
}

/**
 * 格式化创建时间
 */
const formatCreateTime = (time?: string) => {
  if (!time) return '未知'
  return dayjs(time).format('YYYY-MM-DD')
}

/**
 * 跳转到创建页面
 */
const navigateToCreate = () => {
  router.push('/index/circle/editor')
}

onMounted(() => {
  loadCircles()
})
</script>

<style scoped lang="scss">
.recommend-circles-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;

  .n-input {
    max-width: 400px;
  }

  .search-bar-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.circles-grid {
  .empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
  }

  .circles-content {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
  }

  .circle-card {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: all 0.3s;
    cursor: pointer;
    position: relative;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
    }

    .circle-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;

      .circle-basic-info {
        flex: 1;
        min-width: 0;

        .circle-name {
          font-size: 16px;
          font-weight: 600;
          color: #333;
          margin-bottom: 6px;
        }
      }
    }

    .circle-description {
      font-size: 14px;
      color: #666;
      margin-bottom: 16px;
      line-height: 1.6;
    }

    .circle-stats {
      display: flex;
      gap: 16px;
      font-size: 13px;
      color: #999;
      margin-bottom: 16px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .circle-actions {
      text-align: center;
    }
  }

  .pagination {
    margin-top: 24px;
    display: flex;
    justify-content: center;
  }
}
</style>
