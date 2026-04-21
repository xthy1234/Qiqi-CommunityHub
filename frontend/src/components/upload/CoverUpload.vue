<template>
  <div class="cover-upload-wrapper">
    <!--  显示当前 imageUrl 的值 -->
    <!-- <div style="position: fixed; top: 10px; right: 10px; background: rgba(0,0,0,0.8); color: #fff; padding: 10px; z-index: 9999; font-size: 12px;">
      imageUrl: {{ imageUrl }}
    </div> -->

    <!-- 已有封面时显示预览 -->
    <div
      v-if="modelValue"
      class="cover-preview"
      @click="triggerUpload"
    >
      <!--  添加加载状态和错误提示 -->
      <img
        :src="imageUrl"
        alt="封面预览"
        @load="handleImageLoad"
        @error="handleImageError"
        style="opacity: 0; transition: opacity 0.3s;"
        :style="{ opacity: imageLoaded ? 1 : 0 }"
      />

      <!--  图片加载中的占位 -->
      <div v-if="!imageLoaded" class="loading-placeholder">
        <n-spin size="small" />
        <span>加载中...</span>
      </div>

      <div class="cover-mask">
        <Icon
          icon="carbon:edit"
          class="mask-icon"
        />
      </div>
      <div class="cover-actions">
        <n-button
          size="small"
          type="primary"
          @click.stop="triggerUpload"
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
          @click.stop="handleRemove"
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

    <!-- 无封面时显示上传按钮 -->
    <div
      v-else
      class="cover-uploader"
      @click="triggerUpload"
    >
      <Icon
        icon="carbon:add"
        class="uploader-icon"
      />
      <span class="uploader-text">点击上传封面</span>
      <div class="cover-tip">
        建议尺寸：800x600 像素<br />
        支持 JPG、PNG、WebP 格式，大小不超过 10MB
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="coverInputRef"
      type="file"
      accept="image/jpeg,image/jpg,image/png,image/webp"
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
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { uploadAPI } from '@/api/upload'
import { normalizeFileUrl } from '@/utils/fileUrl'
import { useGlobalProperties } from '@/utils/globalProperties'
import ImageCropper from './ImageCropper.vue'

const message = useMessage()
const globalProps = useGlobalProperties()

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const coverInputRef = ref<HTMLInputElement | null>(null)
const showCropper = ref(false)
const selectedFile = ref<File | null>(null)
const imageUrl = ref('')
const imageLoaded = ref(false)

watch(() => props.modelValue, (newVal: string | undefined, oldVal: string | undefined) => {

  if (newVal) {
    const baseUrl = globalProps?.$config?.url || 'http://localhost:8080'

    // 判断是否是新版 API URL
    if (newVal.startsWith('/api/files/')) {
      imageUrl.value = `${baseUrl}${newVal}`

    } else if (newVal.startsWith('http')) {
      // 已经是完整 URL，直接使用
      imageUrl.value = newVal

    } else {
      // 相对路径，拼接 baseUrl
      imageUrl.value = `${baseUrl}/${newVal}`

    }

    // 重置加载状态
    imageLoaded.value = false

  } else {
    imageUrl.value = ''
    imageLoaded.value = false

  }
}, { immediate: true })

// 触发文件选择
const triggerUpload = () => {
  coverInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) {
    return
  }

  selectedFile.value = file
  showCropper.value = true

  target.value = ''
}

// 裁剪成功回调
const handleCropSuccess = (url: string) => {


  // ImageCropper 返回的已经是完整 URL（如 http://localhost:8080/api/files/5/view）
  // 我们需要提取相对路径用于存储
  const baseUrl = globalProps?.$config?.url || 'http://localhost:8080'

  let relativePath = ''
  if (url.startsWith(baseUrl)) {
    // 如果以 baseUrl 开头，提取后面的部分
    relativePath = url.replace(baseUrl, '')

  } else if (url.startsWith('/api/')) {
    // 如果已经是 /api/ 开头的相对路径
    relativePath = url

  } else {
    // 其他情况，直接使用
    relativePath = url
    console.warn('  -  URL 格式异常，直接使用:', relativePath)
  }

  //  先触发更新，再重置状态

  emit('update:modelValue', relativePath)


  message.success('封面上传成功')

  //  不要立即重置 selectedFile，等待下一个 tick
  setTimeout(() => {
    selectedFile.value = null

  }, 100)
}

// 裁剪取消回调
const handleCropCancel = () => {
  selectedFile.value = null
}

// 删除封面
const handleRemove = () => {
  emit('update:modelValue', '')
  message.success('封面已删除')
}

//  图片加载成功处理
const handleImageLoad = () => {

  imageLoaded.value = true
}

// 图片加载失败处理
const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  console.error('[CoverUpload] 图片加载失败')
  console.error('  - 失败的 URL:', img.src)
  console.error('  - 当前 modelValue:', props.modelValue)
  console.error('  - 当前 imageUrl:', imageUrl.value)

  // 尝试使用占位图
  img.src = '/placeholder.svg'
  imageLoaded.value = true
}

</script>

<style lang="scss" scoped>
.cover-upload-wrapper {
  display: inline-block;

  .cover-preview {
    position: relative;
    width: 320px;
    height: 240px;
    border-radius: 8px;
    overflow: hidden;
    border: 2px solid #e4e7ed;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    transition: all 0.3s;

    //  强制确保是圆角矩形，不是圆形
    border-radius: 8px !important;

    &:hover {
      border-color: #18a058;
      box-shadow: 0 4px 16px rgba(24, 160, 88, 0.3);

      .cover-mask {
        opacity: 1;
      }

      .cover-actions {
        opacity: 1;
      }
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;

      //  确保图片不会变成圆形
      border-radius: 0 !important;
    }

    //  加载占位样式
    .loading-placeholder {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background: #f5f5f5;
      color: #999;
      font-size: 12px;
    }

    .cover-mask {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.4);
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

    .cover-actions {
      position: absolute;
      bottom: 12px;
      left: 50%;
      transform: translateX(-50%);
      display: flex;
      gap: 8px;
      opacity: 0;
      transition: opacity 0.3s;
    }
  }

  .cover-uploader {
    width: 320px;
    height: 240px;
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

      .uploader-icon {
        color: #18a058;
      }

      .uploader-text {
        color: #18a058;
      }
    }

    .uploader-icon {
      font-size: 48px;
      color: #8c939d;
      transition: color 0.3s;
    }

    .uploader-text {
      margin-top: 12px;
      font-size: 14px;
      color: #606266;
      transition: color 0.3s;
    }

    .cover-tip {
      margin-top: 12px;
      font-size: 12px;
      color: #909399;
      line-height: 1.6;
      text-align: center;
    }
  }
}
</style>
