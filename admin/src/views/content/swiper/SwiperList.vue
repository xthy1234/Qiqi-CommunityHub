<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <PageHeader
      title="轮播图管理"
      @back="goBack"
    >
      <template #extra>
        <NSpace>
          <NButton type="success" @click="handleCreate">
            <template #icon>
              <Icon icon="ri:add-line" />
            </template>
            新建轮播图
          </NButton>
          <NButton
            type="error"
            @click="handleBatchDelete"
            :disabled="checkedRowKeys.length === 0"
          >
            <template #icon>
              <Icon icon="ri:delete-bin-line" />
            </template>
            批量删除
          </NButton>
        </NSpace>
      </template>
    </PageHeader>

    <!-- 主内容区域 -->
    <div class="page-content">
      <!-- 搜索栏 -->
      <SearchBar @search="handleSearch" @reset="handleReset">
        <NInput
            v-model:value="searchForm.title"
            placeholder="请输入轮播图标题"
            clearable
            style="width: 250px"
        />
        <NSelect
            v-model:value="searchForm.status"
            placeholder="状态"
            :options="statusOptions"
            clearable
            style="width: 120px"
        />
      </SearchBar>

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

      <!-- 新增/编辑对话框 -->
      <CrudDialog
        v-model:visible="editDialogVisible"
        :is-edit="isEdit"
        :form-data="formData"
        :form-rules="formRules"
        :create-title="'新建轮播图'"
        :edit-title="'编辑轮播图'"
        width="700px"
        @submit="handleSubmit"
        @cancel="handleDialogCancel"
        @after-leave="handleDialogAfterLeave"
      >
        <template #form-content>
          <NFormItem label="轮播图标题" path="title">
            <NInput
                v-model:value="formData.title"
                placeholder="请输入轮播图标题"
            />
          </NFormItem>

          <NFormItem label="轮播图片" path="imageUrl">
            <div style="display: flex; gap: 12px; align-items: center;">
              <NUpload
                  :action="uploadUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  accept="image/*"
                  @finish="handleImageUploadFinish"
              >
                <NButton>选择图片</NButton>
              </NUpload>
              <span v-if="formData.imageUrl" style="color: #999; font-size: 13px;">
                {{ getFileName(formData.imageUrl) }}
              </span>
            </div>
            <div v-if="previewVisible" class="image-preview-wrapper">
              <NImage
                  :src="getFullImageUrl(formData.imageUrl)"
                  width="200"
                  object-fit="cover"
              />
            </div>
          </NFormItem>

          <NFormItem label="跳转链接" path="linkUrl">
            <NInput
                v-model:value="formData.linkUrl"
                placeholder="请输入点击跳转的链接（可选）"
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
              <NRadioButton :value="1" label="显示" />
              <NRadioButton :value="0" label="隐藏" />
            </NRadioGroup>
          </NFormItem>

          <NFormItem label="描述信息" path="description">
            <NInput
                v-model:value="formData.description"
                type="textarea"
                placeholder="请输入轮播图描述（可选）"
                :rows="3"
            />
          </NFormItem>
        </template>
      </CrudDialog>

      <!-- 图片预览弹窗 -->
      <NModal
          v-model:show="previewModalVisible"
          preset="dialog"
          title="图片预览"
          :show-icon="false"
          :closable="true"
          style="width: 800px;"
      >
        <div style="text-align: center;">
          <NImage
              :src="currentPreviewUrl"
              width="100%"
              object-fit="contain"
          />
        </div>
      </NModal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import type { DataTableColumns, FormRules, FormInst, UploadFileInfo } from 'naive-ui'
import { NButton, NTag, NSpace, useMessage, useDialog, NModal, NForm, NFormItem, NInput, NUpload, NImage, NInputNumber, NRadioGroup, NRadioButton } from 'naive-ui'
import { swiperApi, type SwiperVO, type SwiperCreateDTO, type SwiperUpdateDTO } from '@/api/swiper'
import { uploadAPI } from '@/api/upload'
import SearchBar from "src/components/form/SearchBar.vue"
import StatusTag from "@/components/display/StatusTag.vue"
import CrudDialog from "src/components/form/CrudDialog.vue"
import PageHeader from "src/components/layout/PageHeader.vue"
import { commonStatusConfigs, imageUploadHelper } from '@/utils/componentHelpers'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const searchForm = ref({
  title: '',
  status: null as number | null
})

// 使用预设的状态配置（适配数字格式，0=启用，1=禁用）
const statusOptions = commonStatusConfigs.enableDisable

const loading = ref(false)
const tableData = ref<SwiperVO[]>([])
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
const previewVisible = ref(false)
const previewModalVisible = ref(false)
const currentPreviewUrl = ref('')

// 表单数据
const formData = ref<SwiperCreateDTO & { id?: number }>({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sort: 1,
  status: 0,
  description: ''
})

// 上传配置
const backendUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
const uploadUrl = `${backendUrl}/files`
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('Token')
  return {
    'Authorization': `Bearer ${token}`
  }
})

const formRules: FormRules = {
  title: {
    required: true,
    message: '请输入轮播图标题',
    trigger: ['blur', 'change']
  },
  imageUrl: {
    required: true,
    message: '请上传轮播图片',
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
    title: '标题',
    key: 'title',
    width: 200,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '图片',
    key: 'imageUrl',
    width: 150,
    render: (row) => {
      return h('div', {
        style: { cursor: 'pointer' },
        onClick: () => handlePreview(row.imageUrl)
      }, [
        h(NImage, {
          src: getFullImageUrl(row.imageUrl),
          width: 100,
          height: 60,
          objectFit: 'cover',
          style: { borderRadius: '4px' }
        })
      ])
    }
  },
  {
    title: '跳转链接',
    key: 'linkUrl',
    width: 200,
    ellipsis: {
      tooltip: true
    },
    render: (row) => {
      return row.linkUrl ? h('a', {
        href: row.linkUrl,
        target: '_blank',
        style: { color: '#18a058' }
      }, {
        default: () => row.linkUrl
      }) : '-'
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
    // 使用 StatusTag 组件
    render: (row) => {
      return h(StatusTag, {
        status: row.status,
        options: commonStatusConfigs.enableDisable
      })
    }
  },
  {
    title: '描述',
    key: 'description',
    width: 200,
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
            default: () => row.status === 0 ? '隐藏' : '显示'
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
    const params = {
      page: pagination.page,
      limit: pagination.pageSize,
      title: searchForm.value.title || undefined,
      status: searchForm.value.status !== null ? searchForm.value.status : undefined
    }

    const res = await swiperApi.getSwiperList(params)

    if (res && (res.code === 0 || res.code === 200)) {
      tableData.value = res.data.list
      pagination.itemCount = res.data.totalCount

      if (tableData.value.length > 0) {

        console.table(tableData.value[0])
      }
    } else {
      console.error('[SwiperList] 加载失败 - 错误码:', res?.code)
      console.error('[SwiperList] 错误信息:', res?.msg)
      message.error(res?.msg || '加载失败')
    }
  } catch (error) {
    console.error('[SwiperList] 加载异常:', error)
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

  searchForm.value.title = ''
  searchForm.value.status = null
  pagination.page = 1
  loadData()
}

const handleCreate = () => {

  isEdit.value = false
  formData.value = {
    title: '',
    imageUrl: '',
    linkUrl: '',
    sort: 1,
    status: 0,
    description: ''
  }
  editDialogVisible.value = true
}

const handleEdit = (row: SwiperVO) => {
  isEdit.value = true
  formData.value = {
    id: row.id,
    title: row.title,
    imageUrl: row.imageUrl,
    linkUrl: row.linkUrl,
    sort: row.sort,
    status: row.status,
    description: row.description
  }
  editDialogVisible.value = true
}

const handleToggleStatus = async (row: SwiperVO) => {
  const newStatus = row.status === 0 ? 1 : 0


  try {
    // 尝试使用 patch 方法

    const res = await swiperApi.updateSwiperStatus(row.id, newStatus)


    if (res && (res.code === 0 || res.code === 200)) {
      message.success('更新成功')
      loadData()
    } else {
      console.error('[SwiperList] 状态更新失败 - 错误码:', res?.code)
      console.error('[SwiperList] 错误信息:', res?.msg)
      message.error(res?.msg || '更新失败')
    }
  } catch (error) {
    console.error('[SwiperList] 状态更新异常:', error)

    // 如果是 CORS 错误，尝试使用 PUT 全量更新
    if (error.message.includes('CORS') || error.message.includes('Network Error')) {
      console.warn(' [SwiperList] CORS 错误，尝试使用全量更新...')
      try {
        const updateData = {
          title: row.title,
          imageUrl: row.imageUrl,
          linkUrl: row.linkUrl,
          sort: row.sort,
          status: newStatus,
          description: row.description
        }

        const res = await swiperApi.updateSwiper(row.id, updateData)

        if (res && (res.code === 0 || res.code === 200)) {
          message.success('更新成功')
          loadData()
        } else {
          message.error(res?.msg || '更新失败')
        }
      } catch (putError) {
        console.error('[SwiperList] PUT 更新也失败了:', putError)
        message.error('更新失败，可能是后端 CORS 配置问题')
      }
    } else {
      message.error('更新失败')
    }
  }
}

const handleDelete = (row: SwiperVO) => {


  dialog.warning({
    title: '确认删除',
    content: `确定要删除轮播图 "${row.title}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {


      try {
        const res = await swiperApi.deleteSwiper(row.id)


        if (res && (res.code === 0 || res.code === 200)) {
          message.success('删除成功')
          loadData()
        } else {
          console.error('[SwiperList] 删除失败 - 错误码:', res?.code)
          console.error('[SwiperList] 错误信息:', res?.msg)
          message.error(res?.msg || '删除失败')
        }
      } catch (error) {
        console.error('[SwiperList] 删除异常:', error)
        message.error('删除失败')
      }
    },
    onNegativeClick: () => {

    }
  })
}

const handleBatchDelete = () => {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请选择要删除的轮播图')
    return
  }


  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 条轮播图吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {


      try {
        const res = await swiperApi.batchDeleteSwipers(checkedRowKeys.value)


        if (res && (res.code === 0 || res.code === 200)) {
          message.success('批量删除成功')
          checkedRowKeys.value = []
          loadData()
        } else {
          console.error('[SwiperList] 批量删除失败 - 错误码:', res?.code)
          console.error('[SwiperList] 错误信息:', res?.msg)
          message.error(res?.msg || '批量删除失败')
        }
      } catch (error) {
        console.error('[SwiperList] 批量删除异常:', error)
        message.error('批量删除失败')
      }
    }
  })
}

// 处理提交 - 由 CrudDialog 内部触发
const handleSubmit = async (validatedData: Record<string, any>) => {


  submitting.value = true
  try {
    // 修复：使用 undefined 而不是 null，让 Axios 自动忽略这些字段
    const submitData: SwiperCreateDTO | SwiperUpdateDTO = {
      title: validatedData.title,
      imageUrl: validatedData.imageUrl,
      linkUrl: validatedData.linkUrl || undefined,  // ← 改为 undefined
      sort: validatedData.sort,
      status: validatedData.status,
      description: validatedData.description || undefined  // ← 改为 undefined
    }


    let res
    if (isEdit.value && formData.value.id) {

      res = await swiperApi.updateSwiper(formData.value.id, submitData as SwiperUpdateDTO)
    } else {

      res = await swiperApi.createSwiper(submitData as SwiperCreateDTO)
    }


    if (res && (res.code === 0 || res.code === 200)) {
      message.success(isEdit.value ? '更新成功' : '创建成功')
      editDialogVisible.value = false
      loadData()
    } else {
      console.error('[SwiperList] 操作失败 - 错误码:', res?.code)
      console.error('[SwiperList] 错误信息:', res?.msg)
      message.error(res?.msg || '操作失败')
    }
  } catch (error: any) {
    console.error('[SwiperList] 操作异常:', error)

    // 如果是 400 错误，打印详细的错误信息
    if (error.response) {
      console.error('[SwiperList] 错误响应状态:', error.response.status)
      console.error('[SwiperList] 错误响应数据:', error.response.data)
      console.error('[SwiperList] 请求的 URL:', error.config?.url)
      console.error('[SwiperList] 请求的数据:', JSON.stringify(error.config?.data))
    }

    message.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 处理对话框取消
const handleDialogCancel = () => {

}

// 处理对话框关闭后
const handleDialogAfterLeave = () => {

}

const handlePreview = (imageUrl: string) => {
  currentPreviewUrl.value = getFullImageUrl(imageUrl)
  previewModalVisible.value = true
}

// 使用工具函数
const getFullImageUrl = imageUploadHelper.getFullImageUrl
const getFileName = imageUploadHelper.getFileName

const handleCheckAll = (keys: any) => {
  checkedRowKeys.value = keys as number[]
}

onMounted(() => {


  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding: 24px;

  .page-content {
    max-width: 1200px;
    margin: 0 auto;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 30px;

    @media (max-width: 768px) {
      padding: 20px;
      border-radius: 8px;
    }
  }
}
</style>
