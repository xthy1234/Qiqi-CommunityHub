// src/api/upload.ts
import http from '@/utils/http'

/**
 * 文件上传服务
 */
export class UploadAPI {
  private uploadUrl = '/files' // 后端文件上传接口

  /**
   * 上传单个文件
   * @param file 要上传的文件
   * @returns Promise<ApiResponse>
   */
  async uploadFile(file: File): Promise<any> {
    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await http.post(this.uploadUrl, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      // 关键修复：直接返回 response.data，因为后端返回格式是 {code, msg, fileName, size, url}

      return response.data
    } catch (error) {
      console.error('❌ [UploadAPI] 文件上传失败:', error)
      throw error
    }
  }

  /**
   * 上传图片文件（带校验）
   * @param file 图片文件
   * @returns Promise<string | null> 返回图片URL或null
   */
  async uploadImage(file: File): Promise<string | null> {
    if (!file.type.startsWith('image/')) {
      console.warn('仅支持上传图片文件')
      return null
    }
    if (file.size > 10 * 1024 * 1024) { // 10MB
      console.warn('图片大小不能超过 10MB!')
      return null
    }

    const result = await this.uploadFile(file)
    // 关键修复：确保返回完整的 URL
    const url = result?.data?.url || result?.fileName || null
    if (url && !url.startsWith('http')) {
      // 如果是相对路径，添加后端地址
      const baseUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
      return `${baseUrl}${url}`
    }
    return url
  }

  /**
   * 上传任意格式文件
   * @param file 文件
   * @returns Promise<string | null> 返回文件URL或null
   */
  async uploadAnyFile(file: File): Promise<string | null> {
    const result = await this.uploadFile(file)
    // 关键修复：确保返回完整的 URL
    const url = result?.data?.url || result?.fileName || null
    if (url && !url.startsWith('http')) {
      // 如果是相对路径，添加后端地址
      const baseUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
      return `${baseUrl}${url}`
    }
    return url
  }
}

// 导出单例
export const uploadAPI = new UploadAPI()
