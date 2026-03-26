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
    <div
      class="sidebar-menu-wrapper"
      :class="{ 'menu-scrollable': menuItems.length > 0 }"
    >
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
        class="sidebar-menu"
        @update:value="handleMenuSelect"
        @update:expanded-keys="handleExpandedChange"
      />
    </div>

    <!-- 底部操作区 -->
    <div class="sidebar-footer">
      <div
        class="manual-toggle-wrapper"
        @click.stop="handleManualToggle"
      >
        <div class="manual-toggle-button">
          <Icon
            :icon="isExpanded ? 'ri:arrow-left-s-line' : 'ri:arrow-right-s-line'"
            width="20"
          />
          <span v-if="isExpanded">{{ sidebarLocked ? '点击收起' : '自动' }}</span>
          <!--          <span v-else>点击展开</span>-->
        </div>
      </div>

      <!-- 固定开关 -->
      <div
        class="toggle-button-wrapper"
        @click="toggleSidebarLock"
      >
        <div class="toggle-button">
          <Icon
            :icon="!sidebarLocked ? 'ri:pushpin-line' : 'ri:lock-unlock-line'"
            width="20"
          />
          <span v-if="isExpanded">{{ sidebarLocked ? '解除锁定' : '锁定展开' }}</span>
        </div>
      </div>

      <!-- 外部链接 -->
      <div
        v-if="isExpanded"
        class="external-links"
      >
        <a
          href="https://github.com"
          target="_blank"
          class="external-link"
          title="GitHub"
        >
          <Icon
            icon="ri:github-fill"
            width="20"
          />
          <span>GitHub</span>
        </a>
      </div>

      <!-- 用户头像区域 -->
      <div
        class="user-avatar-section"
        @click="handleAvatarClick"
      >
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
          <Icon
            icon="ri:user-line"
            :size="20"
          />
        </n-avatar>
        <div
          v-if="isExpanded"
          class="user-info"
        >
          <div class="user-nickname">
            {{ userNickname || '未登录' }}
          </div>
          <div class="user-account">
            {{ userAccount || '点击登录' }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { h } from 'vue'
import { useRouter } from 'vue-router'
import {NMenu, type MenuOption, useDialog} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { useNotificationStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'
import { useCircleChatStore } from '@/stores/circleChat'

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
const pendingCount = ref<number>(0)
const notificationUnreadCount = ref<number>(0)
const chatUnreadCount = ref<number>(0)
const circleChatUnreadCount = ref<number>(0)
const dialog = useDialog()

let closeTimer: ReturnType<typeof setTimeout> | null = null

// 渲染图标函数
const renderIcon = (icon: string) => {
  return () => h(Icon, { icon, size: 18 })
}

// 渲染徽章
const renderBadge = (count: number) => {
  return () => h(
    'span',
    {
      style: {
        position: 'absolute',
        top: '4px',
        right: '8px',
        background: '#f00',
        color: '#fff',
        borderRadius: '50%',
        padding: '2px 6px',
        fontSize: '10px',
        minWidth: '16px',
        textAlign: 'center'
      }
    },
    { default: () => count > 99 ? '99+' : String(count) }
  )
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
      children: [
        {
          label: '文章',
          key: 'discover-article',
          icon: renderIcon('ri:article-line'),
          click: () => navigateToRoute('/index/articleList')
        },
        {
          label: '用户',
          key: 'discover-user',
          icon: renderIcon('ri:user-follow-line'),
          click: () => navigateToRoute('/index/recommend-users')
        },
        {
          label: '圈子',
          key: 'discover-circle',
          icon: renderIcon('ri:group-2-line'),
          click: () => navigateToRoute('/index/recommend-circles')
        }
      ]
    },
    {
      label: '消息',
      key: 'message',
      icon: renderIcon('ri:mail-line'),
      extra: ((chatUnreadCount.value || 0) + (circleChatUnreadCount.value || 0)) > 0 ? renderBadge((chatUnreadCount.value || 0) + (circleChatUnreadCount.value || 0)) : undefined,
      children: [
        {
          label: '私聊',
          key: 'message-chat',
          icon: renderIcon('ri:chat-1-line'),
          extra: (chatUnreadCount.value || 0) > 0 ? renderBadge(chatUnreadCount.value || 0) : undefined,
          click: () => navigateToRoute('/index/chat')
        },
        {
          label: '圈子',
          key: 'message-circle',
          icon: renderIcon('ri:user-group-line'),
          extra: (circleChatUnreadCount.value || 0) > 0 ? renderBadge(circleChatUnreadCount.value || 0) : undefined,
          click: () => navigateToRoute('/index/circle-chat')
        }
      ]
    },
    {
      label: '通知',
      key: 'notification',
      icon: renderIcon('ri:notification-badge-line'),
      extra: notificationUnreadCount.value > 0 ? renderBadge(notificationUnreadCount.value) : undefined,
      click: () => navigateToRoute('/index/notifications')
    },
    {
      label: '创作',
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
    },

    {
      label: '协作',
      key: 'collaboration',
      icon: renderIcon('ri:team-line'),
      children: [
        {
          label: '我的建议',
          key: 'my-suggestions',
          icon: renderIcon('ri:edit-circle-line'),
          click: () => navigateToMySuggestions()
        },
        {
          label: '待审建议',
          key: 'publish-suggestions',
          icon: renderIcon('ri:review-line'),
          click: () => navigateToSuggestions(),
          extra: pendingCount.value > 0 ? renderBadge(pendingCount.value) : undefined
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
        if (found) {return found}
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

// 监听聊天未读数变化
watch(() => {
  const chatStore = useChatStore()
  return chatStore.unreadCount
}, (newCount: number) => {
  chatUnreadCount.value = newCount
}, { immediate: true })

// 监听圈子聊天未读数变化
watch(() => {
  const circleChatStore = useCircleChatStore()
  return circleChatStore.totalUnreadCount
}, (newCount: number) => {
  circleChatUnreadCount.value = newCount
}, { immediate: true })

// 监听通知未读数变化（实时更新角标）
watch(() => {
  const notificationStore = useNotificationStore()
  return notificationStore.unreadCount
}, (newCount :number) => {
  notificationUnreadCount.value = newCount
}, { immediate: true })

const navigateToRoute = (path: string): void => {
  if (!path) {return}
  router.push(path.startsWith('/') ? path : `/index/${path}`)

  // 如果是聊天页面，自动收起侧边栏
  if (path.includes('chat')) {
    isExpanded.value = false
  }
}

const navigateToSuggestions = (): void => {
  // 跳转到待审核建议列表页（作者所有文章的待审建议）
  router.push('/index/article/suggestions?status=0')
}

const navigateToMySuggestions = (): void => {
  // 跳转到"我的建议"列表页（查看自己提交的建议）
  router.push('/index/my-suggestions')
}

const navigateToVersions = (): void => {
  // 版本管理需要先选择文章，这里给出提示
  dialog.info({
    title: '版本管理',
    content: '请先在文章列表中打开要查看版本历史的文章详情页，然后点击"版本历史"按钮。',
    positiveText: '知道了',
    onPositiveClick: () => {
      router.push('/index/articleList')
    }
  })
}

const navigateToContributors = (): void => {
  // 贡献者页面也需要先选择文章
  dialog.info({
    title: '贡献者列表',
    content: '请先在文章列表中打开要查看贡献者的文章详情页，然后查看底部的贡献者列表。',
    positiveText: '知道了',
    onPositiveClick: () => {
      router.push('/index/articleList')
    }
  })
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
    // 🔒 锁定时：保存当前展开状态到 localStorage
    appContext?.$toolUtil?.storageSet('sidebarExpanded', isExpanded.value)
  } else {
    // 🔓 解锁时：清除展开状态记录，并强制收起
    isExpanded.value = false
    expandedKeys.value = []
    appContext?.$toolUtil?.storageRemove('sidebarExpanded')
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
// console.error('加载菜单失败:', error)
    menuItems.value = []
  }
}

const initializeComponent = async (): Promise<void> => {
  const token = appContext?.$toolUtil?.storageGet('Token')
  authToken.value = !!token

  // 🔒 读取锁定状态
  const locked = appContext?.$toolUtil?.storageGet('sidebarLocked')

  if(locked===null){
    sidebarLocked.value = true
    appContext?.$toolUtil?.storageSet('sidebarLocked', true)
  }else{
    sidebarLocked.value = Boolean(locked)
  }

  // 📦 读取展开/收起状态
  const savedExpanded = appContext?.$toolUtil?.storageGet('sidebarExpanded')

  if (savedExpanded === null) {
    isExpanded.value = true
    appContext?.$toolUtil?.storageSet('sidebarExpanded', true)
  } else {
    isExpanded.value = Boolean(savedExpanded)
  }

  await loadMenuData()
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

  // 加载待审核建议数量
  loadPendingSuggestionsCount()
  // 加载未读通知数量
  loadNotificationUnreadCount()
}

const loadPendingSuggestionsCount = async (): Promise<void> => {
  try {
    // TODO: 调用 API 获取待审核建议数量
    // const response = await articleSuggestionAPI.getMyReceivedSuggestions({ status: 0, page: 1, limit: 1 })
    // pendingCount.value = response.data.data?.total || 0
    pendingCount.value = 0 // 临时值，实际应该调用 API
  } catch (error) {
    console.error('加载待审核数量失败:', error)
  }
}

const loadNotificationUnreadCount = async (): Promise<void> => {
  try {
    const notificationStore = useNotificationStore()
    await notificationStore.loadUnreadCount()
    notificationUnreadCount.value = notificationStore.unreadCount
  } catch (error) {
    console.error('加载未读通知数量失败:', error)
  }
}

const loadChatUnreadCount = async (): Promise<void> => {
  try {
    const chatStore = useChatStore()
    await chatStore.loadConversations()
    chatUnreadCount.value = chatStore.unreadCount
  } catch (error) {
    console.error('加载私聊未读数量失败:', error)
  }
}

const loadCircleChatUnreadCount = async (): Promise<void> => {
  try {
    const circleChatStore = useCircleChatStore()
    circleChatUnreadCount.value = circleChatStore.totalUnreadCount
  } catch (error) {
    console.error('加载圈子聊天未读数量失败:', error)
  }
}

const getFullUrl = (path: string, baseUrl?: string): string => {
  if (!path) {return ''}
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

  // 初始化通知 Store
  const notificationStore = useNotificationStore()
  // 如果已建立 WebSocket 连接，订阅通知队列
  if ((window as any).stompClient) {
    notificationStore.initWebSocketSubscription((window as any).stompClient)
  }

  // 加载私聊未读数量
  loadChatUnreadCount()
  // 加载圈子聊天未读数量
  loadCircleChatUnreadCount()
})

const emit = defineEmits<{
  (e: 'update:collapsed', collapsed: boolean): void
}>()

// 监听侧边栏展开状态变化，通知父组件并保存到 localStorage
watch(isExpanded, (newVal: boolean, oldVal: boolean) => {
  //  只有状态真正改变时才保存（避免初始化时重复保存）
  if (newVal !== oldVal) {
    try {
      //  只有在锁定状态下才保存展开状态
      if (sidebarLocked.value) {
        appContext?.$toolUtil?.storageSet('sidebarExpanded', newVal)
      } else {
        // 解锁状态，不保存展开状态
      }
    } catch (error) {
      // 保存展开状态失败
    }

    //  通知父组件
    emit('update:collapsed', !newVal)
  }
})

//  新增：监听锁定状态变化
watch(sidebarLocked, (newLocked: boolean, oldLocked: boolean) => {
  if (newLocked !== oldLocked) {
    if (!newLocked) {
      // 🔓 解锁时，强制收起并清除状态记录
      isExpanded.value = false
      expandedKeys.value = []
      appContext?.$toolUtil?.storageRemove('sidebarExpanded')
    } else {
      // 🔒 锁定时，保存当前展开状态
      appContext?.$toolUtil?.storageSet('sidebarExpanded', isExpanded.value)
    }
  }
})

const handleManualToggle = (): void => {
  if (!sidebarLocked.value && !isExpanded.value) {
    sidebarLocked.value = true
    appContext?.$toolUtil?.storageSet('sidebarLocked', true)
  }

  isExpanded.value = !isExpanded.value
}

</script>

<style scoped lang="scss">
  .sidebar-container {
    width: 64px;
    height: 100vh;
    background: linear-gradient(180deg, #ffffff 0%, #f5f7fa 100%);
    transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: column;
    position: fixed;
    top: 0;
    left: 0;
    //overflow: visible;
    //box-shadow: 2px 0 12px rgba(0, 0, 0, 0.08);
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
      overflow-y: auto;
      overflow-x: hidden;
      min-height: 0;
      position: relative;

      // 自定义滚动条样式
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

      .manual-toggle-wrapper {
        border-bottom: 1px solid #f0f0f0;

        .manual-toggle-button {
          height: 48px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          cursor: pointer;
          color: #303133;
          transition: all 0.3s;
          font-size: 13px;
          background: rgba(24, 160, 88, 0.05);

          &:hover {
            background: rgba(24, 160, 88, 0.1);
            color: #18a058;
          }
        }
      }

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
