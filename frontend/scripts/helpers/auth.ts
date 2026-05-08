import type { Page } from '@playwright/test'

export async function login(page: Page) {
  const user = process.env.E2E_USER
  const pass = process.env.E2E_PASS || '123456'
  const newPass = process.env.E2E_NEW_PASS || '123456'

  if (!user) {
    await page.goto('/')
    const uniq = `${Date.now()}_${Math.floor(Math.random() * 1_000_000_000)}`
    const uname = `e2e_${uniq}`
    const r = await page.request.post('/api/auth/register', {
      data: {
        username: uname,
        password: newPass,
        nickname: uname,
        email: `${uname}@example.com`
      }
    })
    const j: any = await r.json()
    if (j?.code !== 200 || !j?.data?.token) {
      throw new Error(`Register failed: code=${j?.code} msg=${j?.message}`)
    }
    await page.evaluate(
      ([t, u]) => {
        localStorage.setItem('token', t as string)
        localStorage.setItem('user', JSON.stringify(u))
      },
      [j?.data?.token, j?.data?.user]
    )
    await page.reload()
    return
  }

  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名或邮箱').fill(user)
  await page.getByPlaceholder('请输入密码').fill(pass)
  const respPromise = page.waitForResponse((r) => r.url().includes('/api/auth/login') && r.request().method() === 'POST')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  const resp = await respPromise
  const json: any = await resp.json()
  if (json?.code !== 200) {
    throw new Error(`Login failed: code=${json?.code} msg=${json?.message}`)
  }
  const data = json?.data
  let token: string | undefined = data?.token
  let userObj: any = data?.user

  if (!token && data?.mustResetPassword && data?.resetToken) {
    const r = await page.request.post('/api/auth/first-reset', {
      data: { resetToken: data.resetToken, newPassword: newPass }
    })
    if (!r.ok()) {
      throw new Error(`First reset failed: ${r.status()}`)
    }
    const j2: any = await r.json()
    if (j2?.code !== 200) {
      throw new Error(`First reset failed: code=${j2?.code} msg=${j2?.message}`)
    }
    token = j2?.data?.token
    userObj = j2?.data?.user
  }

  if (token) {
    await page.evaluate(
      ([t, u]) => {
        localStorage.setItem('token', t as string)
        localStorage.setItem('user', JSON.stringify(u))
      },
      [token, userObj]
    )
    await page.reload()
  }

  await page.waitForTimeout(300)
  if (page.url().includes('/login')) {
    await page.waitForTimeout(700)
  }

  if (page.url().includes('/first-reset')) {
    await page.getByPlaceholder('请输入新密码').fill(newPass)
    await page.getByPlaceholder('请再次输入新密码').fill(newPass)
    await page.getByRole('button', { name: '保存并登录' }).click()
    await page.waitForResponse((r) => r.url().includes('/api/auth/first-reset') && r.request().method() === 'POST')
    await page.waitForTimeout(300)
  }
}
