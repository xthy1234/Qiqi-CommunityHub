<template>
  <PageContainer
    header-title="发送通知"
    @back="goBack"
  >
    <div class="notification-content">
      <n-alert
        title="通知说明"
        type="info"
        :bordered="false"
        class="notification-tips"
      >
        <div>• 不选择用户时，将发送给所有用户（全员广播）</div>
        <div>• 选择指定用户后，仅发送给选中的用户</div>
        <div>• 紧急通知会优先展示并可能触发强提醒</div>
      </n-alert>

      <n-form
        ref="notificationFormRef"
        :model="notificationForm"
        :rules="notificationRules"
        label-width="120px"
        class="notification-form"
      >
        <n-form-item
          label="接收用户"
          path="userIds"
        >
          <n-select
            v-model:value="notificationForm.userIds"
            multiple
            filterable
            remote
            :options="userOptions"
            :loading="userLoading"
            placeholder="留空表示全员广播，可搜索选择指定用户"
            clearable
            @search="handleUserSearch"
          >
            <template #option="{ option }">
              <div class="user-option">
                <img
                  v-if="option.avatar"
                  :src="option.avatar"
                  class="user-avatar"
                  alt="avatar"
                />
                <div v-else class="user-avatar-placeholder">
                  {{ (option.label as string).charAt(0) }}
                </div>
                <span class="user-info">{{ option.label }}</span>
              </div>
            </template>
          </n-select>
          <template #feedback>
            <div class="form-tip">
              {{ notificationForm.userIds && notificationForm.userIds.length > 0 
                ? `已选择 ${notificationForm.userIds.length} 个用户` 
                : '未选择用户，将发送给所有人' }}
            </div>
          </template>
        </n-form-item>

        <n-form-item
          label="通知标题"
          path="title"
        >
          <n-input
            v-model:value="notificationForm.title"
            placeholder="请输入通知标题"
            maxlength="100"
            show-count
            clearable
          />
        </n-form-item>

        <n-form-item
          label="通知内容"
          path="content"
        >
          <n-input
            v-model:value="notificationForm.content"
            type="textarea"
            placeholder="请输入通知内容"
            :rows="6"
            maxlength="500"
            show-count
          />
        </n-form-item>

        <n-form-item
          label="链接地址"
          path="linkUrl"
        >
          <n-input
            v-model:value="notificationForm.linkUrl"
            placeholder="可选，点击通知跳转的链接地址"
            clearable
          />
        </n-form-item>

        <n-form-item
          label="优先级"
          path="priority"
        >
          <n-radio-group v-model:value="notificationForm.priority">
            <n-space>
              <n-radio :value="1">普通</n-radio>
              <n-radio :value="2">重要</n-radio>
              <n-radio :value="3">紧急</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-form-item
          label="是否置顶"
          path="isTop"
        >
          <n-switch v-model:value="notificationForm.isTop">
            <template #checked>是</template>
            <template #unchecked>否</template>
          </n-switch>
        </n-form-item>

        <n-divider>扩展数据（可选）</n-divider>

        <n-form-item
          label="动作类型"
          path="extra.actionType"
        >
          <n-input
            v-model:value="notificationForm.extra.actionType"
            placeholder="例如：view_announcement、open_activity"
            clearable
          />
          <template #feedback>
            <div class="form-tip">前端可根据此字段执行不同操作</div>
          </template>
        </n-form-item>

        <n-form-item
          label="关联ID"
          path="extra.announcementId"
        >
          <n-input-number
            v-model:value="notificationForm.extra.announcementId"
            placeholder="例如：公告ID、活动ID等"
            clearable
          />
        </n-form-item>

        <n-form-item
          label="Banner图片"
          path="extra.banner"
        >
          <n-input
            v-model:value="notificationForm.extra.banner"
            placeholder="可选，Banner图片URL"
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
            发送通知
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import PageContainer from '@/components/layout/PageContainer.vue'
import { notificationApi } from '@/api/notification'
import { adminUserApi } from '@/api/adminUser'
import type { FormRules, SelectOption } from 'naive-ui'

const router = useRouter()
const message = useMessage()

interface NotificationForm {
  userIds: number[] | null
  title: string
  content: string
  linkUrl: string | null
  priority: number
  isTop: boolean
  extra: {
    actionType?: string
    announcementId?: number
    banner?: string
    [key: string]: any
  }
}

const notificationFormRef = ref(null)
const isSubmitting = ref(false)
const userLoading = ref(false)
const userOptions = ref<SelectOption[]>([])

const notificationForm = reactive<NotificationForm>({
  userIds: null,
  title: '',
  content: '',
  linkUrl: null,
  priority: 1,
  isTop: false,
  extra: {
    actionType: '',
    announcementId: undefined,
    banner: ''
  }
})

const notificationRules = reactive<FormRules>({
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度在 1-100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入通知内容', trigger: 'blur' },
    { min: 1, max: 500, message: '内容长度在 1-500 个字符', trigger: 'blur' }
  ]
})

const handleUserSearch = async (query: string) => {
  if (!query || query.trim() === '') {
    userOptions.value = []
    return
  }

  try {
    userLoading.value = true

    // 使用 adminUserApi 获取用户列表
    const response = await adminUserApi.getUserList({
      page: 1,
      limit: 50,
      account: query.trim()  // 使用账号搜索
    })

    // 响应拦截器已 unwrap，response 就是 { code, msg, data }
    if ((response.code === 0 || response.code === 200) && response.data?.list) {
      userOptions.value = response.data.list.map((user: any) => ({
        label: `${user.nickname || user.account} (${user.id})`,
        value: user.id,
        avatar: user.avatar || null  // 保存头像URL
      }))

    } else {
      userOptions.value = []
      console.warn('未找到用户或响应格式错误')
    }
  } catch (error: any) {
    console.error('搜索用户失败:', error)
    message.error(error.response?.data?.msg || '搜索用户失败')
    userOptions.value = []
  } finally {
    userLoading.value = false
  }
}

const loadRecentUsers = async () => {
  try {
    const response = await adminUserApi.getUserList({
      page: 1,
      limit: 10
    })

    if ((response.code === 0 || response.code === 200) && response.data?.list) {
      userOptions.value = response.data.list.map((user: any) => ({
        label: `${user.nickname || user.account} (${user.id})`,
        value: user.id,
        avatar: user.avatar || null  // 保存头像URL
      }))
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

const handleSubmit = async () => {
  if (!notificationFormRef.value) return
  
  try {
    await notificationFormRef.value.validate()
    
    isSubmitting.value = true
    
    // 构建请求数据
    const requestData: any = {
      title: notificationForm.title,
      content: notificationForm.content,
      linkUrl: notificationForm.linkUrl || null,
      priority: notificationForm.priority,
      isTop: notificationForm.isTop,
      extra: Object.keys(notificationForm.extra).some(key => notificationForm.extra[key])
        ? notificationForm.extra 
        : {}
    }

    // 如果选择了用户，添加 userIds
    if (notificationForm.userIds && notificationForm.userIds.length > 0) {
      requestData.userIds = notificationForm.userIds
    }

    const response = await notificationApi.sendNotification(requestData)

    // 响应拦截器已 unwrap，response 就是 { code, msg }
    if (response.code === 0) {
      message.success(response.msg || '发送成功')
      setTimeout(() => {
        goBack()
      }, 1500)
    } else {
      message.error(response.msg || '发送失败')
    }
  } catch (error: any) {
    console.error('发送通知失败:', error)
    const errorMsg = error.response?.data?.msg || error.message || '发送失败，请检查网络连接'
    message.error(errorMsg)
  } finally {
    isSubmitting.value = false
  }
}

const handleReset = () => {
  if (!notificationFormRef.value) return
  
  notificationForm.userIds = null
  notificationForm.title = ''
  notificationForm.content = ''
  notificationForm.linkUrl = null
  notificationForm.priority = 1
  notificationForm.isTop = false
  notificationForm.extra = {
    actionType: '',
    announcementId: undefined,
    banner: ''
  }
  
  userOptions.value = []
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  // 预加载最近活跃的用户（可选）
  loadRecentUsers()
})


</script>

<style lang="scss" scoped>
.notification-content {
  .notification-tips {
    margin-bottom: 30px;
  }
  
  .notification-form {
    .form-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 5px;
    }
    
    .user-option {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 4px 0;

      .user-avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        object-fit: cover;
        flex-shrink: 0;
      }

      .user-avatar-placeholder {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        font-weight: bold;
        flex-shrink: 0;
      }

      .user-info {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
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
