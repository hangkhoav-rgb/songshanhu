import request from '@/utils/request'
import type { Article, PageResult } from '@/types'

export interface ArticleActionItem {
  id: number
  title: string
  summary?: string
  coverImage?: string
  category: string
  authorId: number
  authorName: string
  views: number
  comments: number
  likes: number
  likedAt?: string
  collectedAt?: string
  actionTime: string
}

export interface ArticleRequest {
  title: string
  content: string
  summary?: string
  category?: string
  coverImage?: string
  longitude?: number
  latitude?: number
  status?: number
}

export const articleApi = {
  // 获取文章列表
  getList(params: {
    current: number
    size: number
    category?: string
    keyword?: string
    status?: number
    mine?: boolean
  }) {
    return request.get<PageResult<Article>>('/article/list', { params })
  },

  getMine(params: {
    current: number
    size: number
    status?: number
    keyword?: string
  }) {
    return request.get<PageResult<Article>>('/article/mine', { params })
  },

  // 获取文章详情
  getDetail(id: string | number) {
    return request.get<Article>(`/article/${id}`)
  },

  toggleLike(id: string | number) {
    return request.post<{ liked: boolean; likes: number }>(`/article/${id}/like`)
  },
  getLikeStatus(id: string | number) {
    return request.get<{ liked: boolean; likes: number }>(`/article/${id}/like/status`)
  },
  toggleCollect(id: string | number) {
    return request.post<{ collected: boolean; collects: number }>(`/article/${id}/collect`)
  },
  getCollectStatus(id: string | number) {
    return request.get<{ collected: boolean; collects: number }>(`/article/${id}/collect/status`)
  },

  // 发布文章
  publish(data: ArticleRequest) {
    return request.post<number>('/article/publish', data)
  },

  getMyActions(params: {
    current: number
    size: number
    liked: boolean
    collected: boolean
  }) {
    return request.get<PageResult<ArticleActionItem>>('/article/actions/mine', { params })
  },

  batchUnlike(articleIds: number[]) {
    return request.post<{ deleted: number }>('/article/like/batch-delete', { articleIds })
  },

  batchUncollect(articleIds: number[]) {
    return request.post<{ deleted: number }>('/article/collect/batch-delete', { articleIds })
  }
}
