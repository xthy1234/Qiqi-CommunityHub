<template>
  <div class="search-bar">
    <slot name="default">
      <!-- 默认插槽：放置搜索表单字段 -->
    </slot>
    
    <div class="search-actions">
      <NButton 
        v-if="showSearch" 
        type="primary" 
        @click="handleSearch" 
        :loading="loading"
      >
        <template #icon>
          <Icon icon="ri:search-line" />
        </template>
        {{ searchText }}
      </NButton>
      
      <NButton 
        v-if="showReset" 
        @click="handleReset"
      >
        {{ resetText }}
      </NButton>
      
      <slot name="extra">
        <!-- 额外操作按钮插槽 -->
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'

interface Props {
  showSearch?: boolean
  showReset?: boolean
  searchText?: string
  resetText?: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showSearch: true,
  showReset: true,
  searchText: '搜索',
  resetText: '重置',
  loading: false
})

const emit = defineEmits<{
  search: []
  reset: []
}>()

const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  emit('reset')
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-left: auto;
}
</style>
