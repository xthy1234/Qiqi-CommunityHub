import { defineStore } from 'pinia'
import { RouteRecordRaw } from 'vue-router'

interface ViewState {
  collapse: boolean
  visitedViews: Array<any>
  cachedViews: Array<string>
  routes: RouteRecordRaw[]
  addRoutes: RouteRecordRaw[]
}

export const useAppStore = defineStore('app', {
  state: (): ViewState => ({
    collapse: false,
    visitedViews: [],
    cachedViews: [],
    routes: [],
    addRoutes: []
  }),

  actions: {
    SET_ROUTES(routes: RouteRecordRaw[]) {
      this.routes = routes
    },
    ADD_VISITED_VIEW(view: any) {
      if (this.visitedViews.some((v: any) => v.path === view.path)) return
      this.visitedViews.push(Object.assign({}, view, {
        title: view.meta?.title || 'no-name'
      }))
    }
    // ... 其他方法
  }
})
