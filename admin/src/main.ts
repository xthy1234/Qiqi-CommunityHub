import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import httpClient from './utils/http'
import toolUtil from './utils/toolUtil'
import AppConfig from './utils/config'
import axios from 'axios'

const app = createApp(App)
const pinia = createPinia()

// 地图配置
import VueAMap, { initAMapApiLoader } from "@vuemap/vue-amap"
import "@vuemap/vue-amap/dist/style.css"

initAMapApiLoader({
  key: "your-key",
  // ... 其他配置
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.use(VueAMap)
app.use(pinia)
app.use(router)

// 挂载全局属性和方法
app.config.globalProperties.$http = httpClient
app.config.globalProperties.$toolUtil = toolUtil
app.config.globalProperties.$config = AppConfig.getApiConfig()

app.mount('#app')
