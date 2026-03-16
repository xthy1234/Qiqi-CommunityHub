<template>
  <div class="main_layout">
    <!-- 主内容区（左侧边栏 + 右侧内容） -->
    <div class="main_container" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <!-- 左侧边栏 -->
      <Sidebar @update:collapsed="handleSidebarCollapsed" />

      <!-- 右侧内容区 -->
      <div class="content_scrollbar">
        <div class="page_content">
          <router-view />
        </div>

        <!-- 底部信息 -->
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import Sidebar from '@/components/Sidebar.vue'
import UserAvatarDropdown from '@/components/UserAvatarDropdown.vue'
import { useGlobalProperties } from '@/utils/globalProperties'

const appContext = useGlobalProperties()
const router = useRouter()

const authToken = ref<boolean>(false)
const currentDate = ref<string>('')
const currentTime = ref<string>('')
const isSidebarCollapsed = ref<boolean>(false)
let timer: any = null

const handleSidebarCollapsed = (collapsed: boolean) => {
  isSidebarCollapsed.value = collapsed
}

const initializePage = async () => {
  const token = appContext?.$toolUtil?.storageGet('Token')
  authToken.value = token
  updateDateTime()
}

const updateDateTime = () => {
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN')
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleLogin = () => {
  router.push('/login')
}

const handleLogout = () => {
  appContext?.$toolUtil?.storageClear()
  router.push('/login')
}

// 每秒更新时间
const startTimer = () => {
  timer = setInterval(() => {
    updateDateTime()
  }, 1000)
}

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

//.header_bar {
//  padding: 0 10%;
//  z-index: 1002;
//  color: #fff;
//  display: flex;
//  box-sizing: border-box;
//  background: rgba(48, 48, 48, 0.4);
//  width: 100%;
//  justify-content: flex-end;
//  border-width: 0 0 0px;
//  align-items: flex-start;
//  position: fixed;
//  border-style: solid;
//  //height: 68px;
//
//  .site_title {
//    color: #fff;
//    left: 10%;
//    background: none;
//    font-weight: 500;
//    display: flex;
//    width: auto;
//    font-size: 22px;
//    line-height: 1.2;
//    align-items: center;
//    position: absolute;
//    //height: 68px;
//  }
//
//  .header_right {
//    color: #fff;
//    background: none;
//    display: flex;
//    width: auto;
//    justify-content: flex-end;
//    align-items: center;
//    position: relative;
//    gap: 20px;
//
//    .weather_time_panel {
//      display: flex;
//
//      .time_info {
//        padding: 0 20px;
//        display: flex;
//        font-size: 14px;
//        line-height: 68px;
//        justify-content: center;
//        align-items: center;
//        gap: 10px;
//
//        .current_date {
//          margin: 0;
//        }
//      }
//    }
//  }
//}

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
