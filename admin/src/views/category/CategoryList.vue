<template>
  <PageContainer
      :header-title="'分类管理'"
      @back="goBack"
  >
    <template #headerExtra>
      <NButton type="primary" @click="handleCreate">
        <Icon
            icon="ri:add-line"
            style="margin-right: 4px;"
        />
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
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NTag, NSpace, useMessage, useDialog } from 'naive-ui'
import { categoryApi, type CategoryVO } from '@/api/category'
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
  router.push('/admin/categories/edit')
}

const handleEdit = (row: CategoryVO) => {
  router.push(`/admin/categories/edit?id=${row.id}`)
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

const handleCheckAll = (keys: any) => {
  checkedRowKeys.value = keys as number[]
}

onMounted(() => {
  loadData()
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
