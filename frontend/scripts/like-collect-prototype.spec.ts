import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { login } from './helpers/auth'

async function publishTestArticle(page: any) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  if (!token) throw new Error('missing token')
  const r = await page.request.post('/api/article/publish', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title: `PROTO LikeCollect ${Date.now()}`,
      summary: 'prototype summary',
      content: '<p>prototype</p>',
      category: 'tech',
      status: 1
    }
  })
  const j: any = await r.json()
  const id = j?.data
  if (!id) throw new Error(`publish returned empty id: ${JSON.stringify(j)}`)
  return id
}

test('generate like/collect prototypes', async ({ page }) => {
  test.setTimeout(120000)

  const outDir = path.resolve(process.cwd(), 'docs', 'assets', 'like-collect')
  fs.mkdirSync(outDir, { recursive: true })

  await login(page)
  const id = await publishTestArticle(page)

  await page.setViewportSize({ width: 375, height: 740 })
  await page.goto(`/article/${id}`)
  await expect(page.getByTestId('like-btn')).toBeVisible()

  await page.screenshot({ path: path.join(outDir, 'state_normal_light.png'), fullPage: false })

  const likeBtn = page.getByTestId('like-btn')
  const likeResp = page.waitForResponse((r) => r.url().includes(`/api/article/${id}/like`) && r.request().method() === 'POST')
  await likeBtn.click()
  await likeResp
  await expect(likeBtn).toHaveAttribute('aria-pressed', 'true')
  await page.screenshot({ path: path.join(outDir, 'state_success_like_light.png'), fullPage: false })

  await page.route(`**/api/article/${id}/collect`, async (route) => {
    await new Promise((r) => setTimeout(r, 800))
    await route.continue()
  })
  const collectBtn = page.getByTestId('collect-btn')
  const collectResp = page.waitForResponse((r) => r.url().includes(`/api/article/${id}/collect`) && r.request().method() === 'POST')
  await collectBtn.click()
  await expect(collectBtn.locator('.lc-spin')).toBeVisible()
  await page.screenshot({ path: path.join(outDir, 'state_loading_collect_light.png'), fullPage: false })
  await collectResp

  await page.unroute(`**/api/article/${id}/collect`)
  await page.route(`**/api/article/${id}/like`, async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ code: 500, message: 'fail', data: null })
    })
  })

  await likeBtn.click()
  await page.waitForTimeout(400)
  await page.screenshot({ path: path.join(outDir, 'state_fail_like_light.png'), fullPage: false })

  await page.unroute(`**/api/article/${id}/like`)
})
