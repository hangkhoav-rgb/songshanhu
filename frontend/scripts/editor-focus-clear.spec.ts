import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'

test('editor focus clears content and moves caret to start', async ({ page }) => {
  await login(page)

  await page.goto('/article/create')

  const editor = page.locator('.ql-editor')
  await expect(editor).toHaveCount(1)

  await editor.click()
  await editor.type('hello world')
  await expect(editor).toContainText('hello world')

  await page.locator('body').click({ position: { x: 2, y: 2 } })
  await editor.click()

  await expect(editor).toHaveText('')

  const selectionIndex = await page.evaluate(() => {
    const q: any = (window as any).__createQuill
    const sel = q?.getSelection?.()
    return sel?.index
  })
  expect(selectionIndex).toBe(0)

  const caretColor = await editor.evaluate((el) => getComputedStyle(el).caretColor)
  expect(caretColor).not.toBe('transparent')
})
