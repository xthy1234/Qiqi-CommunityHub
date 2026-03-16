/**
 * 应用配置管理类
 * 负责管理应用的基础配置信息，包括API地址、项目信息等
 */
class AppConfig {
    /**
     * 获取API配置信息
     * @returns 包含API基础URL的对象
     */
    public getApiConfig(): { url: string } {
        // 从环境变量获取API基础URL，如果没有则使用默认值
        const apiUrl = process.env.VUE_APP_BASE_API_URL || 'http://localhost:8080'
        return {
            url: apiUrl
        }
    }

    /**
     * 获取项目信息
     * @returns 包含项目名称的对象
     */
    public getProjectInfo(): { projectName: string } {
        return {
            projectName: '中文社区交流平台'
        }
    }
}

// 创建并导出配置实例
const appConfig = new AppConfig()

export default appConfig
