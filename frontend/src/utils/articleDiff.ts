//src/utils/articleDiff.ts
import * as jsondiffpatch from 'jsondiffpatch'
import type { Delta } from 'jsondiffpatch'

/**
 * 差异项类型
 */
export interface DiffItem {
  type: 'added' | 'modified' | 'deleted'
  path: string
  oldValue?: any
  newValue?: any
}

/**
 * 将 JSON 内容转换为可读文本（用于展示）
 */
export const jsonToReadableText = (content: object): string => {
  try {
    if (!content || typeof content !== 'object') {
      return ''
    }
    
    // 提取所有文本节点
    const textNodes: string[] = []
    
    const extractText = (node: any) => {
      if (!node) return
      
      if (typeof node === 'string') {
        textNodes.push(node)
        return
      }
      
      if (Array.isArray(node)) {
        node.forEach(extractText)
        return
      }
      
      if (typeof node === 'object') {
        // TipTap JSON 格式通常有 text 字段
        if (node.text) {
          textNodes.push(node.text)
        }
        // 递归处理子节点
        if (node.content && Array.isArray(node.content)) {
          node.content.forEach(extractText)
        }
        // 处理其他可能的文本字段
        Object.values(node).forEach(value => {
          if (typeof value === 'string' && value.length < 500) {
            textNodes.push(value)
          }
        })
      }
    }
    
    extractText(content)
    return textNodes.join('\n')
  } catch (error) {
    console.error('JSON 转文本失败:', error)
    return ''
  }
}

/**
 * 计算两个 JSON 内容的差异
 */
export const calculateDiff = (oldContent: object, newContent: object): Delta => {
  try {
    return jsondiffpatch.diff(oldContent, newContent)
  } catch (error) {
    console.error('计算差异失败:', error)
    return {}
  }
}

/**
 * 将差异对象转换为易读的 HTML
 */
export const renderDiffHtml = (delta: Delta): string => {
  try {
    // @ts-ignore - jsondiffpatch 的 html formatter 类型定义不完整
    const formatter = jsondiffpatch.formatters?.html
    if (!formatter) {
      // 如果没有 html formatter，返回基础文本描述
      return formatDiffAsText(delta)
    }
    // @ts-ignore
    return formatter.format(delta)
  } catch (error) {
    console.error('渲染差异 HTML 失败:', error)
    return formatDiffAsText(delta)
  }
}

/**
 * 格式化差异为文本描述（备用方案）
 */
const formatDiffAsText = (delta: Delta): string => {
  const changes: string[] = []
  
  const traverse = (obj: any, path = '') => {
    for (const key in obj) {
      if (!obj.hasOwnProperty(key)) continue

      const value = obj[key]
      const currentPath = path ? `${path}.${key}` : key

      if (Array.isArray(value) && value.length === 2) {
        // [旧值，新值] 格式
        changes.push(`路径 ${currentPath}: "${value[0]}" → "${value[1]}"`)
      } else if (typeof value === 'object' && value !== null) {
        traverse(value, currentPath)
      }
    }
  }
  
  traverse(delta)

  if (changes.length === 0) {
    return '没有发现差异'
  }

  return changes.join('<br/>')
}

/**
 * 统计差异行数（用于显示贡献度）
 */
export const countDiffLines = (delta: Delta): { added: number; deleted: number } => {
  let added = 0
  let deleted = 0
  
  const countInValue = (value: any) => {
    if (typeof value === 'string') {
      const lines = value.split('\n').length
      return lines
    }
    if (Array.isArray(value) && value.length === 2) {
      // [旧值，新值]
      if (typeof value[0] === 'string') {
        deleted += value[0].split('\n').length
      }
      if (typeof value[1] === 'string') {
        added += value[1].split('\n').length
      }
    } else if (typeof value === 'object' && value !== null) {
      Object.values(value as {}).forEach(countInValue)
    }
  }
  
  Object.values(delta as {}).forEach(countInValue)
  
  return { added, deleted }
}

/**
 * 生成差异摘要
 */
export const generateDiffSummary = (delta: Delta): string => {
  const { added, deleted } = countDiffLines(delta)
  
  if (added === 0 && deleted === 0) {
    return '无变化'
  }
  
  const parts: string[] = []
  if (added > 0) {
    parts.push(`新增${added}行`)
  }
  if (deleted > 0) {
    parts.push(`删除${deleted}行`)
  }
  
  return parts.join('，')
}
