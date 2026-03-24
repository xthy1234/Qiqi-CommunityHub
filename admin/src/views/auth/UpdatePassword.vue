<template>
  <!-- 修改密码页面容器 -->
  <div class="password-page-container">
    <div class="app-contain">
      <!-- 密码修改表单 -->
      <n-form
        class="password-form"
        ref="passwordFormRef"
        :model="passwordForm"
        label-placement="left"
        label-width="100px"
        :rules="validationRules"
      >
        <!-- 当前密码输入框 -->
        <n-form-item label="当前密码" path="currentPassword">
          <n-input
            class="form-input"
            v-model:value="passwordForm.currentPassword"
            type="password"
            show-password-on="click"
            placeholder="请输入当前密码"
          />
        </n-form-item>

        <!-- 新密码输入框 -->
        <n-form-item label="新密码" path="newPassword">
          <n-input
            class="form-input"
            v-model:value="passwordForm.newPassword"
            type="password"
            show-password-on="click"
            placeholder="请输入新密码"
          />
        </n-form-item>

        <!-- 确认新密码输入框 -->
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input
            class="form-input"
            v-model:value="passwordForm.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="请再次输入新密码"
          />
        </n-form-item>

        <!-- 提交按钮区域 -->
        <div class="form-button-container">
          <n-button
            class="submit-button"
            type="primary"
            @click="handleSubmit"
            :loading="isSubmitting"
          >
            {{ isSubmitting ? '提交中...' : '保存' }}
          </n-button>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  reactive,
  getCurrentInstance,
  onMounted,
  h
} from 'vue'
import type { FormInst, FormRule } from 'naive-ui'
import { NForm, NFormItem, NInput, NButton } from 'naive-ui'

// 类型定义
interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

interface UserInfo {
  id?: number
  username?: string
  password?: string
  [key: string]: any
}

// 获取全局属性上下文
const instance = getCurrentInstance()
const globalProperties = instance?.appContext.config.globalProperties

// 响应式数据
const passwordForm = ref<PasswordForm>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const currentUser = ref<UserInfo>({})
const sessionTableName = ref<string>('')
const passwordFormRef = ref<FormInst | null>(null)
const isSubmitting = ref<boolean>(false)

// 表单验证规则
const validationRules = reactive<FormRule>({
  currentPassword: [
    {
      required: true,
      message: '请输入当前密码',
      trigger: 'blur'
    }
  ],
  newPassword: [
    {
      required: true,
      message: '请输入新密码',
      trigger: 'blur'
    },
    {
      min: 6,
      message: '密码长度不能少于6位',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      required: true,
      message: '请确认新密码',
      trigger: 'blur'
    },
    {
      validator: validateConfirmPassword,
      trigger: 'blur'
    }
  ]
})

/**
 * 确认密码验证器
 * @param rule 验证规则
 * @param value 输入值
 * @param callback 回调函数
 */
function validateConfirmPassword(rule: any, value: string, callback: Function): void {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 处理表单提交
 */
const handleSubmit = async (): Promise<void> => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()

    isSubmitting.value = true

    // 验证当前密码（仅管理员需要验证）
    if (sessionTableName.value === 'admin') {
      if (passwordForm.value.currentPassword !== currentUser.value.password) {
        globalProperties?.$toolUtil.message('当前密码不正确', 'error')
        return
      }
      currentUser.value.password = passwordForm.value.newPassword
    }

    // 更新用户信息
    await globalProperties?.$http({
      url: `${sessionTableName.value}/update`,
      method: 'post',
      data: currentUser.value
    })

    globalProperties?.$toolUtil.message('密码修改成功，下次登录将使用新密码', 'success')

    // 重置表单
    resetForm()

  } catch (error) {
    console.error('密码修改失败:', error)
    globalProperties?.$toolUtil.message('密码修改失败，请稍后重试', 'error')
  } finally {
    isSubmitting.value = false
  }
}

/**
 * 重置表单
 */
const resetForm = (): void => {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordFormRef.value?.restoreValidation()
}

/**
 * 获取当前用户信息
 */
const fetchUserInfo = async (): Promise<void> => {
  try {
    sessionTableName.value = globalProperties?.$toolUtil.storageGet('sessionTable') || ''

    const response = await globalProperties?.$http({
      url: `${sessionTableName.value}/session`,
      method: 'get'
    })

    currentUser.value = response?.data?.data || {}
  } catch (error) {
    console.error('获取用户信息失败:', error)
    globalProperties?.$toolUtil.message('获取用户信息失败', 'error')
  }
}

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
/**
 * 密码修改页面容器样式
 */
.password-page-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

/**
 * 应用内容容器
 */
.app-contain {
  width: 100%;
  max-width: 500px;
}

/**
 * 密码表单样式
 */
.password-form {
  border: 0 solid #ddd;
  border-radius: 8px;
  padding: 40px 30px;
  margin: 0;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);

  /**
   * 表单项样式
   */
  :deep(.n-form-item) {
    margin: 0 0 25px 0;
    background: none;
  }

  /**
   * 表单输入框样式
   */
  .form-input {
    border: 2px solid #e1e5ee;
    border-radius: 6px;
    padding: 0 15px;
    background: #ffffff;
    width: 100%;
    line-height: 44px;
    box-sizing: border-box;
    height: 44px;
    transition: all 0.3s ease;

    &:focus-within {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }

  /**
   * 表单按钮容器
   */
  .form-button-container {
    display: flex;
    width: 100%;
    justify-content: center;
    align-items: center;
    margin-top: 30px;

    /**
     * 提交按钮样式
     */
    .submit-button {
      border: 0;
      cursor: pointer;
      border-radius: 6px;
      padding: 0 32px;
      margin: 0;
      color: #fff;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      width: auto;
      font-size: 16px;
      min-width: 120px;
      height: 44px;
      transition: all 0.3s ease;
      font-weight: 500;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .password-page-container {
    padding: 15px;
  }

  .password-form {
    padding: 30px 20px;

    :deep(.n-form-item) {
      width: 100%;
    }
  }
}
</style>
