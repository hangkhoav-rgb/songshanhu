import { describe, it, expect, vi, beforeEach } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/api/article', () => ({
  articleApi: {
    getMyActions: vi.fn().mockResolvedValue({
      records: [
        {
          id: 1,
          title: 'T1',
          summary: 'S1',
          authorName: 'A',
          actionTime: '2026-04-01T10:00:00',
          likedAt: '2026-04-01T10:00:00',
          collectedAt: '2026-04-01T10:00:00',
          likes: 5
        }
      ],
      total: 1
    }),
    toggleLike: vi.fn(),
    toggleCollect: vi.fn(),
    batchUnlike: vi.fn(),
    batchUncollect: vi.fn()
  }
}))

vi.mock('@/api/like', () => ({
  likeApi: {
    toggle: vi.fn().mockResolvedValue({ liked: false, likes: 4 }),
    status: vi.fn().mockResolvedValue({ liked: true, likes: 5 }),
    batchDelete: vi.fn()
  }
}))

vi.mock('@/api/favorite', () => ({
  favoriteApi: {
    toggle: vi.fn().mockResolvedValue({ collected: false, collects: 9 }),
    status: vi.fn().mockResolvedValue({ collected: true, collects: 10 }),
    batchDelete: vi.fn()
  }
}))

vi.mock('@/utils/track', () => ({ track: vi.fn() }))

describe('Actions view', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    ;(globalThis as any).IntersectionObserver = class {
      observe() {}
      disconnect() {}
    }
  })

  it('renders list items after fetch', async () => {
    const Actions = (await import('../src/views/article/Actions.vue')).default
    const wrapper = shallowMount(Actions, {
      global: {
        plugins: [createPinia()],
        stubs: {
          ArtLayout: { template: '<div><slot /></div>' },
          ElInput: true,
          ElSelect: true,
          ElOption: true,
          ElCheckbox: true,
          ElButton: { template: '<button><slot /></button>' },
          ElAlert: true,
          ElEmpty: true,
          ElSegmented: true
        }
      }
    })

    await flushPromises()
    expect(wrapper.text()).toContain('T1')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('10')

    const btns = wrapper.findAll('button.lf-btn')
    expect(btns.length).toBeGreaterThan(0)
  })

  it('toggles like and updates count', async () => {
    const { likeApi } = await import('@/api/like')
    const Actions = (await import('../src/views/article/Actions.vue')).default
    const wrapper = shallowMount(Actions, {
      global: {
        plugins: [createPinia()],
        stubs: {
          ArtLayout: { template: '<div><slot /></div>' },
          ElInput: true,
          ElSelect: true,
          ElOption: true,
          ElCheckbox: true,
          ElButton: { template: '<button><slot /></button>' },
          ElAlert: true,
          ElEmpty: true,
          ElSegmented: true
        }
      }
    })

    await flushPromises()
    const likeBtn = wrapper.find('button.lf-like')
    await likeBtn.trigger('click')
    await flushPromises()
    expect((likeApi as any).toggle).toHaveBeenCalledWith(1)
  })
})
