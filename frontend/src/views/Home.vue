<template>
  <ArtLayout>
    <section class="hero-section">
      <div class="site-container">
        <div class="hero-content">
          <div class="badge-tag">松山湖可视化博客平台 2.0</div>
          <h1 class="serif-text">记录灵感，<br/><span class="gradient-text">探索数据的艺术</span></h1>
          <p class="hero-description">
            集内容创作、实时数据可视化与地理信息展示于一体。在这里，每一个文字都有温度，每一组数据都有故事。
          </p>

          <div class="hero-cta">
            <el-button type="primary" size="large" round @click="router.push('/article')">开始探索</el-button>
            <el-button size="large" round @click="handleCreateClick">立即创作</el-button>
          </div>
        </div>
        
        <div class="hero-visual">
          <div class="glass-card main-preview">
            <div class="card-header">
              <div class="dots"><span></span><span></span><span></span></div>
              <div class="address-bar">visual.songshanhu.com</div>
            </div>
            <div class="card-body">
              <div ref="heroChartRef" class="hero-chart"></div>
              <div class="chart-overlay">
                <div class="data-node">
                  <span class="pulse"></span>
                  <span class="label">松山湖实时动态数据</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 悬浮装饰元素 -->
          <div class="decoration-box box-1">
            <el-icon><Histogram /></el-icon>
            <span>实时分析</span>
          </div>
          <div class="decoration-box box-2">
            <el-icon><MapLocation /></el-icon>
            <span>地理坐标</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据摘要展示 -->
    <section class="quick-stats">
      <div class="site-container">
        <div class="stats-row">
          <div class="stat-card">
            <span class="number">500+</span>
            <span class="label">活跃创作者</span>
          </div>
          <div class="stat-card">
            <span class="number">12k+</span>
            <span class="label">精选博文</span>
          </div>
          <div class="stat-card">
            <span class="number">85%</span>
            <span class="label">数据可视化覆盖</span>
          </div>
          <div class="stat-card">
            <span class="number">Top 1</span>
            <span class="label">区域博客平台</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 最新文章预览 -->
    <section class="latest-articles">
      <div class="site-container">
        <div class="content-grid">
          <div class="main">
            <div class="section-header">
              <div class="section-title"><span>最新博文</span></div>
              <el-button link @click="router.push('/article')">查看全部 <el-icon><ArrowRight /></el-icon></el-button>
            </div>

            <div class="category-tabs glass-card">
              <el-radio-group v-model="activeCategory" size="large" @change="resetAndFetch">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="tech">技术</el-radio-button>
                <el-radio-button label="eco">生态</el-radio-button>
                <el-radio-button label="human">人文</el-radio-button>
              </el-radio-group>
            </div>

            <div class="masonry-wrapper" v-loading="loading">
              <MasonryWall
                :key="masonryKey"
                :items="masonryArticles"
                :column-width="320"
                :gap="16"
                :min-columns="1"
              >
                <template #default="{ item, index }">
                  <div
                    class="glass-card card-hover masonry-card stagger-in"
                    :style="{ animationDelay: `${index * 50}ms` }"
                    @click="router.push(`/article/${item.id}`)"
                  >
                    <div class="card-cover">
                      <img
                        v-if="item.coverImage"
                        :src="normalizeCoverUrl(item.coverImage) || '/favicon.svg'"
                        :alt="item.title || '文章封面'"
                        loading="lazy"
                        decoding="async"
                        @error="(e) => ((e.target as HTMLImageElement).src = '/favicon.svg')"
                      />
                      <div v-else class="placeholder-img">
                        <el-icon :size="32" color="#c0c4cc"><Picture /></el-icon>
                      </div>
                    </div>
                    <div class="card-body">
                      <div class="card-title">{{ item.title }}</div>
                      <div class="card-meta">
                        <span class="pill">{{ getHomeCategoryLabel(item.category) }}</span>
                        <span class="dot">·</span>
                        <span class="muted">{{ item.authorName }}</span>
                        <span class="dot">·</span>
                        <span class="muted">{{ item.views }} 阅读</span>
                      </div>
                      <div v-if="cleanArticleSummary(String(item.summary || ''))" class="card-summary">{{ cleanArticleSummary(String(item.summary || '')) }}</div>
                    </div>
                  </div>
                </template>
              </MasonryWall>

              <el-empty v-if="masonryArticles.length === 0 && !loading" description="暂无博文" />

              <div class="load-more" v-if="masonryArticles.length > 0">
                <el-button :disabled="!hasMore" :loading="loadingMore" round @click="loadMore">
                  {{ hasMore ? '加载更多' : '没有更多了' }}
                </el-button>
              </div>
            </div>
          </div>

          <aside class="sidebar">
            <div class="glass-card side-card">
              <div class="side-title section-title"><span>去创作</span></div>
              <p class="muted side-desc">开始写下一篇与松山湖相关的故事，用图表和地图让内容更有说服力。</p>
              <el-button type="primary" round class="side-btn" @click="handleCreateClick">写文章</el-button>
            </div>

            <div class="glass-card side-card">
              <div class="side-title section-title"><span>写作建议</span></div>
              <ul class="tips">
                <li>封面与正文插图尽量与段落内容关联。</li>
                <li>绑定定位后，详情页会展示地图。</li>
                <li>插入图表后，发布到详情页会自动渲染。</li>
              </ul>
            </div>

            <div class="glass-card side-card">
              <div class="side-title section-title"><span>站点导览</span></div>
              <p class="muted side-desc">从内容到数据，一条龙逛完。</p>
              <div class="quick-links">
                <el-button link @click="router.push('/article')">文章广场</el-button>
                <el-button link @click="router.push('/article/mine')">我的文章</el-button>
                <el-button link @click="router.push('/dashboard')">数据看板</el-button>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </section>

    <section ref="featuresSectionRef" class="features-section">
      <div class="site-container">
        <div class="features-head">
          <div class="kicker">✨ 核心特性</div>
          <h2 class="title">为现代内容创作而生的专业工具</h2>
          <p class="sub">图表、地图与内容在同一块画布里呼吸。</p>
        </div>

        <div class="features-stage" :style="{ '--parallax-y': `${featuresParallaxY}px` }">
          <div class="features-art" aria-hidden="true">
            <svg class="scribble" viewBox="0 0 800 500" xmlns="http://www.w3.org/2000/svg">
              <path d="M60 350 C120 280 200 380 280 300 S420 200 500 260 S640 320 720 250" stroke="rgba(14,165,233,0.22)" stroke-width="2" fill="none" stroke-dasharray="8 6"/>
              <path d="M40 400 C160 340 240 420 360 360 S520 280 640 330 S760 380 800 310" stroke="rgba(139,92,246,0.18)" stroke-width="1.5" fill="none" stroke-dasharray="6 8"/>
              <circle cx="400" cy="250" r="160" stroke="rgba(14,165,233,0.08)" stroke-width="1" fill="none"/>
              <circle cx="400" cy="250" r="220" stroke="rgba(139,92,246,0.06)" stroke-width="1" fill="none"/>
            </svg>
          </div>

          <div class="feature-grid">
            <div
              class="feature-item feature-item--tall"
              v-for="(feature, index) in features"
              :key="index"
              :style="{ '--feat-accent': feature.color, '--feat-bg': feature.bg, '--feat-delay': `${index * 60}ms` }"
            >
              <div class="feature-card-inner">
                <div class="feature-top">
                  <div class="feature-icon">
                    <el-icon :size="24" :color="feature.color">
                      <component :is="feature.icon" />
                    </el-icon>
                  </div>
                  <div class="feature-title">{{ feature.title }}</div>
                </div>
                <div class="feature-desc">{{ feature.desc }}</div>
                <div class="feature-art" aria-hidden="true">
                  <div class="feature-art-dot" :style="{ background: feature.color }"></div>
                </div>
              </div>
              <div class="feature-glow" aria-hidden="true"></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { articleApi } from '@/api/article'
import type { Article } from '@/types'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import MasonryWall from '@yeger/vue-masonry-wall'
import { normalizeCoverUrl } from '@/utils/image'
import { cleanArticleSummary } from '@/utils/articleSummary'

const router = useRouter()
const userStore = useUserStore()
const heroChartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const featuresSectionRef = ref<HTMLElement | null>(null)
const featuresParallaxY = ref(0)
let featuresTop = 0
let parallaxRaf = 0

const syncFeaturesTop = () => {
  const el = featuresSectionRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  featuresTop = rect.top + window.scrollY
}

const onParallaxScroll = () => {
  if (parallaxRaf) return
  parallaxRaf = window.requestAnimationFrame(() => {
    const raw = (window.scrollY - featuresTop) * -0.06
    const clamped = Math.max(-64, Math.min(64, raw))
    featuresParallaxY.value = Math.round(clamped)
    parallaxRaf = 0
  })
}


const activeCategory = ref<'all' | 'tech' | 'eco' | 'human'>('all')
const masonryArticles = ref<Article[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const pageSize = 4
const currentPage = ref(1)
const hasMore = ref(true)
const refreshSeed = ref(0)

const apiCategory = computed(() => {
  if (activeCategory.value === 'all') return 'all'
  if (activeCategory.value === 'tech') return 'tech'
  if (activeCategory.value === 'eco') return 'life'
  return 'other'
})

const masonryKey = computed(() => `${activeCategory.value}-${refreshSeed.value}`)

const getHomeCategoryLabel = (c?: string) => {
  if (c === 'tech') return '技术'
  if (c === 'life') return '生态'
  if (c === 'visual') return '可视化'
  return '人文'
}

const fetchMasonry = async (mode: 'reset' | 'append') => {
  if (mode === 'reset') {
    loading.value = true
    currentPage.value = 1
    hasMore.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const res = await articleApi.getList({
      current: currentPage.value,
      size: pageSize,
      category: apiCategory.value
    })
    const records = res?.records || []
    if (mode === 'reset') {
      masonryArticles.value = records
    } else {
      masonryArticles.value = masonryArticles.value.concat(records)
    }
    hasMore.value = records.length === pageSize
    if (hasMore.value) {
      currentPage.value += 1
    }
  } catch {
    if (mode === 'reset') {
      masonryArticles.value = []
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const resetAndFetch = () => {
  refreshSeed.value += 1
  fetchMasonry('reset')
}

const loadMore = () => {
  if (!hasMore.value || loadingMore.value) return
  fetchMasonry('append')
}

const initHeroChart = () => {
  if (!heroChartRef.value) return
  chartInstance = echarts.init(heroChartRef.value)
  
  const option = {
    grid: { top: 10, bottom: 20, left: 20, right: 20 },
    xAxis: {
      type: 'category',
      data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      show: false
    },
    yAxis: { show: false },
    tooltip: { trigger: 'axis' },
    series: [
      {
        data: [150, 230, 224, 218, 135, 147, 260],
        type: 'line',
        smooth: true,
        lineStyle: { width: 4, color: '#0EA5E9' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.4)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        symbol: 'none'
      },
      {
        data: [100, 180, 150, 240, 170, 210, 190],
        type: 'line',
        smooth: true,
        lineStyle: { width: 4, color: '#67C23A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
          ])
        },
        symbol: 'none'
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const onResize = () => {
  chartInstance?.resize()
  syncFeaturesTop()
  onParallaxScroll()
}

onMounted(() => {
  initHeroChart()
  fetchMasonry('reset')
  syncFeaturesTop()
  onParallaxScroll()
  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', onParallaxScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scroll', onParallaxScroll)
  if (parallaxRaf) window.cancelAnimationFrame(parallaxRaf)
  chartInstance?.dispose()
})

function handleCreateClick() {
  if (userStore.isLoggedIn) {
    router.push('/article/create')
    return
  }
  ElMessageBox.confirm(
    '创作需要登录，登录后将自动进入编辑器（草稿将保留）。',
    '提示',
    {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
      roundButton: true
    }
  ).then(() => {
    router.push({ path: '/login', query: { redirect: '/article/create' } })
  }).catch(() => {})
}


const features = [
  {
    title: '多维数据可视化',
    desc: '内置 ECharts 引擎，一键生成精美统计图表，让你的博文数据更有说服力。',
    icon: 'Histogram',
    color: '#0EA5E9',
    bg: '#ecf5ff'
  },
  {
    title: '地理位置绑定',
    desc: '集成高德地图 JS API，支持文章与地理位置关联，打造独特的空间博客体验。',
    icon: 'MapLocation',
    color: '#67C23A',
    bg: '#f0f9eb'
  },
  {
    title: '沉浸式阅读',
    desc: '极简的排版设计，专注于内容本身，提供最纯粹的阅读体验。',
    icon: 'Reading',
    color: '#E6A23C',
    bg: '#fdf6ec'
  },
  {
    title: '全平台适配',
    desc: '无论是 PC 还是移动端，都能获得一致且流畅的操作体验。',
    icon: 'Monitor',
    color: '#F56C6C',
    bg: '#fef0f0'
  }
]

</script>

<style scoped lang="scss">
.gradient-text {
  background: linear-gradient(135deg, var(--primary-700) 0%, var(--primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  display: inline-block;
}

.hero-section {
  padding: 140px 0 60px;
  background: transparent;
  overflow: hidden;

  .site-container {
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: center;
    gap: 60px;
    padding-left: 0;
    padding-right: 0;
    
    @media (max-width: 992px) {
      grid-template-columns: 1fr;
      text-align: center;
      padding-left: 0;
      padding-right: 0;
    }
  }
}

.latest-articles {
  padding: 40px 0 60px;
}

.category-tabs {
  padding: 16px;
  margin: 14px 0 18px;
}

.masonry-wrapper {
  min-height: 240px;
}

.masonry-card {
  cursor: pointer;
  overflow: hidden;
  border-color: var(--border-soft);
}

.card-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: rgba(14, 165, 233, 0.06);
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-body {
  padding: 14px 14px 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
}

.card-summary {
  margin-top: 10px;
  font-size: 13px;
  color: rgba(15, 23, 42, 0.72);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.quick-links {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.stagger-in {
  opacity: 0;
  transform: translateY(12px);
  animation: fadeUp 420ms var(--ease) forwards;
}

@keyframes fadeUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-content {
  .badge-tag {
    display: inline-block;
    padding: 6px 16px;
      background: rgba(14, 165, 233, 0.10);
      color: var(--primary-700);
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 24px;
    letter-spacing: 0.5px;
  }
  
  h1 {
    font-size: 64px;
    line-height: 1.1;
    margin-bottom: 24px;
    font-weight: 700;
    color: #1d1d1f;
    
    @media (max-width: 576px) { font-size: 40px; }
  }
  
  .hero-description {
    font-size: 20px;
      color: var(--muted);
    line-height: 1.5;
    margin-bottom: 40px;
    max-width: 500px;
    
    @media (max-width: 992px) { margin: 0 auto 40px; }
  }

  .hero-search {
    max-width: 560px;
    margin: -16px 0 22px;

    @media (max-width: 992px) {
      margin-left: auto;
      margin-right: auto;
    }
  }
  
  .hero-cta {
    display: flex;
    gap: 16px;
    @media (max-width: 992px) { justify-content: center; }
    
    .el-button {
      height: 52px;
      padding: 0 32px;
      font-size: 16px;
      font-weight: 500;
    }
  }
}

.hero-visual {
  position: relative;
  
  .main-preview {
    width: 100%;
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 30px 60px rgba(0, 0, 0, 0.12);
    border: 1px solid rgba(0, 0, 0, 0.05);
      background: var(--surface);

    .card-header {
      height: 40px;
      background: var(--surface-3);
      display: flex;
      align-items: center;
      padding: 0 16px;
      gap: 12px;

      .dots {
        display: flex;
        gap: 6px;
        span {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #ff5f56;
          &:nth-child(2) { background: #ffbd2e; }
          &:nth-child(3) { background: #27c93f; }
        }
      }

      .address-bar {
        flex: 1;
        height: 24px;
        background: var(--surface);
        border-radius: 12px;
        font-size: 11px;
        color: var(--muted);
        display: flex;
        align-items: center;
        padding: 0 12px;
      }
    }

    .card-body {
      height: 320px;
      position: relative;
      overflow: hidden;
      
      .hero-chart {
        width: 100%;
        height: 100%;
      }
      
      .chart-overlay {
        position: absolute;
        top: 20px;
        right: 20px;
        z-index: 5;
        
        .data-node {
          background: rgba(255, 255, 255, 0.8);
          backdrop-filter: blur(10px);
          padding: 8px 16px;
          border-radius: 20px;
          display: flex;
          align-items: center;
          gap: 10px;
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
          border: 1px solid rgba(255, 255, 255, 0.5);
          
          .label { font-size: 12px; font-weight: 600; color: var(--text); }
          .pulse {
            width: 8px;
            height: 8px;
            background: #34c759;
            border-radius: 50%;
            position: relative;
            &::after {
              content: '';
              position: absolute;
              width: 100%;
              height: 100%;
              background: inherit;
              border-radius: 50%;
              animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
            }
          }
        }
      }
    }
  }
  
  .decoration-box {
    position: absolute;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(20px);
    padding: 12px 20px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
    font-size: 14px;
    font-weight: 600;
    z-index: 10;
    
    &.box-1 {
      top: -20px;
      right: -20px;
      color: var(--primary-700);
      animation: float 4s ease-in-out infinite;
    }
    
    &.box-2 {
      bottom: 40px;
      left: -30px;
      color: #ff3b30;
      animation: float 4s ease-in-out infinite 1s;
    }
  }
}

.quick-stats {
  padding: 60px 0;
  border-top: 1px solid var(--border-soft);
  border-bottom: 1px solid var(--border-soft);
  background: var(--surface-2);
  
  .stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 30px;
    
    @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
  }
  
  .stat-card {
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .number {
      font-size: 32px;
      font-weight: 700;
      color: var(--text);
      font-family: 'Playfair Display', serif;
    }
    
    .label {
      font-size: 14px;
      color: var(--muted);
      font-weight: 500;
    }
  }
}

.latest-articles {
  padding: 40px 0 70px;
  background: transparent;

  .section-header {
    margin-bottom: 14px;
  }

  .article-preview-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-height: 120px;
  }

  .preview-row {
    padding: 14px;
    display: flex;
    gap: 14px;
    cursor: pointer;
  }

  .preview-main {
    flex: 1;
    min-width: 0;
  }

  .preview-title {
    font-size: 16px;
    font-weight: 900;
    color: var(--text);
    margin-bottom: 6px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .preview-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    margin-bottom: 8px;
  }

  .pill {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border: 1px solid var(--border);
    border-radius: 999px;
    background: #fff;
    color: var(--text);
  }

  .dot { opacity: 0.6; }

  .preview-summary {
    font-size: 13px;
    color: #606266;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .preview-cover {
    width: 160px;
    flex: 0 0 160px;
    img {
      width: 160px;
      height: 96px;
      border-radius: 10px;
      object-fit: cover;
      border: 1px solid var(--border);
    }
  }

  .placeholder-img {
    width: 160px;
    height: 96px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed var(--border);
    background: rgba(0, 0, 0, 0.02);
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

  .side-desc { margin-bottom: 12px; }
  .side-btn { width: 100%; }
  .tips {
    margin: 0;
    padding-left: 18px;
    color: #606266;
    font-size: 13px;
    line-height: 1.7;
  }
}

.features-section {
  position: relative;
  padding: 92px 0 104px;
  overflow: hidden;
  background:
    radial-gradient(900px 520px at 10% 0%, rgba(14, 165, 233, 0.16), transparent 60%),
    radial-gradient(820px 520px at 92% 12%, rgba(139, 92, 246, 0.14), transparent 62%),
    linear-gradient(180deg, rgba(240, 249, 255, 0.6) 0%, rgba(255, 255, 255, 1) 55%, rgba(255, 255, 255, 1) 100%);
}

.features-head {
  max-width: 820px;
  margin: 0 auto 40px;
  text-align: center;
  .kicker {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    border-radius: 999px;
    background: rgba(14, 165, 233, 0.10);
    color: #0369a1;
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 0.3px;
  }
  .title {
    margin-top: 16px;
    font-size: 44px;
    line-height: 1.08;
    letter-spacing: -0.8px;
    font-weight: 750;
    color: #0f172a;
  }
  .sub {
    margin-top: 14px;
    font-size: 18px;
    line-height: 1.6;
    color: rgba(15, 23, 42, 0.62);
  }
}

.features-stage {
  position: relative;
}

.features-art {
  position: absolute;
  inset: -160px -80px;
  pointer-events: none;
  z-index: 0;
  transform: translate3d(0, var(--parallax-y, 0px), 0);
  transition: transform 120ms ease-out;
}

.scribble {
  inline-size: 100%;
  block-size: 100%;
  animation: subtleDrift 18s ease-in-out infinite alternate;
}

@keyframes subtleDrift {
  0% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(8px, -6px) rotate(0.5deg); }
  100% { transform: translate(-4px, 4px) rotate(-0.3deg); }
}

.feature-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  align-items: stretch;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.feature-item {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: default;
  animation: featFadeUp 0.55s cubic-bezier(0.4, 0, 0.2, 1) both;
  animation-delay: var(--feat-delay, 0ms);

  &:nth-child(1) { grid-row: span 1; }
  &:nth-child(2) { grid-row: span 1; }
  &:nth-child(3) { grid-row: span 1; }
  &:nth-child(4) { grid-row: span 1; }

  @keyframes featFadeUp {
    from { opacity: 0; transform: translateY(18px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .feature-card-inner {
    position: relative;
    z-index: 1;
    padding: 28px 24px 24px;
    background: var(--feat-bg, #fff);
    border-radius: 16px;
    border: 1px solid rgba(0, 0, 0, 0.06);
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.28s cubic-bezier(0.4, 0, 0.2, 1);
    min-block-size: 160px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .feature-top {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .feature-icon {
    inline-size: 44px;
    block-size: 44px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.72);
    display: grid;
    place-items: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .feature-title {
    font-size: 17px;
    font-weight: 600;
    color: #0f172a;
    line-height: 1.3;
    letter-spacing: -0.1px;
  }

  .feature-desc {
    font-size: 14px;
    line-height: 1.65;
    color: rgba(15, 23, 42, 0.60);
    flex: 1;
  }

  .feature-art {
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .feature-art-dot {
    inline-size: 6px;
    block-size: 6px;
    border-radius: 50%;
    opacity: 0.7;
  }

  .feature-glow {
    position: absolute;
    bottom: -40px;
    right: -40px;
    inline-size: 120px;
    block-size: 120px;
    border-radius: 50%;
    background: radial-gradient(circle, var(--feat-accent, #0EA5E9) 0%, transparent 70%);
    opacity: 0.14;
    transition: opacity 0.28s ease;
    pointer-events: none;
  }

  &:hover .feature-card-inner {
    transform: translateY(-6px) scale(1.01);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.10);
  }

  &:hover .feature-glow {
    opacity: 0.26;
  }
}

@keyframes blobFloat {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(0, -18px, 0) scale(1.04); }
}

@media (max-width: 992px) {
  .features-head {
    .title { font-size: 36px; }
  }
  .feature-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    grid-auto-rows: 92px;
  }
  .feature-item:nth-child(1) {
    grid-column: 1 / -1;
    grid-row: auto;
  }
  .feature-item:nth-child(2),
  .feature-item:nth-child(3),
  .feature-item:nth-child(4) {
    grid-column: span 3;
    grid-row: auto;
  }
}

@media (max-width: 375px) {
  .features-section {
    padding: 76px 0 88px;
  }
  .features-head {
    margin-bottom: 22px;
    .title { font-size: 28px; }
    .sub { font-size: 16px; }
  }
  .features-art {
    inset: -200px -120px;
  }
  .scribble {
    left: 8%;
    width: 380px;
    height: 180px;
    opacity: 0.45;
  }
  .feature-grid {
    grid-template-columns: 1fr;
    grid-auto-rows: auto;
  }
  .feature-item,
  .feature-item:nth-child(1),
  .feature-item:nth-child(2),
  .feature-item:nth-child(3),
  .feature-item:nth-child(4) {
    grid-column: auto;
    grid-row: auto;
    padding: 18px 18px;
    border-radius: 22px;
  }
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  @media (max-width: 992px) { grid-template-columns: 1fr; }
}

.feature-item {
  background: var(--surface);
  padding: 48px;
  border-radius: 32px;
  border: 1px solid var(--border-soft);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.04);
  }
  
  .feature-icon {
    width: 64px;
    height: 64px;
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 32px;
  }
  
  h3 { font-size: 24px; font-weight: 700; margin-bottom: 16px; }
  p { font-size: 16px; color: var(--muted); line-height: 1.6; }
}

@keyframes ping {
  75%, 100% { transform: scale(3); opacity: 0; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.serif-text { font-family: 'Playfair Display', serif; }
</style>
