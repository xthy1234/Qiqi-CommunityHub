
import { defineStore } from 'pinia'

export const useVisitedStore = defineStore('visited', {
  state: () => ({
    viewedArticles: new Set<number | string>()
  }),
  actions: {
    markViewed(id: number | string) {
      this.viewedArticles.add(id)
    },
    hasViewed(id: number | string): boolean {
      return this.viewedArticles.has(id)
    },
    clearViewed(id: number | string) {
      this.viewedArticles.delete(id)
    },
    clearAll() {
      this.viewedArticles.clear()
    }
  }
})
