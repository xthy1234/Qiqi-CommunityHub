<template>
  <div class="login-container">
    <div class="background-layer" :style="backgroundStyle"></div>

    <div class="login-left">
      <div class="brand-section">
        <h1 class="brand-title">游戏社区管理平台</h1>
        <p class="brand-subtitle">欢迎回来，管理员！</p>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <div class="login-header">
          <h2 class="title">管理员登录</h2>
          <p class="subtitle">请输入您的账号和密码</p>
        </div>

        <NForm
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-placement="left"
          label-width="70px"
          size="large"
        >
          <NFormItem label="账号" path="account">
            <NInput
              v-model:value="loginForm.account"
              placeholder="请输入管理员账号"
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

          <NFormItem>
            <NCheckbox v-model:checked="rememberPassword">
              记住密码
            </NCheckbox>
          </NFormItem>

          <NButton
            type="primary"
            size="large"
            :loading="loading"
            block
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </NButton>

          <div class="register-link">
            <span>还没有账号？</span>
            <NButton text type="primary" @click="goToRegister">
              立即注册
            </NButton>
          </div>
        </NForm>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, NCheckbox, useMessage, NIcon } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'
import authApi from '@/api/auth'

import type { FormRules, FormInst } from 'naive-ui'

interface LoginForm {
  account: string
  password: string
}

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const loginFormRef = ref<FormInst | null>(null)
const loading = ref(false)
const rememberPassword = ref(false)
const backgroundImageUrl = ref('')
const imageLoaded = ref(false)

const loginForm = reactive<LoginForm>({
  account: '',
  password: ''
})

const loginRules: FormRules = {
  account: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ]
}

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

const handleLogin = async () => {

  if (!loginFormRef.value) {
    console.error('===== [登录] 错误：loginFormRef 为 null =====')
    return
  }

  try {
    await loginFormRef.value.validate()

    loading.value = true

    try {

      const response = await authApi.adminLogin({
        account: loginForm.account,
        password: loginForm.password
      })

      const responseData =  response

      if (responseData.code === 200 || responseData.code === 0) {

        const token = responseData.data?.token || responseData.token
        const userInfo = responseData.data?.user || responseData.data

        userStore.setToken(token)

        userStore.setUserInfo(userInfo)
        localStorage.setItem('adminName', loginForm.account)

        if (userInfo.avatar) {
          localStorage.setItem('avatar', userInfo.avatar)
        }
        if (userInfo.nickname) {
          localStorage.setItem('nickname', userInfo.nickname)
        }
        localStorage.setItem('account', loginForm.account)

        if (rememberPassword.value) {
          localStorage.setItem('loginForm', JSON.stringify({
            account: loginForm.account,
            password: loginForm.password
          }))
        } else {
          localStorage.removeItem('loginForm')
        }

        message.success('登录成功')

        const redirectPath = localStorage.getItem('redirectPath') || '/'

        router.push(redirectPath)
      } else {
        console.error('===== [登录] 业务逻辑失败 =====')
        console.error('[登录] 错误码:', responseData.code)
        console.error('[登录] 错误消息:', responseData.msg)
        message.error(responseData.msg || '登录失败')
      }
    } catch (error: any) {
      console.error('===== [登录] API 调用异常 =====')
      console.error('[登录] 错误对象:', error)
      console.error('[登录] 错误名称:', error.constructor.name)
      console.error('[登录] 响应数据:', error.response?.data)
      console.error('[登录] 状态码:', error.response?.status)

      const errorMsg = error.response?.data?.msg || error.msg || '登录失败，请检查账号和密码'
      console.error('[登录] 最终错误消息:', errorMsg)
      message.error(errorMsg)
    } finally {
      loading.value = false
    }
  } catch (validationErrors) {
    console.error('===== [登录] 表单验证失败 =====')
    console.error('[登录] 验证错误:', validationErrors)
    message.error('请填写完整的登录信息')
    return
  }
}

const goToRegister = () => {
  router.push('/register')
}

const loadCachedLogin = () => {
  const cached = localStorage.getItem('loginForm')
  if (cached) {
    try {
      const parsed = JSON.parse(cached)
      loginForm.account = parsed.account
      loginForm.password = parsed.password
      rememberPassword.value = true
    } catch (e) {
      localStorage.removeItem('loginForm')
    }
  }

  userStore.loadUserFromStorage()
}

onMounted(() => {
  loadCachedLogin()
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

  .captcha-row {
    display: flex;
    gap: 12px;
    align-items: center;

    .captcha-input {
      flex: 1;
    }

    .captcha-image {
      width: 120px;
      height: 44px;
      border: 2px solid #e4e7ed;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;
      overflow: hidden;
      flex-shrink: 0;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      &:hover {
        border-color: #18a058;
        transform: scale(1.05);
      }
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
