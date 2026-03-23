import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import FileNode from '@/components/chat/FileNode.vue'

export interface FileNodeOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    fileNode: {
      setFile: (attrs: { src: string; name: string; size: number; mimeType: string; extension: string }) => ReturnType
    }
  }
}

export const FileNodeExtension = Node.create<FileNodeOptions>({
  name: 'fileNode',
  group: 'block',
  atom: true, // 原子节点，不可编辑
  
  addOptions() {
    return {
      HTMLAttributes: {}
    }
  },
  
  addAttributes() {
    return {
      src: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-src'),
        renderHTML: (attributes) => ({
          'data-src': attributes.src
        })
      },
      name: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-name'),
        renderHTML: (attributes) => ({
          'data-name': attributes.name
        })
      },
      size: {
        default: 0,
        parseHTML: (element) => parseInt(element.getAttribute('data-size') || '0', 10),
        renderHTML: (attributes) => ({
          'data-size': attributes.size
        })
      },
      mimeType: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-mime-type'),
        renderHTML: (attributes) => ({
          'data-mime-type': attributes.mimeType
        })
      },
      extension: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-extension'),
        renderHTML: (attributes) => ({
          'data-extension': attributes.extension
        })
      }
    }
  },
  
  parseHTML() {
    return [
      {
        tag: 'file-node'
      }
    ]
  },
  
  renderHTML({ HTMLAttributes }) {
    // 关键修复：原子节点不应该有内容占位符 (0)
    return ['file-node', mergeAttributes(HTMLAttributes)]
  },
  
  addNodeView() {
    return VueNodeViewRenderer(FileNode)
  },
  
  addCommands() {
    return {
      setFile: (attrs: { src: string; name: string; size: number; mimeType: string; extension: string }) => ({ chain }) => {
        return chain()
          .insertContent({
            type: this.name,
            attrs: attrs
          })
          .run()
      }
    }
  }
})

export default FileNodeExtension
