<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <div class="logo-wrapper">
          <Icon icon="ri:user-add-line" :size="48" color="#18a058" />
        </div>
        <h1 class="title">管理员注册</h1>
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
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormRules, FormInst } from 'naive-ui'
import { Icon } from '@iconify/vue'
import apiService from '@/api'

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

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true

    // 🔍 添加调试日志
    console.log('📤 [注册请求] 开始提交注册数据')
    console.log('📝 [注册请求] 表单数据:', {
      account: registerForm.account,
      phone: registerForm.phone,
      nickname: registerForm.nickname,
      email: registerForm.email
    })

    try {
      const registerData = {
        account: registerForm.account,
        password: registerForm.password,
        phone: registerForm.phone,
        nickname: registerForm.nickname,
        email: registerForm.email
      }

      console.log('🌐 [注册请求] API 调用:', apiService.user.adminRegister)
      console.log('📡 [注册请求] 发送 POST 到 /users/admin/register')

      const response = await apiService.user.adminRegister(registerData)
      const responseData = response.data

      console.log('✅ [注册请求] 响应数据:', responseData)

      if (responseData.code === 200 || responseData.code === 0) {
        message.success('注册成功，请登录')
        setTimeout(() => {
          router.push('/login')
        }, 500)
      } else {
        message.error(responseData.msg || '注册失败')
      }
    } catch (error: any) {
      console.error('❌ [注册请求] 注册失败:', error)
      console.error('❌ [注册请求] 错误详情:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        config: error.config
      })
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
</script>

<style lang="scss" scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  padding: 20px;
}

.register-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 520px;
  padding: 48px 40px;

  .register-header {
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
</style>
