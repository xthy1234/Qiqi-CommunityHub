<template>
  <n-modal
    :show="visible"
    @update:show="handleUpdateShow"
    :title="isEdit ? '编辑用户' : '新增用户'"
    :style="{ width: '600px' }"
    :close-on-esc="false"
    :mask-closable="false"
  >
    <NForm
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-placement="left"
      label-width="100px"
    >
      <NFormItem label="账号" path="account">
        <NInput
          v-model:value="formData.account"
          placeholder="请输入账号"
          :disabled="isEdit"
        />
      </NFormItem>

      <NFormItem
        v-if="!isEdit"
        label="密码"
        path="password"
      >
        <NInput
          v-model:value="formData.password"
          type="password"
          placeholder="请输入密码"
          show-password-on="click"
        />
      </NFormItem>

      <NFormItem label="昵称" path="nickname">
        <NInput
          v-model:value="formData.nickname"
          placeholder="请输入昵称"
        />
      </NFormItem>

      <NFormItem label="手机号" path="phone">
        <NInput
          v-model:value="formData.phone"
          placeholder="请输入手机号"
        />
      </NFormItem>

      <NFormItem label="邮箱" path="email">
        <NInput
          v-model:value="formData.email"
          placeholder="请输入邮箱"
        />
      </NFormItem>

      <NFormItem label="性别" path="gender">
        <NRadioGroup v-model:value="formData.gender">
          <NRadio :value="0">保密</NRadio>
          <NRadio :value="1">男</NRadio>
          <NRadio :value="2">女</NRadio>
        </NRadioGroup>
      </NFormItem>

      <NFormItem label="角色" path="roleId">
        <NSelect
          v-model:value="formData.roleId"
          :options="roleOptions"
          placeholder="请选择角色"
        />
      </NFormItem>

      <NFormItem label="生日" path="birthday">
        <NDatePicker
          v-model:value="formData.birthday"
          type="date"
          placeholder="请选择生日"
          format="yyyy-MM-dd"
          style="width: 100%"
        />
      </NFormItem>

      <NFormItem label="个人签名" path="signature">
        <NInput
          v-model:value="formData.signature"
          type="textarea"
          placeholder="请输入个人签名"
          :rows="3"
        />
      </NFormItem>

      <NFormItem label="状态" path="status">
        <NRadioGroup v-model:value="formData.status">
          <NRadio value="ENABLED">启用</NRadio>
          <NRadio value="DISABLED">禁用</NRadio>
        </NRadioGroup>
      </NFormItem>
    </NForm>

    <template #action>
      <NButton @click="handleCancel">取消</NButton>
      <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
        确定
      </NButton>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormRules, FormInst } from 'naive-ui'
import { useMessage } from 'naive-ui'
import apiService from '@/api'

const router = useRouter()
const message = useMessage()

interface UserData {
  id?: number
  account: string
  password?: string
  nickname: string
  phone: string
  email?: string
  avatar?: string
  gender: number
  roleId: number
  birthday?: string
  signature?: string
  status: string
}

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  userData: {
    type: Object as () => UserData | null,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'success'])

const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const roleOptions = ref<any[]>([])


const formData = reactive<UserData>({
  account: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  gender: 0,
  roleId: 0,
  birthday: undefined,
  signature: '',
  status: 'ENABLED'
})

const isEdit = computed(() => !!props.userData?.id)

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

watch(() => props.visible, (val: boolean) => {
  if (val && props.userData) {
    // 编辑模式，填充表单数据
    Object.assign(formData, {
      id: props.userData.id,
      account: props.userData.account,
      nickname: props.userData.nickname,
      phone: props.userData.phone,
      email: props.userData.email,
      gender: props.userData.gender,
      roleId: props.userData.roleId,
      birthday: props.userData.birthday,
      signature: props.userData.signature,
      status: props.userData.status
    })
  } else if (val) {
    // 新增模式，重置表单
    resetForm()
  }
})

watch(() => props.visible, (val: boolean) => {
  if (!val) {
    resetForm()
  }
})

const fetchRoles = async () => {
  try {
    const response = await apiService.role.getAllRoles() as ApiResponse<any[]>
    if (response.data.code === 0 || response.data.code === 200) {
      roleOptions.value = (response.data.data || []).map((role: any) => ({
        label: role.roleName,
        value: role.id
      }))
    } else if (response.data.code === 401) {
      message.error('请先登录')
      setTimeout(() => {
        emit('update:visible', false)
        router.push('/login')
      }, 500)
    }
  } catch (error: any) {
    console.error('获取角色列表失败:', error)
    if (error.response?.status === 401) {
      message.error('请先登录')
      setTimeout(() => {
        emit('update:visible', false)
        router.push('/login')
      }, 500)
    }
  }
}

const resetForm = () => {
  formData.account = ''
  formData.password = ''
  formData.nickname = ''
  formData.phone = ''
  formData.email = ''
  formData.gender = 0
  formData.roleId = 0
  formData.birthday = undefined
  formData.signature = ''
  formData.status = 'ENABLED'
  formRef.value?.restoreValidation()
}

const handleUpdateShow = (value: boolean) => {
  emit('update:visible', value)
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    
    submitLoading.value = true
    
    const submitData = { ...formData }
    if (isEdit.value) {
      // 编辑模式，不需要密码字段
      delete submitData.password
      
      const response = await apiService.user.updateUser(submitData.id!, submitData)
      
      if (response.data.code === 0 || response.data.code === 200) {
        message.success('更新成功')
        emit('update:visible', false)
        emit('success')
      } else {
        message.error(response.data.msg || '更新失败')
      }
    } else {
      // 新增模式
      const response = await apiService.user.createUser(submitData)
      
      if (response.data.code === 0 || response.data.code === 200) {
        message.success('创建成功')
        emit('update:visible', false)
        emit('success')
      } else {
        message.error(response.data.msg || '创建失败')
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

const handleCancel = () => {
  emit('update:visible', false)
}

onMounted(() => {
  fetchRoles()
})
</script>
