import { describe, it, expect } from 'vitest'
import { filterAndSortActions } from '../src/utils/actionsFilter'

const base = [
  { id: 1, title: 'Hello', summary: 'World', actionTime: '2026-04-01T10:00:00', likes: 2 },
  { id: 2, title: 'Vue', summary: 'Dashboard', actionTime: '2026-04-02T10:00:00', likes: 10 },
  { id: 3, title: 'Other', summary: 'note', actionTime: '2026-03-30T10:00:00', likes: 1 }
] as any

describe('filterAndSortActions', () => {
  it('filters by keyword', () => {
    const out = filterAndSortActions(base, { keyword: 'dash', type: 'all', sort: 'latest' })
    expect(out.map((x) => x.id)).toEqual([2])
  })

  it('sorts by earliest', () => {
    const out = filterAndSortActions(base, { keyword: '', type: 'all', sort: 'earliest' })
    expect(out.map((x) => x.id)).toEqual([3, 1, 2])
  })

  it('returns empty for unsupported type', () => {
    const out = filterAndSortActions(base, { keyword: '', type: 'video', sort: 'latest' })
    expect(out).toEqual([])
  })
})

