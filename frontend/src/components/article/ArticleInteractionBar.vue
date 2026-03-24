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
import {computed, defineComponent, h, onMounted, ref} from 'vue'
import {Icon} from '@iconify/vue'
import { getCurrentInstance } from 'vue'
import {interactionAPI} from '@/api/interaction'
import {useChatStore} from '@/stores/chat'
import {useCircleChatStore} from '@/stores/circleChat'
import {storeToRefs} from 'pinia'
import {NAvatar, NButton, NModal, NSelect, NSpace, NTabPane, NTabs, useMessage} from 'naive-ui'
import {getWebSocket} from '@/utils/websocket'
import {circleApi, circleChatApi} from '@/api/circle'
import type {Circle} from '@/types/circleChat'
import messageAPI from '@/api/message'
import {ConversationVO} from "@/types/message";

// 定义全局 Window 接口扩展
declare global {
  interface Window {
    detailArticleData?: {
      title: string
      coverUrl?: string
      authorNickname?: string
      publishTime?: string | null
      id: number | string
    }
  }
}

interface Props {
  articleId: number | string
  likeCount?: number
  favoriteCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  likeCount: 0,
  favoriteCount: 0
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

const isLiked = ref<boolean>(false)
const isDisliked = ref<boolean>(false)
const isFavorited = ref<boolean>(false)
const message = useMessage()

// 检查互动状态
const checkInteractions = async () => {
  try {
    const likeResponse = await interactionAPI.check(props.articleId, 2)
    isLiked.value = likeResponse.data.data?.hasAction || false

    const dislikeResponse = await interactionAPI.check(props.articleId, 3)
    isDisliked.value = dislikeResponse.data.data?.hasAction || false

    const favoriteResponse = await interactionAPI.check(props.articleId, 1)
    isFavorited.value = favoriteResponse.data.data?.hasAction || false

    emit('update', { isLiked: isLiked.value, isDisliked: isDisliked.value, isFavorited: isFavorited.value })
  } catch (error) {
    // console.error('检查互动状态失败:', error)
  }
}

// 点赞
const handleLike = async () => {
  try {
    if (isLiked.value) {
      const params = {
        contentId: props.articleId,
        actionType: 2 as const,
        tableName: 'article'
      }
      await interactionAPI.cancelLike(params)
      isLiked.value = false
      emit('update', {
        isLiked: false,
        isDisliked: isDisliked.value,
        isFavorited: isFavorited.value,
        likeCount: (props.likeCount || 0) - 1
      })
      message.success('已取消点赞')
    } else {
      const params = {
        contentId: props.articleId,
        actionType: 2 as const,
        tableName: 'article'
      }
      await interactionAPI.like(params)
      isLiked.value = true
      const newLikeCount = (props.likeCount || 0) + 1
      let newDislikeCount = props.dislikeCount || 0
      if (isDisliked.value) {
        isDisliked.value = false
        newDislikeCount = (props.dislikeCount || 0) - 1
        const cancelParams = {
          contentId: props.articleId,
          actionType: 3 as const,
          tableName: 'article'
        }
        await interactionAPI.cancelLike(cancelParams)
      }
      message.success('点赞成功')
      emit('update', {
        isLiked: true,
        isDisliked: false,
        isFavorited: isFavorited.value,
        likeCount: newLikeCount,
        dislikeCount: newDislikeCount
      })
    }
  } catch (error: any) {
    if (error.isAxiosError) {
      if (error.response?.status === 400) {
        message.warning(error.response?.data?.msg || '您已经点过赞了')
      } else if (error.response?.status === 500) {
        message.error('服务器错误：' + (error.response?.data?.msg || ''))
      } else {
        message.error(error.response?.data?.msg || '操作失败')
      }
    } else {
      message.error(error.message || '操作失败')
    }
  }
}

// 点踩
const handleDislike = async () => {
  try {
    if (isDisliked.value) {
      const params = {
        contentId: props.articleId,
        actionType: 3 as const,
        tableName: 'article'
      }
      await interactionAPI.cancelLike(params)
      isDisliked.value = false
      emit('update', {
        isLiked: isLiked.value,
        isDisliked: false,
        isFavorited: isFavorited.value,
        dislikeCount: (props.dislikeCount || 0) - 1
      })
      message.success('已取消点踩')
    } else {
      const params = {
        contentId: props.articleId,
        actionType: 3 as const,
        tableName: 'article'
      }
      await interactionAPI.like(params)
      isDisliked.value = true
      const newDislikeCount = (props.dislikeCount || 0) + 1
      let newLikeCount = props.likeCount || 0
      if (isLiked.value) {
        isLiked.value = false
        newLikeCount = (props.likeCount || 0) - 1
        const cancelParams = {
          contentId: props.articleId,
          actionType: 2 as const,
          tableName: 'article'
        }
        await interactionAPI.cancelLike(cancelParams)
      }
      message.success('点踩成功')
      emit('update', {
        isLiked: false,
        isDisliked: true,
        isFavorited: isFavorited.value,
        likeCount: newLikeCount,
        dislikeCount: newDislikeCount
      })
    }
  } catch (error: any) {
    if (error.isAxiosError) {
      if (error.response?.status === 400) {
        message.warning(error.response?.data?.msg || '您已经点过踩了')
      } else if (error.response?.status === 500) {
        message.error('服务器错误：' + (error.response?.data?.msg || ''))
      } else {
        message.error(error.response?.data?.msg || '操作失败')
      }
    } else {
      message.error(error.message || '操作失败')
    }
  }
}

// 收藏
const handleFavorite = async () => {
  try {
    if (isFavorited.value) {
      await interactionAPI.cancel({
        contentId: props.articleId,
        actionType: 1,
        tableName: 'article'
      })
      isFavorited.value = false
      message.success('已取消收藏')
      emit('update', {
        isLiked: isLiked.value,
        isDisliked: isDisliked.value,
        isFavorited: false,
        favoriteCount: (props.favoriteCount || 0) - 1
      })
    } else {
      await interactionAPI.create({
        contentId: props.articleId,
        actionType: 1,
        tableName: 'article',
        remark: '用户手动收藏'
      })
      isFavorited.value = true
      message.success('收藏成功')
      emit('update', {
        isLiked: isLiked.value,
        isDisliked: isDisliked.value,
        isFavorited: true,
        favoriteCount: (props.favoriteCount || 0) + 1
      })
    }
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
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

// 新增：获取用户加入的圈子列表
const loadCircles = async () => {
  try {
    const result = await circleApi.getMyCircles({ page: 1, limit: 50 })
    circles.value = result.list || []

  } catch (error) {
    console.error('❌ [ArticleInteractionBar] 加载圈子列表失败:', error)
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

    // 关键修复：result 就是 ConversationVO[] 数组，不需要 .list
    chatStore.conversations = result as ConversationVO[] || []
  } catch (error) {
    console.error('❌ [ArticleInteractionBar] 加载会话列表失败:', error)
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
  // 关键修复：优先从 window 获取，如果没有则尝试从父组件获取
  const articleData = window.detailArticleData

  if (!articleData) {
    console.error('❌ [ArticleInteractionBar] 文章信息加载失败，window.detailArticleData 未定义')
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
      // 关键修复：构建完整的消息对象，不要直接传 content
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
      // 关键修复：构建完整的消息对象
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

// 举报
const handleReport = () => {
  message.info('举报功能开发中...')
}

// 初始化时检查状态
checkInteractions()
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
