<template>
  <n-card
    class="article-card-component"
    hoverable
    :title="article.title"
    :class="{ 'featured-article': isFeatured }"
    @click="handleClick"
  >
    <template #cover>
      <div class="cover-wrapper">
        <img
          v-if="article.coverUrl"
          :src="getCoverImageUrl(article.coverUrl)"
          alt="封面"
          class="article-cover-img"
          @error="handleImageError"
        />
        <div
          v-else
          class="cover-placeholder"
        >
          <Icon
            icon="ri:image-line"
            width="40"
          />
        </div>

        <!-- 置顶标识 -->
        <div
          v-if="isFeatured"
          class="featured-badge"
        >
          <n-tag
            :type="featuredConfig.type"
            size="small"
            round
          >
            <Icon
              :icon="featuredConfig.icon"
              style="margin-right: 4px;"
            />
            {{ featuredConfig.label }}
          </n-tag>
        </div>
      </div>
    </template>
    
    <template #header-extra>
      <div class="header-actions">
        <n-tag
          v-if="article.categoryName"
          size="small"
          type="info"
          round
        >
          {{ article.categoryName }}
        </n-tag>
        <n-dropdown
          :options="dropdownOptions"
          trigger="click"
          placement="bottom-end"
          @select="handleBlockAction"
        >
          <n-button
            text
            size="tiny"
            class="more-btn"
            @click.stop
          >
            <Icon
              icon="ri:more-fill"
              width="16"
            />
          </n-button>
        </n-dropdown>
      </div>
    </template>
    
    <div class="article-meta">
      <UserAvatarLink
        :user-id="article.authorId || ''"
        :nickname="article.authorNickname"
        :avatar="article.authorAvatar"
        :size="32"
        :show-name="true"
      />
      <div
        v-if="article.publishTime"
        class="publish-time"
      >
        <Icon
          icon="ri:calendar-line"
          width="14"
        />
        {{ formatDate(article.publishTime) }}
      </div>
    </div>
    
    <div class="article-stats">
      <div class="stat-item">
        <Icon
          icon="ri:eye-line"
          width="16"
        />
        <span>{{ formatNumber(article.viewCount || 0) }}</span>
      </div>
      <div class="stat-item">
        <Icon
          icon="ri:thumb-up-line"
          width="16"
        />
        <span>{{ formatNumber(article.likeCount || 0) }}</span>
      </div>
      <div class="stat-item">
        <Icon
          icon="ri:chat-1-line"
          width="16"
        />
        <span>{{ formatNumber(article.commentCount || 0) }}</span>
      </div>
    </div>
  </n-card>

  <!-- 关键词输入弹窗 -->
  <n-modal
    v-model:show="showKeywordModal"
    preset="card"
    title="屏蔽关键词"
    style="width: 400px"
  >
    <n-form
      ref="keywordFormRef"
      :model="keywordForm"
      :rules="keywordFormRules"
      label-placement="left"
      label-width="80"
    >
      <n-form-item
        label="关键词"
        path="keyword"
      >
        <n-input
          v-model:value="keywordForm.keyword"
          placeholder="请输入要屏蔽的关键词"
          maxlength="50"
          show-count
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <n-space justify="end">
        <n-button @click="showKeywordModal = false">
          取消
        </n-button>
        <n-button
          type="primary"
          :loading="submitting"
          @click="handleKeywordSubmit"
        >
          确定
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NTag, NDropdown, NButton, NModal, NForm, NFormItem, NInput, NSpace, useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handleImageError } from '@/utils/userUtils'
import {useGlobalProperties} from "@/utils/globalProperties";
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import { normalizeFileUrl } from '@/utils/fileUrl'
import { blockRuleAPI } from '@/api/blockRule'
import { isArticleFeatured, getFeaturedLevelConfig, type FeaturedLevel } from '@/utils/featuredUtils'

interface ArticleProps {
  id: number | string
  title: string
  coverUrl?: string | null
  categoryId?: number | string
  categoryName?: string
  authorId?: number | string
  authorNickname?: string
  authorAvatar?: string | null
  likeCount?: number
  dislikeCount?: number
  viewCount?: number
  commentCount?: number
  publishTime?: string
  auditStatus?: string
  createTime?: string
  isFeatured?: boolean
  featuredLevel?: FeaturedLevel
  [key: string]: any
}

const props = defineProps<{
  article: ArticleProps
}>()

const router = useRouter()
const appContext = useGlobalProperties()
const message = useMessage()
const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')

const showKeywordModal = ref(false)
const submitting = ref(false)
const keywordFormRef = ref()
const keywordForm = ref({
  keyword: ''
})

const keywordFormRules = {
  keyword: {
    required: true,
    message: '请输入关键词',
    trigger: 'blur'
  }
}

const dropdownOptions = computed(() => [
  {
    label: '屏蔽该作者',
    key: 'block-author',
    icon: () => h(Icon, { icon: 'ri:user-unfollow-line', size: 16 })
  },
  {
    label: '屏蔽此分类',
    key: 'block-category',
    icon: () => h(Icon, { icon: 'ri:folder-off-line', size: 16 })
  },
  {
    label: '屏蔽关键词',
    key: 'block-keyword',
    icon: () => h(Icon, { icon: 'ri:chat-off-line', size: 16 })
  }
])

const isFeatured = computed(() => isArticleFeatured(props.article))

const featuredConfig = computed(() => {
  const level = props.article.featuredLevel ?? 0
  return getFeaturedLevelConfig(level as FeaturedLevel)
})

const getCoverImageUrl = (coverUrl: string): string => {
  if (!coverUrl || coverUrl === 'null') {return '/placeholder.svg'}
  return normalizeFileUrl(coverUrl, baseUrl.value)
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) {return ''}
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return String(num)
}

const handleClick = () => {
  router.push(`/index/articleDetail?id=${props.article.id}`)
}

const handleBlockAction = async (key: string) => {
  try {
    switch (key) {
      case 'block-author':
        await blockAuthor()
        break
      case 'block-category':
        await blockCategory()
        break
      case 'block-keyword':
        showKeywordModal.value = true
        keywordForm.value.keyword = props.article.title.substring(0, 10)
        break
    }
  } catch (error) {
    console.error('屏蔽操作失败:', error)
  }
}

const blockAuthor = async () => {
  if (!props.article.authorId) {
    message.warning('无法获取作者信息')
    return
  }

  try {
    const response = await blockRuleAPI.addRule({
      ruleType: 'author',
      ruleValue: String(props.article.authorId)
    })

    const msg = response.data?.msg
    if (msg) {
      message.success(msg)
    } else {
      message.success('已屏蔽该作者')
    }
  } catch (error: any) {
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('屏蔽失败')
    }
  }
}

const blockCategory = async () => {
  if (!props.article.categoryId) {
    message.warning('无法获取分类信息')
    return
  }

  try {
    const response = await blockRuleAPI.addRule({
      ruleType: 'category',
      ruleValue: String(props.article.categoryId)
    })

    const msg = response.data?.msg
    if (msg) {
      message.success(msg)
    } else {
      message.success('已屏蔽该分类')
    }
  } catch (error: any) {
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('屏蔽失败')
    }
  }
}

const handleKeywordSubmit = async () => {
  try {
    await keywordFormRef.value?.validate()
    submitting.value = true

    const response = await blockRuleAPI.addRule({
      ruleType: 'keyword',
      ruleValue: keywordForm.value.keyword.trim()
    })

    const msg = response.data?.msg
    if (msg) {
      message.success(msg)
    } else {
      message.success('已添加关键词屏蔽')
    }

    showKeywordModal.value = false
    keywordForm.value.keyword = ''
  } catch (error: any) {
    if (error.response?.data?.msg) {
      message.error(error.response.data.msg)
    } else {
      message.error('添加失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.article-card-component {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s;

  &.featured-article {
    border: 2px solid #18a058;
    box-shadow: 0 2px 12px rgba(24, 160, 88, 0.15);
  }

  .cover-wrapper {
    position: relative;

    .article-cover-img {
      width: 100%;
      height: 200px;
      object-fit: cover;
    }

    .cover-placeholder {
      width: 100%;
      height: 200px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
    }

    .featured-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      z-index: 10;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;

    .more-btn {
      opacity: 0;
      transition: opacity 0.3s;

      &:hover {
        background-color: rgba(0, 0, 0, 0.05);
      }
    }
  }

  &:hover {
    .header-actions {
      .more-btn {
        opacity: 1;
      }
    }
  }

  .article-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .publish-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #999;
  }

  .article-stats {
    display: flex;
    justify-content: space-around;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #666;

      &:hover {
        color: #18a058;
      }
    }
  }

  &:hover {
    transform: translateY(-5px);

    :deep(.n-card__cover img) {
      transform: scale(1.05);
    }
  }

  :deep(.n-card-header__main) {
    font-size: 14px;
    font-weight: 500;

    .n-card-header__title {
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
  }
}
</style>
