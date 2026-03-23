<template>
  <div
    class="collapsible-avatar-list"
    :class="{ 'collapsed': isCollapsed }"
  >
    <!-- 展开模式 -->
    <div
      v-if="!isCollapsed"
      class="expanded-view"
    >
      <div
        v-if="showHeader"
        class="header"
      >
        <h3 class="title">
          {{ title }}
        </h3>
        <div class="actions">
          <slot name="header-actions" />
          <n-button
            text
            size="small"
            @click="toggleCollapse"
          >
            <template #icon>
              <n-icon size="18">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                >
                  <path
                    fill="currentColor"
                    d="M15.41 7.41L14 6l-6 6l6 6l1.41-1.41L10.83 12z"
                  />
                </svg>
              </n-icon>
            </template>
          </n-button>
        </div>
      </div>
      
      <div class="content-list">
        <slot name="list-content" />
      </div>
    </div>
    
    <!-- 折叠模式 -->
    <div
      v-else
      class="collapsed-view"
    >
      <div class="collapsed-header">
        <n-button
          text
          size="small"
          @click="toggleCollapse"
        >
          <template #icon>
            <n-icon size="18">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="18"
                height="18"
                viewBox="0 0 24 24"
              >
                <path
                  fill="currentColor"
                  d="M8.59 16.59L13.17 12L8.59 7.41L10 6l6 6l-6 6l-1.41-1.41z"
                />
              </svg>
            </n-icon>
          </template>
        </n-button>
      </div>
      
      <div class="collapsed-items">
        <slot name="collapsed-content" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { NButton, NIcon } from 'naive-ui'

interface Props {
  title?: string
  showHeader?: boolean
  defaultCollapsed?: boolean
  storageKey?: string //  添加存储键名
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  showHeader: true,
  defaultCollapsed: false,
  storageKey: 'collapsible_avatar_list_collapsed' //  默认存储键
})

const emit = defineEmits<{
  (e: 'collapse-change', collapsed: boolean): void
}>()

const isCollapsed = ref(props.defaultCollapsed)
const isInitialized = ref(false) //  标记是否已初始化

//  组件挂载时从 localStorage 读取状态
onMounted(() => {
  try {
    const savedState = localStorage.getItem(props.storageKey!)
    if (savedState !== null) {
      isCollapsed.value = savedState === 'true'

    } else {

    }
  } catch (error) {
    console.warn('⚠️ [CollapsibleAvatarList] 读取 localStorage 失败:', error)
  } finally {
    isInitialized.value = true //  标记初始化完成
  }
})

//  监听状态变化并保存到 localStorage
watch(isCollapsed, (newVal:boolean, oldVal: boolean) => {
  //  只有在初始化完成后才保存（避免初始状态被覆盖）
  if (!isInitialized.value) {
    return
  }

  //  只有状态真正改变时才保存和触发事件
  if (newVal !== oldVal) {
    try {
      localStorage.setItem(props.storageKey!, newVal.toString())

      emit('collapse-change', newVal)
    } catch (error) {
      console.error('❌ [CollapsibleAvatarList] 保存至 localStorage 失败:', error)
    }
  }
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped lang="scss">
.collapsible-avatar-list {
  width: 360px;
  min-width: 360px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  background: #fff;
  transition: all 0.3s ease;
  
  &.collapsed {
    width: 80px;
    min-width: 80px;
  }
}

.expanded-view,
.collapsed-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  .actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .title {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.collapsed-header {
  padding: 20px 10px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: center;
}

.content-list,
.collapsed-items {
  flex: 1;
  overflow-y: auto;
}

.collapsed-items {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
</style>
