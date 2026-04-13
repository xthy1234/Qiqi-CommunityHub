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

    <!-- 图片裁剪组件 -->
    <ImageCropper
      v-model:show="showCropper"
      :image-file="selectedFile"
      crop-type="cover"
      @success="handleCropSuccess"
      @cancel="handleCropCancel"
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
import ImageCropper from './ImageCropper.vue'

const message = useMessage()

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const appContext = useGlobalProperties()

const coverInputRef = ref<HTMLInputElement | null>(null)
const showCropper = ref(false)
const selectedFile = ref<File | null>(null)

const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')
const uploadUrl = computed(() => `${baseUrl.value}/files`)
const uploadHeaders = computed(() => ({
  token: appContext?.$toolUtil?.storageGet('Token') || ''
}))

const imageUrl = computed(() => {
  const result = normalizeFileUrl(props.modelValue, baseUrl.value)

  return result
})

const triggerUpload = () => {

  coverInputRef.value?.click()
}

const handleFileChange = (event: Event) => {

  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {

    return
  }

  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {

    message.error('只能上传图片文件!')
    return
  }
  if (!isLt10M) {

    message.error('图片大小不能超过 10MB!')
    return
  }

  // 打开裁剪窗口
  selectedFile.value = file

  showCropper.value = true

  // 清空输入框
  target.value = ''

}

const handleCropSuccess = (url: string) => {

  emit('update:modelValue', url)
  message.success('封面上传成功')

  // 重置状态
  selectedFile.value = null

}

const handleCropCancel = () => {

  selectedFile.value = null
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
