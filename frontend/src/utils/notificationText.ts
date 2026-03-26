import type { Notification, NotificationType } from '@/types/notification'

/**
 * 获取通知的显示标题
 */
export function getNotificationTitle(notification: Notification): string {
  const { type, extra, title, fromUserNickname } = notification
  
  // 如果有预设标题，直接使用（如系统消息）
  if (title) return title
  
  // 安全获取 extra 中的属性
  const safeExtra = extra || {}
  
  // 根据类型生成标题
  switch (type) {
    case 1: // COMMENT
      return `${(safeExtra as any)?.commenter?.nickname || fromUserNickname || '有人'} 评论了你的文章`
    
    case 2: // LIKE
      if ((safeExtra as any)?.targetType === 'comment') {
        return `${(safeExtra as any)?.liker?.nickname || fromUserNickname || '有人'} 点赞了你的评论`
      }
      return `${(safeExtra as any)?.liker?.nickname || fromUserNickname || '有人'} 点赞了你的文章`
    
    case 3: // FOLLOW
      return `${(safeExtra as any)?.follower?.nickname || fromUserNickname || '有人'} 关注了你`
    
    case 4: // REPLY
      return `${(safeExtra as any)?.replier?.nickname || fromUserNickname || '有人'} 回复了你的评论`
    
    case 5: // ARTICLE_AUDIT
      const auditExtra = safeExtra as any
      return `你的文章《${auditExtra?.articleTitle || ''}》审核已${auditExtra?.result || ''}`
    
    case 6: // CIRCLE_INVITE
      const inviteExtra = safeExtra as any
      return `${inviteExtra?.inviter?.nickname || fromUserNickname || '有人'} 邀请你加入圈子《${inviteExtra?.circleName || ''}》`
    
    case 7: // CIRCLE_REMOVED
      const removedExtra = safeExtra as any
      return `你被 ${removedExtra?.operator?.nickname || '管理员'} 移出了圈子《${removedExtra?.circleName || ''}》`
    
    case 8: // CIRCLE_JOIN
      const joinExtra = safeExtra as any
      return `${joinExtra?.applicant?.nickname || fromUserNickname || '有人'} 申请加入圈子《${joinExtra?.circleName || ''}》`
    
    case 9: // MEMBER_JOIN
      const memberJoinExtra = safeExtra as any
      return `${memberJoinExtra?.newMember?.nickname || fromUserNickname || '有人'} 加入了圈子《${memberJoinExtra?.circleName || ''}》`
    
    case 10: // MEMBER_QUIT
      const quitExtra = safeExtra as any
      return `${quitExtra?.quitter?.nickname || fromUserNickname || '有人'} 退出了圈子《${quitExtra?.circleName || ''}》`
    
    case 11: // MEMBER_REMOVED
      const memberRemovedExtra = safeExtra as any
      return `${memberRemovedExtra?.operator?.nickname || '管理员'} 将 ${memberRemovedExtra?.removedUser?.nickname || '成员'} 移出了圈子《${memberRemovedExtra?.circleName || ''}》`
    
    case 12: // SUGGESTION_SUBMIT
      return `${(safeExtra as any)?.proposer?.nickname || fromUserNickname || '有人'} 为你的文章提交了修改建议`
    
    case 13: // SUGGESTION_REVIEW
      const reviewExtra = safeExtra as any
      return `你的修改建议已${reviewExtra?.result || ''}`
    
    case 14: // SYSTEM_MESSAGE
      return (safeExtra as any)?.title || '系统消息'
    
    default:
      return '系统通知'
  }
}

/**
 * 获取通知的内容描述
 */
export function getNotificationDescription(notification: Notification): string {
  const { type, extra } = notification
  const safeExtra = extra || {}
  
  switch (type) {
    case 1: // COMMENT
      return (safeExtra as any)?.content || ''
    
    case 4: // REPLY
      return (safeExtra as any)?.content || ''
    
    case 5: // ARTICLE_AUDIT
      const auditExtra = safeExtra as any
      return auditExtra?.result === '通过' ? '恭喜，您的文章已通过审核！' : '很抱歉，您的文章未通过审核'
    
    case 13: // SUGGESTION_REVIEW
      const reviewExtra = safeExtra as any
      return reviewExtra?.result === '通过' ? '您的建议已被采纳' : '很抱歉，您的建议未被采纳'
    
    case 14: // SYSTEM_MESSAGE
      return (safeExtra as any)?.content || ''
    
    default:
      return ''
  }
}
