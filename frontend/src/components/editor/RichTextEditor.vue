<template>
  <div class="editor-wrapper">
    <!-- 工具栏 -->
    <div class="toolbar" v-if="editor">
      <!-- 撤销/重做 -->
      <n-button
        size="tiny"
        @click="editor.chain().focus().undo().run()"
        :disabled="!editor.can().undo()"
        quaternary
        title="撤销 (Ctrl+Z)"
      >
        <template #icon>
          <Icon icon="material-symbols:undo" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        @click="editor.chain().focus().redo().run()"
        :disabled="!editor.can().redo()"
        quaternary
        title="重做 (Ctrl+Y)"
      >
        <template #icon>
          <Icon icon="material-symbols:redo" width="16" />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 标题选择器 -->
      <n-select
        v-model:value="currentHeading"
        :options="headingOptions"
        placeholder="正文"
        size="tiny"
        style="width: 100px"
        @update:value="setHeading"
      />

      <n-divider vertical />

      <!-- 基础样式 -->
      <n-button
        size="tiny"
        :type="editor.isActive('bold') ? 'primary' : ''"
        @click="toggleBold"
        quaternary
        title="加粗 (Ctrl+B)"
      >
        <template #icon>
          <Icon icon="material-symbols:format-bold" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('italic') ? 'primary' : ''"
        @click="toggleItalic"
        quaternary
        title="斜体 (Ctrl+I)"
      >
        <template #icon>
          <Icon icon="material-symbols:format-italic" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('underline') ? 'primary' : ''"
        @click="toggleUnderline"
        quaternary
        title="下划线 (Ctrl+U)"
      >
        <template #icon>
          <Icon icon="material-symbols:format-underlined" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('strike') ? 'primary' : ''"
        @click="toggleStrike"
        quaternary
        title="删除线"
      >
        <template #icon>
          <Icon icon="material-symbols:format-strikethrough" width="16" />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 列表 -->
      <n-button
        size="tiny"
        :type="editor.isActive('bulletList') ? 'primary' : ''"
        @click="toggleBulletList"
        quaternary
        title="无序列表"
      >
        <template #icon>
          <Icon icon="material-symbols:format-list-bulleted" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('orderedList') ? 'primary' : ''"
        @click="toggleOrderedList"
        quaternary
        title="有序列表"
      >
        <template #icon>
          <Icon icon="material-symbols:format-list-numbered" width="16" />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 引用和代码 -->
      <n-button
        size="tiny"
        :type="editor.isActive('blockquote') ? 'primary' : ''"
        @click="toggleBlockquote"
        quaternary
        title="引用块"
      >
        <template #icon>
          <Icon icon="material-symbols:format-quote" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('code') ? 'primary' : ''"
        @click="toggleCode"
        quaternary
        title="行内代码"
      >
        <template #icon>
          <Icon icon="material-symbols:code" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive('codeBlock') ? 'primary' : ''"
        @click="toggleCodeBlock"
        quaternary
        title="代码块"
      >
        <template #icon>
          <Icon icon="material-symbols:code-blocks" width="16" />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 插入元素 -->
      <n-button
        size="tiny"
        @click="openLinkDialog"
        quaternary
        title="插入链接 (Ctrl+K)"
      >
        <template #icon>
          <Icon icon="material-symbols:link" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        @click="openImageDialog"
        quaternary
        title="插入图片"
      >
        <template #icon>
          <Icon icon="material-symbols:image" width="16" />
        </template>
      </n-button>

      <n-divider vertical />

      <!-- 对齐方式 -->
      <n-button
        size="tiny"
        :type="editor.isActive({ textAlign: 'left' }) ? 'primary' : ''"
        @click="editor.chain().focus().setTextAlign('left').run()"
        quaternary
        title="左对齐"
      >
        <template #icon>
          <Icon icon="material-symbols:format-align-left" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive({ textAlign: 'center' }) ? 'primary' : ''"
        @click="editor.chain().focus().setTextAlign('center').run()"
        quaternary
        title="居中"
      >
        <template #icon>
          <Icon icon="material-symbols:format-align-center" width="16" />
        </template>
      </n-button>
      <n-button
        size="tiny"
        :type="editor.isActive({ textAlign: 'right' }) ? 'primary' : ''"
        @click="editor.chain().focus().setTextAlign('right').run()"
        quaternary
        title="右对齐"
      >
        <template #icon>
          <Icon icon="material-symbols:format-align-right" width="16" />
        </template>
      </n-button>
    </div>

    <!-- 编辑器内容区 -->
    <editor-content :editor="editor" class="tiptap" />

    <!-- 链接插入对话框 -->
    <n-modal
      v-model:show="linkDialogVisible"
      preset="dialog"
      title="插入链接"
      :style="{ width: '400px' }"
    >
      <n-form :model="linkForm" label-placement="left" label-width="80px">
        <n-form-item label="链接文本">
          <n-input
            v-model:value="linkForm.text"
            placeholder="选中的文本将作为链接文字"
            :disabled="!!selectedText"
          />
        </n-form-item>
        <n-form-item label="链接地址" required>
          <n-input
            v-model:value="linkForm.url"
            placeholder="https://example.com"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="linkDialogVisible = false">取消</n-button>
          <n-button type="primary" @click="insertLink">确定</n-button>
          <n-button
            v-if="editor?.isActive('link')"
            type="error"
            @click="removeLink"
          >
            移除链接
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 图片插入对话框 -->
    <n-modal
      v-model:show="imageDialogVisible"
      preset="dialog"
      title="插入图片"
      :style="{ width: '400px' }"
    >
      <n-form :model="imageForm" label-placement="left" label-width="80px">
        <n-form-item label="图片 URL" required>
          <n-input
            v-model:value="imageForm.url"
            placeholder="https://example.com/image.jpg"
          />
        </n-form-item>
        <n-form-item label="替代文本">
          <n-input
            v-model:value="imageForm.alt"
            placeholder="图片描述（可选）"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="imageDialogVisible = false">取消</n-button>
          <n-button type="primary" @click="insertImage">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onBeforeUnmount } from 'vue'
import { Icon } from '@iconify/vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { useMessage } from 'naive-ui'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Placeholder from '@tiptap/extension-placeholder'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { all, createLowlight } from 'lowlight'
import type { SelectOption } from 'naive-ui'

const message = useMessage()

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const currentHeading = ref('paragraph')
const linkDialogVisible = ref(false)
const imageDialogVisible = ref(false)

const linkForm = reactive({
  text: '',
  url: ''
})

const imageForm = reactive({
  url: '',
  alt: ''
})

const selectedText = ref('')

const headingOptions: SelectOption[] = [
  { label: '标题 1', value: 'h1' },
  { label: '标题 2', value: 'h2' },
  { label: '标题 3', value: 'h3' },
  { label: '正文', value: 'paragraph' }
]

const lowlight = createLowlight(all)

const editor = useEditor({
  content: props.modelValue || '',
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [2, 3],
        HTMLAttributes: {
          class: 'heading'
        }
      },
      codeBlock: false,
      link: false,
      underline: false
    }),
    Image.configure({
      HTMLAttributes: {
        class: 'image'
      }
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        target: '_blank',
        rel: 'noopener noreferrer'
      }
    }),
    TextAlign.configure({
      types: ['heading', 'paragraph']
    }),
    Underline,
    Color,
    Highlight,
    Placeholder.configure({
      placeholder: '开始撰写文章内容...'
    }),
    CodeBlockLowlight.configure({
      lowlight
    })
  ],
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    emit('update:modelValue', html)
    if (editor.isActive('heading', { level: 2 })) {
      currentHeading.value = 'h2'
    } else if (editor.isActive('heading', { level: 3 })) {
      currentHeading.value = 'h3'
    } else {
      currentHeading.value = 'paragraph'
    }
  },
  onSelectionUpdate: ({ editor }) => {
    const { from, to } = editor.state.selection
    selectedText.value = editor.state.doc.textBetween(from, to)

    if (editor.isActive('link')) {
      const { href } = editor.getAttributes('link')
      linkForm.url = href || ''
      linkForm.text = selectedText.value
    }
  },
})

watch(() => props.modelValue, (newVal) => {
  if (editor.value && newVal !== editor.value.getHTML()) {
    editor.value.commands.setContent(newVal)
  }
}, { immediate: true })

const setHeading = (level: string) => {
  if (!editor.value) return

  if (level === 'paragraph') {
    editor.value.chain().focus().setParagraph().run()
  } else if (level === 'h2') {
    editor.value.chain().focus().toggleHeading({ level: 2 }).run()
  } else if (level === 'h3') {
    editor.value.chain().focus().toggleHeading({ level: 3 }).run()
  }
}

const toggleBold = () => {
  editor.value?.chain().focus().toggleBold().run()
}

const toggleItalic = () => {
  editor.value?.chain().focus().toggleItalic().run()
}

const toggleUnderline = () => {
  editor.value?.chain().focus().toggleUnderline().run()
}

const toggleStrike = () => {
  editor.value?.chain().focus().toggleStrike().run()
}

const toggleBulletList = () => {
  editor.value?.chain().focus().toggleBulletList().run()
}

const toggleOrderedList = () => {
  editor.value?.chain().focus().toggleOrderedList().run()
}

const toggleBlockquote = () => {
  editor.value?.chain().focus().toggleBlockquote().run()
}

const toggleCode = () => {
  editor.value?.chain().focus().toggleCode().run()
}

const toggleCodeBlock = () => {
  if (!editor.value) return
  editor.value.chain().focus().toggleCodeBlock().run()
}

const openLinkDialog = () => {
  if (!editor.value) return

  if (!selectedText.value && !editor.value.isActive('link')) {
    message.warning('请先选中要添加链接的文字')
    return
  }

  linkForm.text = selectedText.value
  linkForm.url = ''

  if (editor.value.isActive('link')) {
    const { href } = editor.value.getAttributes('link')
    linkForm.url = href || ''
  }

  linkDialogVisible.value = true
}

const insertLink = () => {
  if (!editor.value || !linkForm.url) return

  if (!selectedText.value && !linkForm.text) {
    linkForm.text = linkForm.url
  }

  editor.value
    .chain()
    .focus()
    .extendMarkRange('link')
    .insertContent({
      type: 'text',
      text: linkForm.text,
      marks: [
        {
          type: 'link',
          attrs: {
            href: linkForm.url
          }
        }
      ]
    })
    .setLink({ href: linkForm.url })
    .run()

  linkDialogVisible.value = false
}

const removeLink = () => {
  if (!editor.value) return
  editor.value.chain().focus().unsetLink().run()
  linkDialogVisible.value = false
}

const openImageDialog = () => {
  imageForm.url = ''
  imageForm.alt = ''
  imageDialogVisible.value = true
}

const insertImage = () => {
  if (!editor.value || !imageForm.url) return

  editor.value
    .chain()
    .focus()
    .setImage({
      src: imageForm.url,
      alt: imageForm.alt
    })
    .run()

  imageDialogVisible.value = false
}

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.destroy()
  }
})
</script>

<style lang="scss" scoped>
.editor-wrapper {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    padding: 8px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;

    :deep(.n-button:not([type="primary"])) {
      color: #909399;
      background-color: transparent;

      &:hover {
        background-color: #ecf5ff;
        color: #409eff;
      }

      &.n-button--type-primary {
        color: #fff;
        background-color: #18a058;

        &:hover {
          background-color: #16955b;
        }
      }

      &[disabled] {
        color: #c0c4cc;
        cursor: not-allowed;
      }
    }
  }

  .tiptap {
    min-height: 480px;
    padding: 16px;
    outline: none;
    font-size: 14px;
    line-height: 1.8;
    background: #fafafa;
    border: 1px solid #e4e7ed;
    transition: background-color 0.3s;

    &:focus-within {
      background-color: #fff;
      border-color: #18a058;
    }

    // 所有需要穿透的选择器都加上 :deep()
    :deep(p.is-editor-empty:first-child::before) {
      color: #adb5bd;
      content: attr(data-placeholder);
      float: left;
      height: 0;
      pointer-events: none;
    }

    :deep(h2), :deep(h3) {
      margin-top: 24px;
      margin-bottom: 16px;
      font-weight: 600;
      line-height: 1.25;
    }

    :deep(h1) {
      font-size: 2em;
      border-bottom: 2px solid #eaecef;
      padding-bottom: 0.3em;
      margin-top: 32px;
      margin-bottom: 16px;
      font-weight: 600;
      line-height: 1.25;
    }

    :deep(h2) {
      font-size: 1.5em;
      border-bottom: 1px solid #eaecef;
      padding-bottom: 0.3em;
    }

    :deep(h3) {
      font-size: 1.25em;
    }

    :deep(p) {
      margin: 0 0 1em 0;
    }

    :deep(ul), :deep(ol) {
      padding-left: 2em;
      margin: 0 0 1em 0;
    }

    :deep(li) {
      margin: 0.5em 0;
    }

    :deep(blockquote) {
      margin: 0;
      padding: 0 1em;
      color: #6a737d;
      border-left: 0.25em solid #dfe2e5;
    }

    :deep(code) {
      padding: 0.2em 0.4em;
      margin: 0;
      font-size: 85%;
      background-color: rgba(27, 31, 35, 0.05);
      border-radius: 3px;
      font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
      color: #e83e8c;
    }

    :deep(pre) {
      padding: 16px;
      overflow: auto;
      font-size: 85%;
      line-height: 1.45;
      background-color: #f6f8fa;
      border: 1px solid #e1e4e8;
      border-radius: 3px;
      margin: 1em 0;
      color: #24292e;

      code {
        padding: 0;
        margin: 0;
        background: none;
        font-size: 100%;
        color: inherit;
      }
    }

    :deep(img) {
      max-width: 100%;
      box-sizing: border-box;
      border-radius: 4px;
      margin: 8px 0;
      cursor: pointer;
    }

    :deep(a) {
      color: #18a058;
      text-decoration: none;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }

    :deep(mark) {
      background-color: #faf089;
      border-radius: 0.4em;
      padding: 0.1em 0.2em;
    }

    :deep(u) {
      text-decoration: underline;
    }

    :deep(s) {
      text-decoration: line-through;
    }
  }
}
</style>
