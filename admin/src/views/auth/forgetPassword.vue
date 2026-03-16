<template>
  <div>
    <div class="forget-view">
      <el-form :model="resetPasswordForm" class="forget-container">
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
              <el-icon v-else><Check /></el-icon>
            </div>
            <div
              class="step-number"
              :class="getStepClass(2)"
            >
              <div class="number" v-if="currentStep < 3">2</div>
              <el-icon v-else><Check /></el-icon>
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
            <el-input
              class="form-input"
              v-model="resetPasswordForm.username"
              placeholder="请输入用户名"
              type="text"
            />
          </div>
          <div class="form-item">
            <div class="item-label">邮箱：</div>
            <el-input
              class="form-input"
              v-model="resetPasswordForm.email"
              placeholder="请输入邮箱"
              type="email"
            />
          </div>
          <div class="button-container">
            <el-button
              class="next-button"
              type="success"
              @click="handleVerifyIdentity"
            >
              验证身份
            </el-button>
            <div class="login-link" @click="navigateToLogin">返回登录</div>
          </div>
        </div>

        <!-- 第二步：输入密保 -->
        <div v-if="currentStep === 2" class="form-step">
          <div class="form-item">
            <div class="item-label">密保问题：</div>
            <el-select
              class="form-select"
              v-model="resetPasswordForm.securityQuestion"
              placeholder="请选择密保问题"
            >
              <el-option
                v-for="question in securityQuestions"
                :key="question.value"
                :label="question.label"
                :value="question.value"
              />
            </el-select>
          </div>
          <div class="form-item">
            <div class="item-label">密保答案：</div>
            <el-input
              class="form-input"
              v-model="resetPasswordForm.securityAnswer"
              placeholder="请输入密保答案"
              type="text"
            />
          </div>
          <div class="button-container">
            <el-button
              class="prev-button"
              @click="currentStep = 1"
            >
              上一步
            </el-button>
            <el-button
              class="next-button"
              type="primary"
              @click="handleVerifySecurity"
            >
              验证密保
            </el-button>
            <div class="login-link" @click="navigateToLogin">返回登录</div>
          </div>
        </div>

        <!-- 第三步：重置密码 -->
        <div v-if="currentStep === 3" class="form-step">
          <div class="form-item">
            <div class="item-label">新密码：</div>
            <el-input
              class="form-input"
              v-model="resetPasswordForm.newPassword"
              placeholder="请输入新密码"
              type="password"
            />
          </div>
          <div class="form-item">
            <div class="item-label">确认密码：</div>
            <el-input
              class="form-input"
              v-model="resetPasswordForm.confirmPassword"
              placeholder="请再次输入新密码"
              type="password"
            />
          </div>
          <div class="button-container">
            <el-button
              class="prev-button"
              @click="currentStep = 2"
            >
              上一步
            </el-button>
            <el-button
              class="reset-button"
              type="warning"
              @click="handleResetPassword"
            >
              重置密码
            </el-button>
            <div class="login-link" @click="navigateToLogin">返回登录</div>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  getCurrentInstance,

  type Ref
} from 'vue'
import { Check } from '@element-plus/icons-vue'

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
      padding: 30px 0 40px;
      flex-direction: column;
      display: flex;
      width: 100%;
      align-items: center;

      /* 进度线条 */
      .progress-line {
        background: #ccc;
        width: 80%;
        height: 2px;
      }

      /* 步骤数字容器 */
      .step-numbers {
        margin: -20px 0 0;
        display: flex;
        width: 80%;
        justify-content: space-between;
        align-items: center;
        height: 40px;

        /* 步骤数字 */
        .step-number {
          border: 4px solid #ddd;
          border-radius: 50%;
          color: #aaa;
          background: #fff;
          display: flex;
          width: 40px;
          font-size: 18px;
          justify-content: center;
          align-items: center;
          height: 40px;

          /* 活跃状态 */
          &.step-number-active {
            color: #0d308460;
            background: #fff;
            font-size: 22px;
            border-color: #0d308460;
          }

          /* 完成状态 */
          &.step-number-completed {
            color: #19a91d;
            background: #fff;
            border-color: #19a91d;
          }
        }
      }
    }

    /* 表单步骤容器 */
    .form-step {
      padding: 20px 0;
    }

    /* 表单项 */
    .form-item {
      margin: 0 auto 30px;
      display: flex;
      width: 70%;
      justify-content: flex-start;
      align-items: center;

      /* 表单标签 */
      .item-label {
        background: none;
        display: block;
        width: 130px;
        font-size: 14px;
        line-height: 36px;
        box-sizing: border-box;
        text-align: right;
        height: 36px;
      }

      /* 输入框样式 */
      :deep(.form-input) {
        border: 3px ridge #eee;
        border-radius: 0;
        padding: 0 10px;
        color: #666;
        background: linear-gradient(
          30deg,
          rgba(227, 231, 242, 1) 0%,
          rgba(255, 255, 255, 1) 20%,
          rgba(255, 255, 255, 1) 80%,
          rgba(227, 231, 242, 1) 100%
        );
        width: 80%;
        line-height: 44px;
        box-sizing: border-box;
        height: 44px;

        /* 移除默认输入框样式 */
        .el-input__wrapper {
          border: none;
          box-shadow: none;
          background: none;
          border-radius: 0;
          height: 100%;
          padding: 0;
        }

        .is-focus {
          box-shadow: none !important;
        }
      }

      /* 下拉选择框样式 */
      :deep(.form-select) {
        border: 3px ridge #eee;
        border-radius: 0;
        padding: 0 10px;
        color: #666;
        background: linear-gradient(
          30deg,
          rgba(227, 231, 242, 1) 0%,
          rgba(255, 255, 255, 1) 20%,
          rgba(255, 255, 255, 1) 80%,
          rgba(227, 231, 242, 1) 100%
        );
        width: 80%;
        line-height: 44px;
        box-sizing: border-box;

        /* 移除默认下拉框样式 */
        .select-trigger {
          height: 100%;

          .el-input {
            height: 100%;

            .el-input__wrapper {
              border: none;
              box-shadow: none;
              background: none;
              border-radius: 0;
              height: 100%;
              padding: 0;
            }

            .is-focus {
              box-shadow: none !important;
            }
          }
        }
      }
    }

    /* 按钮容器 */
    .button-container {
      margin: 20px auto 0;
      display: flex;
      width: 70%;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;

      /* 上一步按钮 */
      :deep(.prev-button) {
        border: 3px ridge rgba(93, 83, 181, 1);
        cursor: pointer;
        border-radius: 4px;
        padding: 0 40px;
        margin: 0 10px 0 0;
        color: #fff;
        background: linear-gradient(
          180deg,
          rgba(191, 187, 233, 1) 0%,
          rgba(139, 133, 203, 1) 50%,
          rgba(111, 100, 203, 1) 51%,
          rgba(93, 83, 181, 1) 100%
        );
        width: auto;
        font-size: 16px;
        height: 44px;

        &:hover {
          opacity: 0.9;
          transform: translateY(-1px);
        }
      }

      /* 下一步按钮 */
      :deep(.next-button) {
        border: 3px ridge rgba(93, 83, 181, 1);
        cursor: pointer;
        border-radius: 4px;
        padding: 0 40px;
        margin: 0 10px 0 0;
        color: #fff;
        background: linear-gradient(
          180deg,
          rgba(191, 187, 233, 1) 0%,
          rgba(139, 133, 203, 1) 50%,
          rgba(111, 100, 203, 1) 51%,
          rgba(93, 83, 181, 1) 100%
        );
        width: auto;
        font-size: 16px;
        height: 44px;

        &:hover {
          opacity: 0.9;
          transform: translateY(-1px);
        }
      }

      /* 重置密码按钮 */
      :deep(.reset-button) {
        border: 3px ridge rgba(93, 83, 181, 1);
        cursor: pointer;
        border-radius: 4px;
        padding: 0 40px;
        margin: 0 10px 0 0;
        color: #fff;
        background: linear-gradient(
          180deg,
          rgba(191, 187, 233, 1) 0%,
          rgba(139, 133, 203, 1) 50%,
          rgba(111, 100, 203, 1) 51%,
          rgba(93, 83, 181, 1) 100%
        );
        width: auto;
        font-size: 16px;
        height: 44px;

        &:hover {
          opacity: 0.9;
          transform: translateY(-1px);
        }
      }

      /* 登录链接 */
      .login-link {
        cursor: pointer;
        padding: 10px 0 0;
        color: #999;
        width: 100%;
        font-size: 14px;
        text-align: right;

        &:hover {
          color: #666;
          text-decoration: underline;
        }
      }
    }
  }
}
</style>
