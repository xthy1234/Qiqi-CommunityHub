<template>
  <div class="register-container">
    <div class="background-layer" :style="backgroundStyle"></div>

    <div class="register-left">
      <div class="brand-section">
        <h1 class="brand-title">游戏社区管理平台</h1>
        <p class="brand-subtitle">加入我们吧！</p>
      </div>
    </div>

    <div class="register-right">
      <div class="register-card">
        <div class="register-header">
          <h2 class="title">管理员注册</h2>
          <p class="subtitle">创建您的管理账号</p>
        </div>

        <NForm
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-placement="left"
            label-width="90px"
            size="large"
        >
          <NFormItem label="账号" path="account">
            <NInput
                v-model:value="registerForm.account"
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
                v-model:value="registerForm.password"
                type="password"
                placeholder="请输入密码"
                show-password-on="click"
            >
              <template #prefix>
                <Icon icon="ri:lock-line" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="确认密码" path="confirmPassword">
            <NInput
                v-model:value="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password-on="click"
            >
              <template #prefix>
                <Icon icon="ri:lock-line" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="昵称" path="nickname">
            <NInput
                v-model:value="registerForm.nickname"
                placeholder="请输入昵称（可选）"
                clearable
            >
              <template #prefix>
                <Icon icon="ri:smile-line" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="手机号" path="phone">
            <NInput
                v-model:value="registerForm.phone"
                placeholder="请输入手机号"
                clearable
            >
              <template #prefix>
                <Icon icon="ri:phone-line" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="邮箱" path="email">
            <NInput
                v-model:value="registerForm.email"
                placeholder="请输入邮箱（可选）"
                clearable
            >
              <template #prefix>
                <Icon icon="ri:mail-line" />
              </template>
            </NInput>
          </NFormItem>

          <NButton
              type="primary"
              size="large"
              :loading="loading"
              block
              @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </NButton>

          <div class="login-link">
            <span>已有账号？</span>
            <NButton text type="primary" @click="goToLogin">
              立即登录
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
import { useMessage } from 'naive-ui'
import type { FormRules, FormInst } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { userApi } from '@/api/user'

interface RegisterForm {
  account: string
  password: string
  confirmPassword: string
  nickname?: string
  phone: string
  email?: string
}

const router = useRouter()
const message = useMessage()
const registerFormRef = ref<FormInst | null>(null)
const loading = ref(false)
const backgroundImageUrl = ref('')
const imageLoaded = ref(false)

const registerForm = reactive<RegisterForm>({
  account: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

const validateConfirmPassword = (_rule: any, value: string) => {
  if (value !== registerForm.password) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  return Promise.resolve()
}

const validatePhone = (_rule: any, value: string) => {
  if (!value) {
    return Promise.resolve()
  }
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(value)) {
    return Promise.reject(new Error('请输入正确的手机号'))
  }
  return Promise.resolve()
}

const validateEmail = (_rule: any, value: string) => {
  if (!value) {
    return Promise.resolve()
  }
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(value)) {
    return Promise.reject(new Error('请输入正确的邮箱地址'))
  }
  return Promise.resolve()
}

const registerRules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在 3-20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6-20 个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '密码只能包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
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

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true

    try {
      const registerData = {
        account: registerForm.account,
        password: registerForm.password,
        phone: registerForm.phone,
        nickname: registerForm.nickname,
        email: registerForm.email
      }

      const response = await userApi.adminRegister(registerData)
      const responseData = response.data

      if (responseData.code === 200 || responseData.code === 0) {
        message.success('注册成功，请登录')
        setTimeout(() => {
          router.push('/login')
        }, 500)
      } else {
        message.error(responseData.msg || '注册失败')
      }
    } catch (error: any) {
      console.error('[注册请求] 注册失败:', error)
      const errorMsg = error.response?.data?.msg || '注册失败，请稍后重试'
      message.error(errorMsg)
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  loadBackgroundImage()
})
</script>

<style lang="scss" scoped>
.register-container {
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

.register-left {
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

.register-right {
  width: 500px;
  background: rgba(255,255,255,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
  max-height: 100vh;
  position: relative;
  z-index: 2;
}

.register-card {
  width: 100%;
  max-width: 460px;

  .register-header {
    text-align: center;
    margin-bottom: 32px;

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

  .login-link {
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
  .register-left {
    display: none;
  }

  .register-right {
    width: 100%;
  }
}
</style>
