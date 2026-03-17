// src/stores/user.ts
import { defineStore } from 'pinia'

interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
  avatar?: string
  role?: string
  phone?: string
}

interface UserState {
  token: string
  userInfo: UserInfo | null
  userId: number | null
  userRole: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    userInfo: null,
    userId: null,
    userRole: ''
  }),

  getters: {
    isLoggedIn(): boolean {
      return !!this.token
    },

    getUserInfo(): UserInfo | null {
      return this.userInfo
    },

    getUserId(): number | null {
      return this.userId
    },

    getUserRole(): string {
      return this.userRole
    }
  },

  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('Token', token)
    },

    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
      if (userInfo.id) {
        this.userId = userInfo.id
        localStorage.setItem('userId', String(userInfo.id))
      }
      if (userInfo.username) {
        localStorage.setItem('userName', userInfo.username)
      }
      if (userInfo.role) {
        this.userRole = userInfo.role
        localStorage.setItem('userRole', userInfo.role)
      }
    },

    loadUserFromStorage() {
      const token = localStorage.getItem('Token')
      const userId = localStorage.getItem('userId')
      const userName = localStorage.getItem('userName')
      const userRole = localStorage.getItem('userRole')

      if (token) {
        this.token = token
      }

      if (userId || userName || userRole) {
        this.userInfo = {
          id: userId ? Number(userId) : 0,
          username: userName || '',
          role: userRole || ''
        }
        this.userId = userId ? Number(userId) : null
        this.userRole = userRole || ''
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      this.userId = null
      this.userRole = ''
      
      localStorage.removeItem('Token')
      localStorage.removeItem('userId')
      localStorage.removeItem('userName')
      localStorage.removeItem('userRole')
      localStorage.removeItem('adminName')
    }
  }
})
