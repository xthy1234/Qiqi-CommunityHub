<template>
  <PageContainer
    header-title="编辑个人资料"
    @back="goBack"
  >
      <n-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="120px"
        class="edit-form"
      >
        <!-- 头像上传 -->
        <n-form-item label="头像">
          <div class="avatar-upload-content">
<!--          <n-avatar-->
<!--            :src="avatarUrl || getDefaultAvatar()"-->
<!--            :size="120"-->
<!--            round-->
<!--            class="avatar-preview"-->
<!--          />-->
            <AvatarUpload
              v-model="avatarUrl"
              upload-action="files"
              :is-disabled="false"
              @change="handleAvatarChange"
            >
              <n-button type="primary" size="small">
                更换头像
              </n-button>
            </AvatarUpload>
          </div>
        </n-form-item>

        <n-grid :cols="12" :x-gap="20">
          <!-- 用户账号（只读） -->
          <n-grid-item :span="6">
            <n-form-item label="用户账号" path="account">
              <n-input
                v-model:value="editForm.account"
                placeholder="用户账号"
                readonly
                disabled
              />
            </n-form-item>
          </n-grid-item>

          <!-- 用户姓名 -->
          <n-grid-item :span="6">
            <n-form-item label="用户姓名" path="nickname">
              <n-input
                v-model:value="editForm.nickname"
                placeholder="请输入用户姓名"
                clearable
              />
            </n-form-item>
          </n-grid-item>

          <!-- 性别选择 -->
          <n-grid-item :span="6">
            <n-form-item label="性别" path="gender">
              <n-select
                v-model:value="editForm.gender"
                :options="genderOptions"
                placeholder="请选择性别"
                clearable
              />
            </n-form-item>
          </n-grid-item>

          <!-- 手机号码 -->
          <n-grid-item :span="6">
            <n-form-item label="手机号码" path="phone">
              <n-input
                v-model:value="editForm.phone"
                placeholder="请输入手机号码"
                maxlength="11"
                clearable
              />
            </n-form-item>
          </n-grid-item>

          <!-- 邮箱地址 -->
          <n-grid-item :span="6">
            <n-form-item label="邮箱" path="email">
              <n-input
                v-model:value="editForm.email"
                placeholder="请输入邮箱地址"
                clearable
              />
            </n-form-item>
          </n-grid-item>

          <!-- 生日选择 -->
          <n-grid-item :span="6">
            <n-form-item label="生日" path="birthday">
              <n-date-picker
                v-model:value="editForm.birthday"
                type="date"
                placeholder="选择生日"
                style="width: 100%"
                format="yyyy-MM-dd"
                value-format="yyyy-MM-dd"
              />
            </n-form-item>
          </n-grid-item>

          <!-- 个性签名 -->
          <n-grid-item :span="12">
            <n-form-item label="个性签名" path="signature">
              <n-input
                v-model:value="editForm.signature"
                type="textarea"
                :rows="4"
                placeholder="请输入个性签名"
                maxlength="200"
                show-count
              />
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <n-button type="primary" @click="handleSubmit" :loading="isSubmitting">
            保存修改
          </n-button>
          <n-button @click="goBack">取消</n-button>
        </div>
      </n-form>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { useMessage } from 'naive-ui'
import { NMessageProvider, useDialog, NButton, NInput, NForm, NFormItem, NAvatar, NSpace } from 'naive-ui'
import { getFullUrl } from '@/utils/userUtils'
import type { FormRules } from 'naive-ui'
import AvatarUpload from '@/components/AvatarUpload.vue'
import PageContainer from '@/components/PageContainer.vue'
import PageHeader from '@/components/PageHeader.vue'
import userService from '@/api/user'

const router = useRouter()
const formRef = ref<any>(null)
const editFormRef = ref<any>(null)
const message = useMessage()
const dialog = useDialog()
const appContext = useGlobalProperties()
const $http = appContext?.$http


interface EditForm {
  id?: number
  account: string
  nickname: string
  avatar: string
  gender: number | null
  phone: string
  email: string
  birthday: string | null
  signature: string
}

const isSubmitting = ref(false)
const avatarUrl = ref('')

const genderOptions = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

const editForm = reactive<EditForm>({
  account: '',
  nickname: '',
  avatar: '',
  gender: null,
  phone: '',
  email: '',
  birthday: null,
  signature: ''
})

const validateMobile = (rule: any, value: string) => {
  if (!value) return Promise.resolve()
  const mobileReg = /^1[3-9]\d{9}$/
  if (!mobileReg.test(value)) {
    return Promise.reject(new Error('请输入正确的手机号码'))
  }
  return Promise.resolve()
}

const validateEmail = (rule: any, value: string) => {
  if (!value) return Promise.resolve()
  const emailReg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!emailReg.test(value)) {
    return Promise.reject(new Error('请输入正确的邮箱地址'))
  }
  return Promise.resolve()
}

const editRules = reactive<FormRules>({
  nickname: [
    { required: true, message: '请输入用户姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { validator: validateMobile, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ]
})

const getDefaultAvatar = (): string => {
  return '/placeholder.svg'
}

const fetchUserInfo = async () => {
  try {
    const currentUser = await userService.getCurrentUser()
    if (!currentUser?.id) {
      message.error('请先登录')
      router.push('/login')
      return
    }

    const response = await $http({
      url: `users/${currentUser.id}`,
      method: 'get',
      params: { detail: true },
      credentials: 'include'
    })
    
    const userData = response.data.data
    if (userData) {
      editForm.account = userData.account || ''
      editForm.nickname = userData.nickname || userData.username || ''
      editForm.avatar = userData.avatar || ''
      editForm.gender = userData.gender ?? null
      editForm.phone = userData.phone || ''
      editForm.email = userData.email || ''
      editForm.birthday = userData.birthday || null
      editForm.signature = userData.signature || ''
      editForm.id = userData.id

      if (editForm.avatar) {
        avatarUrl.value = getFullUrl(editForm.avatar)
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    message.error('获取用户信息失败')
  }
}

const handleAvatarChange = (url: string) => {
  editForm.avatar = url
  if (editForm.avatar) {
    // 直接赋值，因为 AvatarUpload 已经返回完整 URL
    avatarUrl.value = editForm.avatar.startsWith('http')
      ? editForm.avatar
      : `${appContext?.$config?.url || 'http://localhost:8080'}/${editForm.avatar}`
  }
}


const handleSubmit = async () => {
  if (!editFormRef.value) return

  try {
    await editFormRef.value.validate()
    
    isSubmitting.value = true
    
    const updateData = {
      nickname: editForm.nickname,
      avatar: editForm.avatar,
      gender: editForm.gender ?? 0,
      phone: editForm.phone,
      email: editForm.email,
      birthday: editForm.birthday,
      signature: editForm.signature
    }

    const response = await $http({
      url: `users/${editForm.id}`,
      method: 'put',
      data: updateData
    })

    if (response.data && response.data.data) {
      const userInfoKey = 'userInfo'
      const existingCache = appContext?.$toolUtil?.storageGet(userInfoKey)
      const cachedUser = existingCache ? JSON.parse(existingCache) : {}
      const updatedUser = {
        ...cachedUser,
        ...response.data.data,
        token: cachedUser.token,
        refreshToken: cachedUser.refreshToken
      }
      appContext?.$toolUtil?.storageSet(userInfoKey, JSON.stringify(updatedUser))
    }

    message.success('修改成功')
    setTimeout(() => {
      router.push('/index/user/profile')
    }, 500)
  } catch (error: any) {
    console.error('修改失败:', error)
    message.error(error.message || '修改失败')
  } finally {
    isSubmitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.edit-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.edit-form {
  .avatar-upload-content {
    display: flex;
    align-items: center;
    gap: 30px;
    
    .avatar-preview {
      border: 3px solid #e4e7ed;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
  }
  
  .form-actions {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-top: 40px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    
    .n-button {
      min-width: 120px;
      height: 40px;
      font-size: 16px;
    }
  }
}
</style>
