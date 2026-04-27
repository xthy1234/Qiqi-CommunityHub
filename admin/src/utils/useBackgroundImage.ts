import { ref, onMounted } from 'vue'
import { BACKGROUND_SOURCES, MAX_CACHE_AGE, MAX_USE_COUNT, type BackgroundSource } from '@/config/backgroundConfig'

let isLoading = false
let isUpdating = false // 标记是否正在后台更新

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

    // 1. 如果有缓存，先立即显示缓存（保证秒开）
    if (cachedUrl && cachedTime) {
      const now = Date.now()
      const cacheAge = now - parseInt(cachedTime, 10)
      
      // 如果不是静默更新，先展示旧图
      if (!isSilentUpdate) {
        backgroundImageUrl.value = cachedUrl
        imageLoaded.value = true
      }

      // 2. 判断是否需要触发后台更新（时间过期 或 使用次数过多）
      const isExpired = cacheAge >= MAX_CACHE_AGE
      const isOverused = cachedCount >= MAX_USE_COUNT

      if ((isExpired || isOverused) && !isUpdating && !isSilentUpdate) {
        console.log('[背景图] 缓存已过期或使用过频，启动后台静默更新...')
        isUpdating = true
        // 重置计数器
        sessionStorage.setItem(CACHE_KEY_COUNT, '0')
        fetchNewImage(true) 
      } else if (!isSilentUpdate) {
        // 没过期，正常增加使用次数
        sessionStorage.setItem(CACHE_KEY_COUNT, (cachedCount + 1).toString())
      }
      return
    }

    // 3. 没有缓存，直接加载新图
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
        // 【关键修复】对于 direct 类型，我们不能直接存接口 URL，必须通过 Image 对象拿到最终跳转后的 URL
        // 这里我们先发起一个 HEAD 请求或者依靠 Image onload 后的 src
        finalImageUrl = currentSource.url 
      }

      if (!finalImageUrl) throw new Error('No URL found')

      // 预加载图片以获取真实 URL 并确保可显示
      const img = new Image()
      img.onload = () => {
        // img.src 此时已经是经过重定向后的真实图片地址了
        const realUrl = img.src 

        console.log(`[背景图] 图片加载成功 (${isUpdate ? '新图' : '首屏'})`)

        // 更新状态和缓存
        backgroundImageUrl.value = realUrl
        sessionStorage.setItem(CACHE_KEY_URL, realUrl)
        sessionStorage.setItem(CACHE_KEY_TIME, Date.now().toString())
        sessionStorage.setItem(CACHE_KEY_COUNT, '0') // 换新图后重置计数
        
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


