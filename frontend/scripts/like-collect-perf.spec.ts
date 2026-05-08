import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { login } from './helpers/auth'

const viewports = [
  { name: '320x640', width: 320, height: 640 },
  { name: '375x667', width: 375, height: 667 },
  { name: '414x736', width: 414, height: 736 }
]

async function publishTestArticle(page: any) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  if (!token) throw new Error('missing token')
  const r = await page.request.post('/api/article/publish', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title: `PERF LikeCollect ${Date.now()}`,
      summary: 'perf summary',
      content: '<p>perf</p>',
      category: 'tech',
      status: 1
    }
  })
  const j: any = await r.json()
  const id = j?.data
  if (!id) throw new Error(`publish returned empty id: ${JSON.stringify(j)}`)
  return id
}

test('like/collect performance report', async ({ page }) => {
  test.setTimeout(120000)

  const reportsDir = path.resolve(process.cwd(), 'reports')
  fs.mkdirSync(reportsDir, { recursive: true })

  const protoHtml = `<!doctype html><html><head><meta charset="utf-8" /><meta name="viewport" content="width=device-width,initial-scale=1" />
  <style>
    :root{--text:#0f172a;--border-soft:rgba(15,23,42,.12)}
    body{margin:0;padding:16px;font-family:PingFang SC,Helvetica Neue,Arial,sans-serif;background:#fff;color:var(--text)}
    .lc{display:flex;gap:10px;flex-wrap:wrap}
    .lc-btn{display:inline-flex;align-items:center;gap:10px;padding:10px 12px;border-radius:12px;border:1px solid var(--border-soft);background:rgba(255,255,255,.72);min-inline-size:152px}
    .ico{width:18px;height:18px}
    .count{color:rgba(15,23,42,.68)}
  </style></head><body>
  <div class="lc" role="group" aria-label="点赞与收藏">
    <button class="lc-btn" type="button" aria-pressed="false"><svg viewBox="0 0 24 24" class="ico" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M14.5 9V5.5c0-1.657-1.343-3-3-3L7 9v12h11.2c.858 0 1.6-.582 1.8-1.416l2-8A2 2 0 0 0 20.056 9H14.5Z" stroke="rgba(15,23,42,.82)" stroke-width="1.8" stroke-linejoin="round"/><path d="M7 9H4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3V9Z" stroke="rgba(15,23,42,.82)" stroke-width="1.8" stroke-linejoin="round"/></svg><span>点赞</span><span class="count">12</span></button>
    <button class="lc-btn" type="button" aria-pressed="true"><svg viewBox="0 0 24 24" class="ico" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27Z" stroke="rgba(0,82,217,.95)" stroke-width="1.8" stroke-linejoin="round"/></svg><span>收藏</span><span class="count">3</span></button>
  </div></body></html>`
  const dataUrl = `data:text/html,${encodeURIComponent(protoHtml)}`
  const isoStart = Date.now()
  await page.goto(dataUrl)
  const isoMs = Date.now() - isoStart
  const isoPaint = await page.evaluate(() => {
    const p = performance.getEntriesByType('paint') as any[]
    const fcp = p.find((x) => x.name === 'first-contentful-paint')?.startTime ?? null
    const fp = p.find((x) => x.name === 'first-paint')?.startTime ?? null
    return { fp, fcp }
  })

  await login(page)
  const id = await publishTestArticle(page)

  const rows: string[] = []
  rows.push('# 点赞/收藏组件性能报告')
  rows.push('')
  rows.push(`生成时间：${new Date().toLocaleString()}`)
  rows.push('')

  rows.push('## 隔离原型（组件级）')
  rows.push('')
  rows.push(`- 载入耗时（ms）：${isoMs}`)
  rows.push(`- FP ms：${isoPaint.fp}`)
  rows.push(`- FCP ms：${isoPaint.fcp}`)
  rows.push('')

  for (const vp of viewports) {
    await page.setViewportSize({ width: vp.width, height: vp.height })

    let liked = false
    let collected = false
    let likes = 0
    let collects = 0

    await page.route(`**/api/article/${id}`, async (route) => {
      const json = {
        code: 200,
        message: 'ok',
        data: {
          id,
          title: `PERF LikeCollect ${id}`,
          content: '<p>perf</p>',
          summary: 'perf summary',
          category: 'tech',
          authorName: 'perf'
        }
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(json) })
    })

    await page.route(`**/api/article/${id}/like/status`, async (route) => {
      const json = { code: 200, message: 'ok', data: { liked, likes } }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(json) })
    })

    await page.route(`**/api/article/${id}/collect/status`, async (route) => {
      const json = { code: 200, message: 'ok', data: { collected, collects } }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(json) })
    })

    await page.route(`**/api/article/${id}/like`, async (route) => {
      liked = !liked
      likes = Math.max(0, likes + (liked ? 1 : -1))
      const json = { code: 200, message: 'ok', data: { liked, likes } }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(json) })
    })

    await page.route(`**/api/article/${id}/collect`, async (route) => {
      collected = !collected
      collects = Math.max(0, collects + (collected ? 1 : -1))
      const json = { code: 200, message: 'ok', data: { collected, collects } }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(json) })
    })

    const navStart = Date.now()
    await page.goto(`/article/${id}`)
    await expect(page.getByTestId('like-btn')).toBeVisible()
    const firstInteractiveMs = Date.now() - navStart

    const paint = await page.evaluate(() => {
      const p = performance.getEntriesByType('paint') as any[]
      const fcp = p.find((x) => x.name === 'first-contentful-paint')?.startTime ?? null
      const fp = p.find((x) => x.name === 'first-paint')?.startTime ?? null
      return { fp, fcp }
    })

    const heapBefore = await page.evaluate(() => (performance as any).memory?.usedJSHeapSize ?? null)

    const likeBtn = page.getByTestId('like-btn')
    const collectBtn = page.getByTestId('collect-btn')

    const measureToggle = async (btn: any) => {
      const t0 = await page.evaluate(() => performance.now())
      await btn.click()
      await page.waitForTimeout(0)
      const t1 = await page.evaluate(() => performance.now())
      return t1 - t0
    }

    const latencies: number[] = []
    for (let i = 0; i < 10; i++) {
      latencies.push(await measureToggle(likeBtn))
      latencies.push(await measureToggle(collectBtn))
    }

    const heapAfter = await page.evaluate(() => (performance as any).memory?.usedJSHeapSize ?? null)

    const avg = latencies.reduce((a, b) => a + b, 0) / latencies.length
    const p95 = [...latencies].sort((a, b) => a - b)[Math.floor(latencies.length * 0.95)]

    rows.push(`## ${vp.name}`)
    rows.push('')
    rows.push(`- 首次可交互（Like 按钮可见）ms：${firstInteractiveMs}`)
    rows.push(`- FP ms：${paint.fp}`)
    rows.push(`- FCP ms：${paint.fcp}`)
    rows.push(`- 切换延迟 avg/p95（ms，20 次）：${avg.toFixed(2)}/${p95?.toFixed?.(2)}`)
    rows.push(`- usedJSHeapSize before/after：${heapBefore}/${heapAfter}`)
    if (typeof heapBefore === 'number' && typeof heapAfter === 'number' && heapBefore > 0) {
      rows.push(`- 内存增长：${(((heapAfter - heapBefore) / heapBefore) * 100).toFixed(2)}%`)
    }
    rows.push('')
  }

  const out = path.join(reportsDir, 'like-collect-perf.md')
  fs.writeFileSync(out, rows.join('\n'), 'utf-8')
})
