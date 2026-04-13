import httpClient from '@/utils/http'

/**
 * API 响应数据结构
 */
interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/**
 * 积分规则接口
 */
export interface PointsRule {
  id?: number
  ruleCode: string
  ruleName: string
  description?: string
  points: number
  dailyLimit?: number
  isEnabled: boolean
  createTime?: string
  updateTime?: string
}

/**
 * 积分流水接口
 */
export interface PointsTransaction {
  id: number
  userId: number
  userNickname?: string
  ruleCode?: string
  ruleName?: string
  amount: number
  balanceAfter: number
  reason?: string
  relatedId?: number
  relatedType?: string
  createTime: string
}

/**
 * 积分调整参数
 */
export interface AdjustPointsParams {
  userId: number
  amount: number
  reason: string
}

/**
 * 分页查询积分规则（管理员）
 */
export function getPointsRules(params: {
  page?: number
  limit?: number
  isEnabled?: boolean
  keyword?: string
}): Promise<ApiResponse<{ list: PointsRule[]; totalCount: number }>> {
  return httpClient.get('/points-rules', { params })
}

/**
 * 获取启用的规则（无需登录）
 */
export function getEnabledRules(): Promise<ApiResponse<PointsRule[]>> {
  return httpClient.get('/points-rules/enabled')
}

/**
 * 获取规则详情
 */
export function getRuleDetail(id: number): Promise<ApiResponse<PointsRule>> {
  return httpClient.get(`/points-rules/${id}`)
}

/**
 * 按代码查询规则
 */
export function getRuleByCode(ruleCode: string): Promise<ApiResponse<PointsRule>> {
  return httpClient.get(`/points-rules/code/${ruleCode}`)
}

/**
 * 创建积分规则（管理员）
 */
export function createRule(data: Omit<PointsRule, 'id' | 'createTime' | 'updateTime'>): Promise<ApiResponse<number>> {
  return httpClient.post('/points-rules', data)
}

/**
 * 更新积分规则（管理员）
 */
export function updateRule(id: number, data: Partial<PointsRule>): Promise<ApiResponse<void>> {
  return httpClient.put(`/points-rules/${id}`, data)
}

/**
 * 切换规则状态（管理员）
 */
export function toggleRule(id: number): Promise<ApiResponse<void>> {
  return httpClient.patch(`/points-rules/${id}/toggle`)
}

/**
 * 删除积分规则（管理员）
 */
export function deleteRule(id: number): Promise<ApiResponse<void>> {
  return httpClient.delete(`/points-rules/${id}`)
}

/**
 * 批量删除规则（管理员）
 */
export function batchDeleteRules(ids: number[]): Promise<ApiResponse<void>> {
  return httpClient.post('/points-rules/batch-delete', ids)
}

/**
 * 查询所有用户的积分流水（管理员）
 */
export function getTransactions(params: {
  page?: number
  limit?: number
  userId?: number
  ruleCode?: string
  startDate?: string
  endDate?: string
}): Promise<ApiResponse<{ list: PointsTransaction[]; totalCount: number }>> {
  return httpClient.get('/points-transactions', { params })
}

/**
 * 查询指定用户的流水（管理员）
 */
export function getUserTransactions(userId: number, params?: {
  page?: number
  limit?: number
}): Promise<ApiResponse<{ list: PointsTransaction[]; totalCount: number }>> {
  return httpClient.get(`/points-transactions/user/${userId}`, { params })
}

/**
 * 调整用户积分（管理员）
 */
export function adjustPoints(data: AdjustPointsParams): Promise<ApiResponse<void>> {
  return httpClient.post('/points-transactions/adjust', null, { params: data })
}

export default {
  getPointsRules,
  getEnabledRules,
  getRuleDetail,
  getRuleByCode,
  createRule,
  updateRule,
  toggleRule,
  deleteRule,
  batchDeleteRules,
  getTransactions,
  getUserTransactions,
  adjustPoints
}
