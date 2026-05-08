export function actionsCacheKey(liked: boolean, collected: boolean) {
  return `ssl_actions_cache_v1_${liked ? 1 : 0}_${collected ? 1 : 0}`
}

