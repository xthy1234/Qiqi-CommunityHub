<template>
  <div class="cover-upload-wrapper">
    <div class="cover-preview" v-if="modelValue">
      <img :src="imageUrl" alt="封面预览" />
      <div class="cover-actions">
        <n-button size="small" @click="triggerUpload">
          <template #icon>
            <Icon icon="material-symbols:refresh" width="16" />
          </template>
          更换
        </n-button>
        <n-button size="small" type="error" @click="handleRemove">
          <template #icon>
            <Icon icon="material-symbols:delete" width="16" />
          </template>
          删除
        </n-button>
      </div>
    </div>

    <div class="cover-upload-btn" v-else @click="triggerUpload">
      <Icon icon="ri:add-line" width="40" />
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
import { Icon } from '@iconify/vue'
import { useMessage } from 'naive-ui'
import { useGlobalProperties } from '@/utils/globalProperties'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const message = useMessage()
const appContext = useGlobalProperties()

const coverInputRef = ref<HTMLInputElement | null>(null)

const baseUrl = computed(() => appContext?.$config?.url || 'http://localhost:8080')
const uploadUrl = computed(() => `${baseUrl.value}/files`)
const uploadHeaders = computed(() => ({
  token: appContext?.$toolUtil?.storageGet('Token') || ''
}))

const imageUrl = computed(() => {
  if (!props.modelValue) return ''
// console.log('imageUrl', props.modelValue)
  return props.modelValue.startsWith('http')
    ? props.modelValue
    : `${baseUrl.value}/${props.modelValue}`
})

const triggerUpload = () => {
  coverInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

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

  const formDataUpload = new FormData()
  formDataUpload.append('file', file)

  try {
    const response = await appContext?.$http({
      url: uploadUrl.value,
      method: 'post',
      data: formDataUpload,
      headers: {
        'Content-Type': 'multipart/form-data',
        token: uploadHeaders.value.token
      }
    })

    const res = response.data
    if (res.code === 0 || res.code === 200 || res.success) {
      const fileUrl = res.data?.url || res.fileName
      if (fileUrl) {
        emit('update:modelValue', "files/"+fileUrl)
        message.success('封面上传成功')
      } else {
        message.error('上传失败：未获取到图片 URL')
      }
    } else {
      message.error(res.message || '封面上传失败')
    }
  } catch (error) {
// console.error('上传失败:', error)
    message.error('上传失败，请重试')
  } finally {
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
