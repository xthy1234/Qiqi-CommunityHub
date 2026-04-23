<template>
  <div class="avatar-upload-wrapper">
    <n-upload
      :custom-request="customUpload"
      :show-file-list="false"
      :disabled="isDisabled"
      accept="image/jpeg,image/jpg,image/png,image/webp"
    >
      <n-button
        v-if="false"
        type="primary"
      >
        上传头像
      </n-button>

      <div
        v-if="imageUrl"
        class="avatar-preview"
      >
        <img
          :src="imageUrl"
          alt="头像"
          class="avatar-image"
        />
        <div class="avatar-mask">
          <Icon
            icon="carbon:plus"
            class="mask-icon"
          />
        </div>
      </div>
      <div
        v-else
        class="avatar-uploader"
      >
        <Icon
          icon="carbon:plus"
          class="uploader-icon"
        />
      </div>
    </n-upload>

    <!-- 图片裁剪组件 -->
    <ImageCropper
      v-model:show="showCropper"
      :image-file="selectedFile"
      crop-type="avatar"
      @success="handleCropSuccess"
      @cancel="handleCropCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { uploadAPI } from '@/api/upload'
import type { UploadCustomRequestOptions } from 'naive-ui'
import ImageCropper from './ImageCropper.vue'

interface Props {
  modelValue?: string
  uploadAction: string
  isDisabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  isDisabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', url: string): void
}>()

const globalProps = useGlobalProperties()
const imageUrl = ref(props.modelValue)
const showCropper = ref(false)
const selectedFile = ref<File | null>(null)

watch(() => props.modelValue, (newVal : string) => {

  if (newVal) {
    const baseUrl = globalProps.$config?.url || 'http://localhost:8080'
    // 判断是否是新版 API URL
    if (newVal.startsWith('/api/files/')) {
      imageUrl.value = `${baseUrl}${newVal}`
    } else {
      imageUrl.value = newVal.startsWith('http') ? newVal : `${baseUrl}/${newVal}`
    }
  } else {
    imageUrl.value = newVal
  }

})

const customUpload = ({ file }: UploadCustomRequestOptions) => {

  // 不直接上传，而是打开裁剪窗口
  selectedFile.value = file.file as File

  showCropper.value = true

  return {
    abort: () => {

    }
  }
}

const handleCropSuccess = (url: string) => {

  imageUrl.value = url

  // 提取相对路径用于存储
  const baseUrl = globalProps.$config?.url || 'http://localhost:8080'
  const relativePath = url.replace(baseUrl, '')

  emit('update:modelValue', relativePath)
  emit('change', url)

  // 重置状态
  selectedFile.value = null

}

const handleCropCancel = () => {

  selectedFile.value = null
}

const beforeAvatarUpload = ({ file }: { file: File }) => {
  return true
}

</script>

<style lang="scss" scoped>
.avatar-upload-wrapper {
  display: inline-block;

  .avatar-preview,
  .avatar-uploader {
    width: 150px;
    height: 150px;
    border-radius: 50%;
    overflow: hidden;
    border: 3px solid #e4e7ed;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #18a058;
      box-shadow: 0 4px 16px rgba(24, 160, 88, 0.3);
    }
  }

  .avatar-preview {
    position: relative;

    .avatar-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-mask {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s;

      .mask-icon {
        font-size: 48px;
        color: #fff;
      }
    }

    &:hover .avatar-mask {
      opacity: 1;
    }
  }

  .avatar-uploader {
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;

    .uploader-icon {
      font-size: 48px;
      color: #8c939d;
    }
  }

  .avatar-actions {
    margin-top: 12px;
    text-align: center;
  }
}
</style>
