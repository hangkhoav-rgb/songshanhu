import request from '@/utils/request'

export interface CategoryStats {
  name: string
  value: number
}

export interface DailyStats {
  date: string
  count: number
}

export interface LocationStats {
  longitude: number
  latitude: number
  title: string
}

export interface DashboardStats {
  totalViews: number
  totalLikes: number
  totalComments: number
  totalArticles: number
  categoryDistribution: CategoryStats[]
  dailyTrend: DailyStats[]
  locationPoints: LocationStats[]
}

export const statsApi = {
  // 获取数据看板统计
  getDashboardStats() {
    return request.get<DashboardStats>('/stats/dashboard')
  }
}
