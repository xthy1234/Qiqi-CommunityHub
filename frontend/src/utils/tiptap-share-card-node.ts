// src/utils/tiptap-share-card-node.ts
import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import ShareCardNode from '@/components/chat/ShareCardNode.vue'

export interface ShareCardNodeOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    shareCardNode: {
      setShareCard: (attrs: { 
        title: string
        summary?: string
        cover?: string
        url: string
        author?: string
        publishTime?: string
      }) => ReturnType
    }
  }
}

export const ShareCardNodeExtension = Node.create<ShareCardNodeOptions>({
  name: 'shareCardNode',
  group: 'block',
  atom: true,
  
  addOptions() {
    return {
      HTMLAttributes: {}
    }
  },
  
  addAttributes() {
    return {
      title: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-title'),
        renderHTML: (attributes) => ({
          'data-title': attributes.title
        })
      },
      summary: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-summary'),
        renderHTML: (attributes) => ({
          'data-summary': attributes.summary
        })
      },
      cover: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-cover'),
        renderHTML: (attributes) => ({
          'data-cover': attributes.cover
        })
      },
      url: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-url'),
        renderHTML: (attributes) => ({
          'data-url': attributes.url
        })
      },
      author: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-author'),
        renderHTML: (attributes) => ({
          'data-author': attributes.author
        })
      },
      publishTime: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-publish-time'),
        renderHTML: (attributes) => ({
          'data-publish-time': attributes.publishTime
        })
      }
    }
  },
  
  parseHTML() {
    return [
      {
        tag: 'share-card-node'
      }
    ]
  },
  
  renderHTML({ HTMLAttributes }) {
    return ['share-card-node', mergeAttributes(HTMLAttributes)]
  },
  
  addNodeView() {
    return VueNodeViewRenderer(ShareCardNode)
  },
  
  addCommands() {
    return {
      setShareCard: (attrs) => ({ chain }) => {
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

export default ShareCardNodeExtension
