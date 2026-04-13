// src/api/auth.ts

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
 * 管理员登录
 */
export function adminLogin(data: { account: string; password: string }): Promise<ApiResponse<{ token: string; user: any }>> {
  return httpClient.post('/users/admin/login', data)
}

/**
 * 管理员注册
 */
export function adminRegister(data: any): Promise<ApiResponse<any>> {
  return httpClient.post('/users/admin/register', data)
}

/**
 * 用户登录
 */
export function userLogin(data: { username: string; password: string }): Promise<ApiResponse<{ token: string; user: any }>> {
  return httpClient.post('/users/login', data)
}

/**
 * 用户注册
 */
export function userRegister(data: any): Promise<ApiResponse<any>> {
  return httpClient.post('/users/register', data)
}

/**
 * 退出登录
 */
export function logout(): Promise<ApiResponse<void>> {
  return httpClient.post('/users/logout')
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser(): Promise<ApiResponse<any>> {
  return httpClient.get('/users/me')
}

/**
 * 重置密码
 */
export function resetPassword(data: { username: string; newPassword: string }): Promise<ApiResponse<any>> {
  return httpClient.post('/users/reset-password', data)
}

export default {
  adminLogin,
  adminRegister,
  userLogin,
  userRegister,
  logout,
  getCurrentUser,
  resetPassword
}
