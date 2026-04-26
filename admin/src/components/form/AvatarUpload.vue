<template>
  <div class="avatar-upload-wrapper">
    <n-upload
      :custom-request="handleCustomUpload"
      :show-file-list="false"
      :before-upload="beforeAvatarUpload"
      :disabled="isDisabled"
    >
      <n-button type="primary" v-if="false">上传头像</n-button>

      <div v-if="imageUrl" class="avatar-preview">
        <img :src="imageUrl" alt="头像" class="avatar-image" />
        <div class="avatar-mask">
          <Icon icon="carbon:plus" class="mask-icon" />
        </div>
      </div>
      <div v-else class="avatar-uploader">
        <Icon icon="carbon:plus" class="uploader-icon" />
      </div>
    </n-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useMessage } from 'naive-ui'
import { uploadAPI } from '@/api/upload'
import { normalizeFileUrl } from '@/utils/fileUrl'
import type { UploadCustomRequestOptions } from 'naive-ui'

interface Props {
  modelValue?: string
  uploadAction?: string
  isDisabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  uploadAction: '',
  isDisabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', url: string): void
}>()

const message = useMessage()
const imageUrl = ref(props.modelValue)

watch(() => props.modelValue, (newVal : string) => {
  imageUrl.value = normalizeFileUrl(newVal)
})

const handleCustomUpload = async ({ file, onFinish, onError }: UploadCustomRequestOptions) => {
  try {
    const viewUrl = await uploadAPI.uploadImage(file.file as File)

    if (viewUrl) {
      imageUrl.value = viewUrl
      emit('update:modelValue', viewUrl)
      emit('change', viewUrl)
      message.success('头像上传成功')
      onFinish()
    } else {
      message.error('上传失败')
      onError()
    }
  } catch (error) {
    console.error('上传失败:', error)
    message.error('上传失败，请重试')
    onError()
  }
}

const beforeAvatarUpload = ({ file }: { file: File }) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    message.error('图片大小不能超过 10MB!')
    return false
  }

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
