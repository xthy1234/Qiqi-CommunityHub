<!-- src/components/ArticleInteractionBar.vue -->
<template>
  <div class="article-interaction-bar">
    <div 
      class="interaction-item" 
      :class="{ active: isLiked }" 
      @click="handleLike"
    >
      <Icon
        :icon="isLiked ? 'ri:thumb-up-fill' : 'ri:thumb-up-line'"
        width="18"
      />
      <span>{{ isLiked ? '已赞' : '点赞' }} ({{ likeCount || 0 }})</span>
    </div>

    <div 
      class="interaction-item" 
      :class="{ active: isDisliked }" 
      @click="handleDislike"
    >
      <Icon
        :icon="isDisliked ? 'ri:thumb-down-fill' : 'ri:thumb-down-line'"
        width="18"
      />
      <span>{{ isDisliked ? '已踩' : '点踩' }}</span>
    </div>

    <div 
      class="interaction-item" 
      :class="{ active: isFavorited }" 
      @click="handleFavorite"
    >
      <Icon
        :icon="isFavorited ? 'ri:star-fill' : 'ri:star-line'"
        width="18"
      />
      <span>{{ isFavorited ? '已收藏' : '收藏' }} ({{ favoriteCount || 0 }})</span>
    </div>

    <!-- 分享按钮 -->
    <div
      class="interaction-item"
      @click="showShareModal = true"
    >
      <Icon
        icon="ri:share-line"
        width="18"
      />
      <span>分享</span>
    </div>

    <div
      class="interaction-item"
      @click="handleReport"
    >
      <Icon
        icon="ri:alert-line"
        width="18"
      />
      <span>举报</span>
    </div>

    <!-- 举报模态框 -->
    <n-modal
      v-model:show="showReportModal"
      title="举报文章"
      preset="card"
      style="width: 500px;"
    >
      <n-form
        ref="reportFormRef"
        :model="reportForm"
        :rules="reportRules"
        label-placement="top"
      >
        <n-form-item
          label="举报原因"
          path="reportReason"
        >
          <n-input
            v-model:value="reportForm.reportReason"
            type="textarea"
            placeholder="请详细描述举报原因（如：虚假信息、违规内容、侵权等）"
            :rows="4"
            maxlength="500"
            show-count
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showReportModal = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="reporting"
            @click="submitReport"
          >
            提交举报
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分享到聊天模态框 -->
    <n-modal
      v-model:show="showShareModal"
      title="分享到聊天"
      preset="card"
      style="width: 500px;"
    >
      <p>请选择要分享给的好友或圈子：</p>

      <!-- 标签切换 -->
      <n-tabs
        v-model:value="shareTab"
        type="line"
        animated
      >
        <n-tab-pane
          name="private"
          tab="私信"
        >
          <div
            v-if="loadingConversations"
            style="padding: 20px; text-align: center;"
          >
            <n-spin size="small" />
            <span style="margin-left: 8px; color: #999;">加载中...</span>
          </div>
          <div
            v-else-if="conversations.length === 0"
            style="padding: 20px; text-align: center; color: #999;"
          >
            暂无好友会话
          </div>
          <n-select
            v-else
            v-model:value="selectedUserId"
            :options="friendOptions"
            placeholder="选择要分享的好友..."
            filterable
            style="margin-top: 12px;"
          />
        </n-tab-pane>
        <n-tab-pane
          name="circle"
          tab="圈子"
        >
          <div
            v-if="circles.length === 0"
            style="padding: 20px; text-align: center; color: #999;"
          >
            暂无圈子
          </div>
          <n-select
            v-else
            v-model:value="selectedCircleId"
            :options="circleOptions"
            placeholder="选择要分享的圈子..."
            filterable
            style="margin-top: 12px;"
          />
        </n-tab-pane>
      </n-tabs>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showShareModal = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :disabled="!selectedUserId && !selectedCircleId"
            @click="sendArticleToChat"
          >
            发送
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { articleAPI } from '@/api/article' // 修改导入
import {computed, defineComponent, h, onMounted, ref} from 'vue'
import {Icon} from '@iconify/vue'
import { getCurrentInstance } from 'vue'
import {useChatStore} from '@/stores/chat'
import {useCircleChatStore} from '@/stores/circleChat'
import {storeToRefs} from 'pinia'
import {
  NAvatar,
  NButton,
  NModal,
  NSelect,
  NSpace,
  NTabPane,
  NTabs,
  useMessage,
  NInput,
  NForm,
  NFormItem,
  FormInst
} from 'naive-ui'
import {getWebSocket} from '@/utils/websocket'
import {circleApi, circleChatApi} from '@/api/circle'
import type {Circle} from '@/types/circleChat'
import messageAPI from '@/api/message'
import {ConversationVO} from "@/types/message";
import {reportAPI, type ReportCreateDTO} from '@/api/report'
interface Props {
  articleId: number | string
  likeCount?: number
  favoriteCount?: number
  isLiked?: boolean
  isDisliked?: boolean
  isFavorited?: boolean
}
// 定义全局 Window 接口扩展
declare global {
  interface Window {
    detailArticleData?: {
      title: string
      coverUrl?: string
      authorNickname?: string
      publishTime?: string | null
      id: number | string
      version: string
    }
  }
}
const props = withDefaults(defineProps<Props>(), {
  likeCount: 0,
  favoriteCount: 0,
  isLiked: false,
  isDisliked: false,
  isFavorited: false
})

const emit = defineEmits<{
  update: [value: {
    isLiked: boolean
    isDisliked: boolean
    isFavorited: boolean
    likeCount?: number
    dislikeCount?: number
    favoriteCount?: number
  }]
}>()

const isLiked = ref<boolean>(props.isLiked)
const isDisliked = ref<boolean>(props.isDisliked)
const isFavorited = ref<boolean>(props.isFavorited)
const likeCount = ref<number>(props.likeCount || 0)
const dislikeCount = ref<number>(0)
const favoriteCount = ref<number>(props.favoriteCount || 0)
const message = useMessage()

const handleLike = async () => {
  if (!props.articleId) return
  try {
    if (isLiked.value) {
      await articleAPI.cancelLike(props.articleId) // 修改调用
      isLiked.value = false
      likeCount.value--
    } else {
      await articleAPI.like(props.articleId) // 修改调用
      isLiked.value = true
      likeCount.value++
      if (isDisliked.value) {
        isDisliked.value = false
        dislikeCount.value--
      }
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const handleDislike = async () => {
  if (!props.articleId) return
  try {
    if (isDisliked.value) {
      await articleAPI.cancelDislike(props.articleId) // 修改调用
      isDisliked.value = false
      dislikeCount.value--
    } else {
      await articleAPI.dislike(props.articleId) // 修改调用
      isDisliked.value = true
      dislikeCount.value++
      if (isLiked.value) {
        isLiked.value = false
        likeCount.value--
      }
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const handleFavorite = async () => {
  if (!props.articleId) return
  try {
    if (isFavorited.value) {
      await articleAPI.cancelFavorite(props.articleId) // 修改调用
      isFavorited.value = false
      favoriteCount.value--
      message.success('已取消收藏')
    } else {
      await articleAPI.favorite(props.articleId) // 修改调用
      isFavorited.value = true
      favoriteCount.value++
      message.success('收藏成功')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 使用聊天 Store
const chatStore = useChatStore()
const circleChatStore = useCircleChatStore()
const { conversations } = storeToRefs(chatStore)

// 新增：圈子列表状态
const circles = ref<Circle[]>([])
const shareTab = ref<'private' | 'circle'>('private')
const selectedUserId = ref<number | null>(null)
const selectedCircleId = ref<number | null>(null)
const showShareModal = ref(false)
const loadingConversations = ref(false)

// 举报相关状态
const showReportModal = ref(false)
const reporting = ref(false)
const reportFormRef = ref<FormInst | null>(null)

// 简化举报表单，只保留必要字段
const reportForm = ref<Partial<ReportCreateDTO>>({
  reportReason: ''
})

// 举报表单验证规则
const reportRules = {
  reportReason: {
    required: true,
    message: '请填写举报原因',
    trigger: ['blur', 'change']
  }
}

// 新增：获取用户加入的圈子列表
const loadCircles = async () => {
  try {
    const result = await circleApi.getMyCircles({ page: 1, limit: 50 })
    circles.value = result.list || []

  } catch (error) {
    console.error('[ArticleInteractionBar] 加载圈子列表失败:', error)
    circles.value = []
  }
}

// 新增：加载会话列表
const loadConversations = async () => {
  if (conversations.value.length > 0) {

    return
  }

  loadingConversations.value = true
  try {
    // 主动调用 API 获取会话列表
    const result = await messageAPI.getConversations()


    chatStore.conversations = result as ConversationVO[] || []
  } catch (error) {
    console.error('[ArticleInteractionBar] 加载会话列表失败:', error)
  } finally {
    loadingConversations.value = false
  }
}

// 在组件挂载时加载圈子列表和会话列表
onMounted(() => {
  loadCircles()
  loadConversations()
})

// 创建自定义选项组件
const FriendOption = defineComponent({
  props: {
    avatar: String,
    username: String
  },
  setup(props: { avatar?: string | null; username?: string | null }) {
    return () =>
      h('div', {
        style: 'display: flex; align-items: center; gap: 8px; width: 100%;'
      }, [
        h(NAvatar, {
          src: props.avatar || '',
          size: 'small',
          round: true,
          style: 'flex-shrink: 0;'
        }),
        h('div', {
          style: 'display: flex; flex-direction: column; overflow: hidden;'
        }, [
          h('span', {
            style: 'font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'
          }, props.username || '未知用户')
        ])
      ])
  }
})

const CircleOption = defineComponent({
  props: {
    avatar: String,
    name: String,
    memberCount: Number
  },
  setup(props: { avatar?: string | null; name?: string | null; memberCount?: number | null }) {
    return () =>
      h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h(NAvatar, {
          src: props.avatar || '',
          size: 'small',
          round: true,
          style: 'flex-shrink: 0;'
        }),
        h('div', { style: 'display: flex; flex-direction: column;' }, [
          h('span', { style: 'font-weight: 500;' }, props.name || '未知圈子'),
          h('span', { style: 'font-size: 12px; color: #999;' }, `${props.memberCount || 0} 人`)
        ])
      ])
  }
})

// 构建好友选项列表（带头像）
const friendOptions = computed(() => {

  return conversations.value.map((conv: ConversationVO) => ({
    label: (option: any) => h(FriendOption, {
      avatar: conv.avatar,
      username: conv.username
    }),
    value: conv.userId
  }))
})

// 构建圈子选项列表（带头像）
const circleOptions = computed(() => {
  return circles.value.map(circle => ({
    label: (option: any) => h(CircleOption, {
      avatar: circle.avatar,
      name: circle.name,
      memberCount: circle.memberCount
    }),
    value: circle.id
  }))
})

// 将文章分享到聊天
const sendArticleToChat = () => {

  const articleData = window.detailArticleData

  if (!articleData) {
    console.error('[ArticleInteractionBar] 文章信息加载失败，window.detailArticleData 未定义')
    message.error('文章信息加载失败，请刷新页面重试')
    return
  }

  const { title, coverUrl, authorNickname, publishTime, id } = articleData
  const articleUrl = `${window.location.origin}/#/index/articleDetail?id=${id}`

  // 构建TipTap JSON格式的消息内容（对象形式）
  const articleJsonContent = {
    type: 'doc',
    content: [
      {
        type: 'shareCardNode',
        attrs: {
          title: title,
          summary: `作者：${authorNickname || '匿名'}`,
          cover: coverUrl?.startsWith('http') ? coverUrl : `${window.location.origin}/${coverUrl}`,
          url: articleUrl,
          author: authorNickname || '匿名',
          publishTime: publishTime
        }
      }
    ]
  }

  // 判断是私信还是圈子分享
  if (shareTab.value === 'private' && selectedUserId.value) {
    // 私聊场景：传递对象而不是字符串
    const tempMsg = chatStore.addSendingMessage(articleJsonContent, selectedUserId.value)
    const ws = getWebSocket()
    if (ws && ws.isConnected()) {

      const chatMessage = articleJsonContent  // TipTap JSON 对象

      ws.sendPrivateMessage(tempMsg.toUserId, chatMessage)
      message.success('文章已分享给好友')
    } else {
      message.warning('网络连接异常，消息发送失败')
    }
  } else if (shareTab.value === 'circle' && selectedCircleId.value) {
    // 圈子场景
    const tempMsg = circleChatStore.addSendingMessage(articleJsonContent, selectedCircleId.value)
    const ws = getWebSocket()
    if (ws && ws.isConnected()) {

      circleChatApi.sendMessage(selectedCircleId.value, articleJsonContent, 0)
      message.success('文章已分享到圈子')
    } else {
      message.warning('网络连接异常，消息发送失败')
    }
  } else {
    message.warning('请选择要分享的好友或圈子')
    return
  }

  showShareModal.value = false
  selectedUserId.value = null
  selectedCircleId.value = null
}

// 打开举报弹窗
const handleReport = () => {
  // 重置表单
  reportForm.value = {
    reportReason: ''
  }

  showReportModal.value = true

  // 重置验证状态
  setTimeout(() => {
    reportFormRef.value?.restoreValidation()
  }, 0)
}

// 提交举报（简化版）
const submitReport = async () => {
  try {
    await (reportFormRef.value as any)?.validate()

    reporting.value = true

    // 获取文章数据
    const articleData = window.detailArticleData

    if (!articleData || !articleData.id) {
      message.error('文章信息不存在')
      return
    }

    // 构建简化的提交数据
    const submitData: ReportCreateDTO = {
      contentId: Number(articleData.id),
      reportReason: reportForm.value.reportReason!.trim(),
      reportType: 'ARTICLE'
    }

    await reportAPI.createReport(submitData)

    message.success('举报提交成功，管理员将尽快处理')
    showReportModal.value = false
    reportForm.value.reportReason = ''
  } catch (error: any) {
    console.error('[ArticleInteractionBar] 提交举报失败:', error)

    if (error?.errors) {
      // 验证错误
      message.error('请填写完整的举报信息')
    } else {
      // HTTP 错误
      const errorMsg = error.response?.data?.msg || error.message || '举报提交失败，请稍后重试'
      message.error(errorMsg)
    }
  } finally {
    reporting.value = false
  }
}

</script>

<style lang="scss" scoped>
.article-interaction-bar {
  display: flex;
  gap: 30px;
  padding: 20px 0;
  margin-top: 30px;
  border-top: 1px solid #eee;
  justify-content: center;

  .interaction-item {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 10px 20px;
    border-radius: 8px;
    transition: all 0.3s;
    color: #666;
    font-size: 14px;

    &:hover {
      background: #f5f7fa;
      color: #409EFF;
    }

    &.active {
      color: #67C23A;
      background: #f0f9eb;
    }
  }
}
</style>
