import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'
import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'

const viewports = [
  { name: '1920x1080', width: 1920, height: 1080 },
  { name: '1440x900', width: 1440, height: 900 },
  { name: '1366x768', width: 1366, height: 768 },
  { name: '1024x768', width: 1024, height: 768 },
  { name: '375x667', width: 375, height: 667 }
]

const reportsDir = path.resolve(process.cwd(), 'reports')
const visualDir = path.join(reportsDir, 'visual')
const lighthouseDir = path.join(reportsDir, 'lighthouse')

function ensureDir(p: string) {
  fs.mkdirSync(p, { recursive: true })
}

test('dashboard visual regression report', async ({ page, baseURL }) => {
  test.setTimeout(120000)
  ensureDir(visualDir)
  ensureDir(lighthouseDir)

  await login(page)

  const rows: string[] = []
  rows.push('# 数据看板视觉走查报告')
  rows.push('')
  rows.push(`生成时间：${new Date().toLocaleString()}`)
  rows.push('')
  rows.push('## 多分辨率截图与指标')
  rows.push('')

  for (const vp of viewports) {
    await page.setViewportSize({ width: vp.width, height: vp.height })
    await page.goto('/dashboard')
    await page.waitForTimeout(1200)

    const category = page.locator('.category-grid-wrap')
    await expect(category).toBeVisible()

    const panels = category.locator('.category-panel')
    const barPanel = panels.nth(0)
    const piePanel = panels.nth(1)

    const bodyHasHScroll = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
    const gridTemplateColumns = await page.evaluate(() => {
      const el = document.querySelector('.category-grid-wrap') as HTMLElement | null
      return el ? getComputedStyle(el).gridTemplateColumns : ''
    })
    const wrapMinInline = await page.evaluate(() => {
      const el = document.querySelector('.category-grid-wrap') as HTMLElement | null
      return el ? getComputedStyle(el).minInlineSize : ''
    })
    const wrapMaxInline = await page.evaluate(() => {
      const el = document.querySelector('.category-grid-wrap') as HTMLElement | null
      return el ? getComputedStyle(el).maxInlineSize : ''
    })

    const wrapBox = await category.boundingBox()
    const barBox = await barPanel.boundingBox()
    const pieBox = await piePanel.boundingBox()
    expect(wrapBox).toBeTruthy()
    expect(barBox).toBeTruthy()
    expect(pieBox).toBeTruthy()

    const imgName = `dashboard_${vp.name}.png`
    const imgPath = path.join(visualDir, imgName)
    await page.screenshot({ path: imgPath, fullPage: true })

    rows.push(`### ${vp.name}`)
    rows.push('')
    rows.push(`- 截图：![](./visual/${imgName})`)
    rows.push(`- 横向滚动条：${bodyHasHScroll ? '有（不通过）' : '无（通过）'}`)
    rows.push(`- category 容器 offsetWidth/offsetHeight：${Math.round(wrapBox!.width)}/${Math.round(wrapBox!.height)}`)
    rows.push(`- bar panel offsetWidth/offsetHeight：${Math.round(barBox!.width)}/${Math.round(barBox!.height)}`)
    rows.push(`- pie panel offsetWidth/offsetHeight：${Math.round(pieBox!.width)}/${Math.round(pieBox!.height)}`)
    rows.push(`- grid-template-columns：${gridTemplateColumns}`)
    rows.push(`- min-inline-size：${wrapMinInline}`)
    rows.push(`- max-inline-size：${wrapMaxInline}`)
    rows.push('')
  }

  rows.push('## Lighthouse（CLS）')
  rows.push('')
  const base = baseURL || 'http://localhost:3000'
  execFileSync('node', ['./scripts/run-lighthouse-dashboard.mjs'], {
    cwd: process.cwd(),
    env: { ...process.env, LH_BASE_URL: base }
  })

  const lhrJsonPath = path.join(lighthouseDir, 'dashboard.lhr.json')
  const lhr = JSON.parse(fs.readFileSync(lhrJsonPath, 'utf-8'))
  const clsScore = Math.round(((lhr?.audits?.['cumulative-layout-shift']?.score as number) || 0) * 100)
  const clsValue = lhr?.audits?.['cumulative-layout-shift']?.numericValue ?? null
  rows.push(`- CLS 评分：${clsScore}`)
  rows.push(`- CLS 数值：${clsValue}`)
  rows.push(`- 报告：./lighthouse/dashboard.report.html`)
  rows.push('')

  const reportPath = path.join(lighthouseDir, 'dashboard.report.html')
  await page.goto(`file://${reportPath}`)
  await page.setViewportSize({ width: 1280, height: 720 })
  await page.waitForTimeout(800)
  const lhShotName = 'lighthouse_dashboard.png'
  await page.screenshot({ path: path.join(lighthouseDir, lhShotName), fullPage: false })
  rows.push(`- CLS 截图：![](./lighthouse/${lhShotName})`)
  rows.push('')

  const out = path.join(reportsDir, 'dashboard-visual-report.md')
  fs.writeFileSync(out, rows.join('\n'), 'utf-8')
})
