<template>
  <div class="login_container">
    <div class="login_view">
      <n-form
        ref="formRef"
        :model="loginForm"
        class="login_form"
        label-placement="left"
        label-width="90px"
      >
        <div class="title_view">中文社区交流平台登录</div>

        <!-- 用户名输入 -->
        <n-form-item
          v-if="loginType === LoginTypeEnum.USERNAME"
          label="账号："
          path="account"
          :rule="{
            required: true,
            message: '请输入账号',
            trigger: 'blur'
          }"
        >
          <n-input
            v-model:value="loginForm.account"
            placeholder="请输入账号"
          />
        </n-form-item>

        <!-- 密码输入 -->
        <n-form-item
          v-if="loginType === LoginTypeEnum.USERNAME"
          label="密码："
          path="password"
          :rule="{
            required: true,
            message: '请输入密码',
            trigger: 'blur'
          }"
        >
          <n-input
            v-model:value="loginForm.password"
            type="password"
            placeholder="请输入密码"
            @keydown.enter="handleLogin"
          />
        </n-form-item>

        <!-- 用户类型选择 -->
        <n-form-item
          v-if="availableUserRoles.length > 1"
          label="用户类型："
          path="role"
          :rule="{
            required: true,
            message: '请选择角色',
            trigger: 'change'
          }"
        >
          <n-select
            v-model:value="loginForm.role"
            :options="roleOptions"
            placeholder="请选择用户类型"
          />
        </n-form-item>

        <!-- 记住密码选项 -->
        <div class="remember_option" v-if="loginType === LoginTypeEnum.USERNAME">
          <n-checkbox v-model:checked="shouldRememberPassword">
            记住密码
          </n-checkbox>
        </div>

        <!-- 操作按钮 -->
        <div class="button_group">
          <n-button
            class="login_button"
            type="success"
            :loading="loginLoading"
            @click="handleLogin"
            v-if="loginType === LoginTypeEnum.USERNAME"
          >
            登录
          </n-button>
          <n-button
            class="register_button"
            type="primary"
            text
            @click="navigateToRegister(UserType.USER)"
          >
            注册用户
          </n-button>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue"
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import menu from '@/utils/menu'
import { getWebSocket } from '@/utils/websocket'

// 获取 router 实例
const router = useRouter()
const formRef = ref<FormInst | null>(null)
const message = useMessage()

// 类型定义
interface UserRole {
  roleName: string
  hasFrontLogin: string
}

interface LoginForm {
  role: string
  account: string
  password: string
}

// 枚举定义
enum LoginTypeEnum {
  USERNAME = 1,
  OTHER = 2
}

enum UserType {
  USER = 'user'
}

// 响应式数据
const availableUserRoles = ref<UserRole[]>([])
const menuList = ref<UserRole[]>([])
const loginForm = ref<LoginForm>({
  role: '',
  account: '',
  password: ''
})

const loginType = ref<LoginTypeEnum>(LoginTypeEnum.USERNAME)
const shouldRememberPassword = ref<boolean>(true)
const loginLoading = ref<boolean>(false)

// 计算角色选项
const roleOptions = computed(() => {
  return availableUserRoles.value.map(role => ({
    label: role.roleName,
    value: role.roleName
  }))
})

// 导入工具模块
import toolUtil from '@/utils/toolUtil'
import httpClient from '@/utils/http'

/**
 * 导航到注册页面
 */
const navigateToRegister = (userType: UserType): void => {
  router.push(`/${userType}Register`)
}

/**
 * 处理登录逻辑
 */
const handleLogin = async (): Promise<void> => {
  try {
    await formRef.value?.validate()
  } catch (error) {
    return
  }

  executeLogin()
}

/**
 * 执行登录请求
 */
const executeLogin = async (): Promise<void> => {
  loginLoading.value = true

  try {
    const loginUrl = loginForm.value.role === '管理员'
        ? 'users/admin/login'
        : 'users/login'

    const response = await httpClient.post(loginUrl, {
      account: loginForm.value.account,
      password: loginForm.value.password
    })

    // 处理记住密码逻辑
    handlePasswordRemember()

    // 存储认证信息
    storeAuthInfo(response.data.data.token)

    // 直接使用登录返回的 user 数据
    const userData = response.data.data.user
    if (userData) {
      toolUtil.storageSet('userid', userData.id)
      toolUtil.storageSet('nickname', userData.nickname)
      toolUtil.storageSet('account', userData.account)
      toolUtil.storageSet('avatar', userData.avatar)
      toolUtil.storageSet('UserInfo', JSON.stringify(userData))
      toolUtil.storageSet('roleId', userData.roleId)
    }

    // 获取用户菜单
    await fetchUserMenus()

    // 关键修改：登录成功后建立 WebSocket连接
    await initializeWebSocket()

    // 跳转到目标页面
    redirectToTargetPage()

  } catch (error) {
    console.error('登录失败:', error)
    message.error('登录失败，请检查账号和密码')
  } finally {
    loginLoading.value = false
  }
}

/**
 * 初始化 WebSocket连接
 */
const initializeWebSocket = async (): Promise<void> => {
  try {
    const ws = getWebSocket()
    if (ws && !ws.isConnected()) {

      await ws.connect()

    } else if (ws && ws.isConnected()) {

    } else {
      console.warn('⚠️ [登录] WebSocket 实例不存在')
    }
  } catch (error) {
    console.error('❌ [登录] WebSocket连接失败:', error)
    // WebSocket连接失败不影响登录流程
  }
}

/**
 * 获取用户菜单
 */
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

/**
 * 处理密码记住功能
 */
const handlePasswordRemember = (): void => {
  if (shouldRememberPassword.value) {
    const formToSave = { ...loginForm.value }
    delete formToSave.code
    toolUtil.storageSet("frontLoginForm", JSON.stringify(formToSave))
  } else {
    toolUtil.storageRemove("frontLoginForm")
  }
}

/**
 * 存储认证信息
 */
const storeAuthInfo = (token: string): void => {
  toolUtil.storageSet("Token", token)
  toolUtil.storageSet("role", loginForm.value.role)
}

/**
 * 重定向到目标页面
 */
const redirectToTargetPage = (): void => {
  const redirectPath = toolUtil.storageGet('toPath')

  if (redirectPath) {
    router.push(redirectPath)
    toolUtil.storageRemove('toPath')
    return
  }

  router.push('/index/home')
}

/**
 * 获取菜单数据
 */
const loadMenuData = async (): Promise<void> => {
  menu.list() && toolUtil.storageRemove('menus')

  let menus = menu.list()

  if (!menus) {
    try {
      const response = await httpClient.get("menus/auth")

      if (response.data.data && response.data.data.list && response.data.data.list.length > 0) {
        toolUtil.storageSet("menus", response.data.data.list[0].menujson)
        menus = JSON.parse(response.data.data.list[0].menujson)
      }
    } catch (error) {
      console.error('获取旧菜单失败:', error)
    }
  }

  menuList.value = menus || []

  availableUserRoles.value = menuList.value.filter(
    menu => menu.hasFrontLogin === '是'
  )

  if (availableUserRoles.value.length === 0) {
    availableUserRoles.value = [{
      roleName: '用户',
      hasFrontLogin: '是'
    }]
  }
}

/**
 * 初始化页面数据
 */
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
})
</script>

<style lang="scss" scoped>
.login_container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: url(http://clfile.zggen.cn/20231229/e2095d3a74784da2bdff07be79797146.jpg) center/cover no-repeat;
}

.login_view {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;

  .login_form {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 600px;
    min-height: 600px;
    padding: 30px 20px 40px;
    margin: 20px 0;
    background: #fff;
    border-radius: 20px;
    box-shadow: 0px 26px 36px -20px #083f5d;
  }

  .title_view {
    width: 100%;
    margin: 0 0 20px;
    color: #0d6392;
    font-size: 18px;
    text-align: center;
  }

  .remember_option {
    width: 100%;
    margin: 16px 0;
    display: flex;
    justify-content: center;
  }

  .button_group {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    padding: 20px 0 0;
    gap: 16px;

    .login_button {
      width: 300px;
      height: 40px;
      font-size: 14px;
      background: linear-gradient(158deg, rgba(75,149,218,1) 49%, rgba(0,88,171,1) 100%);

      &:hover {
        background: linear-gradient(270deg, rgba(75,149,218,1) 49%, rgba(0,88,171,1) 100%);
      }
    }

    .register_button {
      font-size: 14px;
    }
  }
}
</style>