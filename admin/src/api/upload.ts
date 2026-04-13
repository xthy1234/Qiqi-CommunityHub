// src/api/upload.ts
import http from '@/utils/http'
import { normalizeFileUrl, getFileViewUrl } from '@/utils/fileUrl'

/**
 * 文件上传响应类型
 * 
 * 【重要】根据后端实际响应结构调整
 * 当前后端返回格式（嵌套结构）：
 * {
 *   "code": 0,
 *   "msg": "文件上传成功",
 *   "file": {
 *     "id": 4,
 *     "fileName": "...",
 *     "fileUrl": "/api/files/4",
 *     "viewUrl": "/api/files/4/view",
 *     "downloadUrl": "/api/files/4/download",
 *     ...
 *   }
 * }
 */
export interface UploadResponse {
  code: number
  msg: string
  fileId: number
  fileName: string
  storedFileName: string
  fileUrl: string
  viewUrl: string
  downloadUrl: string
  thumbnailUrl?: string | null
  readableSize: string
  mimeType: string
  fileType: 'image' | 'video' | 'document'
  width?: number | null
  height?: number | null
  duration?: number | null
  md5: string
}

interface BackendFileObject {
  id: number
  fileName: string
  originalFileName: string
  fileUrl: string
  viewUrl: string
  downloadUrl: string
  thumbnailUrl: string | null
  fileSize: number
  readableSize: string
  mimeType: string
  fileType: 'image' | 'video' | 'document'
  width: number | null
  height: number | null
  duration: number | null
  uploaderId: number
  uploaderNickname: string | null
  uploadTime: string
  isPublic: boolean
  status: number
  description: string
  downloadTimes: number
}

interface BackendUploadResponse {
  code: number
  msg: string
  file: BackendFileObject
}

/**
 * 文件上传服务
 */
export class UploadAPI {
  private uploadUrl = '/api/files/upload'

  /**
   * 上传单个文件
   * @param file 要上传的文件
   * @param fileType 文件类型分类：image/video/document
   * @param description 文件描述
   * @param isPublic 是否公开，默认 true
   * @returns Promise<UploadResponse>
   */
  async uploadFile(
    file: File,
    fileType: 'image' | 'video' | 'document' = 'image',
    description: string = '',
    isPublic: boolean = true
  ): Promise<UploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileType', fileType)
    formData.append('description', description)
    formData.append('isPublic', String(isPublic))

    try {
      const response = await http.post(this.uploadUrl, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      const backendData = response.data as unknown as BackendUploadResponse
      
      if (!backendData.file) {
        throw new Error('上传响应格式错误：缺少 file 对象')
      }

      const result: UploadResponse = {
        code: backendData.code,
        msg: backendData.msg,
        fileId: backendData.file.id,
        fileName: backendData.file.originalFileName,
        storedFileName: backendData.file.fileName,
        fileUrl: backendData.file.fileUrl,
        viewUrl: backendData.file.viewUrl,
        downloadUrl: backendData.file.downloadUrl,
        thumbnailUrl: backendData.file.thumbnailUrl,
        readableSize: backendData.file.readableSize,
        mimeType: backendData.file.mimeType,
        fileType: backendData.file.fileType,
        width: backendData.file.width,
        height: backendData.file.height,
        duration: backendData.file.duration,
        md5: ''
      }
      
      return result
    } catch (error) {
      console.error('❌ [UploadAPI] 文件上传失败:', error)
      throw error
    }
  }

  /**
   * 上传图片文件（带校验）
   * @param file 图片文件
   * @param description 图片描述
   * @returns Promise<string | null> 返回图片预览URL或null
   */
  async uploadImage(file: File, description: string = ''): Promise<string | null> {
    if (!file.type.startsWith('image/')) {
      console.warn('仅支持上传图片文件')
      return null
    }
    if (file.size > 10 * 1024 * 1024) {
      console.warn('图片大小不能超过 10MB!')
      return null
    }

    try {
      const result = await this.uploadFile(file, 'image', description)
      
      if (result.code === 0) {
        return result.viewUrl
      } else {
        console.error('上传失败:', result.msg)
        return null
      }
    } catch (error) {
      console.error('❌ [UploadAPI] 图片上传异常:', error)
      return null
    }
  }

  /**
   * 上传视频文件
   * @param file 视频文件
   * @param description 视频描述
   * @returns Promise<string | null> 返回视频预览URL或null
   */
  async uploadVideo(file: File, description: string = ''): Promise<string | null> {
    if (!file.type.startsWith('video/')) {
      console.warn('仅支持上传视频文件')
      return null
    }
    if (file.size > 512 * 1024 * 1024) {
      console.warn('视频大小不能超过 512MB!')
      return null
    }

    try {
      const result = await this.uploadFile(file, 'video', description)
      
      if (result.code === 0) {
        return result.viewUrl
      } else {
        console.error('上传失败:', result.msg)
        return null
      }
    } catch (error) {
      console.error('❌ [UploadAPI] 视频上传异常:', error)
      return null
    }
  }

  /**
   * 上传任意格式文件
   * @param file 文件
   * @param description 文件描述
   * @returns Promise<string | null> 返回文件下载URL或null
   */
  async uploadAnyFile(file: File, description: string = ''): Promise<string | null> {
    try {
      const result = await this.uploadFile(file, 'document', description)
      
      if (result.code === 0) {
        return result.downloadUrl
      } else {
        console.error('上传失败:', result.msg)
        return null
      }
    } catch (error) {
      console.error('❌ [UploadAPI] 文件上传异常:', error)
      return null
    }
  }

  /**
   * 获取文件信息
   * @param fileId 文件ID
   * @returns Promise<any>
   */
  async getFileInfo(fileId: number): Promise<any> {
    try {
      const response = await http.get(`/api/files/${fileId}`)
      return response.data as unknown as any
    } catch (error) {
      console.error('❌ [UploadAPI] 获取文件信息失败:', error)
      throw error
    }
  }

  /**
   * 删除文件
   * @param fileId 文件ID
   * @returns Promise<any>
   */
  async deleteFile(fileId: number): Promise<any> {
    try {
      const response = await http.delete(`/api/files/${fileId}`)
      return response.data
    } catch (error) {
      console.error('❌ [UploadAPI] 删除文件失败:', error)
      throw error
    }
  }
}

// 导出单例
export const uploadAPI = new UploadAPI()
