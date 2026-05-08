/**
 * 创建一个可控轮询器：支持立即执行、暂停/恢复、以及自动清理定时器。
 */
export function createPoller(fn: () => Promise<void> | void, intervalMs: number) {
  let timer: number | undefined
  let stopped = false
  let running = false

  const tick = async () => {
    if (stopped || running) return
    if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return
    running = true
    try {
      await fn()
    } finally {
      running = false
    }
  }

  const start = () => {
    if (timer) return
    stopped = false
    timer = window.setInterval(tick, intervalMs)
  }

  const stop = () => {
    stopped = true
    if (timer) {
      window.clearInterval(timer)
      timer = undefined
    }
  }

  const runNow = () => tick()

  return { start, stop, runNow }
}

