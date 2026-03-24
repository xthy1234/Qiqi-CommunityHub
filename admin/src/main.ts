import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'


import httpClient from './utils/http'
import toolUtil from './utils/toolUtil'
import AppConfig from './utils/config'
import axios from 'axios'
import naive from 'naive-ui'

const app = createApp(App)
const pinia = createPinia()

// 地图配置

// 注册所有图标
app.use(naive)
app.use(pinia)
app.use(router)

// 挂载全局属性和方法
app.config.globalProperties.$http = httpClient
app.config.globalProperties.$toolUtil = toolUtil
app.config.globalProperties.$config = AppConfig.getApiConfig()

app.mount('#app')
