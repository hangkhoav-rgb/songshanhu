export function normalizeCoverUrl(url?: string | null): string {
  if (!url) return ''
  if (url.startsWith('/')) return url
  if (url.startsWith('data:')) return url
  if (!url.startsWith('http://') && !url.startsWith('https://')) return url
  try {
    const u = new URL(url)
    const host = u.host
    if (host === 'images.unsplash.com') {
      const seed = encodeURIComponent(u.pathname.replace(/\//g, '_').slice(0, 64) || 'unsplash')
      return `https://picsum.photos/seed/${seed}/800/450`
    }
    return url
  } catch {
    return url
  }
}
