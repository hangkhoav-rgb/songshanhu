import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'

describe('LikeCollectBar', () => {
  it('renders counts and reflects pressed state', async () => {
    const Comp = (await import('@/components/Interaction/LikeCollectBar.vue')).default
    const wrapper = mount(Comp, {
      props: { liked: true, collected: false, likes: 12, collects: 3 }
    })
    const like = wrapper.get('[data-testid="like-btn"]')
    const collect = wrapper.get('[data-testid="collect-btn"]')
    expect(like.attributes('aria-pressed')).toBe('true')
    expect(collect.attributes('aria-pressed')).toBe('false')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('3')
  })

  it('emits toggle events on click', async () => {
    const Comp = (await import('@/components/Interaction/LikeCollectBar.vue')).default
    const wrapper = mount(Comp, {
      props: { liked: false, collected: false, likes: 0, collects: 0 }
    })
    await wrapper.get('[data-testid="like-btn"]').trigger('click')
    await wrapper.get('[data-testid="collect-btn"]').trigger('click')
    expect(wrapper.emitted('toggle-like')?.length).toBe(1)
    expect(wrapper.emitted('toggle-collect')?.length).toBe(1)
  })

  it('disables interaction when disabled', async () => {
    const Comp = (await import('@/components/Interaction/LikeCollectBar.vue')).default
    const wrapper = mount(Comp, {
      props: { liked: false, collected: false, likes: 0, collects: 0, disabled: true }
    })
    await wrapper.get('[data-testid="like-btn"]').trigger('click')
    expect(wrapper.emitted('toggle-like')).toBeUndefined()
  })
})

