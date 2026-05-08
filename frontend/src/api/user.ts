import request from '@/utils/request'

export interface ProfileUpdateRequest {
  nickname?: string
  gender?: 'male' | 'female' | 'secret'
  bio?: string
  extra?: string
}

export const userApi = {
  getProfile() {
    return request.get<UserProfile>('/user/profile')
  },
  updateProfile(data: ProfileUpdateRequest) {
    return request.put('/user/profile', data, {
      headers: { 'Idempotency-Key': crypto.randomUUID?.() || String(Date.now()) }
    })
  },
  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<Record<string, string>>('/user/avatar', formData)
  }
}

export interface UserProfile {
  id: number
  username: string
  nickname?: string
  email?: string
  role: string
  gender?: 'male' | 'female' | 'secret'
  bio?: string
  extra?: string
  avatarUrls?: Record<string, string>
}
