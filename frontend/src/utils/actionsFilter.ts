import type { ArticleActionItem } from '@/api/article'

export type ActionsSortKey = 'latest' | 'earliest' | 'hot'

export interface ActionsFilterOptions {
  keyword: string
  type: 'all' | 'article' | 'video' | 'product'
  sort: ActionsSortKey
}

/**
 * 对“我的点赞/收藏”列表进行前端筛选与排序。
 * - type 非 article 时直接返回空（当前仅支持文章）
 * - keyword 匹配 title/summary（大小写不敏感）
 * - sort 支持 latest/earliest/hot
 */
export function filterAndSortActions(items: ArticleActionItem[], opt: ActionsFilterOptions) {
  const kw = opt.keyword.trim().toLowerCase()
  if (opt.type !== 'all' && opt.type !== 'article') return []

  let next = items
  if (kw) {
    next = next.filter(
      (it) =>
        (it.title || '').toLowerCase().includes(kw) ||
        (it.summary || '').toLowerCase().includes(kw)
    )
  }

  const arr = [...next]
  if (opt.sort === 'earliest') {
    arr.sort((a, b) => new Date(a.actionTime).getTime() - new Date(b.actionTime).getTime())
  } else if (opt.sort === 'hot') {
    arr.sort((a, b) => (b.likes || 0) - (a.likes || 0))
  } else {
    arr.sort((a, b) => new Date(b.actionTime).getTime() - new Date(a.actionTime).getTime())
  }
  return arr
}

