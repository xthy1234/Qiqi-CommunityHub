/**
 * 文件URL处理工具 - 管理端
 */

/**
 * 标准化文件URL，确保可以正常访问
 * @param url 原始URL
 * @param baseUrl 基础URL（可选，默认从 localStorage 获取）
 * @returns 完整的文件访问URL
 */
export function normalizeFileUrl(
    url: string | null | undefined,
    baseUrl?: string
): string {
  if (!url) {
    return ''
  }

  const base = baseUrl || localStorage.getItem('backendUrl') || 'http://localhost:8080'

  // 如果已经是完整 URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // 如果是新版 API URL (/api/files/xxx)，拼接 baseUrl
  if (url.startsWith('/api/')) {
    return `${base}${url}`
  }

  // 旧版相对路径，拼接 baseUrl
  return `${base}/${url}`
}

/**
 * 获取文件预览URL
 * @param fileIdOrUrl 文件ID或URL
 * @param baseUrl 基础URL
 * @returns 预览URL
 */
export function getFileViewUrl(
    fileIdOrUrl: number | string,
    baseUrl?: string
): string {
  const base = baseUrl || localStorage.getItem('backendUrl') || 'http://localhost:8080'

  if (typeof fileIdOrUrl === 'number') {
    return `${base}/api/files/${fileIdOrUrl}/view`
  }

  // 如果已经是新版 API URL，添加 /view
  if (fileIdOrUrl.startsWith('/api/files/')) {
    return `${base}${fileIdOrUrl}/view`
  }

  // 其他情况直接返回
  return normalizeFileUrl(fileIdOrUrl, base)
}

/**
 * 获取缩略图URL
 * @param fileIdOrUrl 文件ID或URL
 * @param baseUrl 基础URL
 * @returns 缩略图URL
 */
export function getThumbnailUrl(
    fileIdOrUrl: number | string,
    baseUrl?: string
): string {
  const base = baseUrl || localStorage.getItem('backendUrl') || 'http://localhost:8080'

  if (typeof fileIdOrUrl === 'number') {
    return `${base}/api/files/${fileIdOrUrl}/thumbnail`
  }

  // 如果已经是新版 API URL，添加 /thumbnail
  if (fileIdOrUrl.startsWith('/api/files/')) {
    return `${base}${fileIdOrUrl}/thumbnail`
  }

  // 其他情况直接返回
  return normalizeFileUrl(fileIdOrUrl, base)
}

/**
 * 获取下载URL
 * @param fileIdOrUrl 文件ID或URL
 * @param baseUrl 基础URL
 * @returns 下载URL
 */
export function getDownloadUrl(
    fileIdOrUrl: number | string,
    baseUrl?: string
): string {
  const base = baseUrl || localStorage.getItem('backendUrl') || 'http://localhost:8080'

  if (typeof fileIdOrUrl === 'number') {
    return `${base}/api/files/${fileIdOrUrl}/download`
  }

  // 如果已经是新版 API URL，添加 /download
  if (fileIdOrUrl.startsWith('/api/files/')) {
    return `${base}${fileIdOrUrl}/download`
  }

  // 其他情况直接返回
  return normalizeFileUrl(fileIdOrUrl, base)
}
