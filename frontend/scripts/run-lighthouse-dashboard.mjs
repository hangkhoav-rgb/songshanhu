import fs from 'node:fs'
import path from 'node:path'
import { launch } from 'chrome-launcher'
import lighthouse from 'lighthouse'

const baseUrl = process.env.LH_BASE_URL || 'http://localhost:3000'
const apiUrl = process.env.LH_API_URL || 'http://localhost:8082/api'
const pass = process.env.LH_PASS || '123456'
const user = process.env.LH_USER || ''

function ensureDir(p) {
  fs.mkdirSync(p, { recursive: true })
}

async function postJson(url, body) {
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body)
  })
  const text = await r.text()
  try {
    return JSON.parse(text)
  } catch {
    return { code: -1, message: 'invalid json', raw: text }
  }
}

async function getToken() {
  if (user) {
    const login = await postJson(`${apiUrl}/auth/login`, { username: user, password: pass })
    const token = login?.data?.token
    if (login?.code === 200 && token) return token
  }

  const uniq = `${Date.now()}_${Math.floor(Math.random() * 1_000_000)}`
  const username = `lh_${uniq}`
  await postJson(`${apiUrl}/auth/register`, {
    username,
    password: pass,
    nickname: username,
    email: `${username}@example.com`
  })
  const login = await postJson(`${apiUrl}/auth/login`, { username, password: pass })
  const token = login?.data?.token
  if (login?.code !== 200 || !token) {
    throw new Error(`login failed: code=${login?.code} msg=${login?.message}`)
  }
  return token
}

const reportsDir = path.resolve(process.cwd(), 'reports', 'lighthouse')
ensureDir(reportsDir)

const token = await getToken()
const url = `${baseUrl}/dashboard?__token=${encodeURIComponent(token)}`

const chrome = await launch({
  chromeFlags: ['--headless=new', '--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
})

try {
  const result = await lighthouse(url, {
    port: chrome.port,
    output: ['html', 'json'],
    logLevel: 'info',
    onlyCategories: ['performance']
  })

  const html = result?.report?.[0] || ''
  const json = result?.report?.[1] || ''

  fs.writeFileSync(path.join(reportsDir, 'dashboard.report.html'), html)
  fs.writeFileSync(path.join(reportsDir, 'dashboard.lhr.json'), json)
} finally {
  await chrome.kill()
}
