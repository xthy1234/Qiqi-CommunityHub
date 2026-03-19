<!-- src/components/ArticleInteractionBar.vue -->
<template>
  <div class="article-interaction-bar">
    <div 
      class="interaction-item" 
      :class="{ active: isLiked }" 
      @click="handleLike"
    >
      <Icon :icon="isLiked ? 'ri:thumb-up-fill' : 'ri:thumb-up-line'" width="18" />
      <span>{{ isLiked ? '已赞' : '点赞' }} ({{ likeCount || 0 }})</span>
    </div>

    <div 
      class="interaction-item" 
      :class="{ active: isDisliked }" 
      @click="handleDislike"
    >
      <Icon :icon="isDisliked ? 'ri:thumb-down-fill' : 'ri:thumb-down-line'" width="18" />
      <span>{{ isDisliked ? '已踩' : '点踩' }}</span>
    </div>

    <div 
      class="interaction-item" 
      :class="{ active: isFavorited }" 
      @click="handleFavorite"
    >
      <Icon :icon="isFavorited ? 'ri:star-fill' : 'ri:star-line'" width="18" />
      <span>{{ isFavorited ? '已收藏' : '收藏' }} ({{ favoriteCount || 0 }})</span>
    </div>

    <div class="interaction-item" @click="handleShare">
      <Icon icon="ri:share-line" width="18" />
      <span>分享</span>
    </div>

    <div class="interaction-item" @click="handleReport">
      <Icon icon="ri:alert-line" width="18" />
      <span>举报</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { interactionAPI } from '@/api/interaction'

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
const handleLike = async () => {  try {
    if (isLiked.value) {
      // 取消点赞

      const params = {
        contentId: props.articleId,
        actionType: 2 as 2,
        tableName: 'article'
      }


      await interactionAPI.cancelLike(params)
      isLiked.value = false
      // 本地减少点赞数
      emit('update', {
        isLiked: false,
        isDisliked: isDisliked.value,
        isFavorited: isFavorited.value,
        likeCount: (props.likeCount || 0) - 1
      })

      ElMessage.success('已取消点赞')
    } else {
      // 执行点赞

      const params = {
        contentId: props.articleId,
        actionType: 2 as 2,
        tableName: 'article'
      }


      await interactionAPI.like(params)

      // 本地增加点赞数
      isLiked.value = true

      let newLikeCount = (props.likeCount || 0) + 1
      let newDislikeCount = props.dislikeCount || 0

      // 如果之前点踩了，取消点踩并减少计数
      if (isDisliked.value) {
        isDisliked.value = false
        newDislikeCount = (props.dislikeCount || 0) - 1
        // 取消点踩的 API 调用
        const cancelParams = {
          contentId: props.articleId,
          actionType: 3 as 3,
          tableName: 'article'
        }
        await interactionAPI.cancelLike(cancelParams)
      }


      ElMessage.success('点赞成功')

      emit('update', {
        isLiked: true,
        isDisliked: false,
        isFavorited: isFavorited.value,
        likeCount: newLikeCount,
        dislikeCount: newDislikeCount
      })
    }
  } catch (error: any) {
    // console.error('=== 点赞失败 ===')
    // console.error('错误对象:', error)
    // console.error('错误名称:', error.constructor.name)

    if (error.isAxiosError) {
      // console.error('这是 Axios 错误')
      // console.error('错误响应:', error.response)
      // console.error('错误状态码:', error.response?.status)
      // console.error('错误数据:', error.response?.data)
      // console.error('错误消息:', error.message)

      if (error.response?.status === 400) {
        ElMessage.warning(error.response?.data?.msg || '您已经点过赞了')
      } else if (error.response?.status === 500) {
        ElMessage.error('服务器错误：' + (error.response?.data?.msg || ''))
      } else {
        ElMessage.error(error.response?.data?.msg || '操作失败')
      }
    } else {
      // 普通错误
      // console.error('普通错误:', error.message)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 点踩
const handleDislike = async () => {  try {
    if (isDisliked.value) {
      // 取消点踩

      const params = {
        contentId: props.articleId,
        actionType: 3 as 3,
        tableName: 'article'
      }


      await interactionAPI.cancelLike(params)
      isDisliked.value = false
      // 本地减少点踩数
      emit('update', {
        isLiked: isLiked.value,
        isDisliked: false,
        isFavorited: isFavorited.value,
        dislikeCount: (props.dislikeCount || 0) - 1
      })

      ElMessage.success('已取消点踩')
    } else {
      // 执行点踩

      const params = {
        contentId: props.articleId,
        actionType: 3 as 3,
        tableName: 'article'
      }


      await interactionAPI.like(params)

      // 本地增加点踩数
      isDisliked.value = true

      let newDislikeCount = (props.dislikeCount || 0) + 1
      let newLikeCount = props.likeCount || 0

      // 如果之前点赞了，取消点赞并减少计数
      if (isLiked.value) {
        isLiked.value = false
        newLikeCount = (props.likeCount || 0) - 1
        // 取消点赞的 API 调用
        const cancelParams = {
          contentId: props.articleId,
          actionType: 2 as 2,
          tableName: 'article'
        }
        await interactionAPI.cancelLike(cancelParams)
      }


      ElMessage.success('点踩成功')

      emit('update', {
        isLiked: false,
        isDisliked: true,
        isFavorited: isFavorited.value,
        likeCount: newLikeCount,
        dislikeCount: newDislikeCount
      })
    }
  } catch (error: any) {
    // console.error('=== 点踩失败 ===')
    // console.error('错误对象:', error)
    // console.error('错误名称:', error.constructor.name)

    if (error.isAxiosError) {
      // console.error('这是 Axios 错误')
      // console.error('错误响应:', error.response)
      // console.error('错误状态码:', error.response?.status)
      // console.error('错误数据:', error.response?.data)
      // console.error('错误消息:', error.message)

      if (error.response?.status === 400) {
        ElMessage.warning(error.response?.data?.msg || '您已经点过踩了')
      } else if (error.response?.status === 500) {
        ElMessage.error('服务器错误：' + (error.response?.data?.msg || ''))
      } else {
        ElMessage.error(error.response?.data?.msg || '操作失败')
      }
    } else {
      // 普通错误
      // console.error('普通错误:', error.message)
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 收藏
const handleFavorite = async () => {
  try {
    if (isFavorited.value) {
      // 取消收藏
      await interactionAPI.cancel({
        contentId: props.articleId,
        actionType: 1,
        tableName: 'article'
      })
      isFavorited.value = false
      ElMessage.success('已取消收藏')

      emit('update', {
        isLiked: isLiked.value,
        isDisliked: isDisliked.value,
        isFavorited: false,
        favoriteCount: (props.favoriteCount || 0) - 1
      })
    } else {
      // 执行收藏
      await interactionAPI.create({
        contentId: props.articleId,
        actionType: 1,
        tableName: 'article',
        remark: '用户手动收藏'
      })
      isFavorited.value = true
      ElMessage.success('收藏成功')

      emit('update', {
        isLiked: isLiked.value,
        isDisliked: isDisliked.value,
        isFavorited: true,
        favoriteCount: (props.favoriteCount || 0) + 1
      })
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

// 分享
const handleShare = async () => {
  const url = window.location.href
  
  if (navigator.share) {
    try {
      await navigator.share({
        title: document.title,
        text: `推荐文章：${document.title}`,
        url: url
      })
      
      await interactionAPI.create({
        contentId: props.articleId,
        actionType: 4,
        tableName: 'article',
        remark: '用户通过系统分享'
      })
      
      ElMessage.success('分享成功')
    } catch (error) {

    }
  } else {
    try {
      await navigator.clipboard.writeText(url)
      
      await interactionAPI.create({
        contentId: props.articleId,
        actionType: 4,
        tableName: 'article',
        remark: '用户复制链接分享'
      })
      
      ElMessage.success('链接已复制，可以分享给好友了')
    } catch (error) {
      ElMessage.warning('复制失败，请手动复制链接')
    }
  }
}

// 举报
const handleReport = () => {
  ElMessage.info('举报功能开发中...')
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
