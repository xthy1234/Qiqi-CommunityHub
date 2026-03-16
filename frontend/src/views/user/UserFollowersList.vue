<template>
  <PageContainer :header-title="pageTitle" :show-back="true">
    <div class="followers-list-page">
      <!-- 用户信息摘要 -->
      <div class="user-summary">
        <n-skeleton v-if="loading" :width="200" height="30" />
        <template v-else>
          <span class="summary-text">
            <Icon icon="ri:user-community-line" :size="20" />
            {{ userInfo.nickname || userInfo.account }} 拥有 {{ total }} 位粉丝
          </span>
        </template>
      </div>
      
      <!-- 粉丝列表 -->
      <div class="list-container">
        <n-spin :show="loading">
          <div v-if="userList.length > 0" class="user-list">
            <UserCard
              v-for="item in userList"
              :key="item.id"
              :user="item"
              :scene-type="'followers'"
              :currentUserId="currentUserId"
              @follow-change="handleFollowChange"
            />
          </div>
          
          <n-empty
            v-else-if="!loading"
            description="还没有粉丝哦"
            size="large"
          />
        </n-spin>
      </div>
      
      <!-- 分页 -->
      <n-pagination
        v-if="total > 0 && !loading"
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
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NSkeleton, NSpin, NEmpty, NPagination } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/PageContainer.vue'
import UserCard from '@/components/UserCard.vue'
import followApi, { type FollowRecord } from '@/api/follow'
import userService from '@/api/user'
import { useGlobalProperties } from '@/utils/globalProperties'

interface UserInfo {
  id: number
  account: string
  nickname?: string
}

const route = useRoute()
const appContext = useGlobalProperties()
const currentUserId = ref<number | string>('')

const userInfo = ref<UserInfo>({ id: 0, account: '' })
const userList = ref<FollowRecord[]>([])
const loading = ref(true)
const total = ref(0)
const pagination = ref({ page: 1, limit: 20 })

const pageTitle = computed(() => {
  return userInfo.value.nickname
    ? `${userInfo.value.nickname} 的粉丝列表`
    : '粉丝列表'
})

const fetchUserInfo = async () => {
  try {
    const userId = Number(route.params.id)
    const response = await userService.getUserById(userId)
    userInfo.value = {
      id: response.id,
      account: response.account,
      nickname: response.nickname
    }

    // 获取当前登录用户的 ID
    const currentUserStr = appContext?.$toolUtil?.storageGet('userid')
    currentUserId.value = currentUserStr ? Number(currentUserStr) : ''
  } catch (error) {
    console.error('获取用户信息失败:', error)
    appContext?.$toolUtil?.message('获取用户信息失败', 'error')
  }
}

const fetchFollowerList = async () => {
  loading.value = true
  try {
    const userId = Number(route.params.id)
    const response = await followApi.getFollowerList(
      userId,
      pagination.value.page,
      pagination.value.limit
    )

    // 根据实际 API 返回结构调整
    // 后端返回格式：{totalCount, pageSize, totalPage, currPage, list}
    const apiData = response || {}
    userList.value = apiData.list || []
    total.value = apiData.totalCount || 0

  } catch (error) {
    console.error('获取粉丝列表失败:', error)
    appContext.value?.$toolUtil.message('获取粉丝列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  fetchFollowerList()
}

const handlePageSizeChange = (size: number) => {
  pagination.value.limit = size
  pagination.value.page = 1
  fetchFollowerList()
}

const handleFollowChange = ({ userId, isFollowing }: { userId: number; isFollowing: boolean }) => {
  if (!isFollowing) {
    // 取消关注后从列表中移除
    userList.value = userList.value.filter(u => u.id !== userId)
    total.value--
  }
}

onMounted(() => {
  Promise.all([fetchUserInfo(), fetchFollowerList()])
})
</script>

<style lang="scss" scoped>
.followers-list-page {
  max-width: 900px;
  margin: 0 auto;

  .user-summary {
    padding: 20px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;

    .summary-text {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .list-container {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 20px;
    margin-bottom: 20px;

    .user-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
  }

  .pagination {
    display: flex;
    justify-content: center;
  }
}
</style>
