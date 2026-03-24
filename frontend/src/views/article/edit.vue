<template>
  <PageContainer
    :header-title="isEdit ? '编辑文章' : '创作文章'"
    :show-back="false"
  >
    <!-- 表单区域 -->
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="100px"
      class="article-form"
    >
      <!-- 文章标题 -->
      <n-form-item
        label="文章标题"
        path="title"
      >
        <n-input
          v-model:value="formData.title"
          placeholder="请输入文章标题"
          maxlength="100"
          show-count
          clearable
        />
      </n-form-item>

      <!-- 分类选择 -->
      <n-form-item
        label="文章分类"
        path="categoryId"
      >
        <n-select
          v-model:value="formData.categoryId"
          :options="categorySelectOptions"
          placeholder="请选择文章分类"
          clearable
        />
      </n-form-item>

      <!-- 封面上传 -->
      <n-form-item
        label="封面图片"
        path="coverUrl"
      >
        <CoverUpload v-model="formData.coverUrl" />
      </n-form-item>

      <!-- 文章内容编辑器 -->
      <n-form-item
        label=""
        path="content"
      >
        <RichTextEditor v-model="formData.content" />
      </n-form-item>

      <!-- 发布时间（可选） -->
      <n-form-item
        label="发布时间"
        path="publishTime"
      >
        <n-date-picker
          v-model:value="formData.publishTime"
          type="datetime"
          placeholder="选择发布时间"
          format="yyyy-MM-dd HH:mm:ss"
          value-format="yyyy-MM-dd HH:mm:ss"
          style="width: 100%"
          clearable
          :default-value="null"
        />
      </n-form-item>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <n-button
          size="large"
          @click="handleCancel"
        >
          取消
        </n-button>
        <n-button
          size="large"
          :loading="savingDraft"
          @click="handleSaveDraft"
        >
          保存草稿
        </n-button>
        <n-button
          type="primary"
          size="large"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ isEdit ? '保存修改' : '立即发布' }}
        </n-button>
      </div>
    </n-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, useDialog, type FormRules } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'
import { articleAPI, type Article } from '@/api/article'
import CoverUpload from '@/components/upload/CoverUpload.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import PageContainer from "@/components/common/PageContainer.vue";
import { generateHTML, generateJSON } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'

const appContext = useGlobalProperties()
const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

// 将 HTML 转换为 Tiptap JSON 格式的辅助函数
const htmlToContentJson = (html: string) => {
  const extensions = [
    StarterKit.configure({
      heading: { levels: [2, 3] },
      codeBlock: false,
      link: false,
      underline: false
    }),
    Image,
    Link,
    TextAlign,
    Underline
  ]

  try {
    const json = generateJSON(html, extensions)
    return json
  } catch (error) {
    console.error('HTML 转 JSON 失败:', error)
    return { type: 'doc', content: [] }
  }
}

const formRef = ref<any>(null)
const isEdit = ref(false)
const isDraftMode = ref(false)
const draftId = ref('')
const submitting = ref(false)
const savingDraft = ref(false)
const categoryOptions = ref<any[]>([])

const formData = reactive({
  title: '',
  categoryId: '',
  coverUrl: '',
  content: { type: 'doc', content: [] },
  publishTime: undefined as string | undefined | null
})

// 监听文章内容变化
watch(() => formData.content, (newVal:object) => {
  // console.log('=== formData.content 变化 ===')
  // console.log('新值:', JSON.stringify(newVal, null, 2))
  // console.log('类型:', typeof newVal)
  // console.log('是否为对象:', typeof newVal === 'object')
}, { deep: true })

const categorySelectOptions = computed(() => {
  return categoryOptions.value.map((cat: any) => ({
    label: cat.categoryName,
    value: String(cat.id)  // 转换为字符串
  }))
})
// value: cat.id
const rules: FormRules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度应在 2 到 100 个字符之间', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择文章分类', trigger: 'change' }
  ],
  content: []
}

const navigateToHome = () => {
  router.push('/index/home')
}

const handleCancel = () => {
  dialog.warning({
    title: '提示',
    content: '确定要取消吗？未保存的内容将丢失',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      appContext?.$toolUtil?.storageRemove('currentDraftId')
      router.back()
    }
  })
}

// 监听路由变化，清理草稿缓存
watch(() => route.fullPath, (newPath: string, oldPath: string) => {
  if (newPath !== oldPath) {
    // 路由变化时，清理当前草稿 ID 缓存
    appContext?.$toolUtil?.storageRemove('currentDraftId')
  }
})

onUnmounted(() => {
  // 组件卸载时也清理缓存
  appContext?.$toolUtil?.storageRemove('currentDraftId')
})

const handleSaveDraft = async ()  =>  {
  if (!formData.title) {
    message.warning('请输入文章标题')
    return
  }

  if (!formData.categoryId) {
    message.warning('请选择文章分类')
    return
  }

  // 检查内容是否为空
  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !formData.content.content[0]?.content)

  if (isEmptyContent) {
    message.warning('请输入文章内容')
    return
  }

  try {
    savingDraft.value = true
    let response

    if (draftId.value) {

      response = await articleAPI.updateDraft(draftId.value, {
        title: formData.title,
        content: formData.content,
        categoryId: formData.categoryId,
        coverUrl: formData.coverUrl || undefined,
        attachment: null
      })

      if (response.data.code === 0 || response.data.success) {

        message.success('草稿已更新')
      } else {
        message.error(response.data.message || '更新草稿失败')
      }
    } else {
      response = await articleAPI.createDraft({
        title: formData.title,
        content: formData.content,
        categoryId: formData.categoryId,
        coverUrl: formData.coverUrl || undefined,
        attachment: null
      })

      if (response.data.code === 0 || response.data.success) {
        const newDraftId = response.data.data?.id || response.data.id

        if (newDraftId) {
          draftId.value = String(newDraftId)
          isDraftMode.value = true
          appContext?.$toolUtil?.storageSet('currentDraftId', String(newDraftId))
          message.success('草稿保存成功，可以继续完善或提交审核')
          router.replace({
            path: '/index/article/editor',
            query: { draftId: String(newDraftId) }
          })
        } else {
          message.error('保存草稿失败：未获取到草稿 ID')
        }
      } else {
        message.error(response.data.message || '保存草稿失败')
      }
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    message.error('保存草稿失败，请重试')
  } finally {
    savingDraft.value = false
  }
}

const handleSubmit = async () => {
  console.log('=== 点击立即发布 ===')
  console.log('formData:', JSON.stringify(formData, null, 2))

  try {
    await formRef.value?.validate()
    console.log('表单验证通过')
  } catch (errors) {
    console.error('表单验证失败:', errors)
    message.warning('请填写完整的表单信息')
    return
  }

  // 手动检查内容是否为空
  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !formData.content.content[0]?.content)

  if (isEmptyContent) {
    message.warning('请输入文章内容')
    return
  }

  try {
    submitting.value = true
    console.log('开始提交...')

    const url = isEdit.value ? `/articles/${route.query.id}` : '/articles'
    const method = isEdit.value ? 'put' : 'post'

    console.log('请求 URL:', url)
    console.log('请求方法:', method)
    console.log('请求数据:', JSON.stringify(formData, null, 2))

    const response = await appContext?.$http[method](url, formData)

    console.log('响应数据:', response)

    if (response.data.code === 0 || response.data.success) {
      message.success(isEdit.value ? '修改成功' : '发布成功')

      setTimeout(() => {
        router.push('/index/articleList')
      }, 500)
    } else {
      message.error(response.data.message || (isEdit.value ? '修改失败' : '发布失败'))
    }
  } catch (error) {
    console.error('提交失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    const errorMsg = error.response?.data?.message || error.message || (isEdit.value ? '修改失败' : '发布失败')
    message.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

const loadCategoryOptions = async () => {
  try {
    const response = await appContext?.$http.get('/categories/enabled')
    categoryOptions.value = response.data.data || []
  } catch (error) {
    console.error('加载分类选项失败:', error)
  }
}

const loadArticleDetail = async () => {
  if (!route.query.id) {return}

  try {
    const response = await appContext?.$http.get(`/articles/${route.query.id}`)
    const data = response.data.data

    if (data) {
      // 判断 content 是 HTML 还是 JSON 格式
      let contentJson = data.content
      if (typeof data.content === 'string') {
        // 如果是 HTML 字符串，转换为 Tiptap JSON 格式
        contentJson = htmlToContentJson(data.content)
      }

      Object.assign(formData, {
        title: data.title || '',
        categoryId: String(data.categoryId) || '',
        coverUrl: data.coverUrl || '',
        content: contentJson,
        publishTime: data.publishTime || null
      })
    }
  } catch (error) {
    console.error('加载文章详情失败:', error)
    message.error('获取文章信息失败')
  }
}

const loadDraftDetail = async () => {
  const id = route.query.draftId || route.query.id

  if (!id) {return}

  try {
    const response = await articleAPI.getDraftById(id)
    const data = response.data.data

    if (data) {
      draftId.value = data.id || id
      isDraftMode.value = true

      // 判断 content 是 HTML 还是 JSON 格式
      let contentJson = data.content
      if (typeof data.content === 'string') {
        // 如果是 HTML 字符串，转换为 Tiptap JSON 格式
        contentJson = htmlToContentJson(data.content)
      }

      Object.assign(formData, {
        title: data.title || '',
        categoryId: String(data.categoryId) || '',
        coverUrl: data.coverUrl || '',
        content: contentJson,
        publishTime: data.publishTime || null
      })
    }
  } catch (error) {
    console.error('加载草稿详情失败:', error)
    message.error('获取草稿信息失败')
  }
}

onMounted(() => {
  isEdit.value = !!route.query.id
  isDraftMode.value = !!route.query.draftId
  loadCategoryOptions()

  if (isEdit.value) {
    loadArticleDetail()
  }

  if (route.query.draftId) {
    draftId.value = String(route.query.draftId)
    loadDraftDetail()
  }

  if (!route.query.draftId) {
    const savedDraftId = appContext?.$toolUtil?.storageGet('currentDraftId')
    if (savedDraftId) {
      draftId.value = savedDraftId
      isDraftMode.value = true
    }
  }
})
</script>

<style lang="scss" scoped>
.article-editor-container {
  padding: 20px 10%;
  background: #fff;
  min-height: calc(100vh - 60px);

  .breadcrumb_view {
    margin: 10px auto 20px;
  }

  .article-form {
    background: #f9f9f9;
    padding: 30px;
    border-radius: 8px;

    .form-actions {
      display: flex;
      justify-content: center;
      gap: 20px;
      margin-top: 40px;
      padding-top: 20px;
      border-top: 1px solid #ebeef5;

      .n-button {
        min-width: 120px;
      }
    }
  }
}
</style>
