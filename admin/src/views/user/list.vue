<template>
  <div class="page-container">
    <NCard title="用户管理" size="large">
      <template #header-extra>
        <NButton type="primary" @click="handleCreate">
          <template #icon>
            <Icon icon="ri:add-line" />
          </template>
          新建用户
        </NButton>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <NInput
            v-model:value="searchForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px"
        />
        <NInput
            v-model:value="searchForm.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 200px"
        />
        <NSelect
            v-model:value="searchForm.status"
            placeholder="用户状态"
            :options="statusOptions"
            clearable
            style="width: 150px"
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
          striped
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NTag, NSpace } from 'naive-ui'

interface UserItem {
  id: number
  username: string
  phone: string
  role: string
  status: number
  createTime: string
}

const searchForm = ref({
  username: '',
  phone: '',
  status: null as number | null
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const loading = ref(false)
const tableData = ref<UserItem[]>([])

const columns: DataTableColumns = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '用户名',
    key: 'username',
    width: 150
  },
  {
    title: '手机号',
    key: 'phone',
    width: 150
  },
  {
    title: '角色',
    key: 'role',
    width: 120,
    render: (row) => {
      return h(NTag, {
        type: row.role === '管理员' ? 'success' : 'info',
        content: row.role
      })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'error',
        content: row.status === 1 ? '启用' : '禁用'
      })
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, {}, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'primary',
            text: true,
            onClick: () => handleEdit(row),
            content: '编辑'
          }),
          h(NButton, {
            size: 'small',
            type: 'error',
            text: true,
            onClick: () => handleDelete(row),
            content: '删除'
          })
        ]
      })
    }
  }
]

const pagination = {
  page: 1,
  pageSize: 10,
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
}

const loadData = () => {
  loading.value = true
  // TODO: 调用 API 获取数据
  setTimeout(() => {
    tableData.value = [
      { id: 1, username: 'admin', phone: '13800138000', role: '管理员', status: 1, createTime: '2024-01-01 12:00:00' },
      { id: 2, username: 'user1', phone: '13800138001', role: '普通用户', status: 1, createTime: '2024-01-02 12:00:00' }
    ]
    loading.value = false
  }, 500)
}

const handleCreate = () => {
  console.log('创建用户')
}

const handleEdit = (row: UserItem) => {
  console.log('编辑用户', row)
}

const handleDelete = (row: UserItem) => {
  console.log('删除用户', row)
}

const handleSearch = () => {
  loadData()
}

const handleReset = () => {
  searchForm.value = {
    username: '',
    phone: '',
    status: null
  }
  loadData()
}

loadData()
</script>

<style lang="scss" scoped>
.page-container {
  padding: 0;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
</style>
