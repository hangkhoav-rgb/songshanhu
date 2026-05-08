import { describe, it, expect, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

describe('favoriteApi', () => {
  it('toggle posts to /favorite/:id', async () => {
    const req = (await import('@/utils/request')).default as any
    req.post.mockResolvedValueOnce({ collected: true, collects: 1 })
    const { favoriteApi } = await import('@/api/favorite')
    await favoriteApi.toggle(3)
    expect(req.post).toHaveBeenCalledWith('/favorite/3')
  })

  it('status gets /favorite/:id/status', async () => {
    const req = (await import('@/utils/request')).default as any
    req.get.mockResolvedValueOnce({ collected: false, collects: 9 })
    const { favoriteApi } = await import('@/api/favorite')
    await favoriteApi.status(7)
    expect(req.get).toHaveBeenCalledWith('/favorite/7/status')
  })

  it('batchDelete posts /favorite/batch-delete', async () => {
    const req = (await import('@/utils/request')).default as any
    req.post.mockResolvedValueOnce({ deleted: 2 })
    const { favoriteApi } = await import('@/api/favorite')
    await favoriteApi.batchDelete([1, 2])
    expect(req.post).toHaveBeenCalledWith('/favorite/batch-delete', { articleIds: [1, 2] })
  })
})
