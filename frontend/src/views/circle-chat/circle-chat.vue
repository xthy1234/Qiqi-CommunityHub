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
        <n-form-item label="圈子名称" path="name">
          <n-input
            v-model:value="createForm.name"
            placeholder="请输入圈子名称"
            maxlength="20"
            show-count
          />
        </n-form-item>
        
        <n-form-item label="圈子描述" path="description">
          <n-input
            v-model:value="createForm.description"
            type="textarea"
            placeholder="请输入圈子描述（可选）"
            maxlength="200"
            show-count
            :rows="3"
          />
        </n-form-item>
        
        <n-form-item label="圈子类型" path="type">
          <n-radio-group v-model:value="createForm.type">
            <n-space>
              <n-radio :value="1">公开</n-radio>
              <n-radio :value="0">私密</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </n-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useMessage, NForm, NFormItem, NInput, NRadioGroup, NRadio, NSpace, NModal } from 'naive-ui'
import EmptyChat from '@/components/chat/EmptyChat.vue'
import CircleConversationPanel from '@/components/circle-chat/CircleConversationPanel.vue'
import CircleChatDetail from '@/components/circle-chat/CircleChatDetail.vue'
import CircleMemberList from '@/components/circle-chat/CircleMemberList.vue'
import { useCircleChatStore } from '@/stores/circleChat'
import { circleApi, circleMemberApi, circleChatApi, circleWebSocket } from '@/api/circle'
import type { CircleConversation, CircleMessage, CircleMember } from '@/types/circleChat'
import {getWebSocket} from "@/utils/websocket"
import chatService from '@/api/chat'

const store = useCircleChatStore()
const message = useMessage()

// 状态控制
const showMemberList = ref(false)
const showCreateModal = ref(false)
const creatingLoading = ref(false)

// 创建圈子表单
const createFormRef = ref<any>(null)
const createForm = ref({
  name: '',
  description: '',
  type: 1  // 1-公开，0-私密
})

const createRules = {
  name: [
    { required: true, message: '请输入圈子名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
}

/**
 * 加载会话列表
 */
const loadConversations = async () => {
  try {
    console.log('🔍 [圈子聊天] 开始加载会话列表...')
    store.loading = true
    const result = await circleChatApi.getConversations({ page: 1, limit: 20 })
    store.setConversations(result.list)
    console.log('✅ [圈子聊天] 会话列表加载完成，总未读数:', store.totalUnreadCount)

  } catch (error: any) {
    console.error('❌ [圈子聊天] 加载会话列表失败:', error)
    message.error(error.message || '加载失败')
  } finally {
    store.loading = false
  }
}

/**
 * 选择圈子
 */
const handleSelectCircle = async (conv: CircleConversation) => {
  console.log('🔄 [圈子聊天] 用户选择圈子:', conv.circleId)

  try {
    // 1. 获取圈子详情
    console.log('📋 [圈子聊天] 获取圈子详情，circleId:', conv.circleId)
    const circle = await circleApi.getCircleById(conv.circleId)

    // 2. 切换圈子
    console.log('🔀 [圈子聊天] 切换到圈子:', circle.name)
    await store.switchCircle(circle)

    // 3. 加载聊天记录
    console.log('💬 [圈子聊天] 加载聊天记录，circleId:', circle.id)
    await loadChatHistory(circle.id)

    // 4. 加载成员列表
    console.log('👥 [圈子聊天] 加载成员列表，circleId:', circle.id)
    await loadMembers(circle.id)

    // 5. 不再在这里单独订阅，因为 WebSocketManager 已经全局订阅了

  } catch (error: any) {
    console.error('❌ [圈子聊天] 切换圈子失败:', error)
    message.error(error.message || '切换失败')
  }
}

/**
 * 加载聊天记录
 */
const loadChatHistory = async (circleId: number) => {
  try {
    console.log('📥 [圈子聊天] 准备加载聊天记录:', circleId)
    const result = await circleChatApi.getChatHistory(circleId, { page: 1, limit: 20 })
    store.setMessages(result.list.reverse(), true)  // reverse 让最新的在下面

    // 处理撤回消息（转换为系统提示）
    store.processRecalledMessages(store.messages)

    // 处理删除消息（转换为系统提示）
    store.processDeletedMessages(store.messages)

    console.log('✅ [圈子聊天] 聊天记录加载完成，消息数:', result.list.length)

  } catch (error: any) {
    console.error('❌ [圈子聊天] 加载聊天记录失败:', error)
  }
}

/**
 * 加载成员列表
 */
const loadMembers = async (circleId: number) => {
  try {
    console.log('📡 [圈子聊天] 开始加载成员列表:', circleId)
    const result = await circleMemberApi.getMembers(circleId, { page: 1, limit: 100 })
    console.log('📊 [圈子聊天] 成员列表API响应:', result)
    store.setMembers(result.list)
    console.log('✅ [圈子聊天] 成员列表加载完成，成员数:', result.list.length)

  } catch (error: any) {
    console.error('❌ [圈子聊天] 加载成员列表失败:', error)
  }
}

/**
 * 邀请成员
 */
const handleInviteMember = () => {
  if (!store.currentCircle) return
  
  console.log('📨 [圈子聊天] 用户尝试邀请成员到圈子:', store.currentCircle.name)
  // TODO: 打开邀请对话框
  message.info('邀请功能开发中')
}

/**
 * 点击成员
 */
const handleMemberClick = (member: CircleMember) => {
  console.log('👤 [圈子聊天] 用户点击成员:', member.userId, '角色:', member.role)
  // TODO: 显示成员信息或发起私聊
}

/**
 * 创建圈子
 */
const handleCreateCircle = async () => {
  try {
    await createFormRef.value?.validate()
    
    creatingLoading.value = true
    console.log('🆕 [圈子聊天] 创建新圈子:', createForm.value.name)
    await circleApi.createCircle(createForm.value)
    
    message.success('创建成功')
    showCreateModal.value = false
    
    // 刷新列表
    console.log('🔁 [圈子聊天] 创建成功，刷新会话列表')
    await loadConversations()
    
  } catch (error: any) {
    if (error.errors) {
      // 表单验证失败
      return
    }
    console.error('❌ [圈子聊天] 创建圈子失败:', error)
    message.error(error.message || '创建失败')
  } finally {
    creatingLoading.value = false
  }
}

// 生命周期
onMounted(async () => {
  console.log('🎯 [圈子聊天] 页面挂载，开始初始化...')

  // 不再在这里连接 WebSocket，因为登录后已经全局连接了
  // 只需要检查连接状态即可
  const ws = getWebSocket()
  if (!ws) {
    console.warn('⚠️ [圈子聊天] WebSocket 实例不存在')
  }

  await loadConversations()
  console.log('🎉 [圈子聊天] 初始化成功')
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
</style>
