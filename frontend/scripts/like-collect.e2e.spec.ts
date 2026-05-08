import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'

test.describe.configure({ retries: 2 })

async function publishTestArticle(page: any) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  if (!token) throw new Error('missing token')
  const r = await page.request.post('/api/article/publish', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title: `E2E LikeCollect ${Date.now()}`,
      summary: 'e2e summary',
      content: '<p>e2e</p>',
      category: 'tech',
      status: 1
    }
  })
  if (!r.ok()) throw new Error(`publish failed: ${r.status()}`)
  const j: any = await r.json()
  const id = j?.data
  if (!id) throw new Error(`publish returned empty id: ${JSON.stringify(j)}`)
  return id
}

test('like/collect UI toggles and persists', async ({ page }) => {
  await login(page)
  const id = await publishTestArticle(page)

  await page.goto(`/article/${id}`)
  await expect(page.getByTestId('like-btn')).toBeVisible()

  const likeBtn = page.getByTestId('like-btn')
  const collectBtn = page.getByTestId('collect-btn')

  await expect(likeBtn).toHaveAttribute('aria-pressed', 'false')
  await expect(collectBtn).toHaveAttribute('aria-pressed', 'false')

  const likeResp = page.waitForResponse((r) => r.url().includes(`/api/article/${id}/like`) && r.request().method() === 'POST')
  await likeBtn.click()
  await likeResp
  await expect(likeBtn).toHaveAttribute('aria-pressed', 'true')

  const collectResp = page.waitForResponse((r) => r.url().includes(`/api/article/${id}/collect`) && r.request().method() === 'POST')
  await collectBtn.click()
  await collectResp
  await expect(collectBtn).toHaveAttribute('aria-pressed', 'true')

  await page.reload()
  await expect(likeBtn).toHaveAttribute('aria-pressed', 'true')
  await expect(collectBtn).toHaveAttribute('aria-pressed', 'true')
})

test('like UI shows loading spin and does not flip on failure', async ({ page }) => {
  await login(page)
  const id = await publishTestArticle(page)
  await page.goto(`/article/${id}`)

  await page.route(`**/api/article/${id}/like`, async (route) => {
    await new Promise((r) => setTimeout(r, 500))
    await route.fulfill({ status: 500, body: JSON.stringify({ code: 500, message: 'fail', data: null }), contentType: 'application/json' })
  })

  const likeBtn = page.getByTestId('like-btn')
  await expect(likeBtn).toHaveAttribute('aria-pressed', 'false')

  await likeBtn.click()
  await expect(page.locator('.lc-spin')).toBeVisible()
  await page.waitForTimeout(700)
  await expect(likeBtn).toHaveAttribute('aria-pressed', 'false')
})
