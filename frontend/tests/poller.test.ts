import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPoller } from '../src/utils/poller'

describe('createPoller', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('runs immediately and repeats', async () => {
    const fn = vi.fn()
    const p = createPoller(fn, 30_000)
    p.start()
    await p.runNow()
    expect(fn).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(30_000)
    await Promise.resolve()
    expect(fn).toHaveBeenCalledTimes(2)

    p.stop()
    vi.advanceTimersByTime(60_000)
    await Promise.resolve()
    expect(fn).toHaveBeenCalledTimes(2)
  })
})
