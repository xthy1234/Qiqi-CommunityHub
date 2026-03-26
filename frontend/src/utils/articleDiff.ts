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
    
    // 提取所有文本节点，保留段落结构
    const paragraphs: string[] = []
    
    const extractParagraphs = (node: any) => {
      if (!node) return
      
      if (typeof node === 'string') {
        return
      }
      
      if (Array.isArray(node)) {
        node.forEach(extractParagraphs)
        return
      }
      
      if (typeof node === 'object') {
        // TipTap 的 block 节点（paragraph、heading 等）代表独立行
        if (node.type && ['paragraph', 'heading', 'blockquote', 'codeBlock'].includes(node.type)) {
          const blockTexts: string[] = []
          
          const extractFromContent = (contentNode: any) => {
            if (!contentNode) return
            
            if (Array.isArray(contentNode)) {
              contentNode.forEach(extractFromContent)
              return
            }
            
            if (typeof contentNode === 'object') {
              // text 节点才是真正的文本
              if (contentNode.text) {
                blockTexts.push(contentNode.text)
              }
              // 递归处理嵌套的 content
              if (contentNode.content) {
                extractFromContent(contentNode.content)
              }
            }
          }
          
          if (node.content) {
            extractFromContent(node.content)
          }
          
          // 每个 block 作为一行，空 block 用空字符串
          paragraphs.push(blockTexts.join(''))
        } else if (node.type === 'hardBreak') {
          // 显式的换行符
          paragraphs.push('')
        }
        
        // 递归处理根节点的 content 数组
        if (node.content && Array.isArray(node.content)) {
          node.content.forEach(extractParagraphs)
        } else if (node.content && !Array.isArray(node.content)) {
          extractParagraphs(node.content)
        }
      }
    }
    
    extractParagraphs(content)
    
    // 使用换行符连接所有段落
    const result = paragraphs.join('\n')
    console.log('📝 jsonToReadableText 结果:', {
      paragraphs,
      result,
      length: result.length
    })
    return result
  } catch (error) {
    console.error('JSON 转文本失败:', error)
    return ''
  }
}

/**
 * 计算两个 JSON 内容的差异
 */
export const calculateDiff = (oldContent: object | string, newContent: object | string): Delta => {
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
export const renderDiffHtml = (delta: Delta | any): string => {
  try {
    // 如果 delta 是数组形式 [oldValue, newValue]，说明是基础类型的差异
    if (Array.isArray(delta) && delta.length === 2) {
      const [oldText, newText] = delta
      
      // 直接返回文本描述
      if (!oldText && newText) {
        return `<ins>${newText}</ins>`
      }
      if (oldText && !newText) {
        return `<del>${oldText}</del>`
      }
      if (oldText !== newText) {
        return `<del>${oldText || ''}</del><ins>${newText || ''}</ins>`
      }
      return '没有发现差异'
    }
    
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
export const countDiffLines = (delta: any): { added: number; deleted: number } => {
  let added = 0
  let deleted = 0
  
  // 如果 delta 是数组形式 [oldValue, newValue]
  if (Array.isArray(delta) && delta.length === 2) {
    const [oldValue, newValue] = delta
    
    if (typeof oldValue === 'string') {
      const oldLines = oldValue.split('\n').filter(line => line.trim() !== '').length
      deleted = oldLines
    }
    
    if (typeof newValue === 'string') {
      const newLines = newValue.split('\n').filter(line => line.trim() !== '').length
      added = newLines
    }
  } else if (typeof delta === 'object' && delta !== null) {
    // 如果是对象形式的 delta
    const traverse = (value: any) => {
      if (typeof value === 'string') {
        const lines = value.split('\n').length
        // 简单判断：如果包含 \n 说明有多行
        if (value.includes('\n')) {
          added += lines
        }
      } else if (Array.isArray(value) && value.length === 2) {
        const [oldVal, newVal] = value
        if (typeof oldVal === 'string') {
          deleted += oldVal.split('\n').filter(l => l.trim()).length
        }
        if (typeof newVal === 'string') {
          added += newVal.split('\n').filter(l => l.trim()).length
        }
      } else if (typeof value === 'object' && value !== null) {
        Object.values(value).forEach(traverse)
      }
    }
    
    Object.values(delta).forEach(traverse)
  }
  
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
