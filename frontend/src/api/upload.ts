// src/api/upload.ts
import http from '@/utils/http'

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
    description: string = '',
    isPublic: boolean = true
  ): Promise<UploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileType', fileType)
    formData.append('description', description)
    formData.append('isPublic', String(isPublic))

    // console.log('📤 [UploadAPI] 开始上传文件')
    // console.log('📤 [UploadAPI] 接口地址:', this.uploadUrl)
    // console.log('📤 [UploadAPI] 文件类型:', fileType)

    try {
      const response = await http.post(this.uploadUrl, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      // console.log('📥 [UploadAPI] HTTP 响应对象:', response)
      // console.log('📥 [UploadAPI] response.data:', response.data)

      // 【关键】后端将文件信息嵌套在 response.data.file 中
      const backendData = response.data as unknown as BackendUploadResponse
      
      // console.log('📥 [UploadAPI] backendData:', backendData)
      // console.log('📥 [UploadAPI] backendData.code:', backendData.code)
      // console.log('📥 [UploadAPI] backendData.file:', backendData.file)
      
      if (!backendData.file) {
        console.error('❌ [UploadAPI] 后端响应中缺少 file 对象')
        throw new Error('上传响应格式错误：缺少 file 对象')
      }

      // 【重要】映射字段说明：
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
      
      // console.log('📥 [UploadAPI] 映射后的结果:', result)
      // console.log('📥 [UploadAPI] result.code:', result.code)
      // console.log('📥 [UploadAPI] result.viewUrl:', result.viewUrl)
      // console.log('📥 [UploadAPI] result.fileUrl:', result.fileUrl)
      
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
    // console.log('🖼️ [UploadAPI] 开始上传图片')
    // console.log('🖼️ [UploadAPI] 文件名:', file.name)
    // console.log('🖼️ [UploadAPI] 文件大小:', (file.size / 1024 / 1024).toFixed(2), 'MB')

    if (!file.type.startsWith('image/')) {
      console.warn('⚠️ [UploadAPI] 文件类型不是图片:', file.type)
      return null
    }
    if (file.size > 10 * 1024 * 1024) { // 10MB
      console.warn('⚠️ [UploadAPI] 图片大小超过限制:', file.size)
      return null
    }

    try {
      const result = await this.uploadFile(file, 'image', description)
      
      // console.log('🖼️ [UploadAPI] uploadFile 返回结果:', result)
      // console.log('🖼️ [UploadAPI] result.code:', result.code)
      
      // 【关键】检查后端返回的状态码
      if (result.code === 0) {
        // 【重要】返回 viewUrl 而不是 fileUrl
        // viewUrl: /api/files/4/view （可直接用于 <img> 标签）
        // fileUrl: /api/files/4 （返回JSON元数据）
        // console.log('✅ [UploadAPI] 上传成功，viewUrl:', result.viewUrl)
        return result.viewUrl
      } else {
        // 后端返回错误
        console.error('❌ [UploadAPI] 上传失败，错误信息:', result.msg)
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
        // 返回 viewUrl 用于视频播放
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
        // 对于文档类文件，返回 downloadUrl 用于下载
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
      // 后端直接返回数据在响应根级别
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
