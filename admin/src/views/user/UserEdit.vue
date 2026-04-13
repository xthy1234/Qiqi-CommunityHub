<template>
  <PageContainer
    :header-title="isEdit ? '编辑用户' : '新增用户'"
    @back="goBack"
  >
    <n-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      class="edit-form"
    >
      <!-- 账号（编辑时禁用） -->
      <n-grid :cols="12" :x-gap="20">
        <n-grid-item :span="6">
          <n-form-item label="账号" path="account">
            <n-input
              v-model:value="formData.account"
              placeholder="请输入账号"
              :disabled="isEdit"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 密码（仅新增模式） -->
        <n-grid-item v-if="!isEdit" :span="6">
          <n-form-item label="密码" path="password">
            <n-input
              v-model:value="formData.password"
              type="password"
              placeholder="请输入密码"
              show-password-on="click"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 昵称 -->
        <n-grid-item :span="6">
          <n-form-item label="昵称" path="nickname">
            <n-input
              v-model:value="formData.nickname"
              placeholder="请输入昵称"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 手机号 -->
        <n-grid-item :span="6">
          <n-form-item label="手机号" path="phone">
            <n-input
              v-model:value="formData.phone"
              placeholder="请输入手机号"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 邮箱 -->
        <n-grid-item :span="6">
          <n-form-item label="邮箱" path="email">
            <n-input
              v-model:value="formData.email"
              placeholder="请输入邮箱"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 性别 -->
        <n-grid-item :span="6">
          <n-form-item label="性别" path="gender">
            <n-radio-group v-model:value="formData.gender">
              <n-radio :value="0">保密</n-radio>
              <n-radio :value="1">男</n-radio>
              <n-radio :value="2">女</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-grid-item>

        <!-- 角色 -->
        <n-grid-item :span="6">
          <n-form-item label="角色" path="roleId">
            <n-select
              v-model:value="formData.roleId"
              :options="roleOptions"
              placeholder="请选择角色"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 生日 -->
        <n-grid-item :span="6">
          <n-form-item label="生日" path="birthday">
            <n-date-picker
              v-model:value="formData.birthday"
              type="date"
              placeholder="请选择生日"
              format="yyyy-MM-dd"
              value-format="yyyy-MM-dd"
              style="width: 100%"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 状态 -->
        <n-grid-item :span="6">
          <n-form-item label="状态" path="status">
            <n-radio-group v-model:value="formData.status">
              <n-radio :value="0">启用</n-radio>
              <n-radio :value="1">禁用</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-grid-item>

        <!-- 个人签名 -->
        <n-grid-item :span="12">
          <n-form-item label="个人签名" path="signature">
            <n-input
              v-model:value="formData.signature"
              type="textarea"
              :rows="3"
              placeholder="请输入个人签名"
            />
          </n-form-item>
        </n-grid-item>
      </n-grid>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <n-button type="primary" @click="handleSubmit" :loading="submitLoading">
          保存
        </n-button>
        <n-button @click="goBack">取消</n-button>
      </div>
    </n-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormRules } from 'naive-ui'
import PageContainer from '@/components/common/PageContainer.vue'
import { adminUserApi } from '@/api/adminUser'

const router = useRouter()
const route = useRoute()
const message = useMessage()

interface FormData {
  id?: number
  account: string
  password?: string
  nickname: string
  phone: string
  email: string
  gender: number
  roleId: number
  birthday: string | null
  signature: string
  status: number
}

const formRef = ref<any>(null)
const submitLoading = ref(false)
const roleOptions = ref<any[]>([])

const formData = reactive<FormData>({
  account: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  gender: 0,
  roleId: 0,
  birthday: null,
  signature: '',
  status: 0
})

const isEdit = computed(() => !!route.query.id)

const validatePhone = (_rule: any, value: string) => {
  if (!value) return Promise.resolve()
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(value)) {
    return Promise.reject(new Error('请输入正确的手机号'))
  }
  return Promise.resolve()
}

const validateEmail = (_rule: any, value: string) => {
  if (!value) return Promise.resolve()
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(value)) {
    return Promise.reject(new Error('请输入正确的邮箱地址'))
  }
  return Promise.resolve()
}

const formRules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6-20 个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]+$/, message: '密码只能包含字母和数字', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  roleId: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const fetchRoles = async () => {
  try {
    const response = await apiService.role.getAllRoles()
    if (response.code === 0 || response.code === 200) {
      roleOptions.value = (response.data || []).map((role: any) => ({
        label: role.roleName,
        value: role.id
      }))
    } else if (response.code === 401) {
      message.error('请先登录')
      setTimeout(() => {
        router.push('/login')
      }, 500)
    }
  } catch (error: any) {
    console.error('获取角色列表失败:', error)
    if (error.response?.status === 401) {
      message.error('请先登录')
      setTimeout(() => {
        router.push('/login')
      }, 500)
    }
  }
}

const fetchUserInfo = async () => {
  if (!isEdit.value) return
  
  const userId = route.query.id
  if (!userId) return
  
  try {
    const response = await adminUserApi.getAdminUserById(Number(userId))
    
    if (response.code === 0 || response.code === 200) {
      const userData = response.data
      Object.assign(formData, {
        id: userData.id,
        account: userData.account,
        nickname: userData.nickname,
        phone: userData.phone,
        email: userData.email,
        gender: userData.gender,
        roleId: userData.roleId,
        birthday: userData.birthday,
        signature: userData.signature,
        status: userData.status
      })
    } else {
      message.error(
response.msg || '获取用户信息失败')
    }
  } catch (error: any) {
    console.error('获取用户信息失败:', error)
    message.error(error.response?.data?.msg || '获取用户信息失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    
    submitLoading.value = true
    
    const submitData = { ...formData }
    if (isEdit.value) {
      delete submitData.password
      
      const response = await adminUserApi.updateAdminUser(submitData.id!, submitData)
      
      if (response.code === 0 || response.code === 200) {
        message.success('更新成功')
        setTimeout(() => {
          router.push('/users')
        }, 500)
      } else {
        message.error(
response.msg || '更新失败')
      }
    } else {
      const response = await adminUserApi.createUser(submitData)
      
      if (response.code === 0 || response.code === 200) {
        message.success('创建成功')
        setTimeout(() => {
          router.push('/users')
        }, 500)
      } else {
        message.error(
response.msg || '创建失败')
      }
    }
  } catch (error: any) {
    if (error !== false) {
      console.error('提交失败:', error)
      message.error(error.response?.data?.msg || '操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchRoles()
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.edit-form {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  
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
