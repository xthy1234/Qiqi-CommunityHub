<template>
  <n-select
    v-model:value="selectedValue"
    :options="categoryOptions"
    :placeholder="placeholder || '全部分类'"
    clearable
    :disabled="disabled"
    :loading="loading"
    style="width: 200px;"
    @update:value="handleValueChange"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { NSelect } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'

const props = withDefaults(defineProps<{
  modelValue?: number | string | undefined
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  includeAllOption?: boolean
}>(), {
  modelValue: undefined,
  placeholder: '全部分类',
  disabled: false,
  loading: false,
  includeAllOption: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | string | undefined): void
  (e: 'change', value: number | string | undefined): void
}>()

const appContext = useGlobalProperties()
const $http = appContext?.$http

const selectedValue = ref<number | string | undefined>(props.modelValue)
const categoryOptions = ref<any[]>([])

watch(() => props.modelValue, (newVal) => {
  selectedValue.value = newVal
})

const fetchCategories = async () => {
  try {
    const response = await $http.get('/categories')
    const apiData = response.data
    
    let categories: any[] = []
    
    if (apiData && apiData.data && apiData.data.list) {
      categories = Array.isArray(apiData.data.list) ? apiData.data.list : []
    } else if (apiData && apiData.data) {
      categories = Array.isArray(apiData.data) ? apiData.data : []
    } else if (Array.isArray(apiData)) {
      categories = apiData
    }
    
    const options = categories.map((cat: any) => ({
      label: cat.categoryName || cat.name,
      value: cat.id
    }))
    
    if (props.includeAllOption) {
      categoryOptions.value = [
        { label: '全部分类', value: undefined },
        ...options
      ]
    } else {
      categoryOptions.value = options
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
    if (props.includeAllOption) {
      categoryOptions.value = [{ label: '全部分类', value: undefined }]
    }
  }
}

const handleValueChange = (value: number | string | undefined) => {
  emit('update:modelValue', value)
  emit('change', value)
}

onMounted(() => {
  fetchCategories()
})
</script>
