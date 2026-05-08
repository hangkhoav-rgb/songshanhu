import { describe, it, expect, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

describe('likeApi', () => {
  it('toggle posts to /like/:id', async () => {
    const req = (await import('@/utils/request')).default as any
    req.post.mockResolvedValueOnce({ liked: true, likes: 1 })
    const { likeApi } = await import('@/api/like')
    await likeApi.toggle(12)
    expect(req.post).toHaveBeenCalledWith('/like/12')
  })

  it('status gets /like/:id/status', async () => {
    const req = (await import('@/utils/request')).default as any
    req.get.mockResolvedValueOnce({ liked: false, likes: 9 })
    const { likeApi } = await import('@/api/like')
    await likeApi.status(9)
    expect(req.get).toHaveBeenCalledWith('/like/9/status')
  })

  it('batchDelete posts /like/batch-delete', async () => {
    const req = (await import('@/utils/request')).default as any
    req.post.mockResolvedValueOnce({ deleted: 2 })
    const { likeApi } = await import('@/api/like')
    await likeApi.batchDelete([1, 2])
    expect(req.post).toHaveBeenCalledWith('/like/batch-delete', { articleIds: [1, 2] })
  })
})
