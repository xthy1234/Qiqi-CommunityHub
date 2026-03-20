// src/types/discover.ts

export interface UserInfo {
  id: number
  nickname: string
  avatar?: string
  signature?: string
  followerCount?: number
  followingCount?: number
  articleCount?: number
  isFollowing?: boolean
  isOnline?: boolean
}

export interface CircleInfo {
  id: number
  name: string
  avatar?: string
  description?: string
  type: number  // 0-私密，1-公开
  memberCount?: number
  ownerId?: number
  ownerNickname?: string
  isJoined?: boolean
  createTime?: string
}
