import request from '@/utils/request'

export function track(event: string, props?: Record<string, any>) {
  return request
    .post('/monitor/event', {
      event,
      page: location.pathname,
      props: props || {}
    })
    .catch(() => undefined)
}

