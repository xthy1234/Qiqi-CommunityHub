<template>
  <PageContainer
    :header-title="isEdit ? '编辑文章' : '创作文章'"
    :show-back="false"
  >
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="100px"
      class="article-form"
    >
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

      <n-form-item
        label=""
        path="content"
      >
        <RichTextEditor v-model="formData.content" />
      </n-form-item>

      <div
        v-if="isEdit && currentVersion"
        class="version-info"
      >
        <n-tag type="info" size="small">
          当前版本：{{ currentVersion }}
        </n-tag>
      </div>

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
          @click="handleShowPublishModal"
        >
          {{ isEdit ? '发布修改' : '立即发布' }}
        </n-button>
      </div>
    </n-form>

    <!-- 发布确认模态框 -->
    <n-modal
      v-model:show="showPublishModal"
      preset="dialog"
      title="发布文章"
      :closable="false"
      :mask-closable="false"
      :show-icon="false"
      style="width: 600px"
    >
      <n-form
        ref="publishFormRef"
        :model="publishData"
        :rules="publishRules"
        label-placement="left"
        label-width="90px"
      >
        <n-form-item
          label="封面图片"
          path="coverUrl"
        >
          <CoverUpload v-model="publishData.coverUrl" />
        </n-form-item>

        <n-form-item
          label="文章分类"
          path="categoryId"
        >
          <n-select
            v-model:value="publishData.categoryId"
            :options="categorySelectOptions"
            placeholder="请选择文章分类"
            clearable
          />
        </n-form-item>

        <n-form-item
          label="版本类型"
          path="versionType"
        >
          <n-radio-group v-model:value="publishData.versionType">
            <n-space>
              <n-radio :value="0">
                <div class="radio-content">
                  <div>普通更新（小版本）</div>
                  <div class="radio-tip">版本号递增：X.Y → X.(Y+1)</div>
                </div>
              </n-radio>
              <n-radio :value="1">
                <div class="radio-content">
                  <div>重大更新（大版本）</div>
                  <div class="radio-tip">版本号重置：X.Y → (X+1).0</div>
                </div>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-form-item
          label="更新说明"
          path="changeSummary"
        >
          <n-input
            v-model:value="publishData.changeSummary"
            type="textarea"
            placeholder="请简要描述本次更新内容（可选）"
            :rows="3"
            maxlength="200"
            show-count
          />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button
          size="medium"
          @click="showPublishModal = false"
        >
          取消
        </n-button>
        <n-button
          type="primary"
          size="medium"
          :loading="submitting"
          @click="handleConfirmPublish"
        >
          确认发布
        </n-button>
      </template>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, useDialog, type FormRules } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'
import { articleAPI, type Article } from '@/api/article'
import { draftAPI } from '@/api/draft'
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
const publishFormRef = ref<any>(null)
const isEdit = ref(false)
const articleId = ref<string | number>('')
const currentVersion = ref<string>('')
const submitting = ref(false)
const savingDraft = ref(false)
const autoSaveTimer = ref<NodeJS.Timeout | null>(null)
const categoryOptions = ref<any[]>([])
const showPublishModal = ref(false)

const formData = reactive({
  title: '',
  categoryId: '',
  coverUrl: '',
  content: { type: 'doc', content: [] } as { type: string; content: any[] }
})

// 发布表单数据
const publishData = reactive({
  coverUrl: '',
  categoryId: '',
  versionType: 0, // 0=小版本，1=大版本
  changeSummary: ''
})

// 监听文章内容变化
watch(() => formData.content, (newVal:object) => {


}, { deep: true })

// 同步发布表单的封面和分类与主表单
watch(() => formData.coverUrl, (newVal: string) => {
  publishData.coverUrl = newVal
})

watch(() => formData.categoryId, (newVal: string) => {
  publishData.categoryId = newVal
})

const categorySelectOptions = computed(() => {
  return categoryOptions.value.map((cat: any) => ({
    label: cat.categoryName,
    value: String(cat.id)  // 转换为字符串
  }))
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度应在 2 到 100 个字符之间', trigger: 'blur' }
  ],
  categoryId: [],
  content: []
}

// 发布表单验证规则
const publishRules: FormRules = {
  categoryId: [
    { required: true, message: '请选择文章分类', trigger: 'change' }
  ],
  versionType: [
    {
      message: '请选择版本类型',
      validator: (_rule: any, value: number) => {
        // 检查值是否为 0 或 1
        if (value !== 0 && value !== 1) {
          return new Error('请选择版本类型')
        }
        return true
      },
      trigger: 'change'
    }
  ]
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
    onPositiveClick: async () => {

      if (articleId.value) {

        try {
          await draftAPI.deleteDraft(articleId.value)
          appContext?.$toolUtil?.storageRemove('currentDraftId')

        } catch (error) {
          console.error('❌ [handleCancel] 删除草稿失败:', error)
        }
      }
      router.back()
    }
  })
}

// 监听路由变化，清理草稿缓存
watch(() => route.fullPath, (newPath: string, oldPath: string) => {
  if (newPath !== oldPath) {
    appContext?.$toolUtil?.storageRemove('currentDraftId')
  }
})

onUnmounted(() => {

  stopAutoSave()
  appContext?.$toolUtil?.storageRemove('currentDraftId')
})

const startAutoSave = () => {

  stopAutoSave()
  autoSaveTimer.value = setInterval(async () => {
    // 只有当有标题或有内容时才自动保存
    if (formData.title || (formData.content && formData.content.content?.length > 0)) {

      await handleAutoSave()
    } else {

    }
  }, 60000) // 60 秒自动保存一次
}

const stopAutoSave = () => {
  if (autoSaveTimer.value) {

    clearInterval(autoSaveTimer.value)
    autoSaveTimer.value = null
  }
}

const handleAutoSave = async () => {



  try {
    // 如果没有 articleId (即 draftId)，先创建草稿
    if (!articleId.value) {

      const created = await createNewDraft()

      if (!created || !articleId.value) {
        console.error('❌ [handleAutoSave] 创建草稿失败，无法进行自动保存')
        return
      }
    }


    // ✅ 修复：自动保存也传递完整数据（标题、内容、封面、分类）
    const response = await draftAPI.autoSaveDraft(articleId.value, {
      title: formData.title,
      content: formData.content,
      coverUrl: formData.coverUrl,
      categoryId: formData.categoryId
    })

    if (response.data.code === 200 || response.data.msg) {

      message.success('已自动保存')
    } else {
      console.error('❌ [handleAutoSave] 自动保存失败:', response.data.msg)
    }
  } catch (error) {
    console.error('❌ [handleAutoSave] 自动保存异常:', error)
  } finally {

  }
}

const createNewDraft = async () => {

  try {


    const response = await draftAPI.createDraft({
      title: '未命名草稿',
      content: {}
    })

    let draftId = null
    let hasDraft = false
    let returnedArticleId = null

    if (response.data?.code === 200 && response.data?.data) {
      draftId = response.data.data.draftId
      returnedArticleId = response.data.data.articleId
      hasDraft = response.data.data.hasDraft

    }
    else if (response.data?.code === 0 && response.data?.data) {
      draftId = response.data.data.draftId
      returnedArticleId = response.data.data.articleId
      hasDraft = response.data.data.hasDraft

    }
    else if (response.data?.data?.draftId) {
      draftId = response.data.data.draftId
      returnedArticleId = response.data.data.articleId
      hasDraft = response.data.data.hasDraft
    }

    if (draftId !== null && draftId !== undefined) {
      // ✅ 修复：使用局部变量存储，然后再赋值给 ref
      const draftIdStr = String(draftId)
      articleId.value = draftIdStr

      appContext?.$toolUtil?.storageSet('currentDraftId', draftIdStr)

      return true
    } else {
      console.error('❌ [createNewDraft] 创建草稿失败：无法解析响应数据')
      console.error('❌ [createNewDraft] 完整响应:', JSON.stringify(response, null, 2))
      return false
    }
  } catch (error: any) {
    console.error('❌ [createNewDraft] 创建草稿异常:', error)
    console.error('❌ [createNewDraft] error.name:', error.name)
    console.error('❌ [createNewDraft] error.message:', error.message)
    console.error('❌ [createNewDraft] error.stack:', error.stack)
    return false
  } finally {

  }
}

const handleSaveDraft = async () => {
  if (!formData.title) {
    console.warn('⚠️ [handleSaveDraft] 验证失败：缺少文章标题')
    message.warning('请输入文章标题')
    return
  }

  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !(formData.content.content[0] as any)?.content)

  if (isEmptyContent) {
    console.warn('⚠️ [handleSaveDraft] 验证失败：内容为空')
    message.warning('请输入文章内容')
    return
  }

  try {
    savingDraft.value = true

    if (!articleId.value) {
      const created = await createNewDraft()

      if (!created || !articleId.value) {
        console.error('❌ [handleSaveDraft] 创建草稿失败')
        message.error('创建草稿失败')
        return
      }
    }

    const response = await draftAPI.saveDraft(articleId.value, {
      title: formData.title,
      content: formData.content,
      coverUrl: formData.coverUrl,
      categoryId: formData.categoryId
    })

    if (response.data.code === 200 || response.data.msg) {
      const versionInfo = response.data.data || response.data
      currentVersion.value = versionInfo.versionDisplay || `${versionInfo.majorVersion}.${versionInfo.minorVersion}`

      message.success(`草稿已保存，版本：${currentVersion.value}`)

      appContext?.$toolUtil?.storageSet('currentDraftId', String(articleId.value))
    } else {
      console.error('❌ [handleSaveDraft] 保存失败:', response.data.msg)
      message.error(response.data.msg || '保存草稿失败')
    }
  } catch (error: unknown) {
    console.error('❌ [handleSaveDraft] 保存异常:', error)
    message.error('保存草稿失败，请重试')
  } finally {
    savingDraft.value = false
  }
}

// 显示发布模态框
const handleShowPublishModal = async () => {
  try {
    await formRef.value?.validate()

  } catch (errors) {
    console.error('❌ [handleShowPublishModal] 表单验证失败:', errors)
    message.warning('请填写完整的表单信息')
    return
  }

  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !(formData.content.content[0] as any)?.content)

  if (isEmptyContent) {
    console.warn('⚠️ [handleShowPublishModal] 验证失败：内容为空')
    message.warning('请输入文章内容')
    return
  }

  publishData.coverUrl = formData.coverUrl
  publishData.categoryId = formData.categoryId
  publishData.versionType = isEdit.value ? 0 : 1

  showPublishModal.value = true
}

// 确认发布
const handleConfirmPublish = async () => {


  try {
    await publishFormRef.value?.validate()

  } catch (errors) {
    console.error('❌ [handleConfirmPublish] 发布表单验证失败:', errors)
    return
  }

  if (!articleId.value) {
    console.error('❌ [handleConfirmPublish] 验证失败：articleId 不存在')
    message.error('文章 ID 不存在')
    return
  }

  try {
    submitting.value = true


    // ✅ 步骤 1: 先调用保存接口，保存草稿内容（标题、内容、封面、分类）




    const saveResponse = await draftAPI.saveDraft(articleId.value, {
      title: formData.title,
      content: formData.content,
      coverUrl: publishData.coverUrl,
      categoryId: publishData.categoryId,
      changeSummary: publishData.changeSummary || ''
    })


    // ✅ 检查保存是否成功
    if (!(saveResponse.data.code === 200 || saveResponse.data.msg)) {
      console.error('❌ [handleConfirmPublish] 保存草稿失败:', saveResponse.data.msg)
      message.error(saveResponse.data.msg || '保存草稿失败')
      return
    }

    const versionInfo = saveResponse.data.data || saveResponse.data






    // ✅ 步骤 2: 保存成功后，再调用发布接口




    const publishResponse = await draftAPI.publishDraft(articleId.value, {
      versionType: publishData.versionType,
      changeSummary: publishData.changeSummary || ''
    })


    if (publishResponse.data.code === 200 || publishResponse.data.msg) {
      const publishVersionInfo = publishResponse.data.data || publishResponse.data









      message.success(isEdit.value ? '修改成功' : '发布成功')

      // 清理草稿缓存

      appContext?.$toolUtil?.storageRemove('currentDraftId')

      showPublishModal.value = false

      setTimeout(() => {

        router.push('/index/articleList')
      }, 500)
    } else {
      console.error('❌ [handleConfirmPublish] 发布失败:', publishResponse.data.msg)
      message.error(publishResponse.data.msg || (isEdit.value ? '修改失败' : '发布失败'))
    }
  } catch (error) {
    console.error('❌ [handleConfirmPublish] 提交异常:', error)
    console.error('❌ [handleConfirmPublish] 错误详情:', error.response?.data || error.message)
    const errorMsg = error.response?.data?.message || error.message || (isEdit.value ? '修改失败' : '发布失败')
    message.error(errorMsg)
  } finally {
    submitting.value = false


  }
}

const handleSaveMajorVersion = async () => {


  if (!articleId.value) {
    console.error('❌ [handleSaveMajorVersion] 验证失败：articleId 不存在')
    message.error('文章 ID 不存在')
    return
  }

  dialog.warning({
    title: '标记大版本',
    content: '确定要将当前内容标记为大版本吗？（版本号将重置为 X.0）',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {

      try {
        savingDraft.value = true





        const response = await draftAPI.publishDraft(articleId.value, {
          versionType: 1,
          changeSummary: '手动标记大版本'
        })

        if (response.data.code === 200 || response.data.msg) {
          const versionInfo = response.data.data || response.data
          currentVersion.value = versionInfo.versionDisplay || `${versionInfo.majorVersion}.${versionInfo.minorVersion}`

          message.success(`大版本标记成功，当前版本：${currentVersion.value}`)

          setTimeout(() => {

            router.push('/index/articleList')
          }, 500)
        } else {
          console.error('❌ [handleSaveMajorVersion] 标记失败:', response.data.msg)
          message.error(response.data.msg || '标记大版本失败')
        }
      } catch (error) {
        console.error('❌ [handleSaveMajorVersion] 标记异常:', error)
        message.error('标记大版本失败，请重试')
      } finally {
        savingDraft.value = false


      }
    }
  })
}

const handleSubmit = async () => {

  try {
    await formRef.value?.validate()

  } catch (errors) {
    console.error('❌ [handleSubmit] 表单验证失败:', errors)
    message.warning('请填写完整的表单信息')
    return
  }

  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !(formData.content.content[0] as any)?.content)

  if (isEmptyContent) {
    console.warn('⚠️ [handleSubmit] 验证失败：内容为空')
    message.warning('请输入文章内容')
    return
  }


  try {
    submitting.value = true



    if (!articleId.value) {
      console.error('❌ [handleSubmit] 验证失败：articleId 不存在')
      message.error('文章 ID 不存在')
      return
    }




    const response = await draftAPI.publishDraft(articleId.value, {
      versionType: 0,
      changeSummary: ''
    })


    if (response.data.code === 200 || response.data.msg) {
      const versionInfo = response.data.data || response.data

      message.success(isEdit.value ? '修改成功' : '发布成功')

      appContext?.$toolUtil?.storageRemove('currentDraftId')

      setTimeout(() => {

        router.push('/index/articleList')
      }, 500)
    } else {
      console.error('❌ [handleSubmit] 发布失败:', response.data.msg)
      message.error(response.data.msg || (isEdit.value ? '修改失败' : '发布失败'))
    }
  } catch (error: unknown) {
    console.error('❌ [handleSubmit] 提交异常:', error)
    console.error('❌ [handleSubmit] 错误详情:', (error as any).response?.data || (error as any).message)
    const errorMsg = (error as any).response?.data?.message || (error as any).message || (isEdit.value ? '修改失败' : '发布失败')
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
    console.error('❌ [loadCategoryOptions] 加载分类失败:', error)
  }
}

const loadArticleDetail = async () => {
  const id = route.query.id
  if (!id) return

  try {
    const response = await appContext?.$http.get(`/articles/${id}`)
    const data = response.data.data

    if (data) {
      let contentJson = data.content
      if (typeof data.content === 'string') {
        contentJson = htmlToContentJson(data.content)
      }

      Object.assign(formData, {
        title: data.title || '',
        categoryId: String(data.categoryId) || '',
        coverUrl: data.coverUrl || '',
        content: contentJson
      })

      articleId.value = id
    }
  } catch (error) {
    console.error('加载文章详情失败:', error)
    message.error('获取文章信息失败')
  }
}

const loadOrCreateDraft = async (draftId?: string | number, articleIdParam?: string | number): Promise<void> => {
  const passedArticleId = articleIdParam

  const id = draftId || articleId.value






  if (passedArticleId && !draftId) {

    try {
      const response = await draftAPI.createDraft({ articleId: passedArticleId })


      const data = response.data.data || response.data
      const createdDraftId = data.draftId

      if (createdDraftId) {

        articleId.value = String(createdDraftId)

        return loadOrCreateDraft(String(createdDraftId), passedArticleId)
      } else {
        console.error('❌ [loadOrCreateDraft] 未获取到草稿 ID')
        message.error('无法创建草稿，请重试')
        return
      }
    } catch (error: unknown) {
      console.error('❌ [loadOrCreateDraft] 检查/创建草稿失败:', error)
      console.error('❌ [loadOrCreateDraft] 错误详情:', (error as any).response?.data || (error as any).message)
      message.error('加载草稿失败')
      return
    }
  }

  if (!id) {

    return
  }

  try {
    articleId.value = id as string


    const response = await draftAPI.getDraft(id)
    const data = response.data.data || response.data

    if (data) {




      const versionDisplay = data.versionDisplay ||
                            (data.majorVersion !== undefined ? `${data.majorVersion}.${data.minorVersion || 0}` : '1.0')


      const draft = data.draft
      let contentJson = data.content || draft?.content


      if (typeof contentJson === 'string') {

        contentJson = htmlToContentJson(contentJson)
      }
      else if (!contentJson ||
               (typeof contentJson === 'object' && Object.keys(contentJson).length === 0) ||
               !contentJson.type) {

        contentJson = { type: 'doc', content: [] }
      }



      const coverUrl = draft?.coverUrl || ''
      const categoryId = String(draft?.categoryId) || ''

      Object.assign(formData, {
        title: data.title || draft?.title || '',
        categoryId: categoryId,
        coverUrl: coverUrl,
        content: contentJson
      })

      Object.assign(publishData, {
        coverUrl: coverUrl,
        categoryId: categoryId,
        versionType: 0,
        changeSummary: ''
      })

      currentVersion.value = versionDisplay
      if (!isEdit.value && (data.title || draft?.title)) {
        isEdit.value = true

      }
      startAutoSave()
    } else {
      console.warn('⚠️ [loadOrCreateDraft] 草稿数据为空')
    }
  } catch (error: unknown) {
    console.error('❌ [loadOrCreateDraft] 加载草稿失败:', error)
    console.error('❌ [loadOrCreateDraft] 错误详情:', (error as any).response?.data || (error as any).message)
    message.error('获取草稿信息失败')
  } finally {

  }
}

const createNewDraftImmediately = async () => {


  try {
    const created = await createNewDraft()

    if (created && articleId.value) {

      startAutoSave()
    } else {
      console.error('❌ [createNewDraftImmediately] 草稿创建失败')
      message.error('创建草稿失败，请刷新页面重试')
    }
  } catch (error) {
    console.error('❌ [createNewDraftImmediately] 创建草稿异常:', error)
    message.error('创建草稿失败，请刷新页面重试')
  } finally {

  }
}

onMounted(() => {




  isEdit.value = !!route.query.id


  loadCategoryOptions()

  if (route.query.id) {

    const articleIdParam = Number(route.query.id)
    loadOrCreateDraft(undefined, isNaN(articleIdParam) ? route.query.id : articleIdParam)
  }
  else if (route.query.draftId) {

    loadOrCreateDraft(route.query.draftId)
  }
  else {
    const savedDraftId = appContext?.$toolUtil?.storageGet('currentDraftId')


    if (savedDraftId && savedDraftId !== '' && savedDraftId !== 'null' && savedDraftId !== 'undefined') {
      articleId.value = savedDraftId


      loadOrCreateDraft(savedDraftId)
    } else {

      createNewDraftImmediately()
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

    .version-info {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
      padding: 12px;
      background: #f0faff;
      border-radius: 4px;
      border-left: 3px solid #18a058;
    }

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

.radio-content {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .radio-tip {
    font-size: 12px;
    color: #999;
  }
}
</style>
