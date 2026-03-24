<template>
  <PageContainer
    header-title="提交修改建议"
    :show-back="true"
  >
    <div class="editor-wrapper">
      <!-- 文章信息提示 -->
      <n-alert
        v-if="articleInfo"
        type="info"
        :title="`正在为文章提交修改建议：${articleInfo.title}`"
        style="margin-bottom: 20px;"
        closable
      >
        <template #header-extra>
          <n-tag
            v-if="articleInfo.editMode === 1"
            type="success"
            size="small"
          >
            所有人可建议
          </n-tag>
        </template>
      </n-alert>

      <!-- 表单区域 -->
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="top"
        label-width="100px"
      >
        <!-- 标题 -->
        <n-form-item label="建议标题" path="title">
          <n-input
            v-model:value="formData.title"
            placeholder="请输入建议标题（可选）"
            maxlength="100"
            show-count
          />
        </n-form-item>

        <!-- 修改摘要 -->
        <n-form-item label="修改说明" path="changeSummary">
          <n-input
            v-model:value="formData.changeSummary"
            type="textarea"
            placeholder="请简要说明您的修改内容（选填）"
            :rows="2"
            maxlength="500"
            show-count
          />
        </n-form-item>

        <!-- 富文本编辑器 -->
        <n-form-item label="修改内容" path="content">
          <div class="editor-container">
            <RichTextEditor
              ref="editorRef"
              v-model:content="formData.content"
              :editable="true"
              :placeholder="请基于原文内容进行修改"
              height="500px"
            />
          </div>
        </n-form-item>

        <!-- 差异预览 -->
        <n-form-item
          v-if="hasChanges"
          label="修改预览"
        >
          <n-collapse :default-expanded-names="['diff']">
            <n-collapse-item
              title="查看修改差异"
              name="diff"
            >
              <DiffViewer
                :source-content="originalContent"
                :target-content="formData.content"
                source-title="原文内容"
                target-title="修改后内容"
                :show-header="false"
                :show-stats="true"
              />
            </n-collapse-item>
          </n-collapse>
        </n-form-item>

        <!-- 提交按钮 -->
        <n-form-item>
          <n-space>
            <n-button
              type="primary"
              :loading="submitting"
              @click="handleSubmit"
            >
              <template #icon>
                <Icon icon="ri:send-plane-line" />
              </template>
              提交建议
            </n-button>
            <n-button @click="handleCancel">
              取消
            </n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, FormInst, FormRules } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import DiffViewer from '@/components/common/DiffViewer.vue'
import { articleAPI } from '@/api/article'
import { articleSuggestionAPI } from '@/api/articleSuggestion'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// 响应式数据
const formRef = ref<FormInst | null>(null)
const editorRef = ref<any>(null)
const submitting = ref(false)
const articleInfo = ref<any>(null)
const originalContent = ref<object>({})

// 表单数据
const formData = reactive({
  title: '',
  changeSummary: '',
  content: {} as object
})

// 表单验证规则
const formRules: FormRules = {
  content: {
    required: true,
    message: '请填写修改内容',
    trigger: 'blur'
  }
}

// 是否有修改
const hasChanges = computed(() => {
  return JSON.stringify(originalContent.value) !== JSON.stringify(formData.content)
})

/**
 * 加载文章内容
 */
const loadArticleContent = async () => {
  const articleId = route.params.articleId as string
  
  if (!articleId) {
    message.error('缺少文章 ID 参数')
    return
  }

  try {
    const response = await articleAPI.getById(articleId)
    articleInfo.value = response.data.data
    
    // 保存原始内容用于对比
    originalContent.value = response.data.data.content || {}
    
    // 加载内容为编辑器初始值
    formData.content = JSON.parse(JSON.stringify(originalContent.value))
  } catch (error) {
    console.error('加载文章内容失败:', error)
    message.error('加载文章内容失败')
  }
}

/**
 * 提交建议
 */
const handleSubmit = async () => {
  // 验证表单
  await formRef.value?.validate(async (errors) => {
    if (errors) {
      message.warning('请填写完整信息')
      return
    }

    const articleId = route.params.articleId as string
    
    if (!articleId) {
      message.error('缺少文章 ID 参数')
      return
    }

    // 检查是否有修改
    if (!hasChanges.value) {
      message.warning('您还没有进行任何修改')
      return
    }

    submitting.value = true
    try {
      await articleSuggestionAPI.create(articleId, {
        title: formData.title || '修改建议',
        changeSummary: formData.changeSummary,
        content: formData.content
      })

      message.success('建议提交成功！等待作者审核')
      
      // 返回文章详情页
      router.push(`/index/articleDetail?id=${articleId}`)
    } catch (error) {
      console.error('提交建议失败:', error)
      message.error('提交建议失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}

/**
 * 取消操作
 */
const handleCancel = () => {
  const articleId = route.params.articleId as string
  if (articleId) {
    router.push(`/index/articleDetail?id=${articleId}`)
  } else {
    router.back()
  }
}

onMounted(() => {
  loadArticleContent()
})
</script>

<style lang="scss" scoped>
.editor-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

.editor-container {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.n-collapse) {
  width: 100%;
  
  .n-collapse-item {
    .n-collapse-item__content-wrapper {
      .n-collapse-item__content-inner {
        padding: 16px 0;
      }
    }
  }
}
</style>
