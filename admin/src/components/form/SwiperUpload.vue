<template>
  <div class="swiper-upload-wrapper">
    <div class="upload-area" v-if="!modelValue">
      <div class="upload-placeholder" @click="triggerUpload">
        <Icon icon="ri:image-add-line" width="60" />
        <span class="upload-text">点击上传轮播图</span>
        <div class="upload-tip">
          建议尺寸：1920x600 像素<br/>
          支持 jpg、png 格式，大小不超过 10MB
        </div>
      </div>
    </div>

    <div class="preview-area" v-else>
      <div class="preview-container">
        <img :src="imageUrl" alt="轮播图预览" class="preview-image" />
        <div class="preview-overlay">
          <div class="preview-actions">
            <n-button size="small" type="primary" @click="triggerUpload">
              <template #icon>
                <Icon icon="ri:refresh-line" width="16" />
              </template>
              更换图片
            </n-button>
            <n-button size="small" type="error" @click="handleRemove">
              <template #icon>
                <Icon icon="ri:delete-bin-line" width="16" />
              </template>
              删除
            </n-button>
          </div>
        </div>
      </div>
      <div class="preview-info">
        <n-tag type="success" size="small">已上传</n-tag>
        <span class="file-name">{{ getFileName(modelValue) }}</span>
      </div>
    </div>

    <input
      ref="fileInputRef"
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
import { uploadAPI } from '@/api/upload'
import { normalizeFileUrl } from '@/utils/fileUrl'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const message = useMessage()
const fileInputRef = ref<HTMLInputElement | null>(null)

const imageUrl = computed(() => {
  return normalizeFileUrl(props.modelValue)
})

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

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

  try {
    const viewUrl = await uploadAPI.uploadImage(file)

    if (viewUrl) {
      emit('update:modelValue', viewUrl)
      message.success('轮播图上传成功')
    } else {
      message.error('上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
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

const getFileName = (url: string) => {
  if (!url) return ''
  return url.split('/').pop() || url
}
</script>

<style lang="scss" scoped>
.swiper-upload-wrapper {
  .upload-area {
    width: 100%;
    
    .upload-placeholder {
      width: 100%;
      height: 300px;
      border: 2px dashed #d9d9d9;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s;
      background: #fafafa;
      
      &:hover {
        border-color: #18a058;
        background: #f0f9f4;
        
        .upload-text {
          color: #18a058;
        }
      }
      
      .upload-text {
        margin-top: 16px;
        font-size: 16px;
        color: #666;
        transition: color 0.3s;
      }
      
      .upload-tip {
        margin-top: 12px;
        font-size: 13px;
        color: #999;
        line-height: 1.6;
        text-align: center;
      }
    }
  }
  
  .preview-area {
    .preview-container {
      position: relative;
      width: 100%;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid #e4e7ed;
      
      .preview-image {
        display: block;
        width: 100%;
        max-height: 400px;
        object-fit: cover;
      }
      
      .preview-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s;
        
        &:hover {
          opacity: 1;
        }
        
        .preview-actions {
          display: flex;
          gap: 12px;
        }
      }
    }
    
    .preview-info {
      margin-top: 12px;
      display: flex;
      align-items: center;
      gap: 12px;
      
      .file-name {
        font-size: 13px;
        color: #666;
      }
    }
  }
}
</style>
