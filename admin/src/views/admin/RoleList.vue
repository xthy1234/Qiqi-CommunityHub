<template>
  <PageContainer
      :header-title="'角色管理'"
      @back="goBack"
  >
    <template #header-extra>
      <NButton type="primary" @click="handleCreate">
        <template #icon>
          <Icon icon="ri:add-line" />
        </template>
        新建角色
      </NButton>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
          v-model:value="searchForm.roleName"
          placeholder="请输入角色名称"
          clearable
          style="width: 200px"
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

    <!-- 新增/编辑角色对话框 -->
    <NModal
        v-model:show="editDialogVisible"
        preset="dialog"
        :title="isEdit ? '编辑角色' : '新建角色'"
        style="width: 600px"
    >
      <NForm
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-placement="left"
          label-width="140px"
      >
        <NFormItem label="角色名称" path="roleName">
          <NInput
              v-model:value="formData.roleName"
              placeholder="请输入角色名称"
          />
        </NFormItem>

        <NFormItem label="后台登录权限" path="hasBackLogin">
          <NSwitch v-model:value="formData.hasBackLogin" />
        </NFormItem>

        <NFormItem label="后台注册权限" path="hasBackRegister">
          <NSwitch v-model:value="formData.hasBackRegister" />
        </NFormItem>

        <NFormItem label="前台登录权限" path="hasFrontLogin">
          <NSwitch v-model:value="formData.hasFrontLogin" />
        </NFormItem>

        <NFormItem label="前台注册权限" path="hasFrontRegister">
          <NSwitch v-model:value="formData.hasFrontRegister" />
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
import { ref, h, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns, FormRules, FormInst } from 'naive-ui'
import { NButton, NTag, NSpace, useMessage, useDialog, NSwitch, NForm, NFormItem, NInput, NModal } from 'naive-ui'
import { roleApi, type Role } from '@/api/role'
import PageContainer from "@/components/common/PageContainer.vue"

interface PageResponse<T> {
  list: T[]
  totalCount: number
  pageSize: number
  totalPage: number
  currPage: number
}

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  roleName: ''
})

const loading = ref(false)
const tableData = ref<Role[]>([])
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

const formData = ref<Role>({
  roleName: '',
  hasBackLogin: false,
  hasBackRegister: false,
  hasFrontLogin: false,
  hasFrontRegister: false
})

const formRules: FormRules = {
  roleName: {
    required: true,
    message: '请输入角色名称',
    trigger: ['blur', 'change']
  }
}

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
    title: '角色名称',
    key: 'roleName',
    width: 200,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '后台登录',
    key: 'hasBackLogin',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.hasBackLogin ? 'success' : 'default'
      }, {
        default: () => row.hasBackLogin ? '是' : '否'
      })
    }
  },
  {
    title: '后台注册',
    key: 'hasBackRegister',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.hasBackRegister ? 'success' : 'default'
      }, {
        default: () => row.hasBackRegister ? '是' : '否'
      })
    }
  },
  {
    title: '前台登录',
    key: 'hasFrontLogin',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.hasFrontLogin ? 'success' : 'default'
      }, {
        default: () => row.hasFrontLogin ? '是' : '否'
      })
    }
  },
  {
    title: '前台注册',
    key: 'hasFrontRegister',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.hasFrontRegister ? 'success' : 'default'
      }, {
        default: () => row.hasFrontRegister ? '是' : '否'
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 300,
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
            onClick: () => handlePermission(row)
          }, {
            default: () => '权限分配'
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
    const res = await roleApi.getRoleList({
      page: pagination.page,
      limit: pagination.pageSize,
      role: searchForm.value.roleName ? { roleName: searchForm.value.roleName } : undefined
    })
    
    if (res.code === 0 || res.code === 200) {
      tableData.value = res.data.list
      pagination.itemCount = res.data.totalCount
    } else {
      message.error(res.msg || '加载失败')
    }
  } catch (error) {
    message.error('加载失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value.roleName = ''
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  isEdit.value = false
  formData.value = {
    roleName: '',
    hasBackLogin: false,
    hasBackRegister: false,
    hasFrontLogin: false,
    hasFrontRegister: false
  }
  editDialogVisible.value = true
}

const handleEdit = (row: Role) => {
  isEdit.value = true
  formData.value = { ...row }
  editDialogVisible.value = true
}

const handleDelete = (row: Role) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除角色 "${row.roleName}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await roleApi.deleteRole(row.id!)
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
      let res
      if (isEdit.value && formData.value.id) {
        res = await roleApi.updateRole(formData.value.id, formData.value)
      } else {
        res = await roleApi.createRole(formData.value)
      }
      
      if (res.code === 0 || res.code === 200) {
        message.success(isEdit.value ? '更新成功' : '创建成功')
        editDialogVisible.value = false
        loadData()
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

const handlePermission = (row: Role) => {
  router.push(`/admin/role-menus?id=${row.id}&name=${row.roleName}`)
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
