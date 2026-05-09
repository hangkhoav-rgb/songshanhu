<template>
  <ArtLayout force-navbar-scrolled>
    <div class="article-detail-wrapper">
      <div class="site-container detail-container">
        <div class="detail-top">
          <el-result v-if="errorMessage" icon="error" title="文章加载失败" :sub-title="errorMessage">
            <template #extra>
              <el-button type="primary" @click="fetchArticleDetail">重试</el-button>
              <el-button @click="router.back()">返回</el-button>
            </template>
          </el-result>

          <div v-else class="detail-shell">
            <nav v-if="tocItems.length" class="toc-float">
              <div class="toc-title">目录</div>
              <button
                v-for="item in tocItems"
                :key="item.id"
                class="toc-item"
                :class="{ active: activeTocId === item.id }"
                :style="{ paddingLeft: `${10 + (item.level - 1) * 12}px` }"
                type="button"
                @click="scrollToHeading(item.id)"
              >
                {{ item.text }}
              </button>
            </nav>

            <div class="content-grid">
              <main class="main detail-main">
              <article v-if="article.title" class="article-content glass-card" v-loading="loading">
                <header class="article-header">
                  <h1 class="title">{{ article.title }}</h1>
                  <div class="meta">
                    <span class="author">
                      <el-avatar :size="24">{{ (article.authorName || 'U')[0] }}</el-avatar>
                      {{ article.authorName }}
                    </span>
                    <span class="dot">·</span>
                    <span class="time muted">{{ article.createTime }}</span>
                    <span class="dot">·</span>
                    <span class="views muted"><el-icon><View /></el-icon> {{ article.views }}</span>
                  </div>

                  <div class="actions" @click.stop>
                    <button
                      type="button"
                      class="action-circle action-circle--like"
                      :class="{ active: liked }"
                      :disabled="likeSubmitting"
                      :aria-pressed="liked"
                      aria-label="点赞"
                      @click="toggleLike"
                    >
                      <span class="action-circle__ico" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path class="icon-path" d="M14.5 9V5.5c0-1.657-1.343-3-3-3L7 9v12h11.2c.858 0 1.6-.582 1.8-1.416l2-8A2 2 0 0 0 20.056 9H14.5Z" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                          <path class="icon-path" d="M7 9H4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3V9Z" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                        </svg>
                      </span>
                      <span class="action-circle__count">{{ likesCount }}</span>
                      <span class="action-circle__ripple" aria-hidden="true"></span>
                    </button>
                    <button
                      type="button"
                      class="action-circle action-circle--collect"
                      :class="{ active: collected }"
                      :disabled="collectSubmitting"
                      :aria-pressed="collected"
                      aria-label="收藏"
                      @click="toggleCollect"
                    >
                      <span class="action-circle__ico" aria-hidden="true">
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path class="icon-path" d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27Z" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                        </svg>
                      </span>
                      <span class="action-circle__count">{{ collectsCount }}</span>
                      <span class="action-circle__ripple" aria-hidden="true"></span>
                    </button>
                  </div>
                </header>

                <div v-if="normalizeCoverUrl(article.coverImage as any)" class="cover">
                  <img
                    :src="normalizeCoverUrl(article.coverImage as any)"
                    class="cover-img"
                    alt="cover"
                    loading="lazy"
                    decoding="async"
                    @error="onImgError($event, article.coverImage!)"
                  />
                </div>

                <div ref="contentRef" class="content-body" v-html="sanitizedContent"></div>
              </article>
              <el-skeleton v-else animated class="glass-card" :rows="8" />

              <section class="comment-section glass-card" v-loading="commentsLoading">
                <div class="comment-head">
                  <div class="section-title"><span>评论</span></div>
                  <div class="muted">{{ comments.length }} 条</div>
                </div>

                <div class="comment-input-area">
                  <template v-if="userStore.isLoggedIn">
                    <div class="input-wrapper">
                      <el-input
                        v-model="commentText"
                        type="textarea"
                        :rows="3"
                        placeholder="分享你的见解..."
                        maxlength="500"
                        show-word-limit
                      />
                      <div class="input-footer">
                        <el-button type="primary" :loading="submitting" @click="submitComment">发表评论</el-button>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <div class="login-prompt">
                      <p class="muted">登录后参与讨论，分享你的精彩观点</p>
                      <el-button type="primary" plain round @click="router.push('/login')">立即登录</el-button>
                    </div>
                  </template>
                </div>

                <div class="comment-list">
                  <el-alert
                    v-if="commentsErrorMessage"
                    type="error"
                    show-icon
                    :closable="false"
                    :title="commentsErrorMessage"
                    class="comment-error"
                  />
                  <div v-for="comment in comments" :key="comment.id" class="comment-item">
                    <el-avatar :size="40" :src="comment.userAvatar" />
                    <div class="comment-content">
                      <div class="comment-user">
                        <span class="name">{{ comment.username }}</span>
                        <span class="date muted">{{ comment.createTime }}</span>
                      </div>
                      <p class="text">{{ comment.content }}</p>
                    </div>
                  </div>
                  <el-empty v-if="comments.length === 0" description="暂无评论" />
                </div>
              </section>
              </main>

            <aside class="sidebar detail-side">
              <div class="detail-side-inner">
                <ArticleMap
                  v-if="article.longitude && article.latitude"
                  :lng="article.longitude"
                  :lat="article.latitude"
                  :title="article.title"
                  :height="300"
                  :radius="12"
                />

                <AISummary
                  v-if="article.title"
                  :title="String(article.title)"
                  :content="articlePlainText"
                />

                <div class="glass-card side-card" v-if="relatedArticles.length">
                  <div class="side-title section-title"><span>相关推荐</span></div>
                  <div class="related-list">
                    <div
                      v-for="a in relatedArticles"
                      :key="a.id"
                      class="related-item"
                      @click="router.push(`/article/${a.id}`)"
                    >
                      <div class="related-title">{{ a.title }}</div>
                      <div class="related-meta muted">
                        {{ getCategoryLabel(a.category) }} · {{ a.authorName }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </aside>
            </div>
          </div>
        </div>
      </div>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import ArticleMap from '@/components/Map/ArticleMap.vue'
import AISummary from '@/components/AI/AISummary.vue'
import { articleApi } from '@/api/article'
import { commentApi, type Comment } from '@/api/comment'
import type { Article } from '@/types'
import * as echarts from 'echarts'
import DOMPurify from 'dompurify'
import { View } from '@element-plus/icons-vue'
import { normalizeCoverUrl } from '@/utils/image'
import { buildChartOption, normalizeChartConfig } from '@/utils/articleChart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const contentRef = ref<HTMLElement>()
const loading = ref(false)
const errorMessage = ref('')
const commentsLoading = ref(false)
const commentsErrorMessage = ref('')

const commentText = ref('')
const submitting = ref(false)

// 响应式文章数据
const article = ref<Partial<Article>>({})
const comments = ref<Comment[]>([])
const relatedArticles = ref<Article[]>([])

type TocItem = { id: string; text: string; level: 1 | 2 | 3 }
const tocItems = ref<TocItem[]>([])
const activeTocId = ref('')
let headingEls: HTMLElement[] = []

const articlePlainText = computed(() => {
  const html = String((article.value as any)?.content || '')
  if (!html) return ''
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return (doc.body.textContent || '').replace(/\s+/g, ' ').trim()
})

const sanitizedContent = computed(() => {
  return DOMPurify.sanitize((article.value?.content as string) || '', {
    ADD_ATTR: ['data-config', 'data-chart-id']
  })
})

const chartInstances = ref<echarts.ECharts[]>([])

const liked = ref(false)
const collected = ref(false)
const likesCount = ref(0)
const collectsCount = ref(0)
const likeSubmitting = ref(false)
const collectSubmitting = ref(false)

const fetchArticleDetail = async () => {
  const id = route.params.id as string
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await articleApi.getDetail(id)
    article.value = res as any
    likesCount.value = (res as any)?.likes || 0
    await fetchInteractions(id)
    await fetchComments(id)
    await fetchRelatedArticles(id)
    nextTick(() => {
      renderEmbeddedCharts()
      buildToc()
    })
  } catch (error) {
    console.error('获取文章详情失败:', error)
    errorMessage.value = (error as any)?.message || '请检查网络或稍后重试'
  } finally {
    loading.value = false
  }
}

const fetchRelatedArticles = async (id: string | number) => {
  try {
    const res = await articleApi.getList({
      current: 1,
      size: 8,
      category: (article.value as any)?.category || 'all'
    })
    relatedArticles.value = (res?.records || []).filter((a) => String(a.id) !== String(id)).slice(0, 6)
  } catch {
    relatedArticles.value = []
  }
}

const getCategoryLabel = (c?: string) => {
  if (c === 'tech') return '技术'
  if (c === 'life') return '生态'
  if (c === 'visual') return '可视化'
  return '人文'
}

const buildToc = () => {
  const root = contentRef.value
  if (!root) {
    tocItems.value = []
    headingEls = []
    return
  }
  const hs = Array.from(root.querySelectorAll('h1, h2, h3')) as HTMLElement[]
  const items: TocItem[] = []
  hs.forEach((h, idx) => {
    const level = Number(h.tagName.replace('H', '')) as 1 | 2 | 3
    if (![1, 2, 3].includes(level)) return
    if (!h.id) {
      h.id = `h-${idx}`
    }
    const text = (h.textContent || '').trim()
    if (!text) return
    items.push({ id: h.id, text, level })
  })
  tocItems.value = items
  headingEls = hs
  requestAnimationFrame(updateActiveToc)
}

const updateActiveToc = () => {
  if (!headingEls.length) return
  const top = 110
  let current = headingEls[0]
  for (const h of headingEls) {
    const rect = h.getBoundingClientRect()
    if (rect.top - top <= 0) {
      current = h
    } else {
      break
    }
  }
  activeTocId.value = current?.id || ''
}

const scrollToHeading = (id: string) => {
  const el = document.getElementById(id)
  if (!el) return
  const y = el.getBoundingClientRect().top + window.scrollY - 96
  window.scrollTo({ top: y, behavior: 'smooth' })
}

const fetchInteractions = async (id: string | number) => {
  try {
    const [ls, cs] = await Promise.all([
      articleApi.getLikeStatus(id),
      articleApi.getCollectStatus(id)
    ])
    liked.value = Boolean(ls.liked)
    likesCount.value = typeof ls.likes === 'number' ? ls.likes : likesCount.value
    collected.value = Boolean(cs.collected)
    collectsCount.value = typeof cs.collects === 'number' ? cs.collects : 0
  } catch {
  }
}

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (likeSubmitting.value) return
  likeSubmitting.value = true
  try {
    const res = await articleApi.toggleLike(route.params.id as string)
    liked.value = Boolean(res.liked)
    likesCount.value = res.likes
  } catch (e: any) {
    ElMessage.error(e?.message || '点赞失败')
  } finally {
    likeSubmitting.value = false
  }
}

const toggleCollect = async () => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (collectSubmitting.value) return
  collectSubmitting.value = true
  try {
    const res = await articleApi.toggleCollect(route.params.id as string)
    collected.value = Boolean(res.collected)
    collectsCount.value = res.collects
  } catch (e: any) {
    ElMessage.error(e?.message || '收藏失败')
  } finally {
    collectSubmitting.value = false
  }
}

const fetchComments = async (articleId: string | number) => {
  try {
    commentsLoading.value = true
    commentsErrorMessage.value = ''
    const res = await commentApi.getList(articleId)
    comments.value = res
  } catch (error) {
    console.error('获取评论失败:', error)
    commentsErrorMessage.value = '评论加载失败，请稍后重试'
  } finally {
    commentsLoading.value = false
  }
}

const onImgError = (e: Event, url: string) => {
  const target = e.target as HTMLImageElement
  target.src = '/favicon.svg'
  import('@/api/monitor').then(({ monitorApi }) => {
    monitorApi.reportImageError({
      url,
      page: location.pathname,
      userAgent: navigator.userAgent,
      referrer: document.referrer,
      message: 'cover image load failed'
    })
  })
}

const renderEmbeddedCharts = () => {
  if (!contentRef.value) return

  chartInstances.value.forEach(c => c.dispose())
  chartInstances.value = []

  const containers = contentRef.value.querySelectorAll('.embedded-chart-container')
  containers.forEach((el: any) => {
    const configStr = el.getAttribute('data-config')
    if (configStr) {
      try {
        const config = normalizeChartConfig(JSON.parse(configStr))
        const target = el.querySelector('.embedded-chart-canvas') || el
        el.style.margin = '30px 0'
        target.style.height = '350px'
        target.style.width = '100%'
        const tryInit = (triesLeft: number) => {
          if (target.clientWidth > 0 && target.clientHeight > 0) {
            const chartInstance = echarts.init(target)
            chartInstances.value.push(chartInstance)
            chartInstance.setOption(buildChartOption(config), true)
            return
          }
          if (triesLeft <= 0) return
          requestAnimationFrame(() => tryInit(triesLeft - 1))
        }
        tryInit(6)
      } catch (e) {
        console.error('解析图表配置失败:', e)
      }
    }
  })

  const imgs = contentRef.value.querySelectorAll('img')
  imgs.forEach((img) => {
    const el = img as HTMLImageElement
    el.loading = 'lazy'
    el.decoding = 'async'
    if ((el as any).__monitored) return
    ;(el as any).__monitored = true
    el.addEventListener('error', () => {
      const url = el.currentSrc || el.src
      el.src = '/favicon.svg'
      import('@/api/monitor').then(({ monitorApi }) => {
        monitorApi.reportImageError({
          url,
          page: location.pathname,
          userAgent: navigator.userAgent,
          referrer: document.referrer,
          message: 'content image load failed'
        })
      })
    })
  })
}

const resizeEmbeddedCharts = () => {
  chartInstances.value.forEach((chart) => chart.resize())
}



async function submitComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  submitting.value = true
  try {
    const res = await commentApi.add({
      articleId: Number(route.params.id),
      content: commentText.value
    })
    comments.value.unshift(res)
    commentText.value = ''
    ElMessage.success('评论发表成功')
  } catch (error) {
    console.error('发表评论失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchArticleDetail()
  window.addEventListener('scroll', updateActiveToc, { passive: true })
  window.addEventListener('resize', resizeEmbeddedCharts, { passive: true })
})

onUnmounted(() => {
  chartInstances.value.forEach(c => c.dispose())
  chartInstances.value = []
  window.removeEventListener('scroll', updateActiveToc)
  window.removeEventListener('resize', resizeEmbeddedCharts)
})
</script>

<style scoped lang="scss">
.article-detail-wrapper {
  min-height: 100vh;
  padding-top: 0;
  padding-bottom: 70px;
}

.detail-container {
  max-width: 1640px;
}

.detail-shell {
  position: relative;
}

.detail-top {
  padding-top: 110px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 32px;
  align-items: start;
}

.detail-main {
  min-height: 100vh;
  padding-left: 260px;
}

.toc-float {
  position: fixed;
  left: 24px;
  top: 110px;
  width: 220px;
  height: calc(100vh - 140px);
  overflow: auto;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--border);
  backdrop-filter: blur(12px);
  z-index: 3;
}

.toc-title {
  font-size: 12px;
  color: #64748b;
  font-weight: 700;
  margin-bottom: 10px;
}

.toc-item {
  width: 100%;
  text-align: left;
  border: 0;
  background: transparent;
  padding: 8px 10px;
  font-size: 13px;
  color: #0f172a;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.3s var(--ease), color 0.3s var(--ease);
}

.toc-item:hover {
  background: rgba(14, 165, 233, 0.10);
}

.toc-item.active {
  background: rgba(14, 165, 233, 0.16);
  color: #0369a1;
}

.detail-side {
  min-width: 0;
}

.detail-side-inner {
  position: sticky;
  top: 110px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.related-item {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(2, 132, 199, 0.12);
  background: rgba(2, 132, 199, 0.04);
  cursor: pointer;
  transition: transform 0.3s var(--ease), box-shadow 0.3s var(--ease), background 0.3s var(--ease);
}

.related-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(2, 132, 199, 0.16);
  background: rgba(2, 132, 199, 0.06);
}

.related-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-meta {
  margin-top: 6px;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .detail-main {
    padding-left: 0;
  }
  .toc-float {
    display: none;
  }
  .content-grid {
    grid-template-columns: 1fr 300px;
  }
}

@media (max-width: 992px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .detail-side-inner {
    position: static;
  }
}

.article-content {
  padding: 28px 32px;
  margin-bottom: 14px;
  max-width: 860px;
}

.article-header {
  margin-bottom: 16px;
  .title {
    font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    font-size: 32px;
    line-height: 1.4;
    font-weight: 500;
    color: var(--text);
    margin-bottom: 12px;
    letter-spacing: -0.2px;
  }
  .meta {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--muted);
    font-size: 14px;
    .author { display: flex; align-items: center; gap: 8px; color: var(--text); font-weight: 500; }
  }
  .actions {
    margin-top: 14px;
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    align-items: center;
  }
}

.action-circle {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  inline-size: 64px;
  min-block-size: 64px;
  padding: 0 8px;
  border-radius: 50%;
  border: none;
  background: transparent;
  cursor: pointer;
  transform: translateZ(0) scale(1);
  transition:
    transform 280ms cubic-bezier(0.4, 0, 0.2, 1),
    background 280ms ease,
    border-color 280ms ease,
    box-shadow 280ms ease;
  overflow: hidden;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  &:focus-visible {
    outline: 2px solid var(--focus-ring);
    outline-offset: 2px;
  }

  &:active:not(:disabled) {
    transform: translateZ(0) scale(0.94);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.action-circle__ico {
  inline-size: 22px;
  block-size: 22px;
  display: grid;
  place-items: center;
  color: var(--muted);
  transition: color 200ms ease;
  svg { inline-size: 22px; block-size: 22px; }
}

.action-circle__count {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  transition: color 200ms ease;
}

.action-circle__ripple {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  pointer-events: none;
}

/* Like button */
.action-circle--like {
  &.active {
    background: transparent;
    box-shadow: none;
    .action-circle__ico { color: #E25D57; }
    .action-circle__count { color: #E25D57; }
  }

  &:not(:disabled):not(.active):hover {
    background: rgba(226, 93, 87, 0.06);
  }
}

/* Collect button */
.action-circle--collect {
  &.active {
    background: transparent;
    box-shadow: none;
    .action-circle__ico { color: #C79A33; }
    .action-circle__count { color: #C79A33; }
  }

  &:not(:disabled):not(.active):hover {
    background: rgba(199, 154, 51, 0.06);
  }
}

.action-circle.active .action-circle__ico svg .icon-path {
  fill: currentColor;
}

/* Active pop animation */
@keyframes acPop {
  0% { transform: translateZ(0) scale(1); }
  40% { transform: translateZ(0) scale(1.18); }
  70% { transform: translateZ(0) scale(0.95); }
  100% { transform: translateZ(0) scale(1); }
}

@keyframes acRipple {
  0% { transform: scale(0); opacity: 0.7; }
  100% { transform: scale(2.5); opacity: 0; }
}

.action-circle.active:not(.done) {
  animation: acPop 320ms cubic-bezier(0.68, -0.55, 0.27, 1.55) forwards;
  .action-circle__ripple {
    animation: acRipple 400ms ease-out forwards;
  }
}

@media (prefers-reduced-motion: reduce) {
  .action-circle,
  .action-circle__ripple {
    transition: none !important;
    animation: none !important;
  }
}

.dot { opacity: 0.6; }

.content-body {
  font-size: 16px;
  line-height: 1.85;
  color: var(--text);
  max-width: 820px;
  margin: 0;
  :deep(p) { margin-bottom: 1.4em; }
  :deep(h1) { font-size: 28px; font-weight: 700; line-height: 1.25; margin: 1.6em 0 0.6em; color: var(--text); }
  :deep(h2) { font-size: 22px; font-weight: 650; line-height: 1.3; margin: 1.4em 0 0.5em; color: var(--text); border-left: 3px solid var(--primary); padding-left: 12px; }
  :deep(h3) { font-size: 18px; font-weight: 600; line-height: 1.35; margin: 1.2em 0 0.4em; color: var(--text); }
  :deep(blockquote) {
    border-left: 3px solid var(--primary);
    background: rgba(14, 165, 233, 0.07);
    padding: 12px 16px;
    border-radius: 0 8px 8px 0;
    margin: 1.2em 0;
    color: var(--muted);
  }
  :deep(code) {
    font-family: ui-monospace, Consolas, 'Courier New', monospace;
    font-size: 13.5px;
    background: rgba(14, 165, 233, 0.09);
    padding: 2px 6px;
    border-radius: 6px;
    color: var(--primary);
  }
  :deep(pre) {
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 16px;
    overflow-x: auto;
    margin: 1.2em 0;
  }
  :deep(pre code) { background: none; padding: 0; border-radius: 0; color: inherit; font-size: 13px; }
  :deep(img) {
    max-width: 100%;
    height: auto;
    display: block;
    margin: 20px auto;
    border-radius: 12px;
    border: 1px solid var(--border);
  }
  :deep(.embedded-chart-container) {
    border: 1px solid var(--border);
    border-radius: 12px;
    background: rgba(14, 165, 233, 0.06);
    padding: 16px;
    margin: 20px 0;
  }
  :deep(.embedded-chart-container .embedded-block-title),
  :deep(.embedded-chart-container .embedded-block-hint) {
    display: none;
  }
  :deep(.embedded-map-placeholder) {
    border: 1px dashed rgba(14, 165, 233, 0.45);
    border-radius: 12px;
    background: rgba(14, 165, 233, 0.06);
    padding: 14px 16px;
    margin: 20px 0;
  }
}

.cover {
  margin: 12px 0 16px;
  .cover-img {
    width: 100%;
    height: 280px;
    object-fit: cover;
    border-radius: 12px;
    border: 1px solid var(--border);
  }
}

/* 评论区样式 */
.comment-section {
  padding: 18px;
  margin-bottom: 14px;
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.comment-input-area {
  margin-bottom: 40px;
  
  .input-footer {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
  }
  
  .login-prompt {
    background: var(--surface-2);
    border-radius: 12px;
    padding: 32px;
    text-align: center;
    border: 1px dashed rgba(15, 23, 42, 0.18);
    
    p { color: var(--muted); margin-bottom: 16px; font-size: 15px; }
  }
}

.comment-list {
  .comment-error {
    margin-bottom: 18px;
  }
  .comment-item {
    display: flex;
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid var(--border);
    &:last-child { border-bottom: none; }
    
    .comment-user {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 6px;
      .name { font-weight: 600; color: var(--text); font-size: 15px; }
      .date { font-size: 12px; color: var(--muted); }
    }
    
    .text { font-size: 14px; line-height: 1.7; color: var(--text); }
  }
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card {
  padding: 16px;
}

.side-title {
  font-size: 14px;
  font-weight: 800;
  margin-bottom: 10px;
}

.author-card {
  display: flex;
  gap: 12px;
  align-items: center;
}

.author-name {
  font-weight: 900;
  margin-bottom: 2px;
}

.tips {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .cover .cover-img { height: 200px; }
}
</style>
