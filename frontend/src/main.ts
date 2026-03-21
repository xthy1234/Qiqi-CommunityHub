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
} from "naive-ui";

// Element Plus 样式和图标配置
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// Element Plus 国际化配置
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ElementPlus from 'element-plus'

// 地图组件初始化
import VueAMap, { initAMapApiLoader } from "@vuemap/vue-amap"
import "@vuemap/vue-amap/dist/style.css"

// WebSocket 初始化
import { initWebSocket, getWebSocket } from '@/utils/websocket'

// vue3-tiptap-editor 编辑器

// 工具模块导入
import httpClient from './utils/http'
import appConfig from './utils/config'
import utilityTools from './utils/toolUtil'
import menu from './utils/menu'
import httpCall from "./utils/http";

// 创建自定义组件
const app = createApp(App)

// 创建 Pinia 实例
const pinia = createPinia()

// 注册所有 Element Plus 图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 配置 Element Plus
app.use(ElementPlus, { locale: zhCn })


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

// 初始化 WebSocket（仅创建实例，不建立连接）
console.log('🔵 [WebSocket] 开始初始化...')
try {
  const wsUrl = `ws://${window.location.hostname}:8080/ws`
  console.log('🔵 [WebSocket] 准备连接:', wsUrl)
  initWebSocket(wsUrl)
  console.log('✅ [WebSocket] 初始化成功（等待登录后连接）')
} catch (error) {
  console.error('❌ [WebSocket] 初始化失败:', error)
}

// 挂载应用
app.use(pinia)
app.use(router)
app.mount('#app')

// 关键修改：应用挂载后检查登录状态并自动建立 WebSocket连接
;(async () => {
  try {
    // 检查用户是否已登录
    const token = utilityTools.storageGet('Token')
    const userId = utilityTools.storageGet('userid')
    
    if (token && userId) {
      console.log('🔵 [WebSocket] 检测到已登录用户，自动建立连接...')
      
      const ws = getWebSocket()
      if (ws && !ws.isConnected()) {
        await ws.connect()
        console.log('✅ [WebSocket] 自动连接成功')
      } else if (ws && ws.isConnected()) {
        console.log('✅ [WebSocket] 已经连接')
      } else {
        console.warn('⚠️ [WebSocket] 实例不存在')
      }
    } else {
      console.log('ℹ️ [WebSocket] 用户未登录，等待手动登录')
    }
  } catch (error) {
    console.error('❌ [WebSocket] 自动连接失败:', error)
  }
})()

// 导出应用实例供测试使用
export default app
