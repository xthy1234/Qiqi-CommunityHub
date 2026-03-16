import http from '@/utils/http'

/**
 * 基础 API 类 - 提供通用 CRUD 操作
 */
export class BaseAPI<T = any> {
    protected endpoint: string

    constructor(endpoint: string) {
        this.endpoint = endpoint
    }

    /**
     * 获取列表
     */
    getList(params?: any) {
        return http.get(this.endpoint, { params })
    }

    /**
     * 获取详情
     */
    getById(id: number | string) {
        return http.get(`${this.endpoint}/${id}`)
    }

    /**
     * 创建
     */
    create(data: T) {
        return http.post(this.endpoint, data)
    }

    /**
     * 更新
     */
    update(id: number | string, data: Partial<T>) {
        return http.put(`${this.endpoint}/${id}`, data)
    }

    /**
     * 删除
     */
    delete(id: number | string) {
        return http.delete(`${this.endpoint}/${id}`)
    }
}
