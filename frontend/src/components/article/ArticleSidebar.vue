<template>
  <div class="article-sidebar-component">
    <!-- 右侧固定侧边栏（桌面端） -->
    <div class="sidebar-fixed">
      <!-- 作者操作卡片 -->
      <n-card
        v-if="isAuthor"
        class="sidebar-card"
        title="文章管理"
        size="small"
      >
        <n-space
          vertical
          :size="12"
        >
          <n-button
            type="primary"
            block
            @click="$emit('edit')"
          >
            <template #icon>
              <Icon
                icon="ri:edit-line"
                width="18"
              />
            </template>
            编辑此文章
          </n-button>

          <!-- 编辑模式下拉菜单 -->
          <n-dropdown
            v-model:value="editModeDropdown"
            trigger="click"
            :options="editModeOptions"
            @select="handleEditModeSelect"
          >
            <n-button block>
              <template #icon>
                <Icon
                  icon="ri:settings-3-line"
                  width="18"
                />
              </template>
              {{ editMode === 0 ? '仅自己可编辑' : '所有人可建议' }}
              <Icon
                icon="ri:arrow-down-s-line"
                width="16"
              />
            </n-button>
          </n-dropdown>

          <!-- 审核状态（仅管理员可见） -->
          <div
            v-if="article.auditStatus && article.auditStatus !== '1'"
            class="audit-status-tip"
          >
            <n-alert
              :type="article.auditStatus === '2' ? 'error' : 'warning'"
              :closable="false"
              size="small"
            >
              {{ getAuditStatusText(article.auditStatus) }}
            </n-alert>
            <div
              v-if="article.auditReply"
              class="audit-reply-tip"
            >
              <strong>审核回复：</strong>{{ article.auditReply }}
            </div>
          </div>
        </n-space>
      </n-card>

      <!-- 访客操作卡片 -->
      <n-card
        v-else-if="!isAuthor && article.editMode === 1"
        class="sidebar-card"
        title="协作修改"
        size="small"
      >
        <n-alert
          type="info"
          title="本文允许所有人提出修改建议"
          style="margin-bottom: 12px;"
          :closable="false"
          size="small"
        />
        <n-button
          type="success"
          block
          @click="$emit('suggest')"
        >
          <template #icon>
            <Icon
              icon="ri:edit-circle-line"
              width="18"
            />
          </template>
          建议修改
        </n-button>
      </n-card>

      <!-- 文章信息卡片 -->
      <n-card
        class="sidebar-card"
        title="文章信息"
        size="small"
      >
        <n-space
          vertical
          :size="12"
        >
          <n-button
            block
            @click="$emit('versions')"
          >
            <template #icon>
              <Icon
                icon="ri:git-commit-line"
                width="18"
              />
            </template>
            历史版本
          </n-button>

          <!-- 审核建议按钮（仅作者可见） -->
          <n-badge
            v-if="isAuthor"
            :value="pendingSuggestionsCount"
            :show="pendingSuggestionsCount > 0"
            :max="99"
          >
            <n-button
              block
              @click="$emit('review-suggestions')"
            >
              <template #icon>
                <Icon
                  icon="ri:review-line"
                  width="18"
                />
              </template>
              审核建议
            </n-button>
          </n-badge>
        </n-space>
      </n-card>

      <!-- 贡献者列表 -->
      <n-card
        v-if="contributors.length > 0"
        class="sidebar-card"
        title="贡献者"
        size="small"
      >
        <ContributorList
          :contributors="contributors"
          :show-title="false"
          :max-show="5"
          size="32"
        />
      </n-card>
    </div>

    <!-- 底部折叠工具栏（移动端） -->
    <div class="mobile-toolbar">
      <n-popover
        v-model:show="showPopover"
        trigger="manual"
        placement="top"
      >
        <template #trigger>
          <n-button
            circle
            strong
            secondary
            @click="togglePopover"
          >
            <template #icon>
              <Icon
                icon="ri:more-fill"
                width="24"
              />
            </template>
          </n-button>
        </template>
        
        <div class="mobile-menu">
          <!-- 作者操作 -->
          <template v-if="isAuthor">
            <n-button
              text
              block
              @click="$emit('edit'); togglePopover()"
            >
              <template #icon>
                <Icon
                  icon="ri:edit-line"
                  width="18"
                />
              </template>
              编辑文章
            </n-button>
            
            <n-divider style="margin: 8px 0" />
            
            <n-dropdown
              v-model:value="editModeDropdown"
              trigger="click"
              :options="editModeOptions"
              @select="handleEditModeSelect"
            >
              <n-button
                text
                block
              >
                <template #icon>
                  <Icon
                    icon="ri:settings-3-line"
                    width="18"
                  />
                </template>
                {{ editMode === 0 ? '仅自己可编辑' : '所有人可建议' }}
              </n-button>
            </n-dropdown>
            
            <n-divider style="margin: 8px 0" />
            
            <n-badge
              :value="pendingSuggestionsCount"
              :show="pendingSuggestionsCount > 0"
              :max="99"
            >
              <n-button
                text
                block
                @click="$emit('review-suggestions'); togglePopover()"
              >
                <template #icon>
                  <Icon
                    icon="ri:review-line"
                    width="18"
                  />
                </template>
                审核建议
              </n-button>
            </n-badge>
          </template>
          
          <!-- 访客操作 -->
          <template v-else-if="!isAuthor && article.editMode === 1">
            <n-button
              type="success"
              text
              block
              @click="$emit('suggest'); togglePopover()"
            >
              <template #icon>
                <Icon
                  icon="ri:edit-circle-line"
                  width="18"
                />
              </template>
              建议修改
            </n-button>
            
            <n-divider style="margin: 8px 0" />
          </template>
          
          <!-- 通用操作 -->
          <n-button
            text
            block
            @click="$emit('versions'); togglePopover()"
          >
            <template #icon>
              <Icon
                icon="ri:git-commit-line"
                width="18"
              />
            </template>
            历史版本
          </n-button>
        </div>
      </n-popover>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {Icon} from '@iconify/vue'
import {NAlert, NBadge, NButton, NCard, NDivider, NPopover, NSpace, NDropdown, type DropdownOption} from 'naive-ui'
import ContributorList from '@/components/article/ContributorList.vue'
import {getAuditStatusText} from '@/utils/userUtils'

// Props
interface Props {
  isAuthor: boolean
  article: any
  editMode: number
  pendingSuggestionsCount: number
  contributors: any[]
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  (e: 'edit'): void
  (e: 'suggest'): void
  (e: 'versions'): void
  (e: 'review-suggestions'): void
  (e: 'update:editMode', key: number): void
}>()

// 响应式数据
const editModeDropdown = ref<number | string>(props.editMode)
const showPopover = ref(false)

// 计算属性
const editModeOptions = computed<DropdownOption[]>(() => [
  {
    label: '仅自己可编辑',
    key: 0,
    disabled: props.editMode === 0
  },
  {
    label: '所有人可建议',
    key: 1,
    disabled: props.editMode === 1
  }
])

// 方法
const handleEditModeSelect = (key: number) => {
  emit('update:editMode', key)
}

const togglePopover = () => {
  showPopover.value = !showPopover.value
}

// 监听外部编辑模式变化
watch(() => props.editMode, ((newVal: number|string) => {
  editModeDropdown.value = newVal
}))
</script>

<style lang="scss" scoped>
.article-sidebar-component {
  position: relative;
}

.sidebar-fixed {
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  .sidebar-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    
    :deep(.n-card__content) {
      padding: 12px;
    }
  }
  
  .audit-status-tip {
    margin-top: 8px;
    
    .audit-reply-tip {
      margin-top: 8px;
      padding: 8px;
      background: #fef0f0;
      border-radius: 4px;
      font-size: 12px;
      color: #666;
    }
  }
}

.mobile-toolbar {
  display: none;
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  
  :deep(.n-button.n-button--circle) {
    width: 56px;
    height: 56px;
    background: #409EFF;
    color: #fff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    
    &:hover {
      background: #66b1ff;
    }
  }
  
  .mobile-menu {
    min-width: 200px;
    
    .n-button {
      justify-content: flex-start;
      padding: 8px 12px;
      
      :deep(.n-button__icon) {
        margin-right: 8px;
      }
    }
  }
}

// 响应式布局
@media (max-width: 1200px) {
  .sidebar-fixed {
    display: none;
  }
  
  .mobile-toolbar {
    display: block;
  }
}
</style>
