<template>
  <div class="registration_page">
    <div class="register_view">
      <n-form
        ref="formRef"
        :model="registrationForm"
        class="register_form"
        label-placement="left"
        label-width="120px"
      >
        <div class="title_view">{{ projectName }}注册</div>

        <!-- 步骤条 -->
        <n-steps class="steps-container" :current="currentStep + 1" status="process">
          <n-step title="填写账号信息" />
          <n-step title="完善个人资料" />
        </n-steps>

        <!-- 第一步：必填项 -->
        <div v-show="currentStep === 0" class="step-content">
          <n-form-item
            label="账号："
            path="account"
            :rule="{
              required: true,
              message: '账号不能为空',
              trigger: 'blur'
            }"
          >
            <n-input
              v-model:value="registrationForm.account"
              placeholder="请输入账号（仅限英文）"
            />
          </n-form-item>

          <n-form-item
            label="密码："
            path="password"
            :rule="{
              required: true,
              message: '密码不能为空',
              trigger: 'blur'
            }"
          >
            <n-input
              v-model:value="registrationForm.password"
              type="password"
              placeholder="请输入密码"
            />
          </n-form-item>

          <n-form-item
            label="确认密码："
            path="confirmPassword"
            :rule="{
              required: true,
              message: '请再次输入密码',
              trigger: 'blur'
            }"
          >
            <n-input
              v-model:value="registrationForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
            />
          </n-form-item>

          <n-form-item
            label="手机号码："
            path="phone"
            :rule="{
              required: true,
              message: '请输入手机号码',
              trigger: 'blur'
            }"
          >
            <n-input
              v-model:value="registrationForm.phone"
              placeholder="请输入手机号码"
            />
          </n-form-item>
        </div>

        <!-- 第二步：可选项 -->
        <div v-show="currentStep === 1" class="step-content">
          <n-form-item label="头像：">
            <div class="list_file_list">
              <AvatarUpload
                v-model="registrationForm.avatar"
                upload-action="files"
                :is-disabled="false"
                @change="handleAvatarChange"
              />
            </div>
            <div class="avatar-tip">（可选）不上传则使用默认头像</div>
          </n-form-item>

          <n-form-item label="昵称：">
            <n-input
              v-model:value="registrationForm.nickname"
              placeholder="请输入昵称（可选）"
            />
          </n-form-item>

          <n-form-item
            label="邮箱："
            :rule="{
              type: 'email',
              message: '请输入正确的邮箱格式',
              trigger: 'blur'
            }"
          >
            <n-input
              v-model:value="registrationForm.email"
              placeholder="请输入邮箱地址（可选）"
            />
          </n-form-item>
        </div>

        <!-- 操作按钮 -->
        <div class="list_btn">
          <n-button
            v-if="currentStep > 0"
            @click="prevStep"
          >
            上一步
          </n-button>

          <n-button
            v-if="currentStep < steps - 1"
            type="success"
            @click="nextStep"
          >
            下一步
          </n-button>

          <n-button
            v-else
            type="success"
            :loading="submitting"
            @click="handleRegistration"
          >
            注册
          </n-button>

          <div class="login_redirect" @click="navigateToLogin">
            已有账号，直接登录
          </div>
        </div>
      </n-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import AvatarUpload from '@/components/AvatarUpload.vue'
import userApi from '@/api/user'

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
// const route = useRoute()
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

const initializePage = (): void => {}

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
      phone: registrationForm.value.phone,
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

onMounted(() => {
  initializePage()
})
</script>

<style lang="scss" scoped>
.registration_page {
  .register_view {
    background-repeat: no-repeat;
    flex-direction: column;
    background-size: cover;
    background: url(http://clfile.zggen.cn/20231229/e2095d3a74784da2bdff07be79797146.jpg);
    display: flex;
    min-height: 100vh;
    justify-content: center;
    align-items: center;
    position: relative;
    background-position: center center;

    .register_form {
      border-radius: 20px;
      padding: 30px 20px 40px;
      box-shadow: 0px 26px 36px -20px #083f5d;
      margin: 20px 0;
      background: #fff;
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 600px;
      min-height: auto;
    }

    .steps-container {
      width: 100%;
      margin-bottom: 30px;
      padding: 0 20px;
    }

    .step-content {
      width: 100%;
    }

    .title_view {
      padding: 0;
      margin: 0 0 20px;
      color: #0d6392;
      width: 100%;
      font-size: 18px;
      text-align: center;
    }

    .avatar-tip {
      margin-left: 120px;
      color: #999;
      font-size: 12px;
      margin-top: 5px;
    }

    .list_file_list {
      width: calc(100% - 120px);

      :deep(.avatar-upload-wrapper) {
        .avatar-preview,
        .avatar-uploader {
          width: 100px;
          height: 100px;
        }
      }
    }

    .list_btn {
      margin: 20px 0 0;
      display: flex;
      width: 100%;
      justify-content: center;
      align-items: center;
      flex-wrap: wrap;
      gap: 20px;

      .login_redirect {
        cursor: pointer;
        padding: 20px 0 0 80px;
        color: #999;
        width: 80%;
        font-size: 13px;
        text-align: right;

        &:hover {
          color: #0d6392;
        }
      }
    }
  }
}
</style>