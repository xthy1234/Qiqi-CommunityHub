<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">中文社区交流平台 - 管理员登录</h2>

      <el-form
        :model="loginForm"
        :rules="loginRules"
        ref="loginFormRef"
        class="login-form"
        label-width="80px">

        <!-- 用户名 -->
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员账号"
            prefix-icon="User"
            size="large" />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin" />
        </el-form-item>

        <!-- 记住密码 -->
        <el-form-item>
          <el-checkbox
            v-model="rememberPassword"
            label="记住密码"
            size="large" />
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin">
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiService from '@/api'

interface LoginForm {
  username: string
  password: string
}

const router = useRouter()
const loginFormRef = ref()
const loading = ref(false)
const rememberPassword = ref(false)

const loginForm = reactive<LoginForm>({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    loading.value = true

    try {
      const response = await apiService.user.adminLogin({
        username: loginForm.username,
       password: loginForm.password
      })

      const responseData = response.data

      if (responseData.code === 200 || !responseData.code) {
        const token = responseData.data?.token || responseData.token
        const userInfo = responseData.data?.user || responseData.data

        localStorage.setItem('Token', token)
        localStorage.setItem('userId', userInfo.id)
        localStorage.setItem('userName', userInfo.username)
        localStorage.setItem('userRole', userInfo.role || '管理员')
        localStorage.setItem('adminName', loginForm.username)

        if (rememberPassword.value) {
          localStorage.setItem('loginForm', JSON.stringify({
            username: loginForm.username,
           password: loginForm.password
          }))
        } else {
          localStorage.removeItem('loginForm')
        }

        ElMessage.success('登录成功')
        router.push('/')
      } else {
        ElMessage.error(responseData.msg || '登录失败')
      }
    } catch (error: any) {
      console.error('登录失败:', error)
      const errorMsg = error.response?.data?.msg || error.msg || '登录失败，请检查账号和密码'
      ElMessage.error(errorMsg)
    } finally {
      loading.value = false
    }
  })
}

const loadCachedLogin = () => {
  const cached = localStorage.getItem('loginForm')
  if (cached) {
    try {
      const parsed = JSON.parse(cached)
      loginForm.username = parsed.username
      loginForm.password = parsed.password
      rememberPassword.value = true
    } catch (e) {
      localStorage.removeItem('loginForm')
    }
  }
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
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 450px;

  .login-title {
    text-align: center;
    color: #333;
    font-size: 24px;
    margin-bottom: 30px;
    font-weight: 600;
  }

  .login-form {
    .login-btn {
      width: 100%;
      margin-top: 20px;
    }
  }
}
</style>
