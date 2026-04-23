// src/api/captcha.ts
import httpClient from '@/utils/http'

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/**
 * 验证码 API
 */
export const captchaApi = {
  /**
   * 获取图形验证码
   */
  getCaptcha: () => {
    return httpClient.get<ApiResponse<{ captchaKey: string; captchaImage: string }>>('/captcha/image')
  }
}

export default captchaApi
