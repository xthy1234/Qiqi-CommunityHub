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

      <n-form-item
        label="封面图片"
        path="coverUrl"
      >
        <CoverUpload v-model="formData.coverUrl" />
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
        <n-button
          size="small"
          quaternary
          @click="handleSaveMajorVersion"
        >
          标记为大版本
        </n-button>
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
const isEdit = ref(false)
const articleId = ref<string | number>('')
const currentVersion = ref<string>('')
const submitting = ref(false)
const savingDraft = ref(false)
const autoSaveTimer = ref<NodeJS.Timeout | null>(null)
const categoryOptions = ref<any[]>([])

const formData = reactive({
  title: '',
  categoryId: '',
  coverUrl: '',
  content: { type: 'doc', content: [] }
})

// 监听文章内容变化
watch(() => formData.content, (newVal:object) => {
  console.log('📝 [Watcher] 编辑器内容发生变化')
  console.log('📦 [Watcher] 新内容:', newVal)
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
  console.log('❌ [handleCancel] 用户点击取消按钮')
  dialog.warning({
    title: '提示',
    content: '确定要取消吗？未保存的内容将丢失',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      console.log('✅ [handleCancel] 用户确认取消')
      if (articleId.value) {
        console.log('🗑️ [handleCancel] 准备删除草稿，articleId:', articleId.value)
        try {
          await draftAPI.deleteDraft(articleId.value)
          appContext?.$toolUtil?.storageRemove('currentDraftId')
          console.log('✅ [handleCancel] 草稿删除成功')
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
  console.log('🧹 [onUnmounted] 组件销毁，清理定时器和缓存')
  stopAutoSave()
  appContext?.$toolUtil?.storageRemove('currentDraftId')
})

const startAutoSave = () => {
  console.log('⏱️ [startAutoSave] 启动自动保存定时器')
  stopAutoSave()
  autoSaveTimer.value = setInterval(async () => {
    // 只有当有标题或有内容时才自动保存
    if (formData.title || (formData.content && formData.content.content?.length > 0)) {
      console.log('🔄 [startAutoSave] 触发自动保存')
      await handleAutoSave()
    } else {
      console.log('⏭️ [startAutoSave] 跳过自动保存（无内容）')
    }
  }, 60000) // 60 秒自动保存一次
}

const stopAutoSave = () => {
  if (autoSaveTimer.value) {
    console.log('⏹️ [stopAutoSave] 停止自动保存定时器')
    clearInterval(autoSaveTimer.value)
    autoSaveTimer.value = null
  }
}

const handleAutoSave = async () => {
  console.log('💾 [handleAutoSave] === 开始自动保存 ===')
  console.log('🔑 [handleAutoSave] 当前 articleId:', articleId.value)

  try {
    // 如果没有 articleId (即 draftId)，先创建草稿
    if (!articleId.value) {
      console.log('📝 [handleAutoSave] 未检测到草稿 ID，正在创建新草稿...')
      const created = await createNewDraft()
      console.log('📋 [handleAutoSave] 草稿创建结果:', created ? '成功' : '失败')
      if (!created || !articleId.value) {
        console.error('❌ [handleAutoSave] 创建草稿失败，无法进行自动保存')
        return
      }
    }

    console.log('📦 [handleAutoSave] 准备保存的数据:')
    console.log('   - title:', formData.title)
    console.log('   - content 长度:', JSON.stringify(formData.content).length, '字符')

    const response = await draftAPI.autoSaveDraft(articleId.value, {
      content: formData.content,
      title: formData.title
    })

    if (response.data.code === 200 || response.data.msg) {
      console.log('✅ [handleAutoSave] 自动保存成功')
    } else {
      console.error('❌ [handleAutoSave] 自动保存失败:', response.data.msg)
    }
  } catch (error) {
    console.error('❌ [handleAutoSave] 自动保存异常:', error)
  } finally {
    console.log('🏁 [handleAutoSave] === 自动保存结束 ===')
  }
}

const createNewDraft = async () => {
  console.log('🆕 [createNewDraft] === 开始创建新草稿 ===')
  try {
    console.log('📦 [createNewDraft] 请求参数:', {
      title: '未命名草稿',
      content: {}
    })

    console.log('🔍 [createNewDraft] 调用 draftAPI.createDraft...')
    const response = await draftAPI.createDraft({
      title: '未命名草稿',
      content: {}
    })

    console.log('📥 [createNewDraft] API 响应:', response)
    console.log('📥 [createNewDraft] response.data:', response.data)
    console.log('📥 [createNewDraft] response.data.code:', response.data?.code)
    console.log('📥 [createNewDraft] response.data.data:', response.data?.data)
    console.log('📥 [createNewDraft] response.data.draftId:', response.data?.draftId)
    console.log('📥 [createNewDraft] response.data.message:', response.data?.message)
    console.log('📥 [createNewDraft] response.data.msg:', response.data?.msg)

    // 兼容多种响应格式
    let draftId = null
    let hasDraft = false
    let returnedArticleId = null

    // 格式 1: { code: 200, data: { draftId, articleId, hasDraft } }
    if (response.data?.code === 200 && response.data?.data) {
      draftId = response.data.data.draftId
      returnedArticleId = response.data.data.articleId
      hasDraft = response.data.data.hasDraft
      console.log('✅ [createNewDraft] 识别为格式 1（标准格式）')
    }
    // 格式 2: { code: 0, data: { draftId, ... } }
    else if (response.data?.code === 0 && response.data?.data) {
      draftId = response.data.data.draftId
      returnedArticleId = response.data.data.articleId
      hasDraft = response.data.data.hasDraft
      console.log('✅ [createNewDraft] 识别为格式 2（code=0）')
    }
    // 格式 3: { draftId, articleId, hasDraft } （直接返回数据）
    else if (response.data?.draftId) {
      draftId = response.data.draftId
      returnedArticleId = response.data.articleId
      hasDraft = response.data.hasDraft
      console.log('✅ [createNewDraft] 识别为格式 3（直接返回）')
    }
    // 格式 4: { data: { draftId, ... } } （无 code 字段）
    else if (response.data?.data?.draftId) {
      draftId = response.data.data.draftId
      returnedArticleId = response.data.data.articleId
      hasDraft = response.data.data.hasDraft
      console.log('✅ [createNewDraft] 识别为格式 4（无 code 字段）')
    }

    if (draftId !== null && draftId !== undefined) {
      // ✅ 修复：使用局部变量存储，然后再赋值给 ref
      const draftIdStr = String(draftId)
      articleId.value = draftIdStr

      appContext?.$toolUtil?.storageSet('currentDraftId', draftIdStr)

      console.log('✅ [createNewDraft] 草稿创建成功')
      console.log('🔑 [createNewDraft] draftId:', draftId)
      console.log('🔑 [createNewDraft] articleId:', returnedArticleId)
      console.log('🔑 [createNewDraft] hasDraft:', hasDraft)
      console.log('🔑 [createNewDraft] 已设置 articleId.value:', articleId.value)
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
    console.log('🏁 [createNewDraft] === 创建草稿结束 ===')
  }
}

const handleSaveDraft = async () => {
  console.log('✍️ [handleSaveDraft] === 用户点击手动保存 ===')

  if (!formData.title) {
    console.warn('⚠️ [handleSaveDraft] 验证失败：缺少文章标题')
    message.warning('请输入文章标题')
    return
  }

  if (!formData.categoryId) {
    console.warn('⚠️ [handleSaveDraft] 验证失败：缺少文章分类')
    message.warning('请选择文章分类')
    return
  }

  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !formData.content.content[0]?.content)

  if (isEmptyContent) {
    console.warn('⚠️ [handleSaveDraft] 验证失败：内容为空')
    message.warning('请输入文章内容')
    return
  }

  console.log('✅ [handleSaveDraft] 表单验证通过')

  try {
    savingDraft.value = true
    console.log('🔒 [handleSaveDraft] 锁定保存状态')

    // 如果没有 articleId (即 draftId)，先创建草稿
    if (!articleId.value) {
      console.log('📝 [handleSaveDraft] 未检测到草稿 ID，正在创建新草稿...')
      const created = await createNewDraft()
      console.log('📋 [handleSaveDraft] 草稿创建结果:', created ? '成功' : '失败')
      if (!created || !articleId.value) {
        console.error('❌ [handleSaveDraft] 创建草稿失败')
        message.error('创建草稿失败')
        return
      }
    }

    console.log('📦 [handleSaveDraft] 调用 saveDraft 接口，draftId:', articleId.value)

    // 手动保存，创建小版本
    const response = await draftAPI.saveDraft(articleId.value, {})

    if (response.data.code === 200 || response.data.msg) {
      const versionInfo = response.data.data || response.data
      currentVersion.value = versionInfo.versionDisplay || `${versionInfo.majorVersion}.${versionInfo.minorVersion}`

      console.log('✅ [handleSaveDraft] 保存成功')
      console.log('📋 [handleSaveDraft] 版本信息:')
      console.log('   - version:', versionInfo.version)
      console.log('   - majorVersion:', versionInfo.majorVersion)
      console.log('   - minorVersion:', versionInfo.minorVersion)
      console.log('   - versionDisplay:', versionInfo.versionDisplay)

      message.success(`草稿已保存，版本：${currentVersion.value}`)

      appContext?.$toolUtil?.storageSet('currentDraftId', String(articleId.value))
    } else {
      console.error('❌ [handleSaveDraft] 保存失败:', response.data.msg)
      message.error(response.data.msg || '保存草稿失败')
    }
  } catch (error) {
    console.error('❌ [handleSaveDraft] 保存异常:', error)
    message.error('保存草稿失败，请重试')
  } finally {
    savingDraft.value = false
    console.log('🔓 [handleSaveDraft] 解锁保存状态')
    console.log('🏁 [handleSaveDraft] === 手动保存结束 ===')
  }
}

const handleSaveMajorVersion = async () => {
  console.log('🔖 [handleSaveMajorVersion] === 用户点击标记大版本 ===')

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
      console.log('✅ [handleSaveMajorVersion] 用户确认标记大版本')
      try {
        savingDraft.value = true
        console.log('🔒 [handleSaveMajorVersion] 锁定保存状态')

        console.log('📦 [handleSaveMajorVersion] 调用 publishDraft 接口')
        console.log('🔑 [handleSaveMajorVersion] draftId:', articleId.value)
        console.log('📦 [handleSaveMajorVersion] 请求参数:', {
          isMajor: true,
          changeSummary: '手动标记大版本'
        })

        const response = await draftAPI.publishDraft(articleId.value, {
          isMajor: true,
          changeSummary: '手动标记大版本'
        })

        if (response.data.code === 200 || response.data.msg) {
          const versionInfo = response.data.data || response.data
          currentVersion.value = versionInfo.versionDisplay || `${versionInfo.majorVersion}.${versionInfo.minorVersion}`

          console.log('✅ [handleSaveMajorVersion] 标记成功')
          console.log('📋 [handleSaveMajorVersion] 版本信息:')
          console.log('   - version:', versionInfo.version)
          console.log('   - majorVersion:', versionInfo.majorVersion)
          console.log('   - minorVersion:', versionInfo.minorVersion)
          console.log('   - versionDisplay:', versionInfo.versionDisplay)

          message.success(`大版本标记成功，当前版本：${currentVersion.value}`)

          setTimeout(() => {
            console.log('🔄 [handleSaveMajorVersion] 跳转到文章列表')
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
        console.log('🔓 [handleSaveMajorVersion] 解锁保存状态')
        console.log('🏁 [handleSaveMajorVersion] === 标记大版本结束 ===')
      }
    }
  })
}

const handleSubmit = async () => {
  console.log('🚀 [handleSubmit] === 用户点击发布按钮 ===')
  console.log('📦 [handleSubmit] formData:', JSON.stringify(formData, null, 2))

  try {
    await formRef.value?.validate()
    console.log('✅ [handleSubmit] 表单验证通过')
  } catch (errors) {
    console.error('❌ [handleSubmit] 表单验证失败:', errors)
    message.warning('请填写完整的表单信息')
    return
  }

  const isEmptyContent = !formData.content ||
    !formData.content.content ||
    formData.content.content.length === 0 ||
    (formData.content.content.length === 1 &&
     !formData.content.content[0]?.content)

  if (isEmptyContent) {
    console.warn('⚠️ [handleSubmit] 验证失败：内容为空')
    message.warning('请输入文章内容')
    return
  }

  console.log('✅ [handleSubmit] 内容验证通过')

  try {
    submitting.value = true
    console.log('🔒 [handleSubmit] 锁定提交状态')
    console.log('📦 [handleSubmit] 开始提交...')

    if (!articleId.value) {
      console.error('❌ [handleSubmit] 验证失败：articleId 不存在')
      message.error('文章 ID 不存在')
      return
    }

    console.log('📦 [handleSubmit] 调用 publishDraft 接口')
    console.log('🔑 [handleSubmit] draftId:', articleId.value)
    console.log('📦 [handleSubmit] 请求参数:', {
      isMajor: false,
      changeSummary: ''
    })

    // 调用发布接口，传递 draftId
    const response = await draftAPI.publishDraft(articleId.value, {
      isMajor: false,
      changeSummary: ''
    })

    console.log('📥 [handleSubmit] 响应数据:', response)

    if (response.data.code === 200 || response.data.msg) {
      const versionInfo = response.data.data || response.data

      console.log('✅ [handleSubmit] 发布成功')
      console.log('📋 [handleSubmit] 版本信息:')
      console.log('   - articleId:', versionInfo.articleId)
      console.log('   - version:', versionInfo.version)
      console.log('   - majorVersion:', versionInfo.majorVersion)
      console.log('   - minorVersion:', versionInfo.minorVersion)
      console.log('   - versionDisplay:', versionInfo.versionDisplay)

      message.success(isEdit.value ? '修改成功' : '发布成功')

      // 清理草稿缓存
      console.log('🧹 [handleSubmit] 清理草稿缓存')
      appContext?.$toolUtil?.storageRemove('currentDraftId')

      setTimeout(() => {
        console.log('🔄 [handleSubmit] 跳转到文章列表')
        router.push('/index/articleList')
      }, 500)
    } else {
      console.error('❌ [handleSubmit] 发布失败:', response.data.msg)
      message.error(response.data.msg || (isEdit.value ? '修改失败' : '发布失败'))
    }
  } catch (error) {
    console.error('❌ [handleSubmit] 提交异常:', error)
    console.error('❌ [handleSubmit] 错误详情:', error.response?.data || error.message)
    const errorMsg = error.response?.data?.message || error.message || (isEdit.value ? '修改失败' : '发布失败')
    message.error(errorMsg)
  } finally {
    submitting.value = false
    console.log('🔓 [handleSubmit] 解锁提交状态')
    console.log('🏁 [handleSubmit] === 发布流程结束 ===')
  }
}

const loadCategoryOptions = async () => {
  console.log('📂 [loadCategoryOptions] 加载分类选项')
  try {
    const response = await appContext?.$http.get('/categories/enabled')
    categoryOptions.value = response.data.data || []
    console.log('✅ [loadCategoryOptions] 分类加载成功，数量:', categoryOptions.value.length)
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

const loadOrCreateDraft = async (draftId?: string | number) => {
  // 优先使用传入的 draftId，否则使用 articleId.value
  const id = draftId || articleId.value

  console.log('📖 [loadOrCreateDraft] === 开始加载草稿 ===')
  console.log('🔑 [loadOrCreateDraft] 传入的 draftId:', draftId)
  console.log('🔑 [loadOrCreateDraft] 当前的 articleId.value:', articleId.value)
  console.log('🔑 [loadOrCreateDraft] 最终使用的 ID:', id)

  if (!id) {
    console.log('⚠️ [loadOrCreateDraft] ID 为空，跳过加载')
    return
  }

  try {
    articleId.value = id as string
    console.log('🔑 [loadOrCreateDraft] 设置 articleId:', articleId.value)

    // 直接根据 draftId 获取草稿详情
    console.log('📦 [loadOrCreateDraft] 调用 getDraft 接口')
    const response = await draftAPI.getDraft(id)
    const data = response.data.data || response.data

    if (data) {
      console.log('✅ [loadOrCreateDraft] 草稿数据获取成功')
      console.log('📋 [loadOrCreateDraft] 草稿标题:', data.title || data.draft?.title)
      console.log('📋 [loadOrCreateDraft] 是否有草稿对象:', !!data.draft)

      // ✅ 修复：正确处理版本信息
      const versionDisplay = data.versionDisplay ||
                            (data.majorVersion !== undefined ? `${data.majorVersion}.${data.minorVersion || 0}` : '1.0')
      console.log('📋 [loadOrCreateDraft] 版本信息:', versionDisplay)

      const draft = data.draft
      let contentJson = data.content || draft?.content

      console.log('📦 [loadOrCreateDraft] 内容类型:', typeof contentJson)
      console.log('📦 [loadOrCreateDraft] 内容:', contentJson)
      console.log('📦 [loadOrCreateDraft] 是否为空对象:', contentJson && Object.keys(contentJson).length === 0)

      if (typeof contentJson === 'string') {
        console.log('🔄 [loadOrCreateDraft] 将 HTML 转换为 JSON')
        contentJson = htmlToContentJson(contentJson)
      }
      // ✅ 修复：如果是空对象或无效内容，初始化为标准格式
      else if (!contentJson ||
               (typeof contentJson === 'object' && Object.keys(contentJson).length === 0) ||
               !contentJson.type) {
        console.log('⚠️ [loadOrCreateDraft] 内容为空或格式不正确，初始化为标准格式')
        contentJson = { type: 'doc', content: [] }
      }

      console.log('📝 [loadOrCreateDraft] 最终使用的内容:', contentJson)
      console.log('📝 [loadOrCreateDraft] 填充表单数据')

      Object.assign(formData, {
        title: data.title || draft?.title || '',
        categoryId: String(draft?.categoryId) || '',
        coverUrl: draft?.coverUrl || '',
        content: contentJson
      })

      currentVersion.value = versionDisplay
      console.log('📋 [loadOrCreateDraft] 当前版本:', currentVersion.value)

      if (!isEdit.value && (data.title || draft?.title)) {
        isEdit.value = true
        console.log('✏️ [loadOrCreateDraft] 设置为编辑模式')
      }

      console.log('✅ [loadOrCreateDraft] 草稿加载成功:', articleId.value)
      console.log('⏱️ [loadOrCreateDraft] 启动自动保存')
      startAutoSave()
    } else {
      console.warn('⚠️ [loadOrCreateDraft] 草稿数据为空')
    }
  } catch (error) {
    console.error('❌ [loadOrCreateDraft] 加载草稿失败:', error)
    console.error('❌ [loadOrCreateDraft] 错误详情:', error.response?.data || error.message)
    message.error('获取草稿信息失败')
  } finally {
    console.log('🏁 [loadOrCreateDraft] === 加载草稿结束 ===')
  }
}

const createNewDraftImmediately = async () => {
  console.log('⚡ [createNewDraftImmediately] === 立即创建新草稿 ===')

  try {
    const created = await createNewDraft()
    console.log('🔑 [createNewDraftImmediately] '+created)
    if (created && articleId.value) {

      console.log('✅ [createNewDraftImmediately] 草稿创建成功，启动自动保存')
      startAutoSave()
    } else {
      console.error('❌ [createNewDraftImmediately] 草稿创建失败')
      message.error('创建草稿失败，请刷新页面重试')
    }
  } catch (error) {
    console.error('❌ [createNewDraftImmediately] 创建草稿异常:', error)
    message.error('创建草稿失败，请刷新页面重试')
  } finally {
    console.log('🏁 [createNewDraftImmediately] === 创建草稿结束 ===')
  }
}

onMounted(() => {
  console.log('🎬 [onMounted] 组件挂载')
  console.log('🔑 [onMounted] 路由参数 id:', route.query.id)
  console.log('🔑 [onMounted] 路由参数 draftId:', route.query.draftId)

  isEdit.value = !!route.query.id
  console.log('✏️ [onMounted] 是否为编辑模式:', isEdit.value)

  loadCategoryOptions()

  // 如果有 articleId 或 draftId 参数，加载对应的草稿
  if (route.query.id || route.query.draftId) {
    console.log('📖 [onMounted] 检测到路由参数，调用 loadOrCreateDraft')
    loadOrCreateDraft(route.query.id || route.query.draftId)
  } else {
    // 否则尝试从缓存中恢复草稿 ID
    const savedDraftId = appContext?.$toolUtil?.storageGet('currentDraftId')
    console.log('💾 [onMounted] 从缓存读取 draftId:', savedDraftId)

    if (savedDraftId && savedDraftId !== '' && savedDraftId !== 'null' && savedDraftId !== 'undefined') {
      articleId.value = savedDraftId
      console.log('🔑 [onMounted] 已设置 articleId.value:', articleId.value)
      console.log('📖 [onMounted] 使用缓存的 draftId 加载草稿')
      // ✅ 修复：直接传入 draftId 参数
      loadOrCreateDraft(savedDraftId)
    } else {
      // 全新创作模式，立即创建草稿
      console.log('🆕 [onMounted] 全新创作模式，立即创建草稿')
      createNewDraftImmediately()
    }
  }

  console.log('🏁 [onMounted] === 组件挂载完成 ===')
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
</style>
