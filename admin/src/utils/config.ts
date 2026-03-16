/**
 * 应用配置管理模块
 * 提供系统配置信息和项目元数据
 */

// 菜单配置项接口
interface MenuItem {
  /** 菜单名称 */
  name: string
  /** 图标类名 */
  icon: string
  /** 子菜单列表 */
  child: Array<{
    /** 子菜单名称 */
    name: string
    /** 路由路径 */
    url: string
  }>
}

// API 配置接口
interface ApiConfig {
  /** 完整的 API 基础 URL */
  url: string
  /** API 名称 */
  name: string
  /** 菜单列表 */
  menuList: MenuItem[]
}

// 项目信息接口
interface ProjectInfo {
  /** 项目名称 */
  projectName: string
}

/**
 * 配置管理类
 * 统一管理系统配置和项目信息
 */
class AppConfig {
  /**
   * 获取 API 配置信息
   * @returns API 配置对象
   */
  public static getApiConfig(): ApiConfig {
    // 环境变量类型断言，提供默认值以防 undefined
    const baseUrl = process.env.VUE_APP_BASE_API_URL || ''
    const baseApi = process.env.VUE_APP_BASE_API || '/api'
    
    return {
      url: baseUrl + baseApi + '/',
      name: baseApi,
      menuList: [
        {
          name: '帖子',
          icon: 'icon-common1',
          child: [
            {
              name: '帖子',
              url: '/index/tieziList'
            }
          ]
        }
      ]
    }
  }

  /**
   * 获取项目基本信息
   * @returns 项目信息对象
   */
  public static getProjectInfo(): ProjectInfo {
    return {
      projectName: "中文社区交流平台"
    }
  }
  
  /**
   * 获取基础 API 路径
   * @returns 基础 API 路径字符串
   */
  public static getBaseApiPath(): string {
    return process.env.VUE_APP_BASE_API || '/api'
  }
  
  /**
   * 获取完整 API URL
   * @returns 完整的 API 基础 URL
   */
  public static getFullApiUrl(): string {
    const baseUrl = process.env.VUE_APP_BASE_API_URL || ''
    const baseApi = process.env.VUE_APP_BASE_API || '/api'
    return baseUrl + baseApi + '/'
  }
}

export default AppConfig
