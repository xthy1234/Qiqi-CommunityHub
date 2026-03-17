import toolUtil from './toolUtil'
import AppConfig from './config'

export interface GlobalProperties {
  $toolUtil: typeof toolUtil
  $config: {
    url: string
  }
}

/**
 * 获取全局属性对象
 * @returns 包含 toolUtil 和 config 的全局属性对象
 */
export const useGlobalProperties = (): GlobalProperties => {
  return {
    $toolUtil: toolUtil,
    $config: {
      url: AppConfig.getFullApiUrl()
    }
  }
}
