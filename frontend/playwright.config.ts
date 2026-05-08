import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './scripts',
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:3000',
    video: 'on',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry'
  }
})

