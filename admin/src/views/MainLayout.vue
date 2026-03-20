<template>
  <div class="main_layout">
    <!-- 主内容区（左侧边栏 + 右侧内容） -->
    <div class="main_container" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <!-- 左侧边栏 -->
      <Sidebar @update:collapsed="handleSidebarCollapsed" />

      <!-- 右侧内容区 -->
      <div class="content_scrollbar">
        <div class="page_content">
          <router-view v-if="isAuthenticated" />
          <div v-else class="loading-container">
            <el-result icon="warning" title="请先登录" sub-title="您尚未登录，无法访问管理页面">
              <template #extra>
                <el-button type="primary" @click="goToLogin">去登录</el-button>
              </template>
            </el-result>
          </div>
        </div>

<!--        <div class="footer_section">-->
<!--          <div class="company_info"></div>-->
<!--          <div class="record_info"></div>-->
<!--          <div class="contact_info"></div>-->
<!--        </div>-->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/common/Sidebar.vue'
import UserAvatarDropdown from '@/components/UserAvatarDropdown.vue'
import { useGlobalProperties } from '@/utils/globalProperties'

const appContext = useGlobalProperties()
const router = useRouter()
const route = useRoute()

const authToken = ref<boolean>(false)
const isAuthenticated = ref<boolean>(false)
const currentDate = ref<string>('')
const currentTime = ref<string>('')
const isSidebarCollapsed = ref<boolean>(false)
let timer: any = null

const handleSidebarCollapsed = (collapsed: boolean) => {
  isSidebarCollapsed.value = collapsed
}

const startTimer = () => {
  timer = setInterval(() => {
    updateDateTime()
  }, 1000)
}

const checkAuth = () => {
  const token = localStorage.getItem('Token')
  authToken.value = !!token
  isAuthenticated.value = !!token
  return !!token
}

const initializePage = async () => {
  checkAuth()
  updateDateTime()
}

const updateDateTime = () => {
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const goToLogin = () => {
  router.push('/login')
}

const handleLogin = () => {
  goToLogin()
}

const handleLogout = () => {
  appContext?.$toolUtil?.storageClear()
  router.push('/login')
}

// 监听路由变化，检查登录状态
watch(() => route.path, () => {
  if (!checkAuth() && route.path !== '/login') {
    router.push('/login')
  }
})

onMounted(() => {
  initializePage()
  startTimer()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.main_layout {
  width: 100%;
  overflow: hidden;
}



.main_container {
  display: flex;
  overflow: hidden;

  &.sidebar-collapsed {
    .content_scrollbar {
      padding-left: 64px;
    }
  }
}

.content_scrollbar {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: #f5f5f5;
  padding-left: 280px;
  transition: padding-left 0.3s ease;
}

.page_content {
  min-height: calc(100vh - 68px - 120px);
  padding: 20px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

.footer_section {
  border: 0;
  padding: 20px 10% 30px;
  flex-direction: column;
  color: #fff;
  background: #333;
  display: flex;
  width: 100%;
  font-size: 14px;
  min-height: 120px;
  justify-content: center;
  align-items: center;

  .footer_logo {
    border-radius: 100%;
    object-fit: cover;
    display: none;
    width: 44px;
    height: 44px;
  }
}
</style>
