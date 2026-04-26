<template>
  <div class="login-container">
    <div class="background-layer" :style="backgroundStyle"></div>

    <div class="login-left">
      <div class="brand-section">
        <h1 class="brand-title">游戏社区交流平台</h1>
        <p class="brand-subtitle">欢迎回来！</p>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <div class="login-header">
          <h2 class="title">用户登录</h2>
          <p class="subtitle">请输入您的账号和密码</p>
        </div>

        <NForm
          ref="formRef"
          :model="loginForm"
          :rules="formRules"
          label-placement="left"
          label-width="90px"
          size="large"
        >
          <NFormItem label="账号" path="account">
            <NInput
              v-model:value="loginForm.account"
              placeholder="请输入账号"
              clearable
            >
              <template #prefix>
                <Icon icon="ri:user-line" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="密码" path="password">
            <NInput
              v-model:value="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password-on="click"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <Icon icon="ri:lock-line" />
              </template>
            </NInput>
          </NFormItem>

          <!-- 用户类型选择 -->
          <NFormItem
            v-if="availableUserRoles.length > 1"
            label="用户类型"
            path="role"
          >
            <NSelect
              v-model:value="loginForm.role"
              :options="roleOptions"
              placeholder="请选择用户类型"
            />
          </NFormItem>

          <NFormItem>
            <NCheckbox v-model:checked="shouldRememberPassword">
              记住密码
            </NCheckbox>
          </NFormItem>

          <NButton
            type="primary"
            size="large"
            :loading="loginLoading"
            block
            @click="handleLogin"
          >
            {{ loginLoading ? '登录中...' : '登录' }}
          </NButton>

          <div class="register-link">
            <span>还没有账号？</span>
            <NButton text type="primary" @click="navigateToRegister">
              立即注册
            </NButton>
          </div>
        </NForm>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NCheckbox, NSelect, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { Icon } from '@iconify/vue'
import menu from '@/utils/menu'
import { connectWebSocketOnStartup } from '@/utils/websocketInit'
import toolUtil from '@/utils/toolUtil'
import userApi from '@/api/user'

interface UserRole {
  roleName: string
  hasFrontLogin: string
}

interface LoginForm {
  role: string
  account: string
  password: string
}

const router = useRouter()
const formRef = ref<FormInst | null>(null)
const message = useMessage()

const availableUserRoles = ref<UserRole[]>([])
const loginForm = ref<LoginForm>({
  role: '',
  account: '',
  password: ''
})

const shouldRememberPassword = ref<boolean>(true)
const loginLoading = ref<boolean>(false)
const backgroundImageUrl = ref('')
const imageLoaded = ref(false)

const formRules: FormRules = {
  account: {
    required: true,
    message: '请输入账号',
    trigger: 'blur'
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur'
  },
  role: {
    required: true,
    message: '请选择角色',
    trigger: 'change'
  }
}

const roleOptions = computed(() => {
  return availableUserRoles.value.map(role => ({
    label: role.roleName,
    value: role.roleName
  }))
})

const backgroundStyle = computed(() => {
  if (imageLoaded.value && backgroundImageUrl.value) {
    return {
      backgroundImage: `url(${backgroundImageUrl.value})`,
      opacity: 1
    }
  }
  return {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    opacity: imageLoaded.value ? 0 : 1
  }
})

const loadBackgroundImage = () => {
  const cachedUrl = sessionStorage.getItem('loginBackgroundImage')
  const cachedTime = sessionStorage.getItem('loginBackgroundImageTime')

  if (cachedUrl && cachedTime) {
    const now = Date.now()
    const cacheAge = now - parseInt(cachedTime)
    const maxAge = 30 * 60 * 1000

    if (cacheAge < maxAge) {
      backgroundImageUrl.value = cachedUrl
      setTimeout(() => {
        imageLoaded.value = true
      }, 50)
      return
    }
  }

  const img = new Image()
  const url = 'https://image.smallbottle.top/landscape/'

  img.onload = () => {
    backgroundImageUrl.value = img.src
    sessionStorage.setItem('loginBackgroundImage', img.src)
    sessionStorage.setItem('loginBackgroundImageTime', Date.now().toString())

    setTimeout(() => {
      imageLoaded.value = true
    }, 50)
  }

  img.onerror = () => {
    console.warn('[背景图] 加载失败，使用渐变背景')
    imageLoaded.value = false
  }

  img.src = url
}

const navigateToRegister = (): void => {
  router.push('/userRegister')
}

const handleLogin = async (): Promise<void> => {
  try {
    await formRef.value?.validate()
    await executeLogin()
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const executeLogin = async (): Promise<void> => {
  loginLoading.value = true

  try {
    const isAdmin = loginForm.value.role === '管理员'
    const loginData = {
      account: loginForm.value.account,
      password: loginForm.value.password
    }

    const response = isAdmin
      ? await userApi.adminLogin(loginData)
      : await userApi.login(loginData)

    storeAuthInfo(response.token)
    await fetchAndStoreUserInfo(isAdmin)
    await fetchUserMenus()
    await initializeWebSocket()
    handlePasswordRemember()
    redirectToTargetPage()

  } catch (error) {
    console.error('登录失败:', error)
    message.error('登录失败，请检查账号和密码')
  } finally {
    loginLoading.value = false
  }
}

const fetchAndStoreUserInfo = async (isAdmin: boolean): Promise<void> => {
  try {
    if (!isAdmin) {
      const userData = await userApi.getCurrentUser()

      toolUtil.storageSet('userid', userData.id)
      toolUtil.storageSet('nickname', userData.nickname || '')
      toolUtil.storageSet('account', userData.account)
      toolUtil.storageSet('avatar', userData.avatar || '')
      toolUtil.storageSet('UserInfo', JSON.stringify(userData))
      toolUtil.storageSet('roleId', userData.roleId)
    } else {
      toolUtil.storageSet('account', loginForm.value.account)
      toolUtil.storageSet('nickname', '管理员')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const initializeWebSocket = async (): Promise<void> => {
  try {
    await connectWebSocketOnStartup({
      debug: process.env.NODE_ENV === 'development',
      heartbeatInterval: 30000,
      reconnectInterval: 5000,
      maxReconnectAttempts: 5
    })
  } catch (error) {
    console.error('[登录] WebSocket连接失败:', error)
  }
}

const fetchUserMenus = async (): Promise<void> => {
  try {
    const menus = await menu.fetchUserMenus()

    if (menus && menus.length > 0) {
      menus.forEach(menuItem => {
        menuItem.roleName = loginForm.value.role
      })
      toolUtil.storageSet('menus', JSON.stringify(menus))
    }
  } catch (error) {
    console.error('获取菜单失败:', error)
  }
}

const handlePasswordRemember = (): void => {
  if (shouldRememberPassword.value) {
    const formToSave = { ...loginForm.value }
    toolUtil.storageSet('frontLoginForm', JSON.stringify(formToSave))
  } else {
    toolUtil.storageRemove('frontLoginForm')
  }
}

const storeAuthInfo = (token: string): void => {
  toolUtil.storageSet('Token', token)
  toolUtil.storageSet('role', loginForm.value.role)
}

const redirectToTargetPage = (): void => {
  const redirectPath = toolUtil.storageGet('toPath')

  if (redirectPath) {
    router.push(redirectPath)
    toolUtil.storageRemove('toPath')
    return
  }

  router.push('/index/home')
}

const loadMenuData = async (): Promise<void> => {
  menu.list() && toolUtil.storageRemove('menus')

  let menus = menu.list()

  if (!menus) {
    try {
      const response = await menu.list()
      if (response && response.length > 0) {
        menus = response
      }
    } catch (error) {
      console.error('获取菜单失败:', error)
    }
  }

  availableUserRoles.value = (menus || []).filter(
    (menu: UserRole) => menu.hasFrontLogin === '是'
  )

  if (availableUserRoles.value.length === 0) {
    availableUserRoles.value = [{
      roleName: '用户',
      hasFrontLogin: '是'
    }]
  }
}

const initializePage = async (): Promise<void> => {
  await loadMenuData()

  const savedLoginForm = toolUtil.storageGet('frontLoginForm')

  if (savedLoginForm) {
    loginForm.value = JSON.parse(savedLoginForm)
  } else if (availableUserRoles.value.length > 0) {
    loginForm.value.role = availableUserRoles.value[0].roleName
  }
}

onMounted(() => {
  initializePage()
  loadBackgroundImage()
})
</script>

<style lang="scss" scoped>
.login-container {
  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: hidden;
}

.background-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transition: opacity 1s ease-in-out;
  z-index: 0;
}

.login-left {
  flex: 1;
  position: relative;
  z-index: 1;

  .brand-section {
    position: absolute;
    bottom: 80px;
    left: 80px;
    color: #fff;
    z-index: 2;

    .brand-title {
      font-size: 42px;
      font-weight: 700;
      margin: 0 0 16px 0;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    }

    .brand-subtitle {
      font-size: 18px;
      opacity: 0.9;
      margin: 0;
      letter-spacing: 2px;
    }
  }

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(to bottom, rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0.4));
    z-index: 1;
  }
}

.login-right {
  width: 500px;
  background: rgba(255,255,255,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 2;
}

.login-card {
  width: 100%;
  max-width: 460px;

  .login-header {
    text-align: center;
    margin-bottom: 40px;

    .logo-wrapper {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #18a058 0%, #18a058 100%);
      border-radius: 50%;
      margin-bottom: 20px;
      box-shadow: 0 8px 24px rgba(24, 160, 88, 0.3);
    }

    .title {
      font-size: 28px;
      font-weight: 700;
      color: #1a1a1a;
      margin: 0 0 8px 0;
    }

    .subtitle {
      font-size: 14px;
      color: #999;
      margin: 0;
    }
  }

  .register-link {
    margin-top: 24px;
    text-align: center;
    font-size: 14px;
    color: #666;

    span {
      margin-right: 8px;
    }
  }
}

@media (max-width: 768px) {
  .login-left {
    display: none;
  }

  .login-right {
    width: 100%;
  }
}
</style>
