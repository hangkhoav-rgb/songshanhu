<template>
  <ArtLayout force-navbar-scrolled>
    <div class="actions-container site-container">
      <div class="header glass-card">
        <div class="header-left">
          <h1 class="title">我的点赞/收藏</h1>
          <div class="muted subtitle">支持筛选、搜索、双视图与批量管理</div>
        </div>
        <div class="header-right">
          <el-button size="small" :loading="refreshing" @click="refresh">刷新</el-button>
          <el-segmented v-model="viewMode" size="small" :options="viewOptions" />
          <el-button size="small" :type="manageMode ? 'primary' : 'default'" @click="toggleManage">
            {{ manageMode ? '完成' : '管理' }}
          </el-button>
        </div>
      </div>

      <div class="toolbar glass-card">
        <el-input
          v-model="keyword"
          clearable
          size="default"
          placeholder="搜索标题/摘要..."
          class="search"
          @keyup.enter="refresh"
        />
        <el-select v-model="typeFilter" size="default" class="select" @change="refresh">
          <el-option label="全部" value="all" />
          <el-option label="文章" value="article" />
          <el-option label="视频" value="video" disabled />
          <el-option label="商品" value="product" disabled />
        </el-select>
        <el-select v-model="sortKey" size="default" class="select" @change="applyClientFilters">
          <el-option label="最新" value="latest" />
          <el-option label="最早" value="earliest" />
          <el-option label="热度" value="hot" />
        </el-select>
        <el-checkbox v-model="filterCollected" label="收藏" />
        <el-checkbox v-model="filterLiked" label="点赞" />

        <div v-if="manageMode" class="batch">
          <el-button size="small" @click="toggleSelectAll">{{ allSelected ? '取消全选' : '全选' }}</el-button>
          <el-button size="small" type="danger" :disabled="selectedIds.size === 0" :loading="batchDeleting" @click="batchDelete">
            删除({{ selectedIds.size }})
          </el-button>
        </div>
      </div>

      <div
        class="content glass-card"
        data-testid="actions-list"
        @touchstart.passive="onTouchStart"
        @touchmove.passive="onTouchMove"
        @touchend.passive="onTouchEnd"
      >
        <div v-if="pullDistance > 0" class="pull-indicator muted">
          {{ pullDistance >= PULL_TRIGGER ? '松开刷新' : '下拉刷新' }}
        </div>

        <el-alert v-if="errorMsg" type="error" :title="errorMsg" show-icon class="error">
          <template #default>
            <el-button size="small" @click="refresh">重试</el-button>
            <el-button size="small" @click="router.push('/')">去首页逛逛</el-button>
          </template>
        </el-alert>

        <el-empty v-else-if="!loading && displayItems.length === 0" description="暂无数据" class="empty-state">
          <template #image>
            <img class="empty-actions-illus" :src="emptyActionsSvg" alt="暂无数据" />
          </template>
          <el-button type="primary" @click="router.push('/')">去首页逛逛</el-button>
        </el-empty>

        <div v-else>
          <div v-if="viewMode === 'list'" class="list">
            <div
              v-for="it in displayItems"
              :key="it.id"
              class="row card-hover"
              role="button"
              tabindex="0"
              @click="onItemClick(it.id)"
              @keydown.enter="onItemClick(it.id)"
            >
              <div v-if="manageMode" class="sel" @click.stop>
                <el-checkbox :model-value="selectedIds.has(it.id)" @change="(v: any) => setSelected(it.id, !!v)" />
              </div>

              <div class="cover">
                <img
                  v-if="resolveCover(it)"
                  :src="resolveCover(it)"
                  :alt="it.title"
                  loading="lazy"
                  @error="() => onCoverError(it.id)"
                />
                <div v-else class="cover-ph" aria-label="封面占位" />
              </div>

              <div class="main">
                <div class="row-top">
                  <div class="row-title">{{ it.title }}</div>
                </div>

                <div class="row-meta muted">
                  <span>{{ it.authorName }}</span>
                  <span class="dot">·</span>
                  <span>{{ formatTime(it.actionTime) }}</span>
                </div>

                <div v-if="cleanArticleSummary(String(it.summary || ''))" class="row-summary">{{ cleanArticleSummary(String(it.summary || '')) }}</div>
              </div>

              <div class="actions" @click.stop>
                <button
                  type="button"
                  class="lf-btn lf-like"
                  :class="{ active: lfState(it.id).liked }"
                  :aria-pressed="lfState(it.id).liked"
                  @click="toggleLike(it.id)"
                >
                  <span class="lf-ico" aria-hidden="true"></span>
                  <span class="lf-text">点赞</span>
                  <span class="lf-count">{{ lfState(it.id).likes }}</span>
                </button>
                <button
                  type="button"
                  class="lf-btn lf-fav"
                  :class="{ active: lfState(it.id).collected }"
                  :aria-pressed="lfState(it.id).collected"
                  @click="toggleCollect(it.id)"
                >
                  <span class="lf-ico" aria-hidden="true"></span>
                  <span class="lf-text">收藏</span>
                  <span class="lf-count">{{ lfState(it.id).collects }}</span>
                </button>
              </div>
            </div>
          </div>

          <div v-else class="grid">
            <div v-for="it in displayItems" :key="it.id" class="grid-card card-hover" @click="onItemClick(it.id)">
              <div class="grid-cover">
                <img v-if="resolveCover(it)" :src="resolveCover(it)" :alt="it.title" loading="lazy" @error="() => onCoverError(it.id)" />
                <div v-else class="grid-cover-ph" aria-label="封面占位" />
              </div>
              <div class="grid-body">
                <div class="grid-title">{{ it.title }}</div>
                <div class="grid-meta muted">{{ formatTime(it.actionTime) }}</div>

                <div class="grid-actions" @click.stop>
                  <button
                    type="button"
                    class="lf-btn lf-like"
                    :class="{ active: lfState(it.id).liked }"
                    :aria-pressed="lfState(it.id).liked"
                    @click="toggleLike(it.id)"
                  >
                    <span class="lf-ico" aria-hidden="true"></span>
                    <span class="lf-count">{{ lfState(it.id).likes }}</span>
                  </button>
                  <button
                    type="button"
                    class="lf-btn lf-fav"
                    :class="{ active: lfState(it.id).collected }"
                    :aria-pressed="lfState(it.id).collected"
                    @click="toggleCollect(it.id)"
                  >
                    <span class="lf-ico" aria-hidden="true"></span>
                    <span class="lf-count">{{ lfState(it.id).collects }}</span>
                  </button>
                </div>
              </div>
              <div v-if="manageMode" class="grid-sel" @click.stop>
                <el-checkbox :model-value="selectedIds.has(it.id)" @change="(v: any) => setSelected(it.id, !!v)" />
              </div>
            </div>
          </div>

          <div ref="sentinelRef" class="sentinel"></div>
          <div class="load-more">
            <div v-if="loadingMore" class="muted">加载中...</div>
            <div v-else-if="!hasMore && displayItems.length > 0" class="muted">没有更多了</div>
          </div>
        </div>
      </div>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { articleApi, type ArticleActionItem } from '@/api/article'
import { likeApi } from '@/api/like'
import { favoriteApi } from '@/api/favorite'
import { track } from '@/utils/track'
import { actionsCacheKey } from '@/utils/actionsCache'
import { filterAndSortActions } from '@/utils/actionsFilter'
import emptyActionsSvg from '@/assets/empty-actions.svg'
import { normalizeCoverUrl } from '@/utils/image'
import { applyFavoriteToggleOptimistic, applyLikeToggleOptimistic, type LikeFavoriteState } from '@/utils/likeFavorite'
import { cleanArticleSummary } from '@/utils/articleSummary'

const router = useRouter()

const viewOptions = [
  { label: '列表', value: 'list' },
  { label: '宫格', value: 'grid' }
]
const viewMode = ref<'list' | 'grid'>('list')

const filterCollected = ref(true)
const filterLiked = ref(true)
const keyword = ref('')
const typeFilter = ref<'all' | 'article' | 'video' | 'product'>('all')
const sortKey = ref<'latest' | 'earliest' | 'hot'>('latest')

const manageMode = ref(false)
const selectedIds = ref<Set<number>>(new Set())
const batchDeleting = ref(false)

const brokenCoverIds = ref<Set<number>>(new Set())
const resolveCover = (it: ArticleActionItem) => {
  if (brokenCoverIds.value.has(it.id)) return ''
  return normalizeCoverUrl(it.coverImage)
}
const onCoverError = (id: number) => {
  brokenCoverIds.value = new Set(brokenCoverIds.value).add(id)
}

const current = ref(1)
const size = 10
const total = ref(0)

const items = ref<ArticleActionItem[]>([])
const displayItems = ref<ArticleActionItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const refreshing = ref(false)
const errorMsg = ref('')

const lfStateMap = ref<Map<number, LikeFavoriteState>>(new Map())

const lfState = (id: number): LikeFavoriteState => {
  return (
    lfStateMap.value.get(id) || {
      liked: false,
      likes: 0,
      collected: false,
      collects: 0
    }
  )
}

const hasMore = computed(() => items.value.length < total.value)
const cacheKey = computed(() =>
  actionsCacheKey(filterLiked.value, filterCollected.value) + `:${keyword.value.trim()}:${typeFilter.value}:${sortKey.value}`
)

const allSelected = computed(() => displayItems.value.length > 0 && displayItems.value.every((it) => selectedIds.value.has(it.id)))

const sentinelRef = ref<HTMLElement>()
let io: IntersectionObserver | null = null

const PULL_TRIGGER = 80
const pullDistance = ref(0)
let touchStartY = 0
let pulling = false

function formatTime(v: string) {
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return d.toLocaleString()
}

function onItemClick(id: number) {
  if (manageMode.value) {
    setSelected(id, !selectedIds.value.has(id))
    return
  }
  track('actions_item_click', { id })
  router.push(`/article/${id}`)
}

function setSelected(id: number, v: boolean) {
  const next = new Set(selectedIds.value)
  if (v) next.add(id)
  else next.delete(id)
  selectedIds.value = next
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = new Set()
    return
  }
  selectedIds.value = new Set(displayItems.value.map((it) => it.id))
}

function toggleManage() {
  manageMode.value = !manageMode.value
  if (!manageMode.value) {
    selectedIds.value = new Set()
  }
}

function applyClientFilters() {
  displayItems.value = filterAndSortActions(items.value, {
    keyword: keyword.value,
    type: typeFilter.value,
    sort: sortKey.value
  })
}

async function fetchPage(page: number) {
  if (!filterCollected.value && !filterLiked.value) {
    items.value = []
    displayItems.value = []
    total.value = 0
    return
  }

  errorMsg.value = ''
  const isFirst = page === 1
  if (isFirst) {
    loading.value = true
    const cached = localStorage.getItem(cacheKey.value)
    if (cached) {
      try {
        const parsed = JSON.parse(cached)
        if (Array.isArray(parsed?.items)) {
          items.value = parsed.items
          total.value = parsed.total || parsed.items.length
          applyClientFilters()
        }
      } catch {
      }
    }
  } else {
    loadingMore.value = true
  }

  try {
    const res = await articleApi.getMyActions({
      current: page,
      size,
      liked: filterLiked.value,
      collected: filterCollected.value
    })
    const next = res.records || []
    total.value = res.total || 0
    if (page === 1) items.value = next
    else items.value = [...items.value, ...next]
    current.value = page
    if (page === 1) {
      localStorage.setItem(cacheKey.value, JSON.stringify({ items: next, total: total.value, at: Date.now() }))
    }
    applyClientFilters()
    hydrateLfStates(next)
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败'
    if (!navigator.onLine) {
      ElMessage.warning('当前离线：已展示本地缓存（如有）')
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function hydrateLfStates(list: ArticleActionItem[]) {
  const ids = Array.from(new Set(list.map((it) => it.id)))
  if (ids.length === 0) return

  const nextMap = new Map(lfStateMap.value)

  ids.forEach((id) => {
    const it = list.find((x) => x.id === id)
    if (!it) return
    if (!nextMap.has(id)) {
      nextMap.set(id, {
        liked: Boolean(it.likedAt),
        likes: Number.isFinite(it.likes as any) ? (it.likes as any) : 0,
        collected: Boolean(it.collectedAt),
        collects: 0
      })
    }
  })

  lfStateMap.value = nextMap

  await Promise.all(
    ids.map(async (id) => {
      try {
        const [ls, cs] = await Promise.all([likeApi.status(id), favoriteApi.status(id)])
        const cur = lfState(id)
        const merged: LikeFavoriteState = {
          liked: Boolean(ls?.liked),
          likes: typeof ls?.likes === 'number' ? ls.likes : cur.likes,
          collected: Boolean(cs?.collected),
          collects: typeof cs?.collects === 'number' ? cs.collects : cur.collects
        }
        const m = new Map(lfStateMap.value)
        m.set(id, merged)
        lfStateMap.value = m
      } catch {
      }
    })
  )
}

function refresh() {
  refreshing.value = true
  current.value = 1
  fetchPage(1).finally(() => {
    refreshing.value = false
  })
}

function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  fetchPage(current.value + 1)
}

async function toggleLike(id: number) {
  const before = lfState(id)
  const optimistic = applyLikeToggleOptimistic(before)
  lfStateMap.value = new Map(lfStateMap.value).set(id, optimistic)

  const now = new Date().toISOString()
  items.value = items.value.map((it) => {
    if (it.id !== id) return it
    const next = { ...it }
    next.likedAt = optimistic.liked ? now : undefined
    next.likes = optimistic.likes
    next.actionTime = now
    return next
  })
  applyClientFilters()

  try {
    track(optimistic.liked ? 'actions_like' : 'actions_unlike', { id })
    const res = await likeApi.toggle(id)
    const merged: LikeFavoriteState = {
      ...lfState(id),
      liked: Boolean(res?.liked),
      likes: typeof res?.likes === 'number' ? res.likes : lfState(id).likes
    }
    lfStateMap.value = new Map(lfStateMap.value).set(id, merged)
    const now2 = new Date().toISOString()
    items.value = items.value.map((it) => {
      if (it.id !== id) return it
      const next = { ...it }
      next.likedAt = merged.liked ? now2 : undefined
      next.likes = merged.likes
      next.actionTime = now2
      return next
    })
    applyClientFilters()
  } catch (e: any) {
    lfStateMap.value = new Map(lfStateMap.value).set(id, before)
    items.value = items.value.map((it) => {
      if (it.id !== id) return it
      const next = { ...it }
      next.likedAt = before.liked ? now : undefined
      next.likes = before.likes
      return next
    })
    applyClientFilters()
    ElMessage.error(e?.message || '点赞失败')
  }
}

async function toggleCollect(id: number) {
  const before = lfState(id)
  const optimistic = applyFavoriteToggleOptimistic(before)
  lfStateMap.value = new Map(lfStateMap.value).set(id, optimistic)

  const now = new Date().toISOString()
  items.value = items.value.map((it) => {
    if (it.id !== id) return it
    const next = { ...it }
    next.collectedAt = optimistic.collected ? now : undefined
    next.actionTime = now
    return next
  })
  applyClientFilters()

  try {
    track(optimistic.collected ? 'actions_collect' : 'actions_uncollect', { id })
    const res = await favoriteApi.toggle(id)
    const merged: LikeFavoriteState = {
      ...lfState(id),
      collected: Boolean(res?.collected),
      collects: typeof res?.collects === 'number' ? res.collects : lfState(id).collects
    }
    lfStateMap.value = new Map(lfStateMap.value).set(id, merged)
    const now2 = new Date().toISOString()
    items.value = items.value.map((it) => {
      if (it.id !== id) return it
      const next = { ...it }
      next.collectedAt = merged.collected ? now2 : undefined
      next.actionTime = now2
      return next
    })
    applyClientFilters()
  } catch (e: any) {
    lfStateMap.value = new Map(lfStateMap.value).set(id, before)
    items.value = items.value.map((it) => {
      if (it.id !== id) return it
      const next = { ...it }
      next.collectedAt = before.collected ? now : undefined
      return next
    })
    applyClientFilters()
    ElMessage.error(e?.message || '收藏失败')
  }
}

async function batchDelete() {
  const selected = Array.from(selectedIds.value)
  if (selected.length === 0) return
  batchDeleting.value = true
  try {
    const likedIds = items.value.filter((it) => selectedIds.value.has(it.id) && it.likedAt).map((it) => it.id)
    const collectedIds = items.value.filter((it) => selectedIds.value.has(it.id) && it.collectedAt).map((it) => it.id)
    if (likedIds.length) await likeApi.batchDelete(likedIds)
    if (collectedIds.length) await favoriteApi.batchDelete(collectedIds)
    items.value = items.value.filter((it) => !selectedIds.value.has(it.id))
    selectedIds.value = new Set()
    applyClientFilters()
    ElMessage.success('已删除')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  } finally {
    batchDeleting.value = false
  }
}

watch([filterCollected, filterLiked], () => {
  track('actions_filter_change', { liked: filterLiked.value, collected: filterCollected.value })
  refresh()
})

watch([keyword, typeFilter], () => {
  applyClientFilters()
})

onMounted(() => {
  track('actions_page_view')
  fetchPage(1)
  io = new IntersectionObserver(
    (entries) => {
      if (!entries.some((e) => e.isIntersecting)) return
      loadMore()
    },
    { root: null, rootMargin: '200px 0px', threshold: 0 }
  )
  if (sentinelRef.value) io.observe(sentinelRef.value)
})

onUnmounted(() => {
  io?.disconnect()
  io = null
})

function onTouchStart(e: TouchEvent) {
  if (window.scrollY > 0) return
  touchStartY = e.touches[0]?.clientY || 0
  pulling = true
}

function onTouchMove(e: TouchEvent) {
  if (!pulling) return
  const y = e.touches[0]?.clientY || 0
  const dy = Math.max(0, y - touchStartY)
  pullDistance.value = Math.min(120, dy)
}

function onTouchEnd() {
  if (!pulling) return
  pulling = false
  if (pullDistance.value >= PULL_TRIGGER) {
    refresh()
  }
  pullDistance.value = 0
}
</script>

<style scoped lang="scss">
.actions-container {
  padding-top: 100px;
  padding-bottom: 70px;
  max-width: none;
  max-inline-size: clamp(64rem, 96vw, 110rem);
  min-inline-size: min(100%, 20rem);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  --like-primary: var(--ui-like, #ff6b6b);
  --collect-primary: var(--ui-collect, #4ecdc4);
  --ui-inactive: var(--ui-inactive-text, rgba(15, 23, 42, 0.55));
}

.header {
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.subtitle {
  margin-top: 6px;
  font-size: 13px;
}

.header-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.header-right :deep(.el-button) {
  min-block-size: 44px;
  padding: 0 8px;
  font-size: 14px;
  line-height: 1.6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.header-right :deep(.el-segmented) {
  min-block-size: 44px;
  border-radius: 999px;
}

.header-right :deep(.el-segmented__item) {
  min-block-size: 44px;
  font-size: 14px;
  padding-inline: 8px;
  border-radius: 999px;
}

.toolbar {
  margin-bottom: 16px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar :deep(.el-checkbox) {
  margin-right: 0;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: rgba(0, 82, 217, 0.06);
}

.toolbar :deep(.el-checkbox.is-checked) {
  border-color: rgba(0, 82, 217, 0.22);
  background: rgba(0, 82, 217, 0.10);
}

.toolbar :deep(.el-checkbox__label) {
  font-size: 13px;
}

.search {
  flex: 1;
  min-width: 240px;
}

.select {
  width: 140px;
}

.batch {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.content {
  padding: 14px;
  position: relative;
}

.empty-state {
  min-block-size: clamp(18rem, 40vh, 26rem);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.empty-actions-illus {
  inline-size: 12.5rem;
  block-size: 8.75rem;
}

.pull-indicator {
  position: sticky;
  top: 0;
  padding: 10px 0;
  text-align: center;
  font-size: 13px;
}

.error {
  margin-bottom: 14px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: grid;
  grid-template-columns: auto 128px minmax(0, 1fr) auto;
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.78);
  line-height: 1.5;
}

.sel {
  display: flex;
  align-items: flex-start;
  padding-top: 6px;
}

.row:focus-visible {
  outline: 2px solid rgba(0, 82, 217, 0.45);
  outline-offset: 2px;
}

.cover {
  width: 128px;
  height: 80px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-soft);
  background: rgba(0, 82, 217, 0.06);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-ph {
  width: 100%;
  height: 100%;
  background:
    radial-gradient(160px 80px at 20% 20%, rgba(105, 192, 255, 0.35), transparent 65%),
    radial-gradient(160px 80px at 80% 30%, rgba(255, 107, 107, 0.18), transparent 60%),
    linear-gradient(135deg, rgba(0, 82, 217, 0.18), rgba(105, 192, 255, 0.12));
}

.row-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.row-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  padding: 4px 10px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.10);
  background: rgba(0, 0, 0, 0.05);
  color: var(--ui-inactive);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-block-size: 32px;
}

.badge::before {
  content: '';
  inline-size: 24px;
  block-size: 24px;
  display: inline-block;
  background-color: currentColor;
  -webkit-mask: var(--badge-ico) no-repeat center / 24px 24px;
  mask: var(--badge-ico) no-repeat center / 24px 24px;
}

.badge-collect {
  --badge-ico: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27Z'/%3E%3C/svg%3E");
  color: var(--collect-primary);
  border-color: rgba(78, 205, 196, 0.35);
  background: rgba(78, 205, 196, 0.10);
}

.badge-like {
  --badge-ico: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M14.5 9V5.5c0-1.657-1.343-3-3-3L7 9v12h11.2c.858 0 1.6-.582 1.8-1.416l2-8A2 2 0 0 0 20.056 9H14.5Z'/%3E%3Cpath d='M7 9H4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3V9Z'/%3E%3C/svg%3E");
  color: var(--like-primary);
  border-color: rgba(255, 107, 107, 0.35);
  background: rgba(255, 107, 107, 0.10);
}

.row-meta {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.dot {
  margin: 0 8px;
}

.row-summary {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(15, 23, 42, 0.72);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  justify-content: flex-start;
}

.lf-btn {
  min-inline-size: 44px;
  min-block-size: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--divider);
  background: rgba(255, 255, 255, 0.04);
  color: var(--ui-inactive);
  font-size: 14px;
  line-height: 1.6;
  cursor: pointer;
  transform: translateZ(0) scale(1);
  transition: transform 150ms ease-out, background-color 150ms ease-out, border-color 150ms ease-out, color 150ms ease-out;
}

.lf-btn:active {
  transform: translateZ(0) scale(0.96);
}

.lf-btn:hover {
  background: rgba(255, 255, 255, 0.08);
}

.lf-ico {
  inline-size: 20px;
  block-size: 20px;
  background-color: currentColor;
  -webkit-mask: var(--lf-ico) no-repeat center / 20px 20px;
  mask: var(--lf-ico) no-repeat center / 20px 20px;
}

.lf-count {
  font-variant-numeric: tabular-nums;
}

.lf-like {
  --lf-ico: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M14.5 9V5.5c0-1.657-1.343-3-3-3L7 9v12h11.2c.858 0 1.6-.582 1.8-1.416l2-8A2 2 0 0 0 20.056 9H14.5Z'/%3E%3Cpath d='M7 9H4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3V9Z'/%3E%3C/svg%3E");
}

.lf-fav {
  --lf-ico: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27Z'/%3E%3C/svg%3E");
}

.lf-like.active {
  color: #ff4d4f;
  border-color: rgba(255, 77, 79, 0.55);
  background: rgba(255, 77, 79, 0.12);
}

.lf-fav.active {
  color: #fadb14;
  border-color: rgba(250, 219, 20, 0.55);
  background: rgba(250, 219, 20, 0.12);
}

.grid-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.grid-actions .lf-btn {
  padding: 0 10px;
}

@media (max-width: 375px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
  }
  .actions .lf-btn {
    inline-size: 100%;
    justify-content: flex-start;
  }
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.grid-card {
  position: relative;
  border-radius: 20px;
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.78);
  overflow: hidden;
  cursor: pointer;
}

.grid-cover {
  height: 140px;
  background: rgba(0, 82, 217, 0.06);
}

.grid-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.grid-cover-ph {
  width: 100%;
  height: 100%;
  background:
    radial-gradient(220px 140px at 30% 20%, rgba(105, 192, 255, 0.35), transparent 65%),
    radial-gradient(220px 140px at 70% 35%, rgba(255, 107, 107, 0.18), transparent 60%),
    linear-gradient(135deg, rgba(0, 82, 217, 0.18), rgba(105, 192, 255, 0.12));
}

.grid-body {
  padding: 12px;
}

.grid-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:deep(.el-button) {
  border-radius: 6px;
}

:deep(.el-button:not(.is-disabled):not(.el-button--primary):not(.el-button--danger):hover) {
  background-color: rgba(0, 82, 217, 0.1);
  border-color: rgba(0, 82, 217, 0.18);
}

.grid-meta {
  margin-top: 6px;
  font-size: 12px;
}

.grid-tags {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.grid-sel {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--border-soft);
  border-radius: 999px;
  padding: 4px 8px;
}

.sentinel {
  height: 1px;
}

.load-more {
  margin-top: 10px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .actions-container {
    padding-left: 16px;
    padding-right: 16px;
  }
  .row {
    grid-template-columns: 1fr;
  }
  .cover {
    width: 100%;
    height: 170px;
  }
  .actions {
    justify-content: flex-start;
  }
  .grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

@media (max-width: 992px) and (min-width: 769px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
