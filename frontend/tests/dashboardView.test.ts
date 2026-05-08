import { describe, it, expect, vi, beforeEach } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/dashboard', () => ({
  dashboardApi: {
    init: vi.fn().mockResolvedValue({
      stats: {
        totalViews: 0,
        totalLikes: 0,
        totalComments: 0,
        totalArticles: 0,
        dailyTrend: [],
        categoryDistribution: [],
        locationPoints: []
      },
      summary: { likeCount: 0, collectCount: 0, lastActionTime: null },
      actions: { records: [], total: 0, size: 10, current: 1, pages: 0 }
    }),
    actions: vi.fn(),
    summary: vi.fn()
  }
}))

const poller = { start: vi.fn(), stop: vi.fn(), runNow: vi.fn() }
vi.mock('@/utils/poller', () => ({
  createPoller: () => poller
}))

vi.mock('echarts', () => ({
  init: () => ({ setOption: vi.fn(), resize: vi.fn(), dispose: vi.fn() }),
  graphic: { LinearGradient: function () {} }
}))

vi.mock('@/utils/amap', () => ({
  loadAMap: () => Promise.reject(new Error('no amap'))
}))

vi.mock('@/api/article', () => ({
  articleApi: { getMine: vi.fn().mockResolvedValue({ records: [] }) }
}))

vi.mock('vue-grid-layout-v3', () => ({
  GridLayout: {},
  GridItem: {}
}))

describe('Dashboard view', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    poller.start.mockClear()
    poller.stop.mockClear()
  })

  it('calls init and starts poller', async () => {
    const Dashboard = (await import('../src/views/dashboard/Index.vue')).default
    const wrapper = shallowMount(Dashboard, {
      global: {
        plugins: [createPinia()],
        stubs: {
          ArtLayout: { template: '<div><slot /></div>' },
          GridLayout: { template: '<div><slot /></div>' },
          GridItem: { template: '<div><slot /></div>' },
          'el-button': { template: '<button><slot /></button>' },
          'el-empty': { template: '<div><slot /></div>' },
          'el-skeleton': { template: '<div />' },
          'el-icon': { template: '<i />' },
          'el-radio-button': { template: '<div />' },
          'el-radio-group': { template: '<div><slot /></div>' },
          'el-select': { template: '<div />' },
          'el-option': { template: '<div />' },
          'el-table': { template: '<div />' },
          'el-table-column': { template: '<div />' },
          'el-tag': { template: '<div><slot /></div>' },
          'el-link': { template: '<a><slot /></a>' },
          'el-pagination': { template: '<div />' },
          MapLocation: { template: '<div />' }
        },
        directives: {
          loading: () => {}
        }
      }
    })

    await flushPromises()
    await new Promise((r) => setTimeout(r, 0))
    const { dashboardApi } = await import('@/api/dashboard')
    expect(dashboardApi.init).toHaveBeenCalled()
    expect(poller.start).toHaveBeenCalledTimes(1)
    wrapper.unmount()
    expect(poller.stop).toHaveBeenCalledTimes(1)
  })
})
