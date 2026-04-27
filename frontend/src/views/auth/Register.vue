<template>
  <div class="register-container">
    <div
      class="blur-background-layer"
      :style="{
        backgroundImage: backgroundImageUrl ? `url(${backgroundImageUrl})` : 'none',
        opacity: imageLoaded ? 1 : 0
      }"
    ></div>

    <div
      class="main-background-layer"
      :style="{
        backgroundImage: backgroundImageUrl ? `url(${backgroundImageUrl})` : 'none',
        opacity: imageLoaded ? 1 : 0
      }"
    ></div>

    <div class="gradient-mask"></div>

    <div class="register-left">
      <div class="brand-section">
        <h1 class="brand-title">游戏社区交流平台</h1>
        <p class="brand-subtitle">加入我们吧！</p>
      </div>
    </div>

    <div class="register-right">
      <div class="register-card">
        <div class="register-header">
          <h2 class="title">用户注册</h2>
          <p class="subtitle">创建您的账号</p>
        </div>

        <!-- 步骤条 -->
        <n-steps
          class="steps-container"
          :current="currentStep + 1"
          status="process"
        >
          <n-step title="填写账号信息" />
          <n-step title="完善个人资料" />
        </n-steps>

        <NForm
          ref="formRef"
          :model="registrationForm"
          label-placement="left"
          label-width="90px"
          size="large"
        >
          <!-- 第一步：必填项 -->
          <div v-show="currentStep === 0" class="step-content">
            <NFormItem label="账号" path="account">
              <NInput
                v-model:value="registrationForm.account"
                placeholder="请输入账号（仅限英文）"
                clearable
              >
                <template #prefix>
                  <Icon icon="ri:user-line" />
                </template>
              </NInput>
            </NFormItem>

            <NFormItem label="密码" path="password">
              <NInput
                v-model:value="registrationForm.password"
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
                v-model:value="registrationForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password-on="click"
              >
                <template #prefix>
                  <Icon icon="ri:lock-line" />
                </template>
              </NInput>
            </NFormItem>

            <NFormItem label="手机号码" path="phone">
              <NInput
                v-model:value="registrationForm.phone"
                placeholder="请输入手机号码"
                clearable
              >
                <template #prefix>
                  <Icon icon="ri:phone-line" />
                </template>
              </NInput>
            </NFormItem>
          </div>

          <!-- 第二步：可选项 -->
          <div v-show="currentStep === 1" class="step-content">
            <NFormItem label="头像">
              <div class="list_file_list">
                <AvatarUpload
                  v-model="registrationForm.avatar"
                  upload-action="files"
                  :is-disabled="false"
                  @change="handleAvatarChange"
                />
              </div>
              <div class="avatar-tip">
                （可选）不上传则使用默认头像
              </div>
            </NFormItem>

            <NFormItem label="昵称">
              <NInput
                v-model:value="registrationForm.nickname"
                placeholder="请输入昵称（可选）"
                clearable
              >
                <template #prefix>
                  <Icon icon="ri:smile-line" />
                </template>
              </NInput>
            </NFormItem>

            <NFormItem label="邮箱">
              <NInput
                v-model:value="registrationForm.email"
                placeholder="请输入邮箱地址（可选）"
                clearable
              >
                <template #prefix>
                  <Icon icon="ri:mail-line" />
                </template>
              </NInput>
            </NFormItem>
          </div>

          <!-- 操作按钮 -->
          <div class="button-group">
            <NButton
              v-if="currentStep > 0"
              size="large"
              block
              @click="prevStep"
            >
              上一步
            </NButton>

            <NButton
              v-if="currentStep < steps - 1"
              type="primary"
              size="large"
              block
              @click="nextStep"
            >
              下一步
            </NButton>

            <NButton
              v-else
              type="primary"
              size="large"
              :loading="submitting"
              block
              @click="handleRegistration"
            >
              {{ submitting ? '注册中...' : '注册' }}
            </NButton>

            <div class="login-link">
              <span>已有账号？</span>
              <NButton text type="primary" @click="navigateToLogin">
                立即登录
              </NButton>
            </div>
          </div>
        </NForm>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, getCurrentInstance, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import { Icon } from '@iconify/vue'
import AvatarUpload from '@/components/upload/AvatarUpload.vue'
import userApi from '@/api/user'
import { useBackgroundImage } from '@/utils/useBackgroundImage'

interface RegistrationForm {
  account: string
  password: string
  confirmPassword: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
}

const appContext = getCurrentInstance()?.appContext.config.globalProperties
const router = useRouter()
const formRef = ref<FormInst | null>(null)
const message = useMessage()

const projectName = appContext?.$project.projectName

const registrationForm = ref<RegistrationForm>({
  account: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  avatar: '',
  phone: '',
  email: ''
})

const currentStep = ref(0)
const steps = 2
const submitting = ref(false)

const { backgroundImageUrl, imageLoaded } = useBackgroundImage()

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

const handleAvatarChange = (url: string): void => {
  registrationForm.value.avatar = url
}

const validateStep1 = (): boolean => {
  if (!registrationForm.value.account) {
    message.error('账号不能为空')
    return false
  }

  const accountRegex = /^[a-zA-Z0-9_-]+$/
  if (!accountRegex.test(registrationForm.value.account)) {
    message.error('账号只能包含英文字母、数字、下划线或中划线')
    return false
  }

  if (!registrationForm.value.password) {
    message.error('密码不能为空')
    return false
  }

  if (registrationForm.value.password.length < 6) {
    message.error('密码长度不能少于 6 位')
    return false
  }

  if (registrationForm.value.password !== registrationForm.value.confirmPassword) {
    message.error('两次密码输入不一致')
    return false
  }

  if (!registrationForm.value.phone) {
    message.error('请输入手机号码')
    return false
  }

  if (!appContext?.$toolUtil.isMobile(registrationForm.value.phone)) {
    message.error('请输入正确的手机号码格式')
    return false
  }

  return true
}

const validateStep2 = (): boolean => {
  if (registrationForm.value.email && !appContext?.$toolUtil.isEmail(registrationForm.value.email)) {
    message.error('请输入正确的邮箱格式')
    return false
  }

  return true
}

const nextStep = (): void => {
  if (currentStep.value === 0) {
    if (validateStep1()) {
      currentStep.value++
    }
  }
}

const prevStep = (): void => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleRegistration = async (): Promise<void> => {
  if (!validateStep1()) {
    currentStep.value = 0
    return
  }

  if (!validateStep2()) {
    currentStep.value = 1
    return
  }

  submitting.value = true

  try {
    const submitData: any = {
      account: registrationForm.value.account,
      password: registrationForm.value.password,
      phone: registrationForm.value.phone
    }

    if (registrationForm.value.nickname) {
      submitData.nickname = registrationForm.value.nickname
    }

    if (registrationForm.value.email) {
      submitData.email = registrationForm.value.email
    }

    if (registrationForm.value.avatar) {
      const baseUrl = appContext?.$config?.url || 'http://localhost:8080'
      if (registrationForm.value.avatar.startsWith(baseUrl)) {
        submitData.avatar = registrationForm.value.avatar.replace(baseUrl, '')
      } else {
        submitData.avatar = registrationForm.value.avatar
      }
      if (submitData.avatar.startsWith('/')) {
        submitData.avatar = submitData.avatar.substring(1)
      }
    }

    const response = await userApi.createUser(submitData)

    message.success('注册成功', {
      onLeave: () => {
        router.push({ path: "/login" })
      }
    })

  } catch (error) {
    console.error('注册失败:', error)
    message.error('注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const navigateToLogin = (): void => {
  router.push({ path: "/login" })
}
</script>

<style lang="scss" scoped>
.register-container {
  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: hidden;
  background: #ffffff;
}

.blur-background-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-size: cover;
  background-position: center;
  filter: blur(40px) brightness(1.1);
  transform: scale(1.15);
  transition: opacity 0.8s ease-in-out;
  z-index: 0;
}

.main-background-layer {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 80%;

  background-size: auto 100%;
  background-position: center;
  background-repeat: no-repeat;

  transition: opacity 0.8s ease-in-out;
  z-index: 1;
}

.gradient-mask {
  position: absolute;
  top: 0;
  left: 50%;
  right: 0;
  bottom: 0;
  background: linear-gradient(to right, transparent 0%, rgba(255, 255, 255, 0.3) 50%, #ffffff 100%);
  z-index: 2;
  pointer-events: none;
}

.register-left {
  flex: 1;
  position: relative;
  z-index: 3;

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
      text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
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
    background: linear-gradient(to right, rgba(0,0,0,0.1), transparent);
    z-index: 1;
  }
}

.register-right {
  width: 500px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.05);
  overflow-y: auto;
  max-height: 100vh;
  position: relative;
  z-index: 3;
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

  .steps-container {
    width: 100%;
    margin-bottom: 30px;
    padding: 0 20px;
  }

  .step-content {
    width: 100%;
  }

  .avatar-tip {
    margin-left: 90px;
    color: #999;
    font-size: 12px;
    margin-top: 5px;
  }

  .list_file_list {
    width: calc(100% - 90px);

    :deep(.avatar-upload-wrapper) {
      .avatar-preview,
      .avatar-uploader {
        width: 100px;
        height: 100px;
      }
    }
  }

  .button-group {
    margin-top: 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .login-link {
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
