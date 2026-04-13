<template>
  <PageContainer
      :header-title="'分类管理'"
      @back="goBack"
  >
    <template #header-extra>
      <NButton type="primary" @click="handleCreate">
        <template #icon>
          <Icon icon="ri:add-line" />
        </template>
        新建分类
      </NButton>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
          v-model:value="searchForm.categoryName"
          placeholder="请输入分类名称"
          clearable
          style="width: 200px"
      />
      <NSelect
          v-model:value="searchForm.status"
          placeholder="状态"
          :options="statusOptions"
          clearable
          style="width: 120px"
      />
      <NButton type="primary" @click="handleSearch">
        <template #icon>
          <Icon icon="ri:search-line" />
        </template>
        搜索
      </NButton>
      <NButton @click="handleReset">重置</NButton>
    </div>

    <!-- 表格 -->
    <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        :row-key="(row) => row.id"
        :checked-row-keys="checkedRowKeys"
        @update:checked-row-keys="handleCheckAll"
        striped
    />

    <!-- 新增/编辑分类对话框 -->
    <NModal
        v-model:show="editDialogVisible"
        preset="dialog"
        :title="isEdit ? '编辑分类' : '新建分类'"
        style="width: 600px"
    >
      <NForm
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-placement="left"
          label-width="100px"
      >
        <NFormItem label="上级分类" path="parentId">
          <NTreeSelect
              v-model:value="formData.parentId"
              :options="categoryTreeOptions"
              placeholder="请选择上级分类（可选）"
              clearable
              checkable
              style="width: 100%"
          />
        </NFormItem>

        <NFormItem label="分类名称" path="categoryName">
          <NInput
              v-model:value="formData.categoryName"
              placeholder="请输入分类名称"
          />
        </NFormItem>

        <NFormItem label="分类描述" path="description">
          <NInput
              v-model:value="formData.description"
              type="textarea"
              placeholder="请输入分类描述"
              :rows="3"
          />
        </NFormItem>

        <NFormItem label="排序" path="sort">
          <NInputNumber
              v-model:value="formData.sort"
              :min="0"
              style="width: 100%"
          />
        </NFormItem>

        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="formData.status">
            <NRadioButton :value="0" label="启用" />
            <NRadioButton :value="1" label="禁用" />
          </NRadioGroup>
        </NFormItem>
      </NForm>

      <template #action>
        <NButton @click="editDialogVisible = false">取消</NButton>
        <NButton type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </NButton>
      </template>
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns, FormRules, FormInst, TreeSelectOption } from 'naive-ui'
import { NButton, NTag, NSpace, useMessage, useDialog, NSwitch, NForm, NFormItem, NInput, NModal, NRadioGroup, NRadioButton, NTreeSelect, NInputNumber } from 'naive-ui'
import { categoryApi, type CategoryVO, type CategoryCreateDTO, type CategoryUpdateDTO } from '@/api/category'
import PageContainer from "@/components/common/PageContainer.vue"

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  categoryName: '',
  status: null as number | null
})

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '禁用', value: 1 }
]

const loading = ref(false)
const tableData = ref<CategoryVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  }
})

const checkedRowKeys = ref<number[]>([])

const editDialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInst | null>(null)
const categoryTreeData = ref<CategoryTreeVO[]>([])

const formData = ref<CategoryCreateDTO & { id?: number; status?: number }>({
  categoryName: '',
  description: '',
  sort: 0,
  parentId: undefined,
  status: 0
})

const formRules: FormRules = {
  categoryName: {
    required: true,
    message: '请输入分类名称',
    trigger: ['blur', 'change']
  }
}

const categoryTreeOptions = computed<TreeSelectOption[]>(() => {
  const buildTreeOptions = (categories: CategoryTreeVO[], level = 0): TreeSelectOption[] => {
    return categories.map(category => ({
      label: category.categoryName,
      value: category.id,
      key: category.id,
      children: category.children ? buildTreeOptions(category.children, level + 1) : undefined,
      disabled: category.status === 1 // 禁用的分类不能作为父分类
    }))
  }
  
  // 添加根节点选项
  const options: TreeSelectOption[] = [{
    label: '顶级分类',
    value: undefined,
    key: 0
  }]
  
  // 添加现有分类树（排除当前编辑的分类）
  const filteredTree = categoryTreeData.value.filter(c => c.id !== formData.value.id)
  const treeOptions = buildTreeOptions(filteredTree)
  
  return [...options, ...treeOptions]
})

const columns: DataTableColumns = [
  {
    type: 'selection',
    width: 50
  },
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '分类名称',
    key: 'categoryName',
    width: 200,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      const indent = row.parentId ? '─── ' : ''
      return h('span', {}, {
        default: () => `${indent}${row.categoryName}`
      })
    }
  },
  {
    title: '描述',
    key: 'description',
    width: 300,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '排序',
    key: 'sort',
    width: 80
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.status === 0 ? 'success' : 'default'
      }, {
        default: () => row.status === 0 ? '启用' : '禁用'
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, {}, {
        default: () => [
          h(NButton, {
            size: 'small',
            onClick: () => handleEdit(row)
          }, {
            default: () => '编辑'
          }),
          h(NButton, {
            size: 'small',
            type: row.status === 0 ? 'warning' : 'success',
            onClick: () => handleToggleStatus(row)
          }, {
            default: () => row.status === 0 ? '禁用' : '启用'
          }),
          h(NButton, {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row)
          }, {
            default: () => '删除'
          })
        ]
      })
    }
  }
]

const goBack = () => {
  router.back()
}

const loadData = async () => {
  loading.value = true
  try {


    const res = await categoryApi.getCategoryList({
      page: pagination.page,
      limit: pagination.pageSize,
      category: {
        categoryName: searchForm.value.categoryName || undefined,
        status: searchForm.value.status !== null ? searchForm.value.status : undefined
      }
    })




    if (res.code === 0 || res.code === 200) {
      // 兼容两种返回格式
      if (Array.isArray(res.data)) {

        tableData.value = res.data
        pagination.itemCount = res.data.length
      } else if (res.data?.list) {


        // 转换 status 字段：ENABLED -> 0, DISABLED -> 1
        tableData.value = res.data.list.map(item => ({
          ...item,
          status: item.status === 'ENABLED' ? 0 : 1
        }))
        pagination.itemCount = res.data.totalCount

      } else {
        console.warn('⚠️ [分类列表] 未知的数据格式')
        tableData.value = []
        pagination.itemCount = 0
      }
    } else {
      console.error('❌ [分类列表] 接口返回错误:', res.msg)
      message.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('❌ [分类列表] 请求异常:', error)
    message.error('加载失败')
  } finally {
    loading.value = false

  }
}

const loadCategoryTree = async () => {
  try {
    const res = await categoryApi.getCategoryTree()
    if (res.code === 0 || res.code === 200) {
      categoryTreeData.value = res.data
    }
  } catch (error) {
    console.error('加载分类树失败', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value.categoryName = ''
  searchForm.value.status = null
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  isEdit.value = false
  formData.value = {
    categoryName: '',
    description: '',
    sort: 0,
    parentId: undefined,
    status: 0
  }
  editDialogVisible.value = true
}

const handleEdit = (row: CategoryVO) => {
  isEdit.value = true
  formData.value = {
    id: row.id,
    categoryName: row.categoryName,
    description: row.description,
    sort: row.sort,
    parentId: row.parentId,
    status: row.status
  }
  editDialogVisible.value = true
}

const handleToggleStatus = async (row: CategoryVO) => {
  const newStatus = row.status === 0 ? 1 : 0
  try {
    const res = await categoryApi.updateCategoryStatus(row.id, newStatus)
    if (res.code === 0 || res.code === 200) {
      message.success('更新成功')
      loadData()
    } else {
      message.error(res.msg || '更新失败')
    }
  } catch (error) {
    message.error('更新失败')
    console.error(error)
  }
}

const handleDelete = (row: CategoryVO) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除分类 "${row.categoryName}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await categoryApi.deleteCategory(row.id)
        if (res.code === 0 || res.code === 200) {
          message.success('删除成功')
          loadData()
        } else {
          message.error(res.msg || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
        console.error(error)
      }
    }
  })
}

const handleSubmit = async () => {
  await formRef.value?.validate(async (errors) => {
    if (errors) return
    
    submitting.value = true
    try {
      const submitData: CategoryCreateDTO | CategoryUpdateDTO = {
        categoryName: formData.value.categoryName,
        description: formData.value.description,
        sort: formData.value.sort,
        parentId: formData.value.parentId
      }
      
      if (formData.value.status !== undefined) {
        ;(submitData as any).status = formData.value.status
      }
      
      let res
      if (isEdit.value && formData.value.id) {
        res = await categoryApi.updateCategory(formData.value.id, submitData as CategoryUpdateDTO)
      } else {
        res = await categoryApi.createCategory(submitData as CategoryCreateDTO)
      }
      
      if (res.code === 0 || res.code === 200) {
        message.success(isEdit.value ? '更新成功' : '创建成功')
        editDialogVisible.value = false
        loadData()
        loadCategoryTree()
      } else {
        message.error(res.msg || '操作失败')
      }
    } catch (error) {
      message.error('操作失败')
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const handleCheckAll = (keys: any) => {
  checkedRowKeys.value = keys as number[]
}

onMounted(() => {
  loadData()
  loadCategoryTree()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
</style>
