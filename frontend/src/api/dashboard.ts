import request from '@/utils/request'
import type { DashboardStats } from '@/api/stats'

export interface DashboardSummary {
  likeCount: number
  collectCount: number
  lastActionTime: string | null
}

export type DashboardActionType = 'liked' | 'collected'

export interface DashboardAction {
  articleId: number
  title: string
  actionType: DashboardActionType
  actionTime: string
}

export interface MpPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface DashboardInit {
  stats: DashboardStats
  summary: DashboardSummary
  actions: MpPage<DashboardAction>
}

export const dashboardApi = {
  init(params: { current?: number; size?: number; days?: number; actionType?: DashboardActionType | '' } = {}) {
    return request.get<DashboardInit>('/dashboard', { params })
  },
  summary() {
    return request.get<DashboardSummary>('/dashboard/summary')
  },

  actions(params: { current: number; size: number; days?: number; actionType?: DashboardActionType | '' }) {
    return request.get<MpPage<DashboardAction>>('/dashboard/actions', { params })
  }
}
