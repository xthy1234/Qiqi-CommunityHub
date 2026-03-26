export enum NotificationType {
  COMMENT = 1, // 文章被评论
  LIKE = 2, // 点赞
  FOLLOW = 3, // 关注
  REPLY = 4, // 评论被回复
  ARTICLE_AUDIT = 5, // 文章审核结果
  CIRCLE_INVITE = 6, // 邀请进入圈子
  CIRCLE_REMOVED = 7, // 被移出圈子
  CIRCLE_JOIN = 8, // 圈子申请加入
  MEMBER_JOIN = 9, // 圈子有新成员加入
  MEMBER_QUIT = 10, // 圈子成员退出
  MEMBER_REMOVED = 11, // 圈子成员被移出
  SUGGESTION_SUBMIT = 12, // 提交建议
  SUGGESTION_REVIEW = 13, // 建议审核结果
  SYSTEM_MESSAGE = 14, // 系统消息
}

// 用户简单信息
export interface SimpleUser {
  id: number;
  nickname: string;
  avatar?: string;
}

// 各类型 extra 字段的联合类型
export type CommentExtra = {
  articleId: number;
  commentId: number;
  commenter: SimpleUser;
  content: string;
};

export type LikeExtra = {
  targetType: 'article' | 'comment';
  articleId: number;
  commentId?: number;
  liker: SimpleUser;
};

export type FollowExtra = {
  follower: SimpleUser;
};

export type ReplyExtra = {
  articleId: number;
  commentId: number;
  replier: SimpleUser;
  content?: string;
};

export type ArticleAuditExtra = {
  articleId: number;
  articleTitle: string;
  result: '通过' | '拒绝' | string;
};

export type CircleInviteExtra = {
  circleId: number;
  circleName: string;
  inviter: SimpleUser;
};

export type CircleRemovedExtra = {
  circleId: number;
  circleName: string;
  operator: SimpleUser;
};

export type CircleJoinExtra = {
  circleId: number;
  circleName: string;
  applicant: SimpleUser;
};

export type MemberJoinExtra = {
  circleId: number;
  circleName: string;
  newMember: SimpleUser;
};

export type MemberQuitExtra = {
  circleId: number;
  circleName: string;
  quitter: SimpleUser;
};

export type MemberRemovedExtra = {
  circleId: number;
  circleName: string;
  removedUser: SimpleUser;
  operator: SimpleUser;
};

export type SuggestionSubmitExtra = {
  articleId: number;
  suggestionId: number;
  proposer: SimpleUser;
};

export type SuggestionReviewExtra = {
  articleId: number;
  suggestionId: number;
  result: '通过' | '拒绝' | string;
  articleTitle?: string;
};

export type SystemMessageExtra = {
  title: string;
  content: string;
  link?: string;
};

// Extra 的联合类型
export type NotificationExtra = 
  | CommentExtra
  | LikeExtra
  | FollowExtra
  | ReplyExtra
  | ArticleAuditExtra
  | CircleInviteExtra
  | CircleRemovedExtra
  | CircleJoinExtra
  | MemberJoinExtra
  | MemberQuitExtra
  | MemberRemovedExtra
  | SuggestionSubmitExtra
  | SuggestionReviewExtra
  | SystemMessageExtra
  | Record<string, any>;

export interface Notification {
  id: number;
  type: NotificationType;
  title?: string; // 可选，由前端生成
  content?: string | Record<string, any>; // 已废弃，但保留兼容性
  isRead: boolean;
  createTime: string;
  updateTime: string;
  sourceId?: number;
  sourceType?: string;
  extra?: NotificationExtra; // 扩展数据
  fromUserId?: number;
  fromUserAvatar?: string;
  fromUserNickname?: string;
}

// 通知列表请求参数
export interface NotificationListParams {
  page?: number;
  size?: number;
  isRead?: boolean;
  type?: NotificationType;
}

// 通知列表响应
export interface NotificationListResponse {
  list: Notification[];
  total: number;
}

// 未读数量响应
export interface UnreadCountResponse {
  count: number;
}
