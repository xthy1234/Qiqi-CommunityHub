/**
 * 屏蔽规则相关类型定义
 */

/**
 * 规则类型枚举
 */
export type RuleType = 'keyword' | 'author' | 'category'

/**
 * 屏蔽规则接口
 */
export interface BlockRule {
  id: number
  ruleType: RuleType
  ruleValue: string
  enabled: boolean
  createTime: string
}

/**
 * 添加屏蔽规则请求参数
 */
export interface AddBlockRuleRequest {
  ruleType: RuleType
  ruleValue: string
}

/**
 * 启用/禁用规则请求参数
 */
export interface ToggleEnableRequest {
  enabled: boolean
}

/**
 * 规则类型显示名称映射
 */
export const RULE_TYPE_LABELS: Record<RuleType, string> = {
  keyword: '关键词',
  author: '作者',
  category: '分类'
}

/**
 * 获取规则类型的显示名称
 */
export function getRuleTypeLabel(ruleType: RuleType): string {
  return RULE_TYPE_LABELS[ruleType] || ruleType
}
