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
            v-model:value="searchForm.account"
            placeholder="请输入账号"
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
          :remote="true"
          striped
      />
    </NCard>

    <!-- 新增/编辑用户对话框 -->
    <UserEditDialog
        v-model:visible="editDialogVisible"
        :user-data="currentUserData"
        @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NTag, NSpace, useMessage, useDialog } from 'naive-ui'
import UserEditDialog from '@/components/UserEditDialog.vue'
import apiService from '@/api'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

interface PageResponse<T> {
  list: T[]
  totalCount: number
  pageSize: number
  totalPage: number
  currPage: number
}

interface UserItem {
  id: number
  account: string
  nickname: string
  phone: string
  email?: string
  avatar?: string
  gender: number
  roleId: number
  roleName?: string
  status: string
  createTime: string
  lastLoginTime?: string
  lastLoginIp?: string
}

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  account: '',
  phone: '',
  status: null as string | null
})

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' }
]

const loading = ref(false)
const tableData = ref<UserItem[]>([])
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

const editDialogVisible = ref(false)
const currentUserData = ref<UserItem | null>(null)

const columns: DataTableColumns = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '账号',
    key: 'account',
    width: 150,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '昵称',
    key: 'nickname',
    width: 120
  },
  {
    title: '手机号',
    key: 'phone',
    width: 130
  },
  {
    title: '邮箱',
    key: 'email',
    width: 180,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '性别',
    key: 'gender',
    width: 80,
    render: (row) => {
      const rowData = row as unknown as UserItem
      const genderMap: Record<number, string> = {
        0: '保密',
        1: '男',
        2: '女'
      }
      return h(NTag, {
        type: 'info'
      }, {
        default: () => genderMap[rowData.gender] || '未知'
      })
    }
  },
  {
    title: '角色',
    key: 'roleName',
    width: 120,
    render: (row) => {
      const rowData = row as unknown as UserItem
      return h(NTag, {
        type: rowData.roleName === '管理员' ? 'success' : 'info'
      }, {
        default: () => rowData.roleName
      })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      const rowData = row as unknown as UserItem
      return h(NTag, {
        type: rowData.status === 'ENABLED' ? 'success' : 'error'
      }, {
        default: () => rowData.status === 'ENABLED' ? '启用' : '禁用'
      })
    }
  },
  {
    title: '最后登录',
    key: 'lastLoginTime',
    width: 180,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      const rowData = row as unknown as UserItem
      return h(NSpace, {}, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'primary',
            text: true,
            onClick: () => handleEdit(rowData)
          }, {
            default: () => '编辑'
          }),
          h(NButton, {
            size: 'small',
            type: 'error',
            text: true,
            onClick: () => handleDelete(rowData)
          }, {
            default: () => '删除'
          })
        ]
      })
    }
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      limit: pagination.pageSize,
      ...searchForm.value
    }

    const response = await apiService.user.getUserList(params)

    if (response.data.code === 0 || response.data.code === 200) {
      tableData.value = response.data.data.list || []
      pagination.itemCount = response.data.data.totalCount || 0
    } else if (response.data.code === 401) {
      message.error('请先登录')
      setTimeout(() => {
        router.push('/login')
      }, 500)
    } else {
      message.error(response.data.msg || '获取用户列表失败')
    }
  } catch (error: any) {
    console.error('获取用户列表失败:', error)
    if (error.response?.status === 401) {
      message.error('请先登录')
      setTimeout(() => {
        router.push('/login')
      }, 500)
    } else {
      message.error(error.response?.data?.msg || '获取用户列表失败')
    }
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  currentUserData.value = null
  editDialogVisible.value = true
}

const handleEdit = (row: UserItem) => {
  currentUserData.value = { ...row }
  editDialogVisible.value = true
}

const handleDelete = async (row: UserItem) => {
  try {
    dialog.warning({
      title: '警告',
      content: `确定要删除用户 "${row.account}" 吗？`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          const response = await apiService.user.deleteUser(row.id)

          if (response.data.code === 0 || response.data.code === 200) {
            message.success('删除成功')
            loadData()
          } else {
            message.error(response.data.msg || '删除失败')
          }
        } catch (error: any) {
          console.error('删除失败:', error)
          message.error(error.response?.data?.msg || '删除失败')
        }
      }
    })
  } catch (error: any) {
    console.error('删除操作失败:', error)
    message.error('删除失败')
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value = {
    account: '',
    phone: '',
    status: null
  }
  pagination.page = 1
  loadData()
}

onMounted(() => {
  loadData()
})

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
