<template>
  <PageContainer
    header-title="修改密码"
    @back="goBack"
  >
    <div class="change-content">
      <n-alert
        title="密码安全提示"
        type="info"
        :bordered="false"
        class="security-tips"
      >
        密码长度至少 6 位，建议包含字母和数字的组合以提高安全性
      </n-alert>

      <n-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="120px"
        class="password-form"
      >
        <n-form-item
          label="原密码"
          path="oldPassword"
        >
          <n-input
            v-model:value="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password-on="click"
            clearable
          />
        </n-form-item>

        <n-form-item
          label="新密码"
          path="newPassword"
        >
          <n-input
            v-model:value="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password-on="click"
            clearable
          />
          <template #feedback>
            <div class="form-tip">
              密码长度至少 6 位
            </div>
          </template>
        </n-form-item>

        <n-form-item
          label="确认新密码"
          path="confirmPassword"
        >
          <n-input
            v-model:value="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password-on="click"
            clearable
          />
        </n-form-item>

        <div class="form-actions">
          <n-button
            type="primary" 
            :loading="isSubmitting" 
            class="submit-btn"
            @click="handleSubmit"
          >
            确认修改
          </n-button>
          <n-button @click="handleReset">
            重置
          </n-button>
        </div>
      </n-form>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import type { FormRules } from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()
const appContext = useGlobalProperties()
const $http = appContext.$http
const message = useMessage()

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const passwordFormRef = ref(null)
const isSubmitting = ref(false)

const passwordForm = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePasswordStrength = (rule: any, value: string) => {
  if (!value) {
    return Promise.reject(new Error('请输入密码'))
  }
  
  if (value.length < 6) {
    return Promise.reject(new Error('密码长度至少 6 位'))
  }
  
  if (value.length > 20) {
    return Promise.reject(new Error('密码长度不能超过 20 位'))
  }
  
  return Promise.resolve()
}

const validateConfirmPassword = (rule: any, value: string) => {
  if (!value) {
    return Promise.reject(new Error('请确认新密码'))
  }
  
  if (value !== passwordForm.newPassword) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  
  return Promise.resolve()
}

const passwordRules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validatePasswordStrength, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
})

const handleSubmit = async () => {
  if (!passwordFormRef.value) {return}
  
  try {
    await passwordFormRef.value.validate()
    
    isSubmitting.value = true
    
    const userid = appContext?.$toolUtil.storageGet('userid')

    if (!userid) {
      message.error('用户未登录')
      return
    }

    const response = await $http.put(`users/${userid}/password`, null, {
      params: {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      }
    })

    message.success('密码修改成功')
    setTimeout(() => {
      appContext?.$toolUtil.storageClear()
      router.push('/login')
    }, 1000)
  } catch (error: any) {
    console.error('密码修改失败:', error)
    const errorMsg = error.response?.data?.msg || '密码修改失败，请检查原密码是否正确'
    message.error(errorMsg)
  } finally {
    isSubmitting.value = false
  }
}

const handleReset = () => {
  if (!passwordFormRef.value) {return}
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.change-content {
  .security-tips {
    margin-bottom: 30px;
  }
  
  .password-form {
    .form-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 5px;
    }
    
    .form-actions {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-top: 40px;
      padding-top: 20px;
      border-top: 1px solid #ebeef5;
      
      .submit-btn {
        min-width: 150px;
        height: 40px;
        font-size: 16px;
      }
      
      .n-button {
        min-width: 100px;
        height: 40px;
      }
    }
  }
}
</style>
