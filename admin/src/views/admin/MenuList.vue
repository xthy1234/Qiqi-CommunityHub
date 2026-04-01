<template>
  <PageContainer
      :header-title="'菜单管理'"
      @back="goBack"
  >
    <template #header-extra>
      <NButton type="primary" @click="handleCreate">
        <template #icon>
          <Icon icon="ri:add-line" />
        </template>
        新建菜单
      </NButton>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <NInput
          v-model:value="searchForm.menuName"
          placeholder="请输入菜单名称"
          clearable
          style="width: 200px"
      />
      <NSelect
          v-model:value="searchForm.menuType"
          placeholder="菜单类型"
          :options="menuTypeOptions"
          clearable
          style="width: 150px"
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
        striped
    />

    <!-- 新增/编辑菜单对话框 -->
    <NModal
        v-model:show="editDialogVisible"
        preset="dialog"
        :title="isEdit ? '编辑菜单' : '新建菜单'"
        style="width: 700px"
    >
      <NForm
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-placement="left"
          label-width="100px"
      >
        <NFormItem label="菜单类型" path="type">
          <NRadioGroup v-model:value="formData.type">
            <NRadioButton value="1" label="目录" />
            <NRadioButton value="2" label="菜单" />
            <NRadioButton value="3" label="按钮" />
          </NRadioGroup>
        </NFormItem>

        <NFormItem label="上级菜单" path="parentId">
          <NTreeSelect
              v-model:value="formData.parentId"
              :options="menuTreeOptions"
              placeholder="请选择上级菜单"
              clearable
              checkable
              style="width: 100%"
          />
        </NFormItem>

        <NFormItem label="菜单名称" path="name">
          <NInput
              v-model:value="formData.name"
              placeholder="请输入菜单名称"
          />
        </NFormItem>

        <NFormItem label="菜单路径" path="path" v-if="formData.type !== '3'">
          <NInput
              v-model:value="formData.path"
              placeholder="例如：/users"
          />
        </NFormItem>

        <NFormItem label="菜单图标" path="icon" v-if="formData.type === '1' || formData.type === '2'">
          <NInput
              v-model:value="formData.icon"
              placeholder="例如：ri:user-line"
          />
        </NFormItem>

        <NFormItem label="排序" path="sortOrder">
          <NInputNumber
              v-model:value="formData.sortOrder"
              :min="0"
              style="width: 100%"
          />
        </NFormItem>

        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="formData.status">
            <NRadioButton :value="1" label="启用" />
            <NRadioButton :value="0" label="禁用" />
          </NRadioGroup>
        </NFormItem>

        <NFormItem label="按钮权限" path="buttons" v-if="formData.type === '3'">
          <NDynamicTags
              v-model:value="formData.buttons"
              :max="10"
          />
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
import { NButton, NTag, NSpace, useMessage, useDialog, NForm, NFormItem, NInput, NModal, NRadioGroup, NRadioButton, NTreeSelect, NInputNumber, NDynamicTags } from 'naive-ui'
import { menuApi, type Menu } from '@/api/role'
import PageContainer from "@/components/common/PageContainer.vue"

interface PageResponse<T> {
  list: T[]
  total: number
}

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  menuName: '',
  menuType: null as number | null,
  status: null as number | null
})

const menuTypeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮', value: 3 }
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const loading = ref(false)
const tableData = ref<Menu[]>([])
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
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInst | null>(null)

const formData = ref<Menu>({
  parentId: 0,
  name: '',
  path: '',
  type: 2,
  icon: '',
  sortOrder: 0,
  status: 1,
  buttons: []
})

const formRules: FormRules = {
  name: {
    required: true,
    message: '请输入菜单名称',
    trigger: ['blur', 'change']
  },
  path: {
    required: true,
    message: '请输入菜单路径',
    trigger: ['blur', 'change']
  }
}

const menuTreeOptions = computed<TreeSelectOption[]>(() => {
  const buildTreeOptions = (menus: Menu[], level = 0): TreeSelectOption[] => {
    return menus.map(menu => ({
      label: menu.name,
      value: menu.id,
      key: menu.id!,
      children: menu.children ? buildTreeOptions(menu.children, level + 1) : undefined,
      disabled: menu.type === 3 // 按钮不能作为父菜单
    }))
  }
  
  // 添加根节点选项
  const options: TreeSelectOption[] = [{
    label: '顶级菜单',
    value: 0,
    key: 0
  }]
  
  // 添加现有菜单树
  const treeMenus = buildTreeMenus(tableData.value.filter(m => m.parentId === 0))
  return [...options, ...treeMenus]
})

const columns: DataTableColumns = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '菜单名称',
    key: 'name',
    width: 200,
    render: (row) => {
      const indent = ' '.repeat((row.parentId ? 1 : 0) * 4)
      return h(NSpace, {}, {
        default: () => [
          h('span', {
            style: { marginLeft: `${indent}px` }
          }, {
            default: () => row.name
          }),
          row.icon ? h(Icon, { icon: row.icon }) : null
        ]
      })
    }
  },
  {
    title: '类型',
    key: 'type',
    width: 100,
    render: (row) => {
      const typeMap: Record<number, string> = {
        1: '目录',
        2: '菜单',
        3: '按钮'
      }
      return h(NTag, {
        type: row.type === 1 ? 'info' : row.type === 2 ? 'success' : 'warning'
      }, {
        default: () => typeMap[row.type] || '未知'
      })
    }
  },
  {
    title: '路径',
    key: 'path',
    width: 200,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '图标',
    key: 'icon',
    width: 100,
    render: (row) => {
      return row.icon ? h(Icon, { icon: row.icon }) : '-'
    }
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 80
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'default'
      }, {
        default: () => row.status === 1 ? '启用' : '禁用'
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
            type: row.status === 1 ? 'warning' : 'success',
            onClick: () => handleToggleStatus(row)
          }, {
            default: () => row.status === 1 ? '禁用' : '启用'
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
    const res = await menuApi.getMenuList({
      page: pagination.page,
      limit: pagination.pageSize,
      menu: {
        name: searchForm.value.menuName || undefined,
        type: searchForm.value.menuType || undefined,
        status: searchForm.value.status !== null ? searchForm.value.status : undefined
      }
    })
    
    if (res.code === 0 || res.code === 200) {
      tableData.value = res.data.list
      pagination.itemCount = res.data.total
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

const loadMenuTree = async () => {
  try {
    const res = await menuApi.getMenuTree()
    if (res.code === 0 || res.code === 200) {
      // 用于树形选择器
    }
  } catch (error) {
    console.error('加载菜单树失败', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.value.menuName = ''
  searchForm.value.menuType = null
  searchForm.value.status = null
  pagination.page = 1
  loadData()
}

const handleCreate = () => {
  isEdit.value = false
  formData.value = {
    parentId: 0,
    name: '',
    path: '',
    type: 2,
    icon: '',
    sortOrder: 0,
    status: 1,
    buttons: []
  }
  editDialogVisible.value = true
}

const handleEdit = (row: Menu) => {
  isEdit.value = true
  formData.value = { 
    ...row,
    type: String(row.type),
    status: row.status
  }
  editDialogVisible.value = true
}

const handleToggleStatus = async (row: Menu) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    const res = await menuApi.updateMenuStatus(row.id!, newStatus)
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

const handleDelete = (row: Menu) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除菜单 "${row.name}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await menuApi.deleteMenu(row.id!)
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
      const submitData = {
        ...formData.value,
        type: Number(formData.value.type),
        parentId: Number(formData.value.parentId)
      }
      
      let res
      if (isEdit.value && formData.value.id) {
        res = await menuApi.updateMenu(formData.value.id, submitData)
      } else {
        res = await menuApi.createMenu(submitData)
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

onMounted(() => {
  loadData()
  loadMenuTree()
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
