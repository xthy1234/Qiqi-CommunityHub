<template>
  <div
      class="sidebar-container"
      :class="{ 'sidebar-expanded': isExpanded }"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
  >
    <!-- 品牌 Logo 区域 -->
    <div class="sidebar-brand">
      <div class="brand-content">
        <img
            v-if="isExpanded"
            src="/logo.ico"
            alt="Logo"
            class="brand-logo"
        />
        <img
            v-else
            src="/favicon.ico"
            alt="Logo"
            class="brand-icon"
        />
      </div>
    </div>

    <!-- 菜单区域 -->
    <div class="sidebar-menu-wrapper" :class="{ 'menu-scrollable': menuItems.length > 0 }">
      <n-menu
          ref="menuRef"
          v-model:value="activeKey"
          :mode="'vertical'"
          :collapsed="!isExpanded"
          :collapsed-width="64"
          :width="isExpanded ? 280 : 64"
          :collapsed-icon-size="22"
          :options="menuOptions"
          :indent="16"
          :default-expand-all="false"
          :expanded-keys="expandedKeys"
          @update:value="handleMenuSelect"
          @update:expanded-keys="handleExpandedChange"
          class="sidebar-menu"
      />
    </div>

    <!-- 底部操作区 -->
    <div class="sidebar-footer">
      <!-- 固定开关 -->
      <div class="toggle-button-wrapper" @click="toggleSidebarLock">
        <div class="toggle-button">
          <Icon
              :icon="!sidebarLocked ? 'ri:pushpin-line' : 'ri:lock-unlock-line'"
              width="20"
          />
          <span v-if="isExpanded">{{ sidebarLocked ? '解除锁定' : '锁定展开' }}</span>
        </div>
      </div>

      <!-- 外部链接 -->
      <div v-if="isExpanded" class="external-links">
        <a
            href="https://github.com"
            target="_blank"
            class="external-link"
            title="GitHub"
        >
          <Icon icon="ri:github-fill" width="20" />
          <span>GitHub</span>
        </a>
      </div>

      <!-- 用户头像区域 -->
      <div class="user-avatar-section" @click="handleAvatarClick">
        <n-avatar
          v-if="userAvatarUrl"
          :src="userAvatarUrl"
          :size="40"
          round
          class="user-avatar"
        />
        <n-avatar
          v-else
          :size="40"
          round
          class="user-avatar-placeholder"
        >
          <Icon icon="ri:user-line" :size="20" />
        </n-avatar>
        <div v-if="isExpanded" class="user-info">
          <div class="user-nickname">{{ userNickname || '未登录' }}</div>
          <div class="user-account">{{ userAccount || '点击登录' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NMenu, type MenuOption } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useGlobalProperties } from '@/utils/globalProperties'

interface MenuItem {
  menu: string
  child?: Array<{
    name: string
    url: string
  }>
  name?: string
}

const appContext = useGlobalProperties()
const router = useRouter()
const menuRef = ref<any>(null)

const activeKey = ref<string>('home')
const isExpanded = ref<boolean>(false)
const sidebarLocked = ref<boolean>(false)
const menuItems = ref<MenuItem[]>([])
const authToken = ref<boolean>(false)
const expandedKeys = ref<string[]>([])
const previousExpandedKeys = ref<string[]>([])
const userAvatarUrl = ref<string>('')
const userNickname = ref<string>('')
const userAccount = ref<string>('')
// const menuTheme = ref<'light' | 'dark'>('light')

let closeTimer: ReturnType<typeof setTimeout> | null = null

// 渲染图标函数
const renderIcon = (icon: string) => {
  return () => h(Icon, { icon, size: 18 })
}

// 菜单选项
const menuOptions = computed<MenuOption[]>(() => {
  const options: MenuOption[] = [
    {
      label: '首页',
      key: 'home',
      icon: renderIcon('ri:home-line'),
      click: () => navigateToRoute('/index/home')
    },
    {
      label: '发现',
      key: 'discover',
      icon: renderIcon('ri:compass-discover-line'),
      click: () => navigateToRoute('/index/articleList')
    },
    {
      label: '发布',
      key: 'publish',
      icon: renderIcon('ri:add-circle-line'),
      children: [
        {
          label: '发布文章',
          key: 'publish-article',
          icon: renderIcon('ri:edit-line'),
          click: () => navigateToRoute('/index/article/editor')
        },
        {
          label: '草稿箱',
          key: 'publish-draft',
          icon: renderIcon('ri:file-list-line'),
          click: () => navigateToRoute('/index/article/draftList')
        }
      ]
    }
  ]

  // 添加动态菜单
  const items = Array.isArray(menuItems.value) ? menuItems.value : []
  items.forEach((menu, index) => {
    if (menu.child && menu.child.length > 1) {
      options.push({
        label: menu.name || menu.menu,
        key: `menu-${index}`,
        icon: renderIcon('ri:folder-line'),
        children: menu.child.map((child, sort) => ({
          label: child.name,
          key: `menu-${index}-${sort}`,
          icon: renderIcon('ri:document-line'),
          click: () => navigateToRoute(child.url)
        }))
      })
    } else if (menu.child && menu.child.length === 1) {
      options.push({
        label: menu.child[0].name,
        key: `single-${index}`,
        icon: renderIcon('ri:document-line'),
        click: () => navigateToRoute(menu.child[0].url)
      })
    }
  })

  // 个人中心菜单
  if (authToken.value) {
    options.push({
      label: '个人中心',
      key: 'profile',
      icon: renderIcon('ri:user-line'),
      children: [
        {
          label: '个人信息',
          key: 'profile-info',
          icon: renderIcon('ri:user-smile-line'),
          click: () => navigateToRoute('/index/user/profile')
        },
        {
          label: '编辑资料',
          key: 'profile-edit',
          icon: renderIcon('ri:edit-line'),
          click: () => navigateToRoute('/index/user/profile-edit')
        },
        {
          label: '修改密码',
          key: 'profile-password',
          icon: renderIcon('ri:lock-password-line'),
          click: () => navigateToRoute('/index/user/password-change')
        },
        {
          label: '我的收藏',
          key: 'profile-favorite',
          icon: renderIcon('ri:star-line'),
          click: () => navigateToFavorite()
        },
        {
          label: '退出登录',
          key: 'profile-logout',
          icon: renderIcon('ri:logout-box-line'),
          className: 'logout-item',
          click: () => handleLogout()
        }
      ]
    })
  }

  return options
})

const handleMouseEnter = () => {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
  if (!sidebarLocked.value) {
    if (!isExpanded.value) {
      // 收起状态下展开时，恢复之前的展开状态
      expandedKeys.value = [...previousExpandedKeys.value]
    }
    isExpanded.value = true
  }
}

const handleMouseLeave = () => {
  if (!sidebarLocked.value) {
    closeTimer = setTimeout(() => {
      if (isExpanded.value) {
        // 展开状态下收起时，保存当前的展开状态
        previousExpandedKeys.value = [...expandedKeys.value]
        isExpanded.value = false
        expandedKeys.value = []
      }
    }, 200)
  }
}

const handleMenuSelect = (key: string, item: MenuOption) => {
  activeKey.value = key
  // 查找对应的菜单项并执行导航
  const findMenuItem = (options: MenuOption[], searchKey: string): MenuOption | undefined => {
    for (const option of options) {
      if (option.key === searchKey) {
        return option
      }
      if (option.children) {
        const found = findMenuItem(option.children, searchKey)
        if (found) return found
      }
    }
    return undefined
  }

  const menuItem = findMenuItem(menuOptions.value, key)
  if (menuItem && typeof menuItem.click === 'function') {
    menuItem.click()
  }
}

const handleExpandedChange = (keys: string[]) => {
  expandedKeys.value = keys
}

const navigateToRoute = (path: string): void => {
  if (!path) return
  router.push(path.startsWith('/') ? path : `/index/${path}`)
}

const navigateToFavorite = (): void => {
  router.push('/index/favoriteList?centerType=1')
}

const handleLogout = (): void => {
  appContext?.$toolUtil?.storageClear()
  router.push('/login')
}

const handleAvatarClick = (): void => {
  if (authToken.value) {
    router.push('/index/user/profile')
  } else {
    router.push('/login')
  }
}

const toggleSidebarLock = () => {
  sidebarLocked.value = !sidebarLocked.value
  appContext?.$toolUtil?.storageSet('sidebarLocked', sidebarLocked.value)

  if (sidebarLocked.value) {
    isExpanded.value = true
    // 锁定时也恢复之前的展开状态
    expandedKeys.value = [...previousExpandedKeys.value]
  } else {
    previousExpandedKeys.value = [...expandedKeys.value]
    expandedKeys.value = []
  }
}

const loadMenuData = async (): Promise<void> => {
  try {
    const menus = appContext?.$toolUtil?.storageGet('menus')
    if (menus) {
      menuItems.value = Array.isArray(menus) ? menus : []
    } else {
      menuItems.value = []
    }
  } catch (error) {
    console.error('加载菜单失败:', error)
    menuItems.value = []
  }
}

const initializeComponent = async (): Promise<void> => {
  const token = appContext?.$toolUtil?.storageGet('Token')
  authToken.value = !!token

  const locked = appContext?.$toolUtil?.storageGet('sidebarLocked')
  sidebarLocked.value = Boolean(locked)
  isExpanded.value = sidebarLocked.value

  await loadMenuData()

  // 加载用户信息
  loadUserInfo()
}

const loadUserInfo = (): void => {
  const avatar = appContext?.$toolUtil?.storageGet('avatar')
  const nickname = appContext?.$toolUtil?.storageGet('nickname')
  const account = appContext?.$toolUtil?.storageGet('account')

  if (avatar) {
    const baseUrl = appContext?.$config?.url || 'http://localhost:8080'
    userAvatarUrl.value = getFullUrl(avatar, baseUrl)
  }

  userNickname.value = nickname || ''
  userAccount.value = account || ''
}

const getFullUrl = (path: string, baseUrl?: string): string => {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  if (baseUrl) {
    return `${baseUrl}/${path}`
  }
  return path
}

onMounted(() => {
  initializeComponent()
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  width: 80px;
  height: 100vh;
  background: linear-gradient(180deg, #ffffff 0%, #f5f7fa 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  overflow: visible;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.08);
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0;
  z-index: 1000;

  &.sidebar-expanded {
    width: 280px;
  }

  .sidebar-brand {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid #ebeef5;
    background: rgba(245, 247, 250, 0.5);
    flex-shrink: 0;
    z-index: 10;

    .brand-content {
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;

      .brand-logo {
        height: 32px;
        width: auto;
        object-fit: contain;
      }

      .brand-icon {
        width: 40px;
        height: 40px;
        border-radius: 8px;
        object-fit: cover;
      }
    }
  }

  .sidebar-menu-wrapper {
    flex: 1;
    overflow-y: hidden;
    overflow-x: hidden;
    min-height: 0;
    position: relative;
    transition: overflow-y 0.3s ease;

    &.menu-scrollable:hover {
      overflow-y: auto;
    }

    .sidebar-menu {
      background: transparent !important;

      // 一级菜单项
      :deep(.n-menu-item) {
        .n-menu-item-content {
          &::before {
            left: 8px;
          }
        }
      }

      // 二级菜单项 - 更深的背景色
      :deep(.n-submenu-children) {
        .n-menu-item-content {
          &:hover {
            background-color: rgba(24, 160, 88, 0.08);
          }

          &.n-menu-item-content--selected {
            background-color: rgba(24, 160, 88, 0.1);
          }
        }
      }

      // 滚动条美化
      &::-webkit-scrollbar {
        width: 4px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      &::-webkit-scrollbar-thumb {
        background: #dcdfe6;
        border-radius: 2px;

        &:hover {
          background: #c0c4cc;
        }
      }
    }
  }

  .sidebar-footer {
    border-top: 1px solid #ebeef5;
    background: rgba(245, 247, 250, 0.95);
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    z-index: 10;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);

    .toggle-button-wrapper {
      border-top: 1px solid #f0f0f0;

      .toggle-button {
        height: 56px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        cursor: pointer;
        color: #303133;
        transition: all 0.3s;
        font-size: 13px;

        &:hover {
          background: #f5f7fa;
          color: #18a058;
        }
      }
    }

    .external-links {
      padding: 12px 16px;

      .external-link {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 12px;
        color: #303133;
        text-decoration: none;
        border-radius: 6px;
        transition: all 0.3s;

        &:hover {
          background: #f5f7fa;
          color: #18a058;
        }
      }
    }

    .user-avatar-section {
      border-top: 1px solid #f0f0f0;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        background: rgba(24, 160, 88, 0.05);
      }

      .user-avatar {
        flex-shrink: 0;
        border: 2px solid #fff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .user-avatar-placeholder {
        flex-shrink: 0;
        background-color: #e4e7ed;
        border: 2px solid #fff;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .user-info {
        flex: 1;
        overflow: hidden;
        min-width: 0;

        .user-nickname {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-bottom: 2px;
        }

        .user-account {
          font-size: 12px;
          color: #909399;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }
}
</style>
