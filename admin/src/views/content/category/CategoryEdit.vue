<template>
  <PageContainer
    :header-title="isEdit ? '编辑分类' : '新增分类'"
    @back="goBack"
  >
    <n-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      class="edit-form"
    >
      <n-grid :cols="12" :x-gap="20">
        <!-- 上级分类 -->
        <n-grid-item :span="12">
          <n-form-item label="上级分类" path="parentId">
            <n-tree-select
              v-model:value="formData.parentId"
              :options="categoryTreeOptions"
              placeholder="请选择上级分类（可选，不选则为顶级分类）"
              clearable
              style="width: 100%"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 分类名称 -->
        <n-grid-item :span="6">
          <n-form-item label="分类名称" path="categoryName">
            <n-input
              v-model:value="formData.categoryName"
              placeholder="请输入分类名称"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 排序 -->
        <n-grid-item :span="6">
          <n-form-item label="排序" path="sort">
            <n-input-number
              v-model:value="formData.sort"
              :min="0"
              placeholder="数值越小越靠前"
              style="width: 100%"
            />
          </n-form-item>
        </n-grid-item>

        <!-- 状态 -->
        <n-grid-item :span="6">
          <n-form-item label="状态" path="status">
            <n-radio-group v-model:value="formData.status">
              <n-radio :value="0">启用</n-radio>
              <n-radio :value="1">禁用</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-grid-item>

        <!-- 分类描述 -->
        <n-grid-item :span="12">
          <n-form-item label="分类描述" path="description">
            <n-input
              v-model:value="formData.description"
              type="textarea"
              :rows="4"
              placeholder="请输入分类描述（可选）"
            />
          </n-form-item>
        </n-grid-item>
      </n-grid>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <n-button type="primary" @click="handleSubmit" :loading="submitLoading">
          保存
        </n-button>
        <n-button @click="goBack">取消</n-button>
      </div>
    </n-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormRules, TreeSelectOption } from 'naive-ui'
import PageContainer from 'src/components/layout/PageContainer.vue'
import { categoryApi, type CategoryVO, type CategoryTreeVO } from '@/api/category'

const router = useRouter()
const route = useRoute()
const message = useMessage()

interface FormData {
  id?: number
  categoryName: string
  description: string
  sort: number
  parentId: number | undefined
  status: number
}

const formRef = ref<any>(null)
const submitLoading = ref(false)
const categoryTreeData = ref<CategoryTreeVO[]>([])

const formData = reactive<FormData>({
  categoryName: '',
  description: '',
  sort: 0,
  parentId: undefined,
  status: 0
})

const isEdit = computed(() => !!route.query.id)

const categoryTreeOptions = computed<TreeSelectOption[]>(() => {
  const buildTreeOptions = (categories: CategoryTreeVO[]): TreeSelectOption[] => {
    return categories.map(category => ({
      label: category.categoryName,
      value: category.id,
      key: category.id,
      children: category.children ? buildTreeOptions(category.children) : undefined,
      disabled: category.status === 1 || category.id === formData.id
    }))
  }
  
  const options: TreeSelectOption[] = [{
    label: '顶级分类',
    value: undefined,
    key: 0
  }]
  
  const filteredTree = categoryTreeData.value.filter(c => c.id !== formData.id)
  const treeOptions = buildTreeOptions(filteredTree)
  
  return [...options, ...treeOptions]
})

const formRules: FormRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ],
  sort: [
    { required: true, type: 'number', message: '请输入排序值', trigger: 'blur' }
  ],
  status: [
    { required: true, type: 'number', message: '请选择状态', trigger: 'change' }
  ]
}

const fetchCategoryTree = async () => {
  try {
    const response = await categoryApi.getCategoryTree()
    if (response.code === 0 || response.code === 200) {
      categoryTreeData.value = response.data
    } else {
      message.error(response.msg || '获取分类树失败')
    }
  } catch (error: any) {
    console.error('获取分类树失败:', error)
    message.error(error.response?.data?.msg || '获取分类树失败')
  }
}

const fetchCategoryInfo = async () => {
  if (!isEdit.value) return
  
  const categoryId = route.query.id
  if (!categoryId) return
  
  try {
    const response = await categoryApi.getCategoryById(Number(categoryId))
    
    if (response.code === 0 || response.code === 200) {
      const categoryData = response.data
      Object.assign(formData, {
        id: categoryData.id,
        categoryName: categoryData.categoryName,
        description: categoryData.description,
        sort: categoryData.sort,
        parentId: categoryData.parentId,
        status: categoryData.status === 'ENABLED' ? 0 : 1
      })
    } else {
      message.error(response.msg || '获取分类信息失败')
    }
  } catch (error: any) {
    console.error('获取分类信息失败:', error)
    message.error(error.response?.data?.msg || '获取分类信息失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    
    submitLoading.value = true
    
    const submitData = {
      categoryName: formData.categoryName,
      description: formData.description,
      sort: formData.sort,
      parentId: formData.parentId,
      status: formData.status
    }
    
    if (isEdit.value && formData.id) {
      const response = await categoryApi.updateCategory(formData.id, submitData)
      
      if (response.code === 0 || response.code === 200) {
        message.success('更新成功')
        setTimeout(() => {
          router.push('/admin/categories')
        }, 500)
      } else {
        message.error(response.msg || '更新失败')
      }
    } else {
      const response = await categoryApi.createCategory(submitData)
      
      if (response.code === 0 || response.code === 200) {
        message.success('创建成功')
        setTimeout(() => {
          router.push('/admin/categories')
        }, 500)
      } else {
        message.error(response.msg || '创建失败')
      }
    }
  } catch (error: any) {
    if (error !== false) {
      console.error('提交失败:', error)
      message.error(error.response?.data?.msg || '操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchCategoryTree()
  fetchCategoryInfo()
})
</script>

<style lang="scss" scoped>
.edit-form {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  
  .form-actions {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-top: 40px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    
    .n-button {
      min-width: 120px;
      height: 40px;
      font-size: 16px;
    }
  }
}
</style>
