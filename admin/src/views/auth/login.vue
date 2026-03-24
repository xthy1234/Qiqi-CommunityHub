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
  console.log('===== [登录] 开始执行 =====')

  if (!loginFormRef.value) {
    console.error('===== [登录] 错误：loginFormRef 为 null =====')
    return
  }

  console.log('===== [登录] loginFormRef 存在，开始验证表单 =====')
  console.log('[登录] 表单数据:', loginForm)

  try {
    // Naive UI 3.x 使用 await validate()，成功时不返回任何值，失败时抛出错误
    await loginFormRef.value.validate()

    console.log('===== [登录] 表单验证通过，开始提交 =====')
    loading.value = true
    console.log('[登录] loading 状态已设置为 true')

    try {
      console.log('[登录] 准备调用登录 API...')
      console.log('[登录] 请求参数:', {
        account: loginForm.account,
        password: loginForm.password
      })

      const response = await apiService.user.adminLogin({
        account: loginForm.account,
        password: loginForm.password
      })

      console.log('===== [登录] API 响应 =====')
      console.log('[登录] 完整响应:', response)
      console.log('[登录] 响应数据:', response.data)

      const responseData = response.data

      if (responseData.code === 200 || responseData.code === 0) {
        console.log('===== [登录] 登录成功 =====')
        const token = responseData.data?.token || responseData.token
        const userInfo = responseData.data?.user || responseData.data

        console.log('[登录] Token:', token)
        console.log('[登录] 用户信息:', userInfo)

        userStore.setToken(token)
        console.log('[登录] Token 已存储到 userStore')

        userStore.setUserInfo(userInfo)
        console.log('[登录] 用户信息已存储到 userStore')

        localStorage.setItem('adminName', loginForm.account)
        console.log('[登录] adminName 已存储到 localStorage:', loginForm.account)

        if (rememberPassword.value) {
          localStorage.setItem('loginForm', JSON.stringify({
            account: loginForm.account,
            password: loginForm.password
          }))
          console.log('[登录] 账号密码已保存到 localStorage')
        } else {
          localStorage.removeItem('loginForm')
          console.log('[登录] 已清除 localStorage 中的登录信息')
        }

        message.success('登录成功')
        console.log('[登录] 显示成功消息提示')

        const redirectPath = localStorage.getItem('redirectPath') || '/'
        console.log('[登录] 跳转路径:', redirectPath)

        router.push(redirectPath)
        console.log('[登录] 路由跳转已执行')
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
      console.log('===== [登录] 流程结束，loading 已重置 =====')
    }
  } catch (validationErrors) {
    console.error('===== [登录] 表单验证失败 =====')
    console.error('[登录] 验证错误:', validationErrors)
    message.error('请填写完整的登录信息')
    return
  }

  console.log('===== [登录] 整个流程执行完毕 =====')
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
