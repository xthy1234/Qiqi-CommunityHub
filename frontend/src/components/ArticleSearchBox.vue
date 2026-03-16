<template>
  <div class="article-search-box">
    <n-space :size="12" :wrap="!compact">
      <!-- 搜索输入框 -->
      <n-input
        v-model:value="keyword"
        :placeholder="placeholder || '搜索文章...'"
        clearable
        :disabled="disabled"
        :loading="searching"
        style="width: compact ? '100%' : '300px'"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <Icon icon="ri:search-line" />
        </template>
      </n-input>

      <!-- 分类筛选（可选） -->
      <ArticleCategorySelect
        v-if="showCategory"
        v-model="selectedCategory"
        :disabled="disabled"
        @change="handleCategoryChange"
      />

      <!-- 日期范围（可选） -->
      <template v-if="showDateRange">
        <n-date-picker
          v-model:value="startDate"
          type="date"
          placeholder="开始日期"
          :disabled="disabled"
          style="width: 150px;"
          format="yyyy-MM-dd"
          value-format="yyyy-MM-dd"
          @update:value="handleDateChange"
        />
        <n-date-picker
          v-model:value="endDate"
          type="date"
          placeholder="结束日期"
          :disabled="disabled"
          style="width: 150px;"
          format="yyyy-MM-dd"
          value-format="yyyy-MM-dd"
          @update:value="handleDateChange"
        />
      </template>

      <!-- 搜索按钮 -->
      <n-button
        type="primary"
        :loading="searching"
        :disabled="disabled || !keyword?.trim()"
        @click="handleSearch"
      >
        <template #icon>
          <Icon icon="ri:search-eye-line" />
        </template>
        {{ searchBtnText || '搜索' }}
      </n-button>

      <!-- 重置按钮（可选） -->
      <n-button
        v-if="showReset"
        @click="handleReset"
        :disabled="disabled"
      >
        <template #icon>
          <Icon icon="ri:restart-line" />
        </template>
        重置
      </n-button>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NSpace, NInput, NButton, NDatePicker } from 'naive-ui'
import { Icon } from '@iconify/vue'
import ArticleCategorySelect from './ArticleCategorySelect.vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  searching?: boolean
  showCategory?: boolean
  showDateRange?: boolean
  showReset?: boolean
  compact?: boolean
  searchBtnText?: string
}>(), {
  modelValue: '',
  placeholder: '搜索文章...',
  disabled: false,
  searching: false,
  showCategory: false,
  showDateRange: false,
  showReset: false,
  compact: false,
  searchBtnText: '搜索'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'search', params: SearchParams): void
  (e: 'reset'): void
}>()

interface SearchParams {
  keyword: string
  categoryId?: number | string
  startDate?: string
  endDate?: string
}

const keyword = ref(props.modelValue || '')
const selectedCategory = ref<number | string | undefined>(undefined)
const startDate = ref<string | undefined>(undefined)
const endDate = ref<string | undefined>(undefined)

watch(() => props.modelValue, (newVal) => {
  keyword.value = newVal || ''
})

const handleSearch = () => {
  if (!keyword.value?.trim()) return
  
  const params: SearchParams = {
    keyword: keyword.value.trim()
  }
  
  if (selectedCategory.value !== undefined) {
    params.categoryId = selectedCategory.value
  }
  
  if (startDate.value) {
    params.startDate = startDate.value
  }
  
  if (endDate.value) {
    params.endDate = endDate.value
  }
  
  emit('update:modelValue', keyword.value)
  emit('search', params)
}

const handleCategoryChange = () => {
  if (keyword.value?.trim()) {
    handleSearch()
  }
}

const handleDateChange = () => {
  if (keyword.value?.trim()) {
    handleSearch()
  }
}

const handleReset = () => {
  keyword.value = ''
  selectedCategory.value = undefined
  startDate.value = undefined
  endDate.value = undefined
  
  emit('update:modelValue', '')
  emit('reset')
}
</script>

<style lang="scss" scoped>
.article-search-box {
  width: 100%;
  
  .n-space {
    width: 100%;
  }
}
</style>
