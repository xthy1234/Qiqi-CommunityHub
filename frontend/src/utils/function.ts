/**
 * 防抖函数
 * @param fn 要执行的函数
 * @param delay 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: any[]) => any>(
    fn: T,
    delay: number
): (...args: Parameters<T>) => void {
    let timer: ReturnType<typeof setTimeout> | null = null
    
    return function(this: any, ...args: Parameters<T>) {
        if (timer) {
            clearTimeout(timer)
        }
        
        timer = setTimeout(() => {
            fn.apply(this, args)
        }, delay)
    }
}

/**
 * 节流函数
 * @param fn 要执行的函数
 * @param interval 间隔时间（毫秒）
 * @returns 节流后的函数
 */
export function throttle<T extends (...args: any[]) => any>(
    fn: T,
    interval: number
): (...args: Parameters<T>) => void {
    let lastTime = 0
    
    return function(this: any, ...args: Parameters<T>) {
        const now = Date.now()
        
        if (now - lastTime >= interval) {
            lastTime = now
            fn.apply(this, args)
        }
    }
}
