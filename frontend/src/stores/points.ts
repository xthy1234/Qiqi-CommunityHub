// src/stores/points.ts
import { defineStore } from 'pinia'
import signInService, { type PointsInfo, type PointsTransaction, type PaginationParams } from '@/api/signIn'

interface PointsState {
  points: number                      // 当前积分
  streak: number                      // 连续签到天数
  signedInToday: boolean              // 今日是否已签到
  lastSignInDate?: string             // 上次签到日期
  transactions: PointsTransaction[]   // 积分流水列表
  transactionTotal: number            // 流水总数
  loading: boolean                    // 加载状态
}

export const usePointsStore = defineStore('points', {
  state: (): PointsState => ({
    points: 0,
    streak: 0,
    signedInToday: false,
    lastSignInDate: undefined,
    transactions: [],
    transactionTotal: 0,
    loading: false
  }),

  getters: {
    /**
     * 是否可以签到
     */
    canSignIn: (state): boolean => {
      return !state.signedInToday
    },

    /**
     * 获取今日签到奖励提示
     */
    signInBonusText: (state): string => {
      if (state.signedInToday) {
        return '今日已签到'
      }
      
      const nextStreak = state.streak + 1
      let bonusText = '基础奖励 10 积分'
      
      if (nextStreak >= 7) {
        bonusText += ' + 连续 7 天额外 20 积分'
      } else if (nextStreak >= 3) {
        bonusText += ' + 连续 3 天额外 10 积分'
      }
      
      return bonusText
    }
  },

  actions: {
    /**
     * 加载积分信息
     */
    async loadPointsInfo() {
      try {
        this.loading = true
        
        // 并行调用获取完整数据
        const [pointsData, statusData] = await Promise.all([
          signInService.getPointsInfo(),
          signInService.getSignInStatus()
        ])
        
        this.points = pointsData.points
        this.streak = pointsData.streak
        this.signedInToday = pointsData.signedIn
        this.lastSignInDate = pointsData.lastSignInDate
      } catch (error) {
        console.error('加载积分信息失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 执行签到
     */
    async signIn() {
      if (this.signedInToday) {
        throw new Error('今日已签到')
      }

      try {
        this.loading = true
        const result = await signInService.doSignIn()
        
        // 更新积分信息
        this.points = result.balance
        this.streak = result.streak
        this.signedInToday = true
        
        return result
      } catch (error) {
        console.error('签到失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 刷新签到状态
     */
    async refreshSignInStatus() {
      try {
        const status = await signInService.getSignInStatus()
        this.signedInToday = status.signedIn
      } catch (error) {
        console.error('刷新签到状态失败:', error)
      }
    },

    /**
     * 加载积分流水
     * @param params 查询参数
     */
    async loadTransactions(params: PaginationParams) {
      try {
        this.loading = true
        const result = await signInService.getTransactions(params)
        
        this.transactions = result.list
        this.transactionTotal = result.total
        
        return result
      } catch (error) {
        console.error('加载积分流水失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 清空积分状态（退出登录时调用）
     */
    resetPointsState() {
      this.points = 0
      this.streak = 0
      this.signedInToday = false
      this.lastSignInDate = undefined
      this.transactions = []
      this.transactionTotal = 0
    }
  }
})
