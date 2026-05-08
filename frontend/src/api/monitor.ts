import request from '@/utils/request'

export interface ImageErrorReportRequest {
  url: string
  page?: string
  userAgent?: string
  referrer?: string
  message?: string
}

export const monitorApi = {
  reportImageError(data: ImageErrorReportRequest) {
    return request.post('/monitor/image-error', data)
  }
}

