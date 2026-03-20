<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-wrapper">
          <Icon icon="ri:admin-line" :size="48" color="#18a058" />
        </div>
        <h1 class="title">中文社区管理平台</h1>
        <p class="subtitle">管理员登录</p>
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormRules, FormInst } from 'naive-ui'
import { Icon } from '@iconify/vue'
import apiService from '@/api'
import { useUserStore } from '@/stores/user'

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

const refreshCaptcha = async () => {
  try {
    const response = await apiService.captcha.getCaptcha()
    const responseData = response.data

    if (responseData.code === 0 || responseData.code === 200) {
      captchaImage.value = responseData.data?.captchaImage || ''
      captchaKey.value = responseData.data?.captchaKey || ''
    }
  } catch (error: any) {
    console.error('获取验证码失败:', error)
    message.error('获取验证码失败')
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true

    try {
      const response = await apiService.user.adminLogin({
        account: loginForm.account,
        password: loginForm.password
      })

      const responseData = response.data

      if (responseData.code === 200 || responseData.code === 0) {
        const token = responseData.data?.token || responseData.token
        const userInfo = responseData.data?.user || responseData.data

        userStore.setToken(token)
        userStore.setUserInfo(userInfo)

        localStorage.setItem('adminName', loginForm.account)

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
        message.error(responseData.msg || '登录失败')
      }
    } catch (error: any) {
      console.error('登录失败:', error)
      const errorMsg = error.response?.data?.msg || error.msg || '登录失败，请检查账号和密码'
      message.error(errorMsg)
    } finally {
      loading.value = false
    }
  })
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
})
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 480px;
  padding: 48px 40px;

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
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
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
</style>
