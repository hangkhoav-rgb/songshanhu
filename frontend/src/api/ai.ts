import request from '@/utils/request'

export interface SummarizeRequest {
  title?: string
  content: string
}

export interface SummarizeResponse {
  summary: string
}

export const aiApi = {
  summarize(data: SummarizeRequest) {
    return request.post<SummarizeResponse>('/ai/summary', data)
  }
}

