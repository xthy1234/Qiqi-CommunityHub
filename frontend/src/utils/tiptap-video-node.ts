import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import VideoNode from '@/components/nodes/VideoNode.vue'

export interface Annotation {
  time: number
  title: string
  content: string
}

export interface VideoNodeOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    videoNode: {
      setVideo: (attrs: { 
        src: string
        poster?: string
        title?: string
        duration?: number
        annotations?: Annotation[]
      }) => ReturnType
    }
  }
}

export const VideoNodeExtension = Node.create<VideoNodeOptions>({
  name: 'videoNode',
  group: 'block',
  atom: true,
  
  addOptions() {
    return {
      HTMLAttributes: {}
    }
  },
  
  addAttributes() {
    return {
      src: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-src'),
        renderHTML: (attributes) => ({
          'data-src': attributes.src
        })
      },
      poster: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-poster'),
        renderHTML: (attributes) => ({
          'data-poster': attributes.poster
        })
      },
      title: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-title'),
        renderHTML: (attributes) => ({
          'data-title': attributes.title
        })
      },
      duration: {
        default: 0,
        parseHTML: (element) => {
          const duration = element.getAttribute('data-duration')
          return duration ? parseFloat(duration) : 0
        },
        renderHTML: (attributes) => ({
          'data-duration': attributes.duration
        })
      },
      annotations: {
        default: [],
        parseHTML: (element) => {
          const annotationsStr = element.getAttribute('data-annotations')
          try {
            return annotationsStr ? JSON.parse(annotationsStr) : []
          } catch {
            return []
          }
        },
        renderHTML: (attributes) => {
          const annotations = attributes.annotations || []
          return {
            'data-annotations': JSON.stringify(annotations)
          }
        }
      }
    }
  },
  
  parseHTML() {
    return [
      {
        tag: 'video-node'
      }
    ]
  },
  
  renderHTML({ HTMLAttributes }) {
    return ['video-node', mergeAttributes(this.options.HTMLAttributes, HTMLAttributes)]
  },
  
  addNodeView() {
    return VueNodeViewRenderer(VideoNode)
  },
  
  addCommands() {
    return {
      setVideo: (attrs) => ({ chain }) => {
        return chain()
          .insertContent({
            type: this.name,
            attrs: {
              src: attrs.src,
              poster: attrs.poster || null,
              title: attrs.title || '',
              duration: attrs.duration || 0,
              annotations: attrs.annotations || []
            }
          })
          .run()
      }
    }
  }
})

export default VideoNodeExtension
