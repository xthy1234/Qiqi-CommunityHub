<template>
  <n-config-provider :theme="null">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui'
import type * as ECharts from 'echarts'
import { provide, defineComponent } from 'vue'

// 引入 ECharts 并全局注入
import * as echarts from 'echarts'

/**
 * 全局依赖注入
 * 向所有子组件提供 ECharts 实例和基础 API URL
 */
provide<ECharts.ECharts>('echarts', echarts)
provide<string>('baseUrl', process.env.VUE_APP_BASE_API || '')

/**
 * 防抖函数类型定义
 * @param fn - 需要防抖的函数
 * @param delay - 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
type DebounceFunction<T extends (...args: any[]) => void> = (...args: Parameters<T>) => void

/**
 * 创建防抖函数
 * 在指定时间内只执行最后一次调用
 * @param fn - 需要防抖的函数
 * @param delay - 延迟时间（毫秒）
 * @returns 防抖处理后的函数
 */
const createDebounce = <T extends (...args: any[]) => void>(fn: T, delay: number): DebounceFunction<T> => {
	let timerId: ReturnType<typeof setTimeout> | null = null
	
	return function(this: any, ...args: Parameters<T>) {
		const context = this

		// 清除之前的定时器
		if (timerId) {
			clearTimeout(timerId)
		}

		// 设置新的定时器
		timerId = setTimeout(() => {
			fn.apply(context, args)
		}, delay)
	}
}

/**
 * 自定义 ResizeObserver 类
 * 对原生的 ResizeObserver 进行防抖包装，避免频繁触发回调
 * @constructor
 */
interface ResizeObserverCallback {
	(entries: ResizeObserverEntry[], observer: ResizeObserver): void
}

class DebouncedResizeObserver extends window.ResizeObserver {
	constructor(callback: ResizeObserverCallback) {
		// 使用防抖函数包装回调，延迟 16ms（约 60fps）
		const debouncedCallback = createDebounce(callback, 16)
		super(debouncedCallback)
	}
}

// 替换全局 ResizeObserver 为防抖版本
window.ResizeObserver = DebouncedResizeObserver
</script>

<style lang="scss">
#app {
	font-family: Avenir, Helvetica, Arial, sans-serif;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;
	color: #2c3e50;
}

// ==================== 表单模型按钮样式 ====================
.formModel_btn_box {
	display: flex;
	width: 100%;
	justify-content: center;
	align-items: center;

	// 取消按钮
	.formModel_cancel {
		border: 0;
		cursor: pointer;
		border-radius: 4px;
		padding: 0 24px;
		margin: 0 20px 0 0;
		color: #fff;
		background: linear-gradient(180deg, rgba(255,228,218,1) 0%, rgba(246,192,173,1) 50%, rgba(255,151,112,1) 51%, rgba(242,90,32,1) 100%);
		width: auto;
		font-size: 16px;
		min-width: 110px;
		height: 40px;
	}

	// 确定按钮
	.formModel_confirm {
		border: 0;
		cursor: pointer;
		border-radius: 4px;
		padding: 0 24px;
		margin: 0 20px 0 0;
		color: #fff;
		background: linear-gradient(180deg, rgba(191,187,233,1) 0%, rgba(139,133,203,1) 50%, rgba(111,100,203,1) 51%, rgba(93,83,181,1) 100%);
		width: auto;
		font-size: 16px;
		min-width: 110px;
		height: 40px;
	}
}

// ==================== 应用内容区域样式 ====================
.app-contain {
	padding: 20px 20px;
	margin: 0;
	color: #666;
	width: 100%;
	font-size: 14px;
	min-height: 100vh;
	position: relative;
	height: auto;
}

// 全局重置
body {
	margin: 0;
}

* {
	box-sizing: border-box;
}

// ==================== Element Plus 组件样式覆盖 ====================
.el-select .el-input {
	font-size: inherit;
}

.el-input__inner {
	color: inherit;
}

// 导航栏样式
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