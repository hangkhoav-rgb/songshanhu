import request from '@/utils/request'

export interface Comment {
  id: number
  articleId: number
  userId: number
  username: string
  userAvatar?: string
  content: string
  createTime: string
}

export interface CommentRequest {
  articleId: number
  content: string
}

export const commentApi = {
  // 获取评论列表
  getList(articleId: string | number) {
    return request.get<Comment[]>(`/comment/list/${articleId}`)
  },

  // 发表评论
  add(data: CommentRequest) {
    return request.post<Comment>('/comment/add', data)
  }
}
