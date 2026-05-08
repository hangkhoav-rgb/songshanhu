import request from '@/utils/request'

export const favoriteApi = {
  toggle(articleId: number) {
    return request.post<{ collected: boolean; collects: number }>(`/favorite/${articleId}`)
  },
  status(articleId: number) {
    return request.get<{ collected: boolean; collects: number }>(`/favorite/${articleId}/status`)
  },
  batchDelete(articleIds: number[]) {
    return request.post<{ deleted: number }>('/favorite/batch-delete', { articleIds })
  }
}
