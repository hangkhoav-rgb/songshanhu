import request from '@/utils/request'

export const uploadApi = {
  fetchImage(url: string) {
    const params = new URLSearchParams()
    params.set('url', url)
    return request.post<{ url: string; md5: string; filename: string }>('/upload/fetch-image', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
  },
  uploadImage(file: File) {
    const fd = new FormData()
    fd.append('file', file)
    return request.post<{ url: string; md5: string; filename: string }>('/upload/image', fd)
  }
}
