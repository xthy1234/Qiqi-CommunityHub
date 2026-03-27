
export function tiptapToText(doc: any): string {
  if (!doc || !doc.content) return ''

  const lines: string[] = []

  const traverse = (node: any): string => {
    if (node.type === 'text') {
      return node.text || ''
    }

    if (node.type === 'paragraph') {
      let text = ''
      if (node.content) {
        text = node.content.map(traverse).join('')
      }
      lines.push(text)
    }
    else if (node.type === 'heading') {
      let text = ''
      if (node.content) {
        text = node.content.map(traverse).join('')
      }
      lines.push(`${'#'.repeat(node.attrs.level)} ${text}`)
    }
    else if (node.type === 'bulletList') {
      if (node.content) {
        node.content.forEach((item: any) => {
          const text = item.content?.map(traverse).join('') || ''
          lines.push(`- ${text}`)
        })
      }
    }
    else if (node.type === 'orderedList') {
      if (node.content) {
        node.content.forEach((item: any, idx: number) => {
          const text = item.content?.map(traverse).join('') || ''
          lines.push(`${idx+1}. ${text}`)
        })
      }
    }
    else if (node.type === 'image') {
      lines.push(`[图片：${node.attrs.alt || node.attrs.src || '未知'}]`)
    }
    else if (node.type === 'codeBlock') {
      const code = node.content?.map(traverse).join('') || ''
      lines.push('')
      lines.push(code)
      lines.push('')
    }
    else if (node.type === 'listItem' && node.content) {
      node.content.forEach(traverse)
    }
    else if (node.content) {
      node.content.forEach(traverse)
    }

    return ''
  }

  doc.content.forEach(traverse)
  return lines.join('\n')
}
