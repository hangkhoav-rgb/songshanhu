import request from '@/utils/request'

export const likeApi = {
  toggle(articleId: number) {
    return request.post<{ liked: boolean; likes: number }>(`/like/${articleId}`)
  },
  status(articleId: number) {
    return request.get<{ liked: boolean; likes: number }>(`/like/${articleId}/status`)
  },
  batchDelete(articleIds: number[]) {
    return request.post<{ deleted: number }>('/like/batch-delete', { articleIds })
  }
}
