import { defineStore } from 'pinia'

/**
 * 音频项接口定义
 */
interface AudioItem {
  id: string | number
  title: string
  url: string
  [key: string]: any
}

/**
 * 使用 Pinia 定义音频 store
 */
export const useAudioStore = defineStore('audio', {
  state: () => ({
    /** 音频列表 */
    audioList: [] as AudioItem[],
    /** 当前音频索引 */
    audioIndex: -1 as number
  }),
  
  getters: {
    /** 获取当前音频 */
    currentAudio: (state): AudioItem | null => {
      return state.audioIndex >= 0 && state.audioIndex < state.audioList.length
        ? state.audioList[state.audioIndex]
        : null
    },
    
    /** 获取音频列表长度 */
    audioListLength: (state): number => {
      return state.audioList.length
    },
    
    /** 检查是否有音频 */
    hasAudio: (state): boolean => {
      return state.audioList.length > 0
    }
  },
  
  actions: {
    /** 初始化音频状态 */
    initializeAudioState(): void {
      this.audioList = []
      this.audioIndex = -1
    },
    
    /** 设置音频列表 */
    setAudioList(audioList: AudioItem[]): void {
      this.audioList = audioList
    },
    
    /** 设置当前音频索引 */
    setCurrentAudioIndex(index: number): void {
// console.log('Setting current audio index:', index)
      this.audioIndex = index
    },

    /** 设置最后一个音频为当前播放 */
    setCurrentToLastAudio(): void {
      const lastIndex = Math.max(0, this.audioList.length - 1)
      this.audioIndex = lastIndex
    },

    /** 删除指定索引的音频 */
    removeAudioByIndex(index: number): void {
      if (index < 0 || index >= this.audioList.length) {
        console.warn('Invalid audio index for removal:', index)
        return
      }

      // 调整当前播放索引
      if (index === this.audioIndex) {
        if (this.audioIndex > 0) {
          this.audioIndex = this.audioIndex - 1
        }
      } else if (index < this.audioIndex) {
        this.audioIndex = this.audioIndex - 1
      }

      // 从列表中移除音频
      this.audioList.splice(index, 1)
    }
  }
})
