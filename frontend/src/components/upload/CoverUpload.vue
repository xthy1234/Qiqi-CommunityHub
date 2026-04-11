<template>
  <div class="cover-upload-wrapper">
    <div
      v-if="modelValue"
      class="cover-preview"
    >
      <img
        :src="imageUrl"
        alt="封面预览"
      />
      <div class="cover-actions">
        <n-button
          size="small"
          @click="triggerUpload"
        >
          <template #icon>
            <Icon
              icon="material-symbols:refresh"
              width="16"
            />
          </template>
          更换
        </n-button>
        <n-button
          size="small"
          type="error"
          @click="handleRemove"
        >
          <template #icon>
            <Icon
              icon="material-symbols:delete"
              width="16"
            />
          </template>
          删除
        </n-button>
      </div>
    </div>

    <div
      v-else
      class="cover-upload-btn"
      @click="triggerUpload"
    >
      <Icon
        icon="ri:add-line"
        width="40"
      />
      <span>点击上传封面图</span>
      <div class="cover-tip">
        建议尺寸：800x600 像素，支持 jpg、png 格式，大小不超过 10MB
      </div>
    </div>

    <input
      ref="coverInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessage } from 'naive-ui'
import { uploadAPI } from '@/api/upload'
import { normalizeFileUrl } from '@/utils/fileUrl'
import {useGlobalProperties} from "@/utils/globalProperties";
import { Icon } from '@iconify/vue'
const message = useMessage()

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const appContext = useGlobalProperties()

const coverInputRef = ref<HTMLInputElement | null>(null)

const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')
const uploadUrl = computed(() => `${baseUrl.value}/files`)
const uploadHeaders = computed(() => ({
  token: appContext?.$toolUtil?.storageGet('Token') || ''
}))

// 【调试】计算图片显示URL
// 作用：将上传接口返回的 fileUrl（如 /api/files/3）转换为完整的可访问URL
// 示例：/api/files/3 → http://localhost:8080/api/files/3
const imageUrl = computed(() => {

  const result = normalizeFileUrl(props.modelValue, baseUrl.value)

  return result
})

const triggerUpload = () => {
  coverInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {return}

  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    message.error('只能上传图片文件!')
    return
  }
  if (!isLt2M) {
    message.error('图片大小不能超过 10MB!')
    return
  }

  try {
    console.log('📤 [CoverUpload] 调用 uploadAPI.uploadImage...')

    // 【关键】调用上传接口
    // 期望返回：UploadResponse 对象，包含 fileUrl 字段
    // 实际返回结构需要根据后端响应调整
    const response = await uploadAPI.uploadImage(file, '文章封面')

    // 【调试】检查返回值
    if (response) {



      // 触发更新，将 fileUrl（如 /api/files/3）传递给父组件
      emit('update:modelValue', response)

      message.success('封面上传成功')
    } else {
      // 【错误情况】response 为 null 或 undefined
      console.error('❌ [CoverUpload] 上传失败：response 为空')
      console.error('❌ [CoverUpload] 可能原因：')
      console.error('   1. uploadAPI.uploadImage 内部捕获了异常并返回 null')
      console.error('   2. 后端返回的 code !== 0')
      console.error('   3. 网络请求失败')
      message.error('上传失败：未获取到图片 URL')
    }
  } catch (error: any) {
    // 【异常情况】上传过程抛出异常
    console.error('❌ [CoverUpload] 上传过程发生异常:', error)
    console.error('❌ [CoverUpload] 错误信息:', error.message)
    console.error('❌ [CoverUpload] 错误堆栈:', error.stack)
    message.error('上传失败，请重试')
  } finally {
    // 清空文件输入框，允许重复选择同一文件
    if (target) {
      target.value = ''

    }
  }
}

const handleRemove = () => {

  emit('update:modelValue', '')
}
</script>

<style lang="scss" scoped>
.cover-upload-wrapper {
  .cover-preview {
    position: relative;
    display: inline-block;
    border-radius: 4px;
    overflow: hidden;
    border: 1px solid #e4e7ed;

    img {
      display: block;
      max-width: 300px;
      height: auto;
    }

    .cover-actions {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      opacity: 0;
      transition: opacity 0.3s;

      &:hover {
        opacity: 1;
      }
    }
  }

  .cover-upload-btn {
    width: 300px;
    height: 200px;
    border: 2px dashed #d9d9d9;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: border-color 0.3s;
    color: #909399;

    &:hover {
      border-color: #18a058;
      color: #18a058;
    }

    span {
      margin-top: 8px;
      font-size: 14px;
    }

    .cover-tip {
      margin-top: 10px;
      font-size: 12px;
      color: #999;
      line-height: 1.5;
      text-align: center;
    }
  }
}
</style>
