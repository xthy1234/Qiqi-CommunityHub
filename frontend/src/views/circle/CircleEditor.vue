<template>
  <PageContainer
    :header-title="isEdit ? '编辑圈子' : '创建圈子'"
    :show-back="false"
  >
    <div class="circle-editor-page">
      <!-- 表单区域 -->
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="100px"
        class="circle-form"
      >
        <!-- 圈子名称 -->
        <n-form-item
          label="圈子名称"
          path="name"
        >
          <n-input
            v-model:value="formData.name"
            placeholder="请输入圈子名称"
            maxlength="20"
            show-count
            clearable
          />
        </n-form-item>

        <!-- 圈子头像 -->
        <n-form-item label="圈头像">
          <div class="avatar-upload-content">
            <AvatarUpload
              v-model="avatarUrl"
              upload-action="files"
              :is-disabled="false"
              @change="handleAvatarChange"
            >
              <n-button
                type="primary"
                size="small"
              >
                更换头像
              </n-button>
            </AvatarUpload>
          </div>
        </n-form-item>
        <!-- 圈子描述 -->
        <n-form-item
          label="圈子描述"
          path="description"
        >
          <n-input
            v-model:value="formData.description"
            type="textarea"
            placeholder="请输入圈子描述（可选）"
            maxlength="200"
            show-count
            :rows="4"
            clearable
          />
        </n-form-item>

        <!-- 圈子类型 -->
        <n-form-item
          label="圈子类型"
          path="type"
        >
          <n-radio-group v-model:value="formData.type">
            <n-space>
              <n-radio :value="1">
                <div class="radio-content">
                  <strong>公开</strong>
                  <div class="desc">
                    任何人都可以查看和申请加入
                  </div>
                </div>
              </n-radio>
              <n-radio :value="0">
                <div class="radio-content">
                  <strong>私密</strong>
                  <div class="desc">
                    只有被邀请的成员才能加入
                  </div>
                </div>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <n-button
            size="large"
            @click="handleCancel"
          >
            取消
          </n-button>
          <n-button
            size="large"
            :loading="saving"
            @click="handleSave"
          >
            {{ isEdit ? '保存修改' : '创建圈子' }}
          </n-button>
        </div>
      </n-form>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {useMessage, useDialog, type FormRules, NButton, NFormItem} from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'
import { circleApi } from '@/api/circle'
import AvatarUpload from '@/components/upload/AvatarUpload.vue'
import PageContainer from '@/components/common/PageContainer.vue'

const appContext = useGlobalProperties()
const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const formRef = ref<any>(null)
const isEdit = ref(false)
const saving = ref(false)
const circleId = ref('')
const avatarUrl = ref('')

const formData = reactive({
  name: '',
  avatar: '',
  description: '',
  type: 1  // 1-公开，0-私密
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入圈子名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
}

const handleAvatarChange = (url: string) => {
  formData.avatar = url
  if (formData.avatar) {
    avatarUrl.value = formData.avatar.startsWith('http')
        ? formData.avatar
        : `${appContext?.$config?.url || 'http://localhost:8080'}/${formData.avatar}`
  }
}

/**
 * 返回首页
 */
const navigateToHome = () => {
  router.push('/index/home')
}

/**
 * 取消操作
 */
const handleCancel = () => {
  dialog.warning({
    title: '提示',
    content: '确定要取消吗？未保存的内容将丢失',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      router.back()
    }
  })
}

/**
 * 保存圈子
 */
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    
    saving.value = true
    
    if (isEdit.value && circleId.value) {
      // 更新圈子
      await circleApi.updateCircle(parseInt(circleId.value), {
        name: formData.name,
        description: formData.description || undefined,
        avatar: formData.avatar || undefined,
        type: formData.type
      })
      
      message.success('修改成功')
    } else {
      // 创建新圈子
      const result = await circleApi.createCircle({
        name: formData.name,
        description: formData.description || undefined,
        avatar: formData.avatar || undefined,
        type: formData.type
      })
      
      message.success('创建成功')
      
      // 跳转到圈子聊天页面
      setTimeout(() => {
        router.push('/index/circle-chat')
      }, 500)
    }
    
  } catch (error: any) {
    if (error.errors) {
      // 表单验证失败
      return
    }
    console.error('保存圈子失败:', error)
    message.error(error.message || (isEdit.value ? '修改失败' : '创建失败'))
  } finally {
    saving.value = false
  }
}

/**
 * 加载圈子详情
 */
const loadCircleDetail = async () => {
  if (!route.query.id) {return}
  
  try {
    const data = await circleApi.getCircleById(parseInt(route.query.id as string))
    
    if (data) {
      Object.assign(formData, {
        name: data.name || '',
        avatar: data.avatar || '',
        description: data.description || '',
        type: data.type ?? 1
      })
    }
  } catch (error) {
    console.error('加载圈子详情失败:', error)
    message.error('获取圈子信息失败')
  }
}

onMounted(() => {
  isEdit.value = !!route.query.id
  
  if (isEdit.value) {
    circleId.value = route.query.id as string
    loadCircleDetail()
  }
})
</script>

<style scoped lang="scss">
.circle-editor-page {
  padding: 20px 10%;
  background: #fff;
  min-height: calc(100vh - 60px);
  
  .circle-form {
    background: #f9f9f9;
    padding: 30px;
    border-radius: 8px;
    max-width: 800px;
    margin: 0 auto;
    
    .radio-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
      
      strong {
        font-size: 14px;
        color: #333;
      }
      
      .desc {
        font-size: 12px;
        color: #999;
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
      }
    }
  }
}
</style>
