// src/utils/userUtils.ts
import toolUtil from './toolUtil'

/**
 * 获取头像 URL
 * @param avatar 头像路径或 URL
 * @returns 完整的头像 URL
 */
export function getAvatarUrl(avatar?: string): string {
  if (!avatar) {
    return '/default-avatar.png'
  }
  
  // 如果已经是完整 URL，直接返回
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  
  // 否则拼接基础 URL
  const baseUrl = process.env.VUE_APP_BASE_API || ''
  return `${baseUrl}${avatar}`
}

/**
 * 获取头像 initials（姓名首字母）
 * @param nickname 昵称
 * @returns 首字母
 */
export function getAvatarInitials(nickname?: string): string {
  if (!nickname) {
    return '用户'
  }
  
  // 如果是中文，返回第一个字
  if (/^[\u4e00-\u9fa5]+$/.test(nickname)) {
    return nickname.charAt(0)
  }
  
  // 如果是英文，返回首字母大写
  return nickname.charAt(0).toUpperCase()
}

/**
 * 格式化性别文本
 * @param gender 性别值 (0:未知，1:男，2:女)
 * @returns 性别文本
 */
export function getGenderText(gender?: number): string {
  switch (gender) {
    case 1:
      return '男'
    case 2:
      return '女'
    default:
      return '未知'
  }
}

/**
 * 格式化日期时间
 * @param dateTime 日期时间字符串
 * @returns 格式化后的字符串
 */
export function formatDateTime(dateTime?: string): string {
  if (!dateTime) {
    return '-'
  }
  
  try {
    const date = new Date(dateTime)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return dateTime
  }
}

/**
 * 格式化日期
 * @param date 日期字符串
 * @returns 格式化后的字符串
 */
export function formatDate(date?: string): string {
  if (!date) {
    return '-'
  }
  
  try {
    const d = new Date(date)
    return d.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (e) {
    return date
  }
}

/**
 * 获取当前登录用户 ID
 * @returns 用户 ID
 */
export function getCurrentUserId(): number | null {
  const userId = toolUtil.storageGet('userId')
  return userId ? Number(userId) : null
}

/**
 * 获取当前登录用户信息
 * @returns 用户信息
 */
export function getCurrentUserInfo(): { id?: number; username?: string; avatar?: string } {
  return {
    id: getCurrentUserId() || undefined,
    username: toolUtil.storageGet('userName'),
    avatar: toolUtil.storageGet('avatar')
  }
}

// ... existing code ...

/**
 * 获取完整 URL
 * @param path 相对路径或完整 URL
 * @returns 完整的 URL
 */
export function getFullUrl(path?: string): string {
  if (!path) return ''

  // 如果已经是完整 URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }

  // 否则拼接基础 URL
  const baseUrl = process.env.VUE_APP_BASE_API || ''
  return `${baseUrl}${path}`
}


