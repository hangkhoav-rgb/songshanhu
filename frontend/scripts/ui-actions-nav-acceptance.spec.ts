import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { login } from './helpers/auth'

test.use({ video: 'on' })

async function ensureActionsData(page: any) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  if (!token) throw new Error('missing token')

  const publish = await page.request.post('/api/article/publish', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title: `UI验收 Like/Fav ${Date.now()}`,
      summary: 'ui acceptance',
      content: '<p>ui acceptance</p>',
      category: 'tech',
      status: 1,
      coverImage: 'https://images.unsplash.com/photo-1512100356132-db7f5c423136?q=80&w=800'
    }
  })
  const pj: any = await publish.json()
  const id = pj?.data
  if (!id) throw new Error(`publish failed: ${JSON.stringify(pj)}`)

  await page.request.post(`/api/article/${id}/like`, { headers: { Authorization: `Bearer ${token}` } })
  await page.request.post(`/api/article/${id}/collect`, { headers: { Authorization: `Bearer ${token}` } })
  return id
}

test.afterEach(async ({ page }, testInfo) => {
  const v = page.video()
  if (!v) return
  const reportsDir = path.resolve(process.cwd(), 'reports', 'ui')
  fs.mkdirSync(reportsDir, { recursive: true })
  const src = await v.path()
  const name = testInfo.title.replace(/[^a-zA-Z0-9_-]+/g, '_')
  fs.copyFileSync(src, path.join(reportsDir, `${name}.webm`))
})

test('actions UI screenshots and 4-state interaction', async ({ page }) => {
  test.setTimeout(120000)

  const reportsDir = path.resolve(process.cwd(), 'reports', 'ui')
  fs.mkdirSync(reportsDir, { recursive: true })

  await login(page)
  await ensureActionsData(page)

  await page.setViewportSize({ width: 375, height: 760 })
  await page.goto('/article/actions')
  await expect(page.getByRole('heading', { name: '我的点赞/收藏' })).toBeVisible()

  await page.waitForTimeout(300)
  await page.screenshot({ path: path.join(reportsDir, 'actions_light.png'), fullPage: false })

  const firstRow = page.locator('[data-testid="actions-list"] .row').first()
  await expect(firstRow).toBeVisible()

  const favBtn = firstRow.locator('button.lf-fav')
  const likeBtn = firstRow.locator('button.lf-like')
  if (await favBtn.count()) {
    await favBtn.click()
    await page.waitForTimeout(250)
  }
  if (await likeBtn.count()) {
    await likeBtn.click()
    await page.waitForTimeout(250)
  }
})
