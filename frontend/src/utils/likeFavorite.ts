export type LikeFavoriteState = {
  liked: boolean
  likes: number
  collected: boolean
  collects: number
}

export function clampNonNegative(n: number) {
  if (!Number.isFinite(n)) return 0
  return Math.max(0, Math.trunc(n))
}

export function applyLikeToggleOptimistic(prev: LikeFavoriteState): LikeFavoriteState {
  const liked = !prev.liked
  const likes = clampNonNegative(prev.likes + (liked ? 1 : -1))
  return { ...prev, liked, likes }
}

export function applyFavoriteToggleOptimistic(prev: LikeFavoriteState): LikeFavoriteState {
  const collected = !prev.collected
  const collects = clampNonNegative(prev.collects + (collected ? 1 : -1))
  return { ...prev, collected, collects }
}

