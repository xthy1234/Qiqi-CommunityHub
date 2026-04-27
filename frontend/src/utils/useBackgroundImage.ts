import { ref, onMounted } from 'vue'
import { BACKGROUND_SOURCES, MAX_CACHE_AGE, MAX_USE_COUNT, type BackgroundSource } from '@/config/backgroundConfig'

let isLoading = false
let isUpdating = false

export function useBackgroundImage() {
  const backgroundImageUrl = ref<string>('')
  const imageLoaded = ref<boolean>(false)
  
  const CACHE_KEY_URL = 'loginBackgroundImage'
  const CACHE_KEY_TIME = 'loginBackgroundImageTime'
  const CACHE_KEY_COUNT = 'loginBackgroundUseCount'

  const loadBackgroundImage = (isSilentUpdate = false) => {
    if (isLoading && !isSilentUpdate) return

    console.log(`[背景图] ${isSilentUpdate ? '后台静默更新' : '开始加载流程'}...`)
    
    const cachedUrl = sessionStorage.getItem(CACHE_KEY_URL)
    const cachedTime = sessionStorage.getItem(CACHE_KEY_TIME)
    const cachedCount = parseInt(sessionStorage.getItem(CACHE_KEY_COUNT) || '0', 10)

    if (cachedUrl && cachedTime) {
      const now = Date.now()
      const cacheAge = now - parseInt(cachedTime, 10)
      
      if (!isSilentUpdate) {
        backgroundImageUrl.value = cachedUrl
        imageLoaded.value = true
      }

      const isExpired = cacheAge >= MAX_CACHE_AGE
      const isOverused = cachedCount >= MAX_USE_COUNT

      if ((isExpired || isOverused) && !isUpdating && !isSilentUpdate) {
        console.log('[背景图] 缓存已过期或使用过频，启动后台静默更新...')
        isUpdating = true
        sessionStorage.setItem(CACHE_KEY_COUNT, '0')
        fetchNewImage(true) 
      } else if (!isSilentUpdate) {
        sessionStorage.setItem(CACHE_KEY_COUNT, (cachedCount + 1).toString())
      }
      return
    }

    fetchNewImage(false)
  }

  const fetchNewImage = (isUpdate: boolean) => {
    isLoading = true
    const randomIndex = Math.floor(Math.random() * BACKGROUND_SOURCES.length)
    tryLoadFromSource(randomIndex, isUpdate)
  }

  const tryLoadFromSource = async (index: number, isUpdate: boolean) => {
    if (index >= BACKGROUND_SOURCES.length) {
      console.error('[背景图] 所有源均加载失败')
      isLoading = false
      isUpdating = false
      return
    }

    const currentSource = BACKGROUND_SOURCES[index % BACKGROUND_SOURCES.length]
    
    try {
      let finalImageUrl = ''

      if (currentSource.type === 'json') {
        const queryParams = new URLSearchParams(currentSource.params || {}).toString()
        const apiUrl = `${currentSource.url}${queryParams ? '?' + queryParams : ''}`
        const response = await fetch(apiUrl, { headers: currentSource.headers || {} })
        if (!response.ok) throw new Error('API Error')
        const data = await response.json()
        
        if (currentSource.id.startsWith('nekosia')) {
          finalImageUrl = data.image?.compressed?.url || ''
        } else if (currentSource.id.startsWith('nekos_best')) {
          finalImageUrl = data.results?.[0]?.url || ''
        }
      } else {
        finalImageUrl = currentSource.url 
      }

      if (!finalImageUrl) throw new Error('No URL found')

      const img = new Image()
      img.onload = () => {
        const realUrl = img.src 

        console.log(`[背景图] 图片加载成功 (${isUpdate ? '新图' : '首屏'})`)

        backgroundImageUrl.value = realUrl
        sessionStorage.setItem(CACHE_KEY_URL, realUrl)
        sessionStorage.setItem(CACHE_KEY_TIME, Date.now().toString())
        sessionStorage.setItem(CACHE_KEY_COUNT, '0')
        
        if (!isUpdate) {
          imageLoaded.value = true
        }
        
        isLoading = false
        isUpdating = false
      }
      img.onerror = () => {
        console.warn(`[背景图] 图片加载失败，尝试下一个源...`)
        tryLoadFromSource(index + 1, isUpdate)
      }
      img.src = finalImageUrl

    } catch (error) {
      console.warn(`[背景图] 源请求出错:`, error)
      tryLoadFromSource(index + 1, isUpdate)
    }
  }

  onMounted(() => {
    loadBackgroundImage()
  })

  return {
    backgroundImageUrl,
    imageLoaded
  }
}
