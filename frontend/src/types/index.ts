// API 响应类型
export interface Result<T = any> {
  code: number
  message: string
  data: T
}

// 用户类型
export interface User {
  id: number
  username: string
  email: string
  nickname?: string
  avatar?: string
  role: string
  createTime: string
}

// 文章类型
export interface Article {
  id: number
  title: string
  content: string
  summary: string
  coverImage?: string
  longitude?: number
  latitude?: number
  authorId: number
  authorName: string
  category: string
  status: number // 0-草稿,1-待审核,2-已发布,3-驳回,4-下架
  lastReviewReason?: string
  lastReviewTime?: string
  lastReviewResult?: string
  views: number
  likes: number
  comments: number
  createTime: string
  updateTime: string
}

// 分页结果 (MyBatis-Plus 格式)
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 图表配置
export interface ChartConfig {
  id: string
  type: 'bar' | 'line' | 'pie' | 'scatter' | 'map'
  title: string
  options: any
  data?: any[]
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  email: string
  password: string
  nickname?: string
}

// 登录响应
export interface AuthResponse {
  token: string
  user: User
}

export namespace AuthResponse {
  export interface UserResponse extends User {}
}
