NEW_FILE_CODE
import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
  }
}
