import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import '@/assets/css/iconfont.css'
import {
  NButton, 
  NInput,
  NCard,
  NLayout,
  NForm,
  NFormItem,
  NSelect,
  NCheckbox,
  NSteps,
  NStep,
  NMessageProvider,
  NSpin,
  NCarousel,
  NEmpty,
  NGrid,
  NGridItem,
  NUpload,
  NBreadcrumb,
  NBreadcrumbItem,
  NPagination,
  NModal,
  NTag,
  NSpace,
  NDatePicker,
  NAlert,
  NImage,
  NAvatar,
  NDivider,
  NDialogProvider,
  NDropdown,
  NRadio,
  NRadioGroup
} from "naive-ui"

// 地图组件初始化
import VueAMap, { initAMapApiLoader } from "@vuemap/vue-amap"
import "@vuemap/vue-amap/dist/style.css"

// WebSocket 初始化
import { connectWebSocketOnStartup } from '@/utils/websocketInit'

// vue3-tiptap-editor 编辑器

// 工具模块导入
import httpClient from './utils/http'
import appConfig from './utils/config'
import utilityTools from './utils/toolUtil'
import menu from './utils/menu'
import httpCall from "./utils/http"

// 创建自定义组件
const app = createApp(App)

// 创建 Pinia 实例
const pinia = createPinia()

//naive ui 组件
app.component('NButton', NButton)
app.component('NInput', NInput)
app.component('NCard', NCard)
app.component('NLayout', NLayout)
app.component('NForm', NForm)
app.component('NFormItem', NFormItem)
app.component('NSelect', NSelect)
app.component('NCheckbox', NCheckbox)
app.component('NSteps', NSteps)
app.component('NStep', NStep)
app.component('NMessageProvider', NMessageProvider)
app.component('NSpin', NSpin)
app.component('NCarousel', NCarousel)
app.component('NEmpty', NEmpty)
app.component('NGrid', NGrid)
app.component('NGridItem', NGridItem)
app.component('NUpload', NUpload)
app.component('NBreadcrumb', NBreadcrumb)
app.component('NBreadcrumbItem', NBreadcrumbItem)
app.component('NPagination', NPagination)
app.component('NModal', NModal)
app.component('NTag', NTag)
app.component('NSpace', NSpace)
app.component('NDatePicker', NDatePicker)
app.component('NAlert', NAlert)
app.component('NImage', NImage)
app.component('NAvatar', NAvatar)
app.component('NDivider', NDivider)
app.component('NDialogProvider', NDialogProvider)
app.component('NDropdown', NDropdown)
app.component('NRadio', NRadio)
app.component('NRadioGroup', NRadioGroup)

// 初始化地图服务 - 使用环境变量或默认值
try {
  const amapKey = process.env.VUE_APP_AMAP_KEY || 'your_amap_key_here'
  const amapSecurityCode = process.env.VUE_APP_AMAP_SECURITY_CODE || ''
  
  if (amapKey && amapKey !== 'your_amap_key_here') {
    initAMapApiLoader({
      key: amapKey,
      securityJsCode: amapSecurityCode,
      plugins: [
        "AMap.Autocomplete",
        "AMap.PlaceSearch",
        "AMap.Scale",
        "AMap.OverView",
        "AMap.ToolBar",
        "AMap.MapType",
        "AMap.PolyEditor",
        "AMap.CircleEditor",
        "AMap.Geocoder",
        "AMap.Geolocation",
        "AMap.Marker"
      ]
    })
    app.use(VueAMap)
  }
} catch (mapInitError) {
// console.warn('地图服务初始化失败:', mapInitError)
}

// 注册自定义组件
// app.component('editor', Editor)
// app.component('uploads', Upload)

// 全局属性配置 - 确保正确的顺序和配置
app.config.globalProperties.$config = appConfig.getApiConfig()
app.config.globalProperties.$project = appConfig.getProjectInfo().projectName
app.config.globalProperties.$toolUtil = utilityTools
app.config.globalProperties.$http = httpCall

// 挂载 menu 到全局属性
app.config.globalProperties.$menu = menu

// 挂载应用
app.use(pinia)
app.use(router)
app.mount('#app')

// 添加全局消息工具（在应用挂载后）
app.config.globalProperties.$message = app.config.globalProperties.$message || {}
app.config.globalProperties.$dialog = app.config.globalProperties.$dialog || {}
app.config.globalProperties.$notification = app.config.globalProperties.$notification || {}

// 导出应用实例供测试使用
export default app

async function bootstrap() {
  const app = createApp(App)
  
  // 设置全局 Vue 实例引用（供 WebSocket 使用）
  ;(window as any).__vue_app__ = app
  
  // 挂载 Pinia
  app.use(pinia)
  
  // 挂载路由
  app.use(router)
  
  // 初始化 WebSocket（异步，不阻塞应用启动）
  connectWebSocketOnStartup({
    debug: process.env.NODE_ENV === 'development',
    heartbeatInterval: 30000,
    reconnectInterval: 5000,
    maxReconnectAttempts: 5
  }).catch(error => {
    console.error('WebSocket 初始化失败:', error)
  })

  app.mount('#app')
}
