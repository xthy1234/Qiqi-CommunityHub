// src/utils/tiptap-video-annotation-ref.ts
import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import VideoAnnotationRefNode from '@/components/editor/nodes/VideoAnnotationRefNode.vue'

export interface AnnotationRefOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    videoAnnotationRef: {
      setVideoAnnotationRef: (attrs: {
        videoNodeId: string      // 视频节点ID
        annotationId: string     // 注释ID
        displayText?: string     // 显示文本（如 "见视频注释①"）
      }) => ReturnType
    }
  }
}

export const VideoAnnotationRefExtension = Node.create<AnnotationRefOptions>({
  name: 'videoAnnotationRef',
  group: 'inline',
  inline: true,
  atom: true,
  
  addOptions() {
    return {
      HTMLAttributes: {}
    }
  },
  
  addAttributes() {
    return {
      videoNodeId: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-video-node-id'),
        renderHTML: (attributes) => ({
          'data-video-node-id': attributes.videoNodeId
        })
      },
      annotationId: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-annotation-id'),
        renderHTML: (attributes) => ({
          'data-annotation-id': attributes.annotationId
        })
      },
      displayText: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-display-text'),
        renderHTML: (attributes) => ({
          'data-display-text': attributes.displayText
        })
      }
    }
  },
  
  parseHTML() {
    return [
      {
        tag: 'video-annotation-ref'
      }
    ]
  },
  
  renderHTML({ HTMLAttributes }) {
    return ['video-annotation-ref', mergeAttributes(this.options.HTMLAttributes, HTMLAttributes)]
  },
  
  addNodeView() {
    return VueNodeViewRenderer(VideoAnnotationRefNode)
  },
  
  addCommands() {
    return {
      setVideoAnnotationRef: (attrs) => ({ chain }) => {
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
