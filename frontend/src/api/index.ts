import request from '@/utils/request'
import type { User, LoginRequest, RegisterRequest, Article, PageResult } from '@/types'

// 认证相关
export const authApi = {
  login(data: LoginRequest) {
    return request.post<{ token?: string; mustResetPassword?: boolean; resetToken?: string; user: User }>('/auth/login', data)
  },
  firstReset(data: { resetToken: string; newPassword: string }) {
    return request.post<{ token: string; user: User }>('/auth/first-reset', data)
  },
  register(data: RegisterRequest) {
    return request.post<{ token: string; user: User }>('/auth/register', data)
  },
  logout() {
    return request.post('/auth/logout')
  },
  getCurrentUser() {
    return request.get<User>('/auth/current')
  }
}

// 文章相关
export const articleApi = {
  getList(params: { page: number; size: number; keyword?: string; categoryId?: number }) {
    return request.get<PageResult<Article>>('/article/list', { params })
  },
  getDetail(id: number) {
    return request.get<Article>(`/article/${id}`)
  },
  create(data: Partial<Article>) {
    return request.post<Article>('/article', data)
  },
  update(id: number, data: Partial<Article>) {
    return request.put<Article>(`/article/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/article/${id}`)
  },
  like(id: number) {
    return request.post(`/article/${id}/like`)
  },
  collect(id: number) {
    return request.post(`/article/${id}/collect`)
  }
}

// 用户相关
export const userApi = {
  getProfile() {
    return request.get<User>('/user/profile')
  },
  updateProfile(data: Partial<User>) {
    return request.put<User>('/user/profile', data)
  },
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<{ url: string }>('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
