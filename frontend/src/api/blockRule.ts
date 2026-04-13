/**
 * 屏蔽规则 API 接口
 */
import http from '@/utils/http'
import { BaseAPI } from './base'
import type { BlockRule, AddBlockRuleRequest, ToggleEnableRequest } from '@/types/blockRule'

class BlockRuleAPI extends BaseAPI<BlockRule> {
  constructor() {
    super('/api/block-rules')
  }

  /**
   * 添加屏蔽规则
   * @param data 规则数据
   */
  addRule(data: AddBlockRuleRequest) {
    return http.post(this.endpoint, data)
  }

  /**
   * 获取当前用户的所有屏蔽规则
   */
  getRules() {
    return http.get<BlockRule[]>(this.endpoint)
  }

  /**
   * 启用/禁用屏蔽规则
   * @param id 规则ID
   * @param enabled 启用状态
   */
  toggleEnable(id: number, enabled: boolean) {
    return http.put(`${this.endpoint}/${id}/enable`, { enabled })
  }

  /**
   * 删除屏蔽规则
   * @param id 规则ID
   */
  deleteRule(id: number) {
    return http.delete(`${this.endpoint}/${id}`)
  }
}

export const blockRuleAPI = new BlockRuleAPI()
