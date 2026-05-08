import { describe, it, expect, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('dashboardApi', () => {
  it('init calls /dashboard with params', async () => {
    const req = (await import('@/utils/request')).default as any
    req.get.mockResolvedValueOnce({})
    const { dashboardApi } = await import('@/api/dashboard')
    await dashboardApi.init({ current: 2, size: 5, days: 7, actionType: 'liked' })
    expect(req.get).toHaveBeenCalledWith('/dashboard', {
      params: { current: 2, size: 5, days: 7, actionType: 'liked' }
    })
  })
})

