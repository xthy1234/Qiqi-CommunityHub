// src/api/upload.ts
import http from '@/utils/http'

/**
 * 文件上传响应类型
 * 
 *  根据后端实际响应结构调整
 * 当前后端返回格式（嵌套结构）：
 * {
 *   "code": 0,
 *   "msg": "文件上传成功",
 *   "file": {
 *     "id": 4,
 *     "fileName": "...",
 *     "fileUrl": "/api/files/4",        // 文件信息接口（返回JSON）
 *     "viewUrl": "/api/files/4/view",    // 文件预览链接（可直接用于img/video标签）
 *     "downloadUrl": "/api/files/4/download",  // 文件下载链接
 *     ...
 *   }
 * }
 */
export interface UploadResponse {
  code: number              // 状态码：0 表示成功
  msg: string               // 响应消息
  fileId: number            // 文件ID
  fileName: string          // 原始文件名
  storedFileName: string    // 存储的文件名
  fileUrl: string           // 文件信息接口URL（返回JSON元数据）
  viewUrl: string           // 文件预览URL（用于img/video等标签直接显示）
  downloadUrl: string       // 文件下载URL（触发浏览器下载）
  thumbnailUrl?: string | null  // 缩略图URL
  readableSize: string      // 可读的文件大小
  mimeType: string          // MIME类型
  fileType: 'image' | 'video' | 'document'  // 文件类型分类
  width?: number | null     // 图片宽度
  height?: number | null    // 图片高度
  duration?: number | null  // 视频时长（秒）
  md5: string               // 文件MD5值
}

/**
 * 后端文件对象结构（嵌套在 response.data.file 中）
 */
interface BackendFileObject {
  id: number
  fileName: string                    // 存储的文件名
  originalFileName: string            // 原始文件名
  fileUrl: string                     // 文件信息接口URL
  viewUrl: string                     // 文件预览URL
  downloadUrl: string                 // 文件下载URL
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

/**
 * 后端上传响应结构
 */
interface BackendUploadResponse {
  code: number
  msg: string
  file: BackendFileObject
}

/**
 * 文件上传服务
 */
export class UploadAPI {
  private uploadUrl = '/api/files/upload' // 新版文件上传接口

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
    description = '',
    isPublic = true
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

      //  后端将文件信息嵌套在 response.data.file 中
      const backendData = response.data as unknown as BackendUploadResponse

      if (!backendData.file) {
        console.error('[UploadAPI] 后端响应中缺少 file 对象')
        throw new Error('上传响应格式错误：缺少 file 对象')
      }

      //  映射字段说明：
      // - viewUrl: 用于 img/video 标签直接显示（如 <img src="/api/files/4/view" />）
      // - fileUrl: 用于获取文件元数据（返回JSON）
      // - downloadUrl: 用于触发下载
      const result: UploadResponse = {
        code: backendData.code,
        msg: backendData.msg,
        fileId: backendData.file.id,
        fileName: backendData.file.originalFileName,
        storedFileName: backendData.file.fileName,
        fileUrl: backendData.file.fileUrl,        // 文件信息接口
        viewUrl: backendData.file.viewUrl,        // 文件预览链接（重点使用）
        downloadUrl: backendData.file.downloadUrl, // 文件下载链接
        thumbnailUrl: backendData.file.thumbnailUrl,
        readableSize: backendData.file.readableSize,
        mimeType: backendData.file.mimeType,
        fileType: backendData.file.fileType,
        width: backendData.file.width,
        height: backendData.file.height,
        duration: backendData.file.duration,
        md5: '' // 后端未返回 MD5，留空
      }

      return result
    } catch (error) {
      console.error('[UploadAPI] 文件上传失败:', error)
      throw error
    }
  }

  /**
   * 上传图片文件（带校验）
   * @param file 图片文件
   * @param description 图片描述
   * @returns Promise<string> 返回图片预览URL
   * @throws Error 上传失败时抛出错误
   */
  async uploadImage(file: File, description = ''): Promise<string> {
    if (!file.type.startsWith('image/')) {
      throw new Error('仅支持上传图片文件')
    }
    
    const maxSize = 10 * 1024 * 1024 // 10MB
    if (file.size > maxSize) {
      throw new Error(`图片大小不能超过${(maxSize / 1024 / 1024).toFixed(0)}MB`)
    }

    try {
      const result = await this.uploadFile(file, 'image', description)
      
      if (result.code === 0) {
        return result.viewUrl
      } else {
        throw new Error(result.msg || '图片上传失败')
      }
    } catch (error) {
      if (error instanceof Error) {
        throw error
      }
      throw new Error('图片上传异常，请稍后重试')
    }
  }

  /**
   * 上传视频文件
   * @param file 视频文件
   * @param description 视频描述
   * @returns Promise<string> 返回视频预览URL
   * @throws Error 上传失败时抛出错误
   */
  async uploadVideo(file: File, description = ''): Promise<string> {
    if (!file.type.startsWith('video/')) {
      throw new Error('仅支持上传视频文件')
    }
    
    const maxSize = 1024 * 1024 * 1024 // 1GB
    if (file.size > maxSize) {
      throw new Error(`视频大小不能超过${(maxSize / 1024 / 1024 / 1024).toFixed(0)}GB`)
    }

    try {
      const result = await this.uploadFile(file, 'video', description)
      
      if (result.code === 0) {
        return result.viewUrl
      } else {
        throw new Error(result.msg || '视频上传失败')
      }
    } catch (error) {
      if (error instanceof Error) {
        throw error
      }
      throw new Error('视频上传异常，请稍后重试')
    }
  }

  /**
   * 上传任意格式文件
   * @param file 文件
   * @param description 文件描述
   * @returns Promise<string> 返回文件下载URL
   * @throws Error 上传失败时抛出错误
   */
  async uploadAnyFile(file: File, description = ''): Promise<string> {
    try {
      const result = await this.uploadFile(file, 'document', description)
      
      if (result.code === 0) {
        return result.downloadUrl
      } else {
        throw new Error(result.msg || '文件上传失败')
      }
    } catch (error) {
      if (error instanceof Error) {
        throw error
      }
      throw new Error('文件上传异常，请稍后重试')
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
      // 后端直接返回数据在响应根级别
      return response.data as unknown as any
    } catch (error) {
      console.error('[UploadAPI] 获取文件信息失败:', error)
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
      console.error('[UploadAPI] 删除文件失败:', error)
      throw error
    }
  }
}

// 导出单例
export const uploadAPI = new UploadAPI()
