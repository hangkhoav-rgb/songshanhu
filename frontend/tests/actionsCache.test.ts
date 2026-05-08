import { describe, expect, it } from 'vitest'
import { actionsCacheKey } from '../src/utils/actionsCache'

describe('actionsCacheKey', () => {
  it('creates stable key', () => {
    expect(actionsCacheKey(true, true)).toBe('ssl_actions_cache_v1_1_1')
    expect(actionsCacheKey(true, false)).toBe('ssl_actions_cache_v1_1_0')
    expect(actionsCacheKey(false, true)).toBe('ssl_actions_cache_v1_0_1')
    expect(actionsCacheKey(false, false)).toBe('ssl_actions_cache_v1_0_0')
  })
})

