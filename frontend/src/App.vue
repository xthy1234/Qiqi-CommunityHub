<template>
  <n-message-provider>
    <n-dialog-provider>
      <router-view />
    </n-dialog-provider>
  </n-message-provider>
</template>

<script setup lang="ts">
/**
 * 防抖函数实现
 * @param fn 需要防抖的函数
 * @param delay 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
const createDebounce = <T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): ((...args: Parameters<T>) => void) => {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  return function(...args: Parameters<T>): void {
    const context = this

    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    timeoutId = setTimeout(() => {
      try {
        fn.apply(context, args)
      } catch (error) {
        // 忽略因 DOM 元素不存在导致的错误
        if (error instanceof TypeError &&
            error.message.includes('parameter 1 is not of type')) {
         return
        }
        throw error
      }
    }, delay)
  }
}

/**
 * 扩展 ResizeObserver 类，添加防抖功能
 */
const OriginalResizeObserver = window.ResizeObserver

// 重写 ResizeObserver 以添加防抖功能
window.ResizeObserver = class EnhancedResizeObserver extends OriginalResizeObserver {
  constructor(callback: ResizeObserverCallback) {
    // 为回调函数添加防抖处理
    const debouncedCallback = createDebounce((entries: ResizeObserverEntry[], observer: ResizeObserver) => {
      try {
        callback(entries, observer)
      } catch (error) {
        // 忽略因 DOM 元素不存在导致的错误
        if (error instanceof TypeError &&
            error.message.includes('parameter 1 is not of type')) {
         return
        }
        throw error
      }
    }, 16)
    super(debouncedCallback)
  }
}
</script>

<style lang="scss">
body {
  margin: 0;
}

* {
  box-sizing: border-box;
}

.app-contain {
  width: 100%;
}

.section_title {
  border: 0px solid #ddd;
  border-radius: 0px;
  padding: 0 0 0 80px;
  margin: 20px 0;
  color: #fff;
  background: url(http://clfile.zggen.cn/20231229/c16b1e8143674870b2388c54506a0997.png) no-repeat left center / 99% 100%;
  font-weight: 500;
  letter-spacing: 2px;
  width: 100%;
  font-size: 16px;
  line-height: 40px;
  text-align: left;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

nav {
  padding: 30px;

  a {
    font-weight: bold;
    color: #2c3e50;

    &.router-link-exact-active {
      color: #42b983;
    }
  }
}
</style>
