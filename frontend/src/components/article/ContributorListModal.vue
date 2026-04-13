<template>
  <n-modal
    v-model:show="modalVisible"
    preset="card"
    title="贡献者列表"
    :style="{ width: '800px', maxWidth: '90vw' }"
    :closable="true"
  >
    <!-- 排序选项 -->
    <div class="sort-options">
      <n-space>
        <span>排序方式：</span>
        <n-radio-group
          v-model:value="orderBy"
          @update:value="loadContributors"
        >
          <n-radio-button value="score">
            按贡献分
          </n-radio-button>
          <n-radio-button value="lastContributedAt">
            按活跃时间
          </n-radio-button>
        </n-radio-group>
      </n-space>
      
      <div class="total-count">
        共 {{ total }} 位贡献者
      </div>
    </div>

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

    <!-- 贡献者列表 -->
    <div
      v-else-if="contributors.length > 0"
      class="contributors-list"
    >
      <div
        v-for="(contributor, index) in contributors"
        :key="contributor.userId"
        class="contributor-card"
        :class="{ 'top-3': index < 3 }"
      >
        <div class="rank">
          {{ index + 1 }}
        </div>
        
        <UserAvatarLink
          :user-id="contributor.userId"
          :nickname="contributor.nickname"
          :avatar="contributor.avatar"
          :size="48"
          show-name
        />
        
        <div class="stats-grid">
          <div class="stat-item added">
            <span class="label">新增</span>
            <span class="value">+{{ contributor.addedLines }}</span>
          </div>
          <div class="stat-item modified">
            <span class="label">修改</span>
            <span class="value">~{{ contributor.modifiedLines }}</span>
          </div>
          <div class="stat-item deleted">
            <span class="label">删除</span>
            <span class="value">-{{ contributor.deletedLines }}</span>
          </div>
        </div>
        
        <div class="score-badge">
          <div class="score">
            {{ contributor.score.toFixed(1) }}
          </div>
          <div class="label">
            贡献分
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <n-empty
      v-else
      description="暂无贡献者"
      style="padding: 40px;"
    />

    <!-- 分页 -->
    <div class="pagination-container">
      <n-pagination
        v-model:page="currentPage"
        v-model:page-size="pageSize"
        :item-count="total"
        :page-sizes="[10, 20, 50]"
        show-size-picker
        @update-page="loadContributors"
        @update-page-size="handlePageSizeChange"
      />
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NModal, NCard, NSpace, NRadioButton, NRadioGroup, NSkeleton, NEmpty, NPagination } from 'naive-ui'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import { articleContributorAPI, type Contributor } from '@/api/contributor'

const props = defineProps<{
  articleId: number | string
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

// 响应式数据
const modalVisible = ref(false)
const loading = ref(false)
const contributors = ref<Contributor[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const orderBy = ref<'score' | 'lastContributedAt'>('score')

// 监听 show 属性变化
watch(() => props.show, (newVal) => {
  modalVisible.value = newVal
  if (newVal) {
    loadContributors()
  }
}, { immediate: true })

// 监听 modalVisible 变化，同步到父组件
watch(modalVisible, (newVal) => {
  emit('update:show', newVal)
})

/**
 * 加载贡献者列表
 */
const loadContributors = async () => {
  loading.value = true
  try {
    const response = await articleContributorAPI.getList(props.articleId, {
      page: currentPage.value,
      limit: pageSize.value,
      orderBy: orderBy.value
    })
    
    const result = response.data
    if (result.code === 0) {
      contributors.value = result.data.list || result.data
      total.value = result.data.total || result.data.length
    }
  } catch (error) {
    console.error('加载贡献者列表失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 处理页面大小变化
 */
const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadContributors()
}
</script>

<style lang="scss" scoped>
.sort-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  
  .total-count {
    font-size: 14px;
    color: #666;
    font-weight: 500;
  }
}

.loading-container {
  padding: 20px;
}

.contributors-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contributor-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }
  
  &.top-3 {
    background: linear-gradient(135deg, #fff9e6 0%, #ffffff 100%);
    border: 1px solid #ffd700;
  }
  
  .rank {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #e0e0e0;
    color: #666;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;
    font-size: 16px;
    
    .top-3 & {
      background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
      color: #fff;
    }
  }
  
  .stats-grid {
    display: flex;
    gap: 16px;
    margin-left: auto;
    margin-right: 24px;
    
    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 60px;
      
      .label {
        font-size: 12px;
        color: #999;
        margin-bottom: 4px;
      }
      
      .value {
        font-size: 16px;
        font-weight: 600;
        
        &.added { color: #52c41a; }
        &.modified { color: #1890ff; }
        &.deleted { color: #ff4d4f; }
      }
    }
  }
  
  .score-badge {
    text-align: center;
    padding: 8px 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 8px;
    min-width: 80px;
    
    .score {
      font-size: 20px;
      font-weight: bold;
    }
    
    .label {
      font-size: 12px;
      opacity: 0.9;
    }
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
