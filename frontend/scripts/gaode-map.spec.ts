import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'

const hasAmap = !!process.env.VITE_AMAP_KEY && !!process.env.VITE_AMAP_SECURITY_JS_CODE

test('gaode map sdk loads and dashboard map renders', async ({ page }) => {
  test.skip(!hasAmap, '缺少 VITE_AMAP_KEY/VITE_AMAP_SECURITY_JS_CODE')
  const amapRequests: number[] = []
  page.on('response', (res) => {
    const url = res.url()
    if (url.includes('webapi.amap.com/maps')) {
      amapRequests.push(res.status())
    }
  })

  await login(page)

  await page.goto('/dashboard')

  await expect(page.locator('[data-map-ready="true"]')).toHaveCount(1, { timeout: 20000 })
  expect(amapRequests.length).toBeGreaterThan(0)
  expect(amapRequests.every((s) => s === 200)).toBeTruthy()
})

test('screenshots at 1920x1080, 1366x768, 375x667', async ({ page }) => {
  test.skip(!hasAmap, '缺少 VITE_AMAP_KEY/VITE_AMAP_SECURITY_JS_CODE')
  await login(page)

  const cases = [
    { w: 1920, h: 1080, name: '1920x1080' },
    { w: 1366, h: 768, name: '1366x768' },
    { w: 375, h: 667, name: '375x667' }
  ]

  for (const c of cases) {
    await page.setViewportSize({ width: c.w, height: c.h })
    await page.goto('/dashboard')
    await expect(page.locator('[data-map-ready="true"]')).toHaveCount(1, { timeout: 20000 })
    await expect(page).toHaveScreenshot(`dashboard-${c.name}.png`, { fullPage: true })
  }
})
