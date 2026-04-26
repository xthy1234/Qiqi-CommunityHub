NEW_FILE_CODE
<template>
  <n-card
    :bordered="false"
    class="carousel-card"
  >
    <n-spin
      v-if="isLoading"
      description="加载轮播图中..."
    >
      <n-skeleton
        text
        style="height: 350px; width: 100%"
      />
    </n-spin>
    <n-carousel
      v-else-if="images.length > 0"
      show-arrow
      autoplay
      class="custom-carousel"
    >
      <img
        v-for="(item, index) in images"
        :key="item.id || index"
        class="carousel-image"
        :src="getImageUrl(item)"
        :alt="item.title || '轮播图'"
        @error="handleImageError"
        @click="handleClick(item)"
      />
    </n-carousel>
    <n-empty
      v-else
      description="暂无轮播图"
    />
  </n-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { normalizeFileUrl } from '@/utils/fileUrl'

interface CarouselItem {
  id?: number | string
  imageUrl?: string
  linkUrl?: string
  title?: string
  [key: string]: any
}

const props = defineProps<{
  apiEndpoint?: string
}>()

const emit = defineEmits<{
  (e: 'click', item: CarouselItem): void
}>()

const appContext = useGlobalProperties()
const images = ref<CarouselItem[]>([])
const isLoading = ref<boolean>(false)

const getImageUrl = (item: CarouselItem): string => {
  const url = item.imageUrl || item.linkUrl || ''
  if (!url) {return ''}
  
  return normalizeFileUrl(url)
}

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = '/placeholder.svg'
}

const handleClick = (item: CarouselItem) => {
  emit('click', item)
}

const fetchCarouselImages = async (): Promise<void> => {
  isLoading.value = true
  try {
    const endpoint = props.apiEndpoint || 'swipers/enabled'
    const response = await appContext?.$http.get(endpoint)
    const apiData = response.data.data
    images.value = Array.isArray(apiData) ? apiData : (apiData?.list || [])
  } catch (error) {
    console.error('[Carousel] 加载轮播图失败:', error)
    images.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchCarouselImages()
})
</script>

<style lang="scss" scoped>
.carousel-card {
  margin-bottom: 30px;
  overflow: hidden;

  :deep(.n-carousel) {
    max-width: 100%;
  }

  .carousel-image {
    width: 100%;
    height: 350px;
    object-fit: cover;
    cursor: pointer;
    border-radius: 8px;
    transition: transform 0.3s;

    &:hover {
      transform: scale(1.02);
    }
  }
}

@media (max-width: 768px) {
  .carousel-card {
    .carousel-image {
      height: 200px;
    }
  }
}
</style>
