<!-- src/views/discover/RecommendUsers.vue -->
<template>
  <PageContainer header-title="推荐用户" :show-back="false">
    <div class="recommend-users-page">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索感兴趣的用户..."
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
      </div>

      <!-- 用户列表 -->
      <div class="users-grid">
        <n-spin :show="loading">
          <div v-if="users.length === 0 && !loading" class="empty-state">
            <n-empty description="暂无推荐用户" />
          </div>

          <div v-else class="users-content">
            <UserCard
              v-for="user in users"
              :key="user.id"
              :user="user"
              @click="handleUserClick"
              @follow="handleFollow"
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
import { useMessage, NInput, NSpin, NEmpty, NPagination, NIcon } from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import UserCard from '@/components/discover/UserCard.vue'
import type { UserInfo } from '@/types/discover'
import userApi from '@/api/user'

const router = useRouter()
const message = useMessage()

const users = ref<UserInfo[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const total = ref(0)
const pagination = ref({
  page: 1,
  limit: 20,
  totalPages: 0
})

/**
 * 加载推荐用户列表
 */
const loadUsers = async () => {
  try {
    loading.value = true
    
    const result = await userApi.getRecommendedUsers({
      page: pagination.value.page,
      limit: pagination.value.limit,
      keyword: searchKeyword.value
    })
    
    users.value = result.list.map((item: any) => ({
      id: item.id,
      nickname: item.nickname,
      avatar: item.avatar,
      signature: item.signature,
      followerCount: item.followerCount,
      followingCount: item.followingCount,
      articleCount: item.articleCount,
      isFollowing: item.isFollowing,
      isOnline: item.isOnline
    }))
    total.value = result.total
    pagination.value.totalPages = result.totalPage
    
  } catch (error: any) {
    console.error('加载推荐用户失败:', error)
    message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索用户
 */
const handleSearch = () => {
  pagination.value.page = 1
  loadUsers()
}

/**
 * 点击用户卡片
 */
const handleUserClick = (user: UserInfo) => {
  router.push(`/user/${user.id}`)
}

/**
 * 关注/取消关注用户
 */
const handleFollow = async (user: UserInfo) => {
  try {
    // TODO: 调用后端 API 关注/取消关注
    // await $http.post(`/users/follow/${user.id}`)
    
    user.isFollowing = !user.isFollowing
    message.success(user.isFollowing ? '关注成功' : '已取消关注')
    
  } catch (error: any) {
    console.error('操作失败:', error)
    message.error(error.message || '操作失败')
  }
}

/**
 * 页码变化
 */
const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadUsers()
}

/**
 * 每页数量变化
 */
const handlePageSizeChange = (pageSize: number) => {
  pagination.value.limit = pageSize
  pagination.value.page = 1
  loadUsers()
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.recommend-users-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 24px;
  
  .n-input {
    max-width: 400px;
  }
}

.users-grid {
  .empty-state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
  }

  .users-content {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .user-card {
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

    .user-avatar-wrapper {
      position: relative;
      display: inline-block;
      margin-bottom: 16px;

      .online-indicator {
        position: absolute;
        bottom: 4px;
        right: 4px;
        width: 12px;
        height: 12px;
        background: #18a058;
        border: 2px solid #fff;
        border-radius: 50%;
      }
    }

    .user-info {
      margin-bottom: 16px;

      .user-name {
        font-size: 16px;
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
      }

      .user-signature {
        font-size: 13px;
        color: #999;
        margin-bottom: 12px;
        line-height: 1.6;
      }

      .user-stats {
        display: flex;
        gap: 16px;
        font-size: 13px;
        color: #666;

        .stat-item {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .user-actions {
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
