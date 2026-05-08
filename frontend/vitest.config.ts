import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  test: {
    environment: 'jsdom',
    include: ['tests/**/*.{test,spec}.ts'],
    exclude: ['**/node_modules/**', '**/dist/**', 'scripts/**'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      reportsDirectory: './coverage',
      include: [
        'src/utils/likeFavorite.ts',
        'src/api/like.ts',
        'src/api/favorite.ts'
      ],
      exclude: [
        'src/utils/amap.ts',
        'src/utils/location.ts',
        'src/utils/track.ts',
        'src/utils/request.ts'
      ],
      thresholds: {
        lines: 90,
        functions: 90,
        branches: 90,
        statements: 90
      }
    }
  }
})
