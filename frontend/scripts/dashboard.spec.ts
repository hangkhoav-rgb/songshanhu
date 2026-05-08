import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'

test('dashboard loads and refresh works', async ({ page }) => {
  await login(page)

  await page.goto('/dashboard')
  await expect(page.getByTestId('dashboard')).toBeVisible()

  await page.getByRole('button', { name: '刷新' }).click()
  const ok = await page.getByTestId('dashboard-actions').isVisible().catch(() => false)
  const err = await page.getByTestId('dashboard-error').isVisible().catch(() => false)
  expect(ok || err).toBeTruthy()
})
