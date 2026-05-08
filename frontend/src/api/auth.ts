import request from '@/utils/request'
import type { LoginRequest, RegisterRequest, AuthResponse } from '@/types'

export const authApi = {
  // 用户登录
  login(data: LoginRequest) {
    return request.post<AuthResponse>('/auth/login', data)
  },

  // 用户注册
  register(data: RegisterRequest) {
    return request.post<string>('/auth/register', data)
  }
}
