import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'

test('my actions page loads and basic interactions work', async ({ page }) => {
  await login(page)

  await page.goto('/article/actions')
  await expect(page.getByTestId('actions-list')).toBeVisible()

  const collected = page.getByText('收藏').first()
  const liked = page.getByText('点赞').first()

  await collected.click()
  await liked.click()
  await expect(page.getByTestId('actions-list')).toBeVisible()

  await liked.click()
  await collected.click()
  await expect(page.getByTestId('actions-list')).toBeVisible()

  await page.getByRole('button', { name: '管理' }).click()
  await expect(page.getByText('删除(')).toBeVisible()
})
