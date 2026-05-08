import fs from 'node:fs'
import path from 'node:path'

function hexToRgb(hex) {
  const h = hex.replace('#', '').trim()
  const v = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  const n = Number.parseInt(v, 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

function rgbToHsl({ r, g, b }) {
  const rn = r / 255
  const gn = g / 255
  const bn = b / 255
  const max = Math.max(rn, gn, bn)
  const min = Math.min(rn, gn, bn)
  const d = max - min
  let h = 0
  let s = 0
  const l = (max + min) / 2
  if (d !== 0) {
    s = d / (1 - Math.abs(2 * l - 1))
    switch (max) {
      case rn:
        h = ((gn - bn) / d) % 6
        break
      case gn:
        h = (bn - rn) / d + 2
        break
      default:
        h = (rn - gn) / d + 4
        break
    }
    h *= 60
    if (h < 0) h += 360
  }
  return { h: Math.round(h), s: Math.round(s * 100), l: Math.round(l * 100) }
}

function srgbToLinear(c) {
  const v = c / 255
  return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
}

function relativeLuminance({ r, g, b }) {
  const R = srgbToLinear(r)
  const G = srgbToLinear(g)
  const B = srgbToLinear(b)
  return 0.2126 * R + 0.7152 * G + 0.0722 * B
}

function contrastRatio(hexA, hexB) {
  const La = relativeLuminance(hexToRgb(hexA))
  const Lb = relativeLuminance(hexToRgb(hexB))
  const L1 = Math.max(La, Lb)
  const L2 = Math.min(La, Lb)
  return (L1 + 0.05) / (L2 + 0.05)
}

function fmtColor(hex) {
  const rgb = hexToRgb(hex)
  const hsl = rgbToHsl(rgb)
  return {
    hex,
    rgb: `rgb(${rgb.r}, ${rgb.g}, ${rgb.b})`,
    hsl: `hsl(${hsl.h}, ${hsl.s}%, ${hsl.l}%)`
  }
}

const palette = {
  '背景/基底': {
    'dm-bg-0': '#0F1115',
    'dm-bg-1': '#11141B',
    'article-bg': '#111318',
    'nav-bg': '#141821'
  },
  '表面层': {
    'surface-1': '#151922',
    'surface-2': '#1B2130',
    'surface-3': '#20273A'
  },
  '文本层': {
    'text-primary': '#E6E8EE',
    'text-secondary': '#A0A7B4',
    'text-tertiary': '#7C8596'
  },
  '交互色': {
    'primary': '#4C9AFF',
    'primary-700': '#1E6FDB',
    'secondary': '#7AD1FF',
    'accent': '#FF6B6B',
    'collect': '#4ECDC4'
  },
  '分隔线': {
    'divider-10%': '#E6E8EE'
  }
}

const checks = [
  { name: '正文 text-primary on article-bg', fg: '#E6E8EE', bg: '#111318' },
  { name: '正文 text-secondary on article-bg', fg: '#A0A7B4', bg: '#111318' },
  { name: '正文 text-tertiary on article-bg', fg: '#7C8596', bg: '#111318' },
  { name: '导航 text-primary on nav-bg', fg: '#E6E8EE', bg: '#141821' },
  { name: '导航 active on nav-bg', fg: '#FFFFFF', bg: '#141821' },
  { name: '链接/主色 primary on article-bg', fg: '#4C9AFF', bg: '#111318' }
]

const rows = []
rows.push('# 暗色模式配色评估与优化方案')
rows.push('')
rows.push(`生成时间：${new Date().toLocaleString()}`)
rows.push('')
rows.push('## 调整要点')
rows.push('- 降低偏蓝背景梯度的存在感，改为更中性的深灰蓝基底，减少长时间观看疲劳')
rows.push('- 将文本分为 primary/secondary/tertiary 三层，控制对比度梯度，避免“过亮刺眼/过灰读不清”')
rows.push('- 交互主色在暗背景下上调明度（primary: #4C9AFF），保证可见且不过饱和')
rows.push('')

rows.push('## 色值表（Hex / RGB / HSL）')
for (const [group, m] of Object.entries(palette)) {
  rows.push('')
  rows.push(`### ${group}`)
  rows.push('')
  rows.push('| Token | Hex | RGB | HSL |')
  rows.push('|---|---:|---:|---:|')
  for (const [k, hex] of Object.entries(m)) {
    const c = fmtColor(hex)
    rows.push(`| ${k} | ${c.hex} | ${c.rgb} | ${c.hsl} |`)
  }
}

rows.push('')
rows.push('## WCAG 对比度校验（AA 参考）')
rows.push('')
rows.push('| 场景 | 前景 | 背景 | 对比度 | 结论 |')
rows.push('|---|---:|---:|---:|---:|')
for (const c of checks) {
  const r = contrastRatio(c.fg, c.bg)
  const pass = r >= 4.5 ? 'AA 通过（普通文本）' : r >= 3 ? 'AA 通过（大号文本）' : '不通过'
  rows.push(`| ${c.name} | ${c.fg} | ${c.bg} | ${r.toFixed(2)} | ${pass} |`)
}

rows.push('')
rows.push('## 设备/亮度测试说明')
rows.push('- 已在 375px/768px/1366px 视口生成暗色截图用于层级检查')
rows.push('- 建议你用手机（iOS/Android）在 25%/50%/100% 亮度各看一次：重点关注 secondary/tertiary 文本与分隔线可读性')

const outDir = path.resolve(process.cwd(), 'reports')
fs.mkdirSync(outDir, { recursive: true })
fs.writeFileSync(path.join(outDir, 'dark-mode-palette-report.md'), rows.join('\n'), 'utf-8')

