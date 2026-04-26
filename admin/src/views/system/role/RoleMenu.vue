<template>
  <PageContainer
      :header-title="`权限分配 - ${roleName}`"
      @back="goBack"
  >
    <template #header-extra>
      <NSpace>
        <NButton @click="handleExpandAll">展开全部</NButton>
        <NButton @click="handleCollapseAll">折叠全部</NButton>
        <NButton type="primary" @click="handleSave" :loading="saving">
          保存权限
        </NButton>
      </NSpace>
    </template>

    <NGrid cols="2" x-gap="16" y-gap="16">
      <NGi>
        <NCard title="角色信息">
          <NDescriptions bordered :column="1">
            <NDescriptionsItem label="角色名称">
              {{ roleName }}
            </NDescriptionsItem>
            <NDescriptionsItem label="后台登录">
              <NTag :type="roleInfo?.hasBackLogin ? 'success' : 'default'">
                {{ roleInfo?.hasBackLogin ? '是' : '否' }}
              </NTag>
            </NDescriptionsItem>
            <NDescriptionsItem label="后台注册">
              <NTag :type="roleInfo?.hasBackRegister ? 'success' : 'default'">
                {{ roleInfo?.hasBackRegister ? '是' : '否' }}
              </NTag>
            </NDescriptionsItem>
            <NDescriptionsItem label="前台登录">
              <NTag :type="roleInfo?.hasFrontLogin ? 'success' : 'default'">
                {{ roleInfo?.hasFrontLogin ? '是' : '否' }}
              </NTag>
            </NDescriptionsItem>
            <NDescriptionsItem label="前台注册">
              <NTag :type="roleInfo?.hasFrontRegister ? 'success' : 'default'">
                {{ roleInfo?.hasFrontRegister ? '是' : '否' }}
              </NTag>
            </NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NGi>

      <NGi>
        <NCard title="操作按钮">
          <NSpace vertical>
            <NButton block @click="handleCheckAll">全选</NButton>
            <NButton block @click="handleUncheckAll">取消全选</NButton>
            <NButton block @click="handleCheckParentsWithChildren">选中父节点及子节点</NButton>
            <NButton block type="error" @click="handleClearPermissions">清空权限</NButton>
          </NSpace>
        </NCard>
      </NGi>
    </NGrid>

    <NCard title="菜单权限配置" style="margin-top: 16px;">
      <NTree
          ref="treeRef"
          :data="menuTreeData"
          :checked-keys="checkedKeys"
          :expanded-keys="expandedKeys"
          checkable
          @update:checked-keys="handleCheckUpdate"
          @update:expanded-keys="handleExpandUpdate"
      />
    </NCard>

    <NModal
        v-model:show="buttonPermissionVisible"
        preset="dialog"
        title="按钮权限配置"
        style="width: 600px"
    >
      <NForm>
        <NFormItem label="可选按钮权限">
          <NCheckboxGroup v-model:value="currentButtons">
            <NSpace item-style="display: flex;">
              <NCheckbox value="查看" />
              <NCheckbox value="新增" />
              <NCheckbox value="修改" />
              <NCheckbox value="删除" />
              <NCheckbox value="导出" />
              <NCheckbox value="导入" />
              <NCheckbox value="审核" />
              <NCheckbox value="推荐" />
              <NCheckbox value="置顶" />
            </NSpace>
          </NCheckboxGroup>
        </NFormItem>
      </NForm>

      <template #action>
        <NButton @click="buttonPermissionVisible = false">取消</NButton>
        <NButton type="primary" @click="handleSaveButtons">确定</NButton>
      </template>
    </NModal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { TreeOption } from 'naive-ui'
import { useMessage, useDialog, NTree, NCard, NDescriptions, NDescriptionsItem, NTag, NButton, NSpace, NGrid, NGi, NModal, NForm, NFormItem, NCheckboxGroup, NCheckbox } from 'naive-ui'
import { roleApi, type Role } from '@/api/role'
import { menuApi, type Menu } from '@/api/menu'
import { roleMenuApi, type RoleMenu } from '@/api/roleMenu'
import PageContainer from "src/components/layout/PageContainer.vue"

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()

const roleId = ref<number>(0)
const roleName = ref<string>('')
const roleInfo = ref<Role | null>(null)
const menuTreeData = ref<Menu[]>([])
const checkedKeys = ref<number[]>([])
const expandedKeys = ref<number[]>([])
const saving = ref(false)
const buttonPermissionVisible = ref(false)
const currentMenuId = ref<number | null>(null)
const currentButtons = ref<string[]>([])

const treeRef = ref<any>(null)

const goBack = () => {
  router.back()
}

const loadRoleInfo = async () => {
  try {
    const res = await roleApi.getRoleById(roleId.value)
    if (res.code === 0 || res.code === 200) {
      roleInfo.value = res.data
    }
  } catch (error) {
    console.error('加载角色信息失败', error)
  }
}

const loadMenuTree = async () => {
  try {
    const res = await menuApi.getMenuTree()
    if (res.code === 0 || res.code === 200) {
      menuTreeData.value = res.data
      // 初始化展开的节点为所有一级菜单
      expandedKeys.value = res.data
        .filter(m => m.parentId === 0)
        .map(m => m.id!)
    }
  } catch (error) {
    message.error('加载菜单树失败')
    console.error(error)
  }
}

const loadRolePermissions = async () => {
  try {
    const res = await roleMenuApi.getMenusByRole(roleId.value)
    if (res.code === 0 || res.code === 200) {
      checkedKeys.value = res.data.map(rm => rm.menuId)
    }
  } catch (error) {
    console.error('加载角色权限失败', error)
  }
}

const handleCheckUpdate = (keys: number[]) => {
  checkedKeys.value = keys
}

const handleExpandUpdate = (keys: number[]) => {
  expandedKeys.value = keys
}

const handleExpandAll = () => {
  const getAllKeys = (menus: Menu[]): number[] => {
    let keys: number[] = []
    menus.forEach(menu => {
      if (menu.id) keys.push(menu.id)
      if (menu.children) {
        keys = [...keys, ...getAllKeys(menu.children)]
      }
    })
    return keys
  }
  
  expandedKeys.value = getAllKeys(menuTreeData.value)
}

const handleCollapseAll = () => {
  expandedKeys.value = []
}

const handleCheckAll = () => {
  const getAllKeys = (menus: Menu[]): number[] => {
    let keys: number[] = []
    menus.forEach(menu => {
      if (menu.id) keys.push(menu.id)
      if (menu.children) {
        keys = [...keys, ...getAllKeys(menu.children)]
      }
    })
    return keys
  }
  
  checkedKeys.value = getAllKeys(menuTreeData.value)
}

const handleUncheckAll = () => {
  checkedKeys.value = []
}

const handleCheckParentsWithChildren = () => {
  // 选中所有有子节点的父节点及其子节点
  const keys: number[] = []
  
  const traverse = (menus: Menu[]) => {
    menus.forEach(menu => {
      if (menu.id) {
        keys.push(menu.id)
      }
      if (menu.children && menu.children.length > 0) {
        traverse(menu.children)
      }
    })
  }
  
  traverse(menuTreeData.value)
  checkedKeys.value = keys
}

const handleClearPermissions = () => {
  dialog.warning({
    title: '确认清空',
    content: '确定要清空该角色的所有权限吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await roleMenuApi.deleteRoleMenus(roleId.value)
        if (res.code === 0 || res.code === 200) {
          message.success('清空成功')
          checkedKeys.value = []
        } else {
          message.error(res.msg || '清空失败')
        }
      } catch (error) {
        message.error('清空失败')
        console.error(error)
      }
    }
  })
}

const handleSave = async () => {
  saving.value = true
  try {
    const res = await roleMenuApi.saveRoleMenus(roleId.value, checkedKeys.value)
    if (res.code === 0 || res.code === 200) {
      message.success('保存成功')
    } else {
      message.error(res.msg || '保存失败')
    }
  } catch (error) {
    message.error('保存失败')
    console.error(error)
  } finally {
    saving.value = false
  }
}

watch(route, (newRoute) => {
  const id = newRoute.query.id as string
  const name = newRoute.query.name as string
  
  if (id) {
    roleId.value = Number(id)
    roleName.value = name || '未知角色'
    loadRoleInfo()
    loadMenuTree()
    loadRolePermissions()
  }
}, { immediate: true })

onMounted(() => {
  const id = route.query.id as string
  const name = route.query.name as string
  
  if (id) {
    roleId.value = Number(id)
    roleName.value = name || '未知角色'
    loadRoleInfo()
    loadMenuTree()
    loadRolePermissions()
  } else {
    message.error('缺少角色 ID 参数')
  }
})
</script>

<style scoped>
:deep(.n-tree-node) {
  padding: 8px 12px;
}

:deep(.n-tree-node-content) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
