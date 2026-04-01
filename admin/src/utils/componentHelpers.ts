// src/utils/componentHelpers.ts

/**
 * 状态标签配置辅助函数
 * 快速生成 StatusTag 组件的 options 配置
 */
export function createStatusOptions<T = any>(
  config: Array<{ value: T; label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }>
) {
  return config
}

/**
 * 常用的状态配置
 */
export const commonStatusConfigs = {
  // 启用/禁用
  enableDisable: createStatusOptions([
    { value: 0, label: '启用', type: 'success' },
    { value: 1, label: '禁用', type: 'error' }
  ]),
  
  // 显示/隐藏
  showHide: createStatusOptions([
    { value: 1, label: '显示', type: 'success' },
    { value: 0, label: '隐藏', type: 'default' }
  ]),
  
  // 审核状态
  auditStatus: createStatusOptions([
    { value: 0, label: '待审核', type: 'warning' },
    { value: 1, label: '已通过', type: 'success' },
    { value: 2, label: '已拒绝', type: 'error' }
  ]),
  
  // 性别
  gender: createStatusOptions([
    { value: 0, label: '保密', type: 'default' },
    { value: 1, label: '男', type: 'info' },
    { value: 2, label: '女', type: 'success' }
  ])
}

/**
 * 图片上传辅助函数
 */
export const imageUploadHelper = {
  /**
   * 获取完整图片 URL
   */
  getFullImageUrl(url: string): string {
    if (!url) return ''
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url
    }
    const baseUrl = localStorage.getItem('backendUrl') || 'http://localhost:8080'
    
    // 修复：确保 baseUrl 以 / 结尾，且 url 不以 / 开头
    const normalizedBaseUrl = baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
    const normalizedUrl = url.startsWith('/') ? url.substring(1) : url
    
    return `${normalizedBaseUrl}${normalizedUrl}`
  },
  
  /**
   * 从 URL 获取文件名
   */
  getFileName(url: string): string {
    return url.split('/').pop() || url
  }
}
