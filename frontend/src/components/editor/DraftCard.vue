<template>
  <n-card
    class="draft-card-component"
    hoverable
    @click="handleClick"
  >
    <div class="draft-card-content">
      <!-- 选择框 -->
      <div
        class="select-checkbox"
        @click.stop
      >
        <n-checkbox
          :checked="isSelected"
          @update:checked="handleSelectChange"
        />
      </div>

      <!-- 封面图 -->
      <div
        class="cover-wrapper"
        @click.stop="handleClick"
      >
        <img
          v-if="draft.coverUrl"
          :src="getCoverImageUrl(draft.coverUrl)"
          alt="封面"
          class="cover-image"
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
      </div>

      <!-- 内容区域 -->
      <div class="content-wrapper">
        <!-- 标题 -->
        <div
          class="draft-title"
          @click.stop="handleClick"
        >
          {{ draft.title }}
        </div>

        <!-- 分类和创建时间 -->
        <div class="draft-meta">
          <span class="meta-item">
            <Icon
              icon="material-symbols:category"
              width="14"
            />
            {{ draft.categoryName || '未分类' }}
          </span>
          <span class="meta-item">
            <Icon
              icon="material-symbols:access-time"
              width="14"
            />
            {{ formatDate(draft.createTime) }}
          </span>
        </div>

        <!-- 操作按钮 -->
        <div
          class="draft-actions"
          @click.stop
        >
          <n-button
            type="primary"
            size="small"
            @click="handleEdit"
          >
            <template #icon>
              <Icon
                icon="ri:edit-line"
                width="16"
              />
            </template>
            编辑
          </n-button>
          
          <n-button
            type="success"
            size="small"
            @click="handleSubmit"
          >
            <template #icon>
              <Icon
                icon="ri:send-plane-line"
                width="16"
              />
            </template>
            提交审核
          </n-button>
          
          <n-button
            type="error"
            size="small"
            @click="handleDelete"
          >
            <template #icon>
              <Icon
                icon="ri:delete-bin-line"
                width="16"
              />
            </template>
            删除
          </n-button>
        </div>
      </div>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NCheckbox, NButton } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handleImageError } from '@/utils/userUtils'
import { useGlobalProperties } from '@/utils/globalProperties'

interface DraftProps {
  id: number | string
  title: string
  coverUrl?: string
  categoryId?: number | string
  categoryName?: string
  createTime?: string
  isSelected?: boolean
  [key: string]: any
}

const props = defineProps<{
  draft: DraftProps
}>()

const emit = defineEmits<{
  (e: 'select', draft: DraftProps): void
  (e: 'edit', draft: DraftProps): void
  (e: 'submit', draft: DraftProps): void
  (e: 'delete', draft: DraftProps): void
  (e: 'click', draft: DraftProps): void
}>()

const router = useRouter()
const appContext = useGlobalProperties()
const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')

const isSelected = computed(() => props.draft.isSelected || false)

const getCoverImageUrl = (coverUrl?: string): string => {
  if (!coverUrl) {return '/placeholder.svg'}
  
  if (coverUrl.startsWith('http')) {
    return coverUrl
  }
  
  return `${baseUrl.value}/${coverUrl.split(',')[0]}`
}

const formatDate = (dateStr?: string): string => {
  if (!dateStr) {return ''}
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleSelectChange = (checked: boolean) => {
  emit('select', props.draft)
}

const handleClick = () => {
  emit('click', props.draft)
}

const handleEdit = () => {
  emit('edit', props.draft)
}

const handleSubmit = () => {
  emit('submit', props.draft)
}

const handleDelete = () => {
  emit('delete', props.draft)
}
</script>

<style lang="scss" scoped>
.draft-card-component {
  height: 100%;
  transition: all 0.3s;
  cursor: pointer;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .draft-card-content {
    display: flex;
    gap: 15px;
  }

  .select-checkbox {
    display: flex;
    align-items: flex-start;
    
    :deep(.n-checkbox) {
      margin-top: 2px;
    }
  }

  .cover-wrapper {
    flex-shrink: 0;
    width: 180px;
    height: 120px;
    border-radius: 6px;
    overflow: hidden;
    background: #f5f5f5;

    .cover-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s;
    }

    &:hover .cover-image {
      transform: scale(1.05);
    }

    .cover-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
    }
  }

  .content-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;

    .draft-title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
      line-height: 1.5;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .draft-meta {
      display: flex;
      gap: 16px;
      font-size: 13px;
      color: #666;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .draft-actions {
      display: flex;
      gap: 10px;
      margin-top: auto;
      padding-top: 12px;
      border-top: 1px solid #f0f0f0;

      .n-button {
        flex: 1;
        max-width: 100px;
      }
    }
  }
}

@media (max-width: 768px) {
  .draft-card-component {
    .draft-card-content {
      flex-direction: column;
    }

    .cover-wrapper {
      width: 100%;
      height: 200px;
    }

    .draft-actions {
      .n-button {
        max-width: none;
      }
    }
  }
}
</style>
