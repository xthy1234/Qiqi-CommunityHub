<template>
  <div>
    <div class="forget-view">
      <n-form :model="resetPasswordForm" class="forget-container">
        <!-- 页面标题 -->
        <div class="forget-title">
          中文社区交流平台{{ getPageTitle(currentStep) }}
        </div>

        <!-- 步骤指示器 -->
        <div class="step-indicator">
          <div class="progress-line"></div>
          <div class="step-numbers">
            <div
              class="step-number"
              :class="getStepClass(1)"
            >
              <div class="number" v-if="currentStep < 2">1</div>
              <Icon v-else icon="ri:check-line" />
            </div>
            <div
              class="step-number"
              :class="getStepClass(2)"
            >
              <div class="number" v-if="currentStep < 3">2</div>
              <Icon v-else icon="ri:check-line" />
            </div>
            <div
              class="step-number"
              :class="getStepClass(3)"
            >
              <div class="number">3</div>
            </div>
          </div>
        </div>

        <!-- 第一步：找回密码 -->
        <div v-if="currentStep === 1" class="form-step">
          <div class="form-item">
            <div class="item-label">用户名：</div>
            <n-input
              class="form-input"
              v-model:value="resetPasswordForm.username"
              placeholder="请输入用户名"
              type="text"
            />
          </div>
          <div class="form-item">
            <div class="item-label">邮箱：</div>
            <n-input
              class="form-input"
              v-model:value="resetPasswordForm.email"
              placeholder="请输入邮箱"
              type="email"
            />
          </div>
          <div class="button-container">
            <n-button
              class="next-button"
              type="success"
              @click="handleVerifyIdentity"
            >
              验证身份
            </n-button>
            <div class="login-link" @click="navigateToLogin">返回登录</div>
          </div>
        </div>

        <!-- 第二步：输入密保 -->
        <div v-if="currentStep === 2" class="form-step">
          <div class="form-item">
            <div class="item-label">密保问题：</div>
            <n-select
              class="form-select"
              v-model:value="resetPasswordForm.securityQuestion"
              placeholder="请选择密保问题"
              :options="securityQuestions"
            />
          </div>
          <div class="form-item">
            <div class="item-label">密保答案：</div>
            <n-input
              class="form-input"
              v-model:value="resetPasswordForm.securityAnswer"
              placeholder="请输入密保答案"
              type="text"
            />
          </div>
          <div class="button-container">
            <n-button
              class="prev-button"
              @click="currentStep = 1"
            >
              上一步
            </n-button>
            <n-button
              class="next-button"
              type="primary"
              @click="handleVerifySecurity"
            >
              验证密保
            </n-button>
            <div class="login-link" @click="navigateToLogin">返回登录</div>
          </div>
        </div>

        <!-- 第三步：重置密码 -->
        <div v-if="currentStep === 3" class="form-step">
          <div class="form-item">
            <div class="item-label">新密码：</div>
            <n-input
              class="form-input"
              v-model:value="resetPasswordForm.newPassword"
              placeholder="请输入新密码"
              type="password"
            />
          </div>
          <div class="form-item">
            <div class="item-label">确认密码：</div>
            <n-input
              class="form-input"
              v-model:value="resetPasswordForm.confirmPassword"
              placeholder="请再次输入新密码"
              type="password"
            />
          </div>
          <div class="button-container">
            <n-button
              class="prev-button"
              @click="currentStep = 2"
            >
              上一步
            </n-button>
            <n-button
              class="reset-button"
              type="warning"
              @click="handleResetPassword"
            >
              重置密码
            </n-button>
            <div class="login-link" @click="navigateToLogin">返回登录</div>
          </div>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  getCurrentInstance,
  type Ref
} from 'vue'
import { Icon } from '@iconify/vue'
import { NForm, NInput, NButton, NSelect } from 'naive-ui'

// ==================== 类型定义 ====================
/**
 * 重置密码表单数据接口
 */
interface ResetPasswordForm {
  username: string
  email: string
  securityQuestion: string
  securityAnswer: string
  newPassword: string
  confirmPassword: string
}

/**
 * 密保问题选项接口
 */
interface SecurityQuestion {
  value: string
  label: string
}

/**
 * 步骤状态类型
 */
type StepStatus = 'default' | 'active' | 'completed'

// ==================== 全局实例 ====================
const instance = getCurrentInstance()
const globalProperties = instance?.appContext.config.globalProperties

// ==================== 响应式数据 ====================
/** 当前步骤 */
const currentStep: Ref<number> = ref(1)

/** 重置密码表单数据 */
const resetPasswordForm: Ref<ResetPasswordForm> = ref({
  username: '',
  email: '',
  securityQuestion: '',
  securityAnswer: '',
  newPassword: '',
  confirmPassword: ''
})

/** 用户信息表单 */
const userInformationForm: Ref<any> = ref({})

/** 密保问题选项 */
const securityQuestions: Ref<SecurityQuestion[]> = ref([
  { value: 'question1', label: '您的出生地是哪里？' },
  { value: 'question2', label: '您母亲的姓名是？' },
  { value: 'question3', label: '您最喜欢的颜色是？' },
  { value: 'question4', label: '您第一个宠物的名字是？' },
  { value: 'question5', label: '您就读的第一所学校是？' }
])

// ==================== 计算属性 ====================
/**
 * 根据步骤获取页面标题
 * @param step 当前步骤
 * @returns 对应的标题文本
 */
const getPageTitle = (step: number): string => {
  const titles = ['找回密码', '输入密保', '重置密码']
  return titles[step - 1] || ''
}

/**
 * 获取步骤指示器的CSS类名
 * @param stepNumber 步骤编号
 * @returns 对应的CSS类名
 */
const getStepClass = (stepNumber: number): string => {
  if (currentStep.value === stepNumber) {
    return 'step-number-active'
  } else if (currentStep.value > stepNumber) {
    return 'step-number-completed'
  }
  return 'step-number-default'
}

// ==================== 表单验证 ====================
/**
 * 验证邮箱格式
 * @param email 邮箱地址
 * @returns 是否为有效邮箱
 */
const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * 验证密码强度
 * @param password 密码
 * @returns 是否满足密码要求
 */
const isValidPassword = (password: string): boolean => {
  // 至少8位，包含字母和数字
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/
  return passwordRegex.test(password)
}

// ==================== 步骤处理 ====================
/**
 * 处理身份验证
 */
const handleVerifyIdentity = (): void => {
  // 验证必填字段
  if (!resetPasswordForm.value.username) {
    globalProperties?.$toolUtil.message('请输入用户名', 'error')
    return
  }

  if (!resetPasswordForm.value.email) {
    globalProperties?.$toolUtil.message('请输入邮箱', 'error')
    return
  }

  // 验证邮箱格式
  if (!isValidEmail(resetPasswordForm.value.email)) {
    globalProperties?.$toolUtil.message('请输入有效的邮箱地址', 'error')
    return
  }

  // 模拟身份验证（实际项目中应该调用API）
  setTimeout(() => {
    globalProperties?.$toolUtil.message('身份验证成功', 'success')
    currentStep.value = 2
  }, 1000)
}

/**
 * 处理密保验证
 */
const handleVerifySecurity = (): void => {
  // 验证必填字段
  if (!resetPasswordForm.value.securityQuestion) {
    globalProperties?.$toolUtil.message('请选择密保问题', 'error')
    return
  }

  if (!resetPasswordForm.value.securityAnswer) {
    globalProperties?.$toolUtil.message('请输入密保答案', 'error')
    return
  }

  // 模拟密保验证（实际项目中应该调用API）
  setTimeout(() => {
    globalProperties?.$toolUtil.message('密保验证成功', 'success')
    currentStep.value = 3
  }, 1000)
}

/**
 * 处理密码重置
 */
const handleResetPassword = (): void => {
  // 验证必填字段
  if (!resetPasswordForm.value.newPassword) {
    globalProperties?.$toolUtil.message('请输入新密码', 'error')
    return
  }

  if (!resetPasswordForm.value.confirmPassword) {
    globalProperties?.$toolUtil.message('请确认新密码', 'error')
    return
  }

  // 验证密码强度
  if (!isValidPassword(resetPasswordForm.value.newPassword)) {
    globalProperties?.$toolUtil.message('密码至少8位，需包含字母和数字', 'error')
    return
  }

  // 验证密码一致性
  if (resetPasswordForm.value.newPassword !== resetPasswordForm.value.confirmPassword) {
    globalProperties?.$toolUtil.message('两次输入的密码不一致', 'error')
    return
  }

  // 模拟密码重置（实际项目中应该调用API）
  setTimeout(() => {
    globalProperties?.$toolUtil.message('密码重置成功', 'success', () => {
      navigateToLogin()
    })
  }, 1000)
}

// ==================== 页面导航 ====================
/**
 * 跳转到登录页面
 */
const navigateToLogin = (): void => {
  globalProperties?.$router.push({
    path: '/login'
  })
}
</script>

<style lang="scss" scoped>
/**
 * 忘记密码页面容器样式
 */
.forget-view {
  flex-direction: column;
  background-size: 100% 100% !important;
  display: flex;
  min-height: 100vh;
  justify-content: center;
  align-items: center;
  position: relative;
  background: url(http://clfile.zggen.cn/20240413/7f0c2ebb13cf40f19448f505adafd523.jpg) no-repeat center center;

  /* 表单容器 */
  .forget-container {
    border: 10px outset #849acf;
    border-radius: 4px;
    padding: 0 20px 40px 0;
    margin: 0 auto;
    flex-direction: column;
    background: #fff;
    display: flex;
    width: 688px;
    justify-content: flex-start;
    flex-wrap: wrap;

    /* 页面标题 */
    .forget-title {
      padding: 0;
      margin: 30px 0 20px 12%;
      color: #333;
      font-weight: 600;
      width: 78%;
      font-size: 22px;
      text-align: center;
    }

    /* 步骤指示器 */
    .step-indicator {
      position: relative;
      margin: 20px 0;
      width: 100%;
      display: flex;
      justify-content: center;

      .progress-line {
        position: absolute;
        top: 50%;
        left: 10%;
        width: 80%;
        height: 2px;
        background: #e0e0e0;
        transform: translateY(-50%);
        z-index: 0;
      }

      .step-numbers {
        display: flex;
        width: 100%;
        justify-content: space-between;
        z-index: 1;

        .step-number {
          border-radius: 50%;
          width: 40px;
          height: 40px;
          display: flex;
          justify-content: center;
          align-items: center;
          font-weight: bold;

          &.step-number-default {
            background: #e0e0e0;
            color: #999;
          }

          &.step-number-active {
            background: #409eff;
            color: #fff;
            transform: scale(1.1);
          }

          &.step-number-completed {
            background: #67c23a;
            color: #fff;
          }

          .number {
            font-size: 16px;
          }
        }
      }
    }

    /* 表单项 */
    .form-step {
      padding: 20px 10% 30px;
      width: 100%;

      .form-item {
        margin-bottom: 20px;
        display: flex;
        flex-direction: column;

        .item-label {
          margin-bottom: 8px;
          color: #333;
          font-size: 14px;
        }

        .form-input,
        .form-select {
          width: 80%;
        }
      }

      .button-container {
        display: flex;
        gap: 15px;
        margin-top: 30px;
        justify-content: center;

        .next-button,
        .prev-button,
        .reset-button {
          min-width: 100px;
        }

        .login-link {
          cursor: pointer;
          color: #409eff;
          font-size: 14px;
          display: flex;
          align-items: center;
          margin-left: 10px;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }
  }
}
</style>
