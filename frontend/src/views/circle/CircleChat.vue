<!-- src/views/circle-chat/index.vue -->
<template>
  <div class="circle-chat-page">
    <!-- 左侧：圈子会话列表 -->
    <CircleConversationPanel
      :conversations="store.conversations"
      :loading="store.loading"
      :active-circle-id="store.currentCircle?.id"
      @select-circle="handleSelectCircle"
      @refresh="loadConversations"
    />
      
    <!-- 中间：聊天区域 -->
    <div class="chat-main">
      <CircleChatDetail 
        v-if="store.currentCircle" 
        @show-members="showMemberList = true"
        @show-pending-applications="showPendingApplications"
      />
      <EmptyChat v-else />
    </div>
      
    <!-- 右侧：成员列表 (需要时显示) -->
    <transition name="slide-right">
      <CircleMemberList
        v-if="showMemberList"
        :members="store.members"
        :loading="store.loading"
        @back="showMemberList = false"
        @invite="handleInviteMember"
        @member-click="handleMemberClick"
      />
    </transition>
  </div>

  <!-- 创建圈子对话框 -->
  <n-modal
    v-model:show="showCreateModal"
    title="创建新圈子"
    preset="dialog"
    :positive-button-props="{ loading: creatingLoading }"
    @positive-click="handleCreateCircle"
  >
    <n-form
      ref="createFormRef"
      :model="createForm"
      :rules="createRules"
      label-placement="left"
      label-width="80px"
    >
      <n-form-item
        label="圈子名称"
        path="name"
      >
        <n-input
          v-model:value="createForm.name"
          placeholder="请输入圈子名称"
          maxlength="20"
          show-count
        />
      </n-form-item>
        
      <n-form-item
        label="圈子描述"
        path="description"
      >
        <n-input
          v-model:value="createForm.description"
          type="textarea"
          placeholder="请输入圈子描述（可选）"
          maxlength="200"
          show-count
          :rows="3"
        />
      </n-form-item>
        
      <n-form-item
        label="圈子类型"
        path="type"
      >
        <n-radio-group v-model:value="createForm.type">
          <n-space>
            <n-radio :value="1">
              公开
            </n-radio>
            <n-radio :value="0">
              私密
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>
    </n-form>
  </n-modal>

  <!-- 邀请成员对话框 -->
  <n-modal
    v-model:show="showInviteModal"
    title="邀请成员"
    preset="dialog"
    positive-text="生成邀请链接"
    :positive-button-props="{ loading: inviteLoading }"
    @positive-click="handleGenerateInvite"
  >
    <div class="invite-content">
      <p class="invite-tip">
        生成邀请链接后，分享给好友即可加入圈子
      </p>

      <div
        v-if="inviteLink"
        class="invite-link-box"
      >
        <n-input
          :value="inviteLink"
          readonly
          size="large"
        >
          <template #suffix>
            <n-button
              text
              type="primary"
              @click="copyInviteLink"
            >
              复制
            </n-button>
          </template>
        </n-input>
        <p class="invite-expire">
          有效期至：{{ inviteExpireTime }}
        </p>
      </div>
    </div>
  </n-modal>

  <!-- 接受邀请对话框 -->
  <n-modal
    v-model:show="showAcceptInviteModal"
    title="接受邀请"
    preset="dialog"
    positive-text="加入圈子"
    :positive-button-props="{ loading: acceptInviteLoading }"
    @positive-click="handleAcceptInvite"
  >
    <n-form
      ref="acceptInviteFormRef"
      :model="acceptInviteForm"
      :rules="acceptInviteRules"
      label-placement="left"
      label-width="80px"
    >
      <n-form-item
        label="邀请码"
        path="inviteCode"
      >
        <n-input
          v-model:value="acceptInviteForm.inviteCode"
          placeholder="请输入邀请码"
          maxlength="20"
        />
      </n-form-item>
    </n-form>
  </n-modal>

  <!-- 待审核申请列表对话框 -->
  <n-modal
    v-model:show="showPendingApplicationsModal"
    title="待审核申请"
    preset="card"
    style="width: 600px"
  >
    <n-spin :show="applicationsLoading">
      <n-list
        v-if="pendingApplications.length > 0"
        hoverable
      >
        <n-list-item
          v-for="app in pendingApplications"
          :key="app.userId"
        >
          <template #prefix>
            <n-avatar
              :src="app.avatar"
              round
              size="medium"
            />
          </template>

          <div class="application-item">
            <div class="applicant-info">
              <div class="applicant-name">
                {{ app.nickname }}
              </div>
              <div class="applicant-time">
                申请时间：{{ formatJoinTime(app.joinTime) }}
              </div>
            </div>

            <div class="application-actions">
              <n-button
                size="small"
                type="success"
                @click="handleApproveApplication(app.userId)"
              >
                通过
              </n-button>
              <n-button
                size="small"
                type="error"
                @click="handleRejectApplication(app.userId)"
              >
                拒绝
              </n-button>
            </div>
          </div>
        </n-list-item>
      </n-list>

      <n-empty
        v-else
        description="暂无待审核申请"
      />
    </n-spin>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useMessage, NForm, NFormItem, NInput, NRadioGroup, NRadio, NSpace, NModal, NAvatar, NList, NListItem, NButton, NSpin, NEmpty } from 'naive-ui'
import EmptyChat from '@/components/chat/private/EmptyChat.vue'
import CircleConversationPanel from '@/components/chat/circle/CircleConversationPanel.vue'
import CircleChatDetail from '@/components/chat/circle/CircleChatDetail.vue'
import CircleMemberList from '@/components/chat/circle/CircleMemberList.vue'
import { useCircleChatStore } from '@/stores/circleChat'
import { circleApi } from '@/api/circle'
import { circleMemberApi } from '@/api/circleMember'
import { circleChatApi } from '@/api/circleChat'
import { ensureCircleWebSocketConnected } from '@/api/circleWebSocket'

import type { CircleConversation, CircleMessage, CircleMember } from '@/types/circleChat'
import {getWebSocket} from "@/utils/websocket"
import chatService from '@/api/chat'
import dayjs from 'dayjs'
const store = useCircleChatStore()
const message = useMessage()

// 状态控制
const showMemberList = ref(false)
const showCreateModal = ref(false)
const creatingLoading = ref(false)

// 邀请相关状态
const showInviteModal = ref(false)
const inviteLoading = ref(false)
const inviteLink = ref('')
const inviteExpireTime = ref('')

// 接受邀请相关状态
const showAcceptInviteModal = ref(false)
const acceptInviteLoading = ref(false)
const acceptInviteFormRef = ref<any>(null)
const acceptInviteForm = ref({
  inviteCode: ''
})
const acceptInviteRules = {
  inviteCode: [
    { required: true, message: '请输入邀请码', trigger: 'blur' }
  ]
}

// 待审核申请相关状态
const showPendingApplicationsModal = ref(false)
const applicationsLoading = ref(false)
const pendingApplications = ref<CircleMember[]>([])

/**
 * 加载会话列表
 */
const loadConversations = async () => {
  try {

    store.loading = true
    const result = await circleChatApi.getConversations({ page: 1, limit: 20 })
    store.setConversations(result.list)

  } catch (error: any) {
    console.error('[圈子聊天] 加载会话列表失败:', error)
    message.error(error.message || '加载失败')
  } finally {
    store.loading = false
  }
}

/**
 * 选择圈子
 */
const handleSelectCircle = async (conv: CircleConversation) => {

  try {
    // 1. 获取圈子详情

    const circle = await circleApi.getCircleById(conv.circleId)

    await store.switchCircle(circle)

    await loadChatHistory(circle.id)

    await loadMembers(circle.id)

  } catch (error: any) {
    console.error('[圈子聊天] 切换圈子失败:', error)
    message.error(error.message || '切换失败')
  }
}

/**
 * 加载聊天记录
 */
const loadChatHistory = async (circleId: number) => {
  try {

    const result = await circleChatApi.getChatHistory(circleId, { page: 1, limit: 20 })
    store.setMessages(result.list.reverse(), true)  // reverse 让最新的在下面

    // 处理撤回消息（转换为系统提示）
    store.processRecalledMessages(store.messages)

    // 处理删除消息（转换为系统提示）
    store.processDeletedMessages(store.messages)

  } catch (error: any) {
    console.error('[圈子聊天] 加载聊天记录失败:', error)
  }
}

/**
 * 加载成员列表
 */
const loadMembers = async (circleId: number) => {
  try {

    const result = await circleMemberApi.getMembers(circleId, { page: 1, limit: 100 })

    store.setMembers(result.list)

  } catch (error: any) {
    console.error('[圈子聊天] 加载成员列表失败:', error)
  }
}

/**
 * 邀请成员
 */
const handleInviteMember = () => {
  if (!store.currentCircle) {return}

  // 重置状态
  inviteLink.value = ''
  inviteExpireTime.value = ''
  showInviteModal.value = true
}

/**
 * 生成邀请链接
 */
const handleGenerateInvite = async () => {
  if (!store.currentCircle) {return false}

  try {
    inviteLoading.value = true

    // 这里需要选择一个用户ID来邀请，实际应用中应该弹出用户选择器
    // 暂时使用示例，实际应该让用户选择要邀请的用户
    message.info('请选择要邀请的用户（功能开发中）')

    // TODO: 实现用户选择器，获取 userId
    // const userId = await selectUserToInvite()
    // const result = await circleMemberApi.inviteMember(store.currentCircle.id, userId)
    // inviteLink.value = result.inviteLink
    // inviteExpireTime.value = dayjs(result.expireTime).format('YYYY-MM-DD HH:mm:ss')

    return false // 阻止对话框关闭
  } catch (error: any) {
    console.error('[圈子聊天] 生成邀请链接失败:', error)
    message.error(error.message || '生成失败')
    return false
  } finally {
    inviteLoading.value = false
  }
}

/**
 * 复制邀请链接
 */
const copyInviteLink = () => {
  if (navigator.clipboard && inviteLink.value) {
    navigator.clipboard.writeText(inviteLink.value)
    message.success('已复制到剪贴板')
  }
}

/**
 * 显示接受邀请对话框
 */
const showAcceptInviteDialog = () => {
  acceptInviteForm.value.inviteCode = ''
  showAcceptInviteModal.value = true
}

/**
 * 接受邀请
 */
const handleAcceptInvite = async () => {
  if (!store.currentCircle) {return false}

  try {
    await acceptInviteFormRef.value?.validate()

    acceptInviteLoading.value = true
    await circleMemberApi.acceptInvite(store.currentCircle.id, acceptInviteForm.value.inviteCode)

    message.success('加入成功')
    showAcceptInviteModal.value = false

    // 刷新会话列表
    await loadConversations()

    return true
  } catch (error: any) {
    if (error.errors) {
      return false
    }
    console.error('[圈子聊天] 接受邀请失败:', error)
    message.error(error.message || '加入失败')
    return false
  } finally {
    acceptInviteLoading.value = false
  }
}

/**
 * 显示待审核申请列表
 */
const showPendingApplications = async () => {
  if (!store.currentCircle) {return}

  try {
    applicationsLoading.value = true
    showPendingApplicationsModal.value = true

    const result = await circleMemberApi.getPendingApplications(store.currentCircle.id, {
      page: 1,
      limit: 50
    })

    pendingApplications.value = result.list
  } catch (error: any) {
    console.error('[圈子聊天] 加载待审核申请失败:', error)

    // 根据错误信息给出友好提示
    if (error.response?.data?.msg === '无权限查看') {
      message.warning('您没有权限查看待审核申请')
    } else {
      message.error(error.message || '加载失败')
    }

    showPendingApplicationsModal.value = false
  } finally {
    applicationsLoading.value = false
  }
}

/**
 * 通过申请
 */
const handleApproveApplication = async (userId: number) => {
  if (!store.currentCircle) {return}

  try {
    await circleMemberApi.reviewApplication(store.currentCircle.id, userId, true, '欢迎加入')

    message.success('已通过申请')

    // 刷新待审核列表
    await showPendingApplications()

    // 刷新成员列表
    await loadMembers(store.currentCircle.id)
  } catch (error: any) {
    console.error('[圈子聊天] 审核失败:', error)
    message.error(error.message || '审核失败')
  }
}

/**
 * 拒绝申请
 */
const handleRejectApplication = async (userId: number) => {
  if (!store.currentCircle) {return}

  try {
    await circleMemberApi.reviewApplication(store.currentCircle.id, userId, false, '抱歉，暂不符合要求')

    message.success('已拒绝申请')

    // 刷新待审核列表
    await showPendingApplications()
  } catch (error: any) {
    console.error('[圈子聊天] 拒绝申请失败:', error)
    message.error(error.message || '操作失败')
  }
}

/**
 * 格式化加入时间
 */
const formatJoinTime = (time?: string) => {
  if (!time) {return '未知'}
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

/**
 * 点击成员
 */
const handleMemberClick = (member: CircleMember) => {

  // TODO: 显示成员信息或发起私聊
}

/**
 * 创建圈子
 */
const handleCreateCircle = async () => {
  try {
    await createFormRef.value?.validate()
    
    creatingLoading.value = true

    await circleApi.createCircle(createForm.value)
    
    message.success('创建成功')
    showCreateModal.value = false
    
    // 刷新列表

    await loadConversations()
    
  } catch (error: any) {
    if (error.errors) {
      // 表单验证失败
      return
    }
    console.error('[圈子聊天] 创建圈子失败:', error)
    message.error(error.message || '创建失败')
  } finally {
    creatingLoading.value = false
  }
}

// 生命周期
onMounted(async () => {
  //  新增：确保 WebSocket 已连接
  await ensureCircleWebSocketConnected().catch((error) => {
    console.error('[圈子聊天] WebSocket连接失败:', error)
  })

  await loadConversations()

})

onUnmounted(() => {
  // 不再需要清理，因为使用全局订阅
  // unsubscribeMessage?.()
})
</script>

<style scoped lang="scss">
.circle-chat-page {
  display: flex;
  height: calc(100vh - 30px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chat-main {
  flex: 1;
  min-width: 0;
  background: #f5f5f5;
  position: relative;
}

// 成员列表滑入动画
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.slide-right-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.invite-content {
  .invite-tip {
    font-size: 14px;
    color: #666;
    margin-bottom: 16px;
  }

  .invite-link-box {
    .invite-expire {
      font-size: 12px;
      color: #999;
      margin-top: 8px;
    }
  }
}

.application-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;

  .applicant-info {
    flex: 1;

    .applicant-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
      margin-bottom: 4px;
    }

    .applicant-time {
      font-size: 12px;
      color: #999;
    }
  }

  .application-actions {
    display: flex;
    gap: 8px;
  }
}
</style>
