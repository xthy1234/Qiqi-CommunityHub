<template>
  <n-modal
    v-model:show="showModal"
    :mask-closable="false"
    preset="card"
    title="裁剪图片"
    style="width: 800px"
  >
    <div class="cropper-container">
      <div class="cropper-wrapper">
        <!--  添加 @change 事件监听裁剪框变化 -->
        <VueCropper
          ref="cropperRef"
          :img="imageSrc"
          :output-size="option.size"
          :output-type="option.outputType"
          :info="option.info"
          :can-scale="option.canScale"
          :auto-crop="option.autoCrop"
          :auto-crop-width="option.autoCropWidth"
          :auto-crop-height="option.autoCropHeight"
          :fixed="option.fixed"
          :fixed-number="option.fixedNumber"
          :full="option.full"
          :fixed-box="option.fixedBox"
          :can-move="option.canMove"
          :can-move-box="option.canMoveBox"
          :original="option.original"
          :center-box="option.centerBox"
          :height="option.height"
          :info-true="option.infoTrue"
          :max-img-size="option.maxImgSize"
          :enlarge="option.enlarge"
          :mode="option.mode"
          @real-time="handleRealTime"
          @change="handleChange"
        />
      </div>

      <!-- 预览区域 -->
      <div class="preview-section">

        <!-- 显示裁剪信息 -->
        <div class="crop-info-box">
          <div class="tip">
            · 拖动边框调整裁剪区域<br/>
            · 最终上传的图片会是裁剪框内的内容<br/>
            · 下方是图片的缩略图
          </div>
        </div>

        <div
          v-if="previews.url"
          class="thumbnail-preview"
        >
          <img
            :src="previews.url"
            alt="缩略图"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <div class="crop-controls">
          <n-space>
            <n-button @click="rotateLeft">
              <template #icon>
                <Icon icon="material-symbols:rotate-left" />
              </template>
              左旋转
            </n-button>
            <n-button @click="rotateRight">
              <template #icon>
                <Icon icon="material-symbols:rotate-right" />
              </template>
              右旋转
            </n-button>
            <n-button @click="scaleUp">
              <template #icon>
                <Icon icon="material-symbols:add" />
              </template>
              放大
            </n-button>
            <n-button @click="scaleDown">
              <template #icon>
                <Icon icon="material-symbols:remove" />
              </template>
              缩小
            </n-button>
          </n-space>
        </div>
        <div class="action-buttons">
          <n-button @click="handleCancel">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="uploading"
            @click="handleConfirm"
          >
            确认裁剪并上传
          </n-button>
        </div>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import { uploadAPI } from '@/api/upload'
import { useGlobalProperties } from '@/utils/globalProperties'

interface Props {
  show: boolean
  imageFile: File | null
  cropType?: 'avatar' | 'cover'
}

const props = withDefaults(defineProps<Props>(), {
  cropType: 'avatar'
})

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success', url: string): void
  (e: 'cancel'): void
}>()

const globalProps = useGlobalProperties()
const cropperRef = ref<any>(null)
const uploading = ref(false)
const imageSrc = ref('')

// 裁剪配置
const option = reactive({
  size: 1,
  outputType: 'jpeg',
  info: true,
  canScale: true,
  autoCrop: true,
  autoCropWidth: 0,
  autoCropHeight: 0,
  fixed: false,
  fixedNumber: [1, 1],
  full: false,
  fixedBox: false,
  canMove: true,
  canMoveBox: true,
  original: false,
  centerBox: true,
  height: 400,
  infoTrue: true,
  maxImgSize: 3000,
  enlarge: 1,
  mode: 'contain'
})

// 根据裁剪类型调整配置
if (props.cropType === 'avatar') {
  option.fixed = true
  option.fixedNumber = [1, 1]
  option.autoCropWidth = 200
  option.autoCropHeight = 200
  option.centerBox = true


} else if (props.cropType === 'cover') {
  option.fixed = true
  option.fixedNumber = [4, 3]
  option.autoCropWidth = 800
  option.autoCropHeight = 600
  option.centerBox = false





}

const previews = ref<any>({})
const showModal = ref(props.show)

// 监听 show 变化
watch(() => props.show, (newVal:  boolean) => {

  showModal.value = newVal
  if (newVal && props.imageFile) {

    const reader = new FileReader()
    reader.onload = (e) => {
      imageSrc.value = e.target?.result as string

    }
    reader.onerror = (err) => {
      console.error('[ImageCropper] 文件读取失败:', err)
    }
    reader.readAsDataURL(props.imageFile)
  } else {

  }
})

// 监听内部状态变化同步到父组件
watch(showModal, (newVal: boolean) => {

  emit('update:show', newVal)
})

//  实时预览 - 只更新 URL
const handleRealTime = (data: any) => {
  // 只保留 url，其他的不需要
  previews.value = {
    url: data.url
  }
}

//  @change 事件
const handleChange = (data: any) => {
  previews.value = {
    url: data.url
  }
}

// 左旋转
const rotateLeft = () => {

  cropperRef.value.rotateLeft()
}

// 右旋转
const rotateRight = () => {

  cropperRef.value.rotateRight()
}

// 放大
const scaleUp = () => {

  cropperRef.value.changeScale(1)
}

// 缩小
const scaleDown = () => {

  cropperRef.value.changeScale(-1)
}

// 取消
const handleCancel = () => {

  showModal.value = false
  emit('cancel')
}

// 确认裁剪并上传
const handleConfirm = async () => {

  if (!cropperRef.value) {
    console.error('[ImageCropper] cropperRef 不存在！')
    return
  }

  try {
    uploading.value = true



    // 获取裁剪后的 blob
    cropperRef.value.getCropBlob(async (blob: Blob) => {

      try {




        // 将 blob 转换为 file
        const fileName = props.imageFile?.name || 'cropped-image.jpg'
        const croppedFile = new File([blob], fileName, { type: blob.type })

        // 调用上传接口
        const response = await uploadAPI.uploadImage(croppedFile, '裁剪后的图片')


        if (response) {
          const baseUrl = globalProps.$config?.url || 'http://localhost:8080'
          let fullUrl = response

          // 处理 URL 格式
          if (!response.startsWith('http')) {
            fullUrl = `${baseUrl}${response}`
          }


          // 触发成功回调
          emit('success', fullUrl)
          showModal.value = false

          if (globalProps.$toolUtil?.message) {
            globalProps.$toolUtil.message('图片上传成功', 'success')
          }
        } else {
          console.error('[ImageCropper] 上传返回为空')
          throw new Error('上传失败')
        }
      } catch (error) {
        console.error('[ImageCropper] 图片上传失败:', error)
        if (globalProps.$toolUtil?.message) {
          globalProps.$toolUtil.message('图片上传失败', 'error')
        }
      } finally {
        uploading.value = false

      }
    })
  } catch (error) {
    console.error('[ImageCropper] 裁剪失败:', error)
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
.cropper-container {
  display: flex;
  gap: 20px;
  min-height: 450px;
}

.cropper-wrapper {
  flex: 1;
  height: 400px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.preview-section {
  width: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;

  .preview-title {
    font-size: 14px;
    color: #666;
    margin: 0;
  }

  // 裁剪信息框
  .crop-info-box {
    width: 100%;
    padding: 12px;
    background: #f5f5f5;
    border-radius: 6px;
    font-size: 12px;

    .info-item {
      margin-bottom: 6px;
      display: flex;
      justify-content: space-between;

      .label {
        color: #666;
      }

      .value {
        color: #333;
        font-weight: 500;
      }
    }

    .tip {
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid #ddd;
      color: #999;
      line-height: 1.6;
    }
  }

  // 缩略图预览（统一为长方形）
  .thumbnail-preview {
    max-width: 200px;
    max-height: 200px;
    border: 1px solid #ddd;
    border-radius: 8px;  //  圆角矩形
    overflow: hidden;
    background: #fff;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      max-width: 100%;
      max-height: 100%;
      width: auto;
      height: auto;
      object-fit: contain;
      display: block;
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .crop-controls {
    display: flex;
    gap: 12px;
  }

  .action-buttons {
    display: flex;
    gap: 12px;
  }
}
</style>
