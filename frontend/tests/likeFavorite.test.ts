import { describe, it, expect } from 'vitest'
import { applyFavoriteToggleOptimistic, applyLikeToggleOptimistic, clampNonNegative } from '@/utils/likeFavorite'

describe('likeFavorite helpers', () => {
  it('clamps to non-negative integers', () => {
    expect(clampNonNegative(-1)).toBe(0)
    expect(clampNonNegative(1.9)).toBe(1)
    expect(clampNonNegative(Number.NaN)).toBe(0)
  })

  it('toggles like optimistically and updates counts', () => {
    const s1 = applyLikeToggleOptimistic({ liked: false, likes: 0, collected: false, collects: 0 })
    expect(s1.liked).toBe(true)
    expect(s1.likes).toBe(1)

    const s2 = applyLikeToggleOptimistic({ ...s1 })
    expect(s2.liked).toBe(false)
    expect(s2.likes).toBe(0)
  })

  it('toggles favorite optimistically and updates counts', () => {
    const s1 = applyFavoriteToggleOptimistic({ liked: false, likes: 0, collected: false, collects: 0 })
    expect(s1.collected).toBe(true)
    expect(s1.collects).toBe(1)

    const s2 = applyFavoriteToggleOptimistic({ ...s1 })
    expect(s2.collected).toBe(false)
    expect(s2.collects).toBe(0)
  })

  it('never decrements below zero', () => {
    const s1 = applyLikeToggleOptimistic({ liked: true, likes: 0, collected: false, collects: 0 })
    expect(s1.liked).toBe(false)
    expect(s1.likes).toBe(0)

    const s2 = applyFavoriteToggleOptimistic({ liked: false, likes: 0, collected: true, collects: 0 })
    expect(s2.collected).toBe(false)
    expect(s2.collects).toBe(0)
  })
})

