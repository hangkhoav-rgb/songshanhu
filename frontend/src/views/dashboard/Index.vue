<template>
  <ArtLayout force-navbar-scrolled>
    <div class="dashboard-container site-container" data-testid="dashboard">
      <header class="dashboard-header">
        <div class="header-left">
          <h1 class="serif-text">数据看板</h1>
          <p>洞察您的内容影响力，让数据讲述创作的故事。</p>
        </div>
        <div class="header-right">
          <div class="muted last-updated">更新于：{{ lastUpdated || '—' }}</div>
          <el-button size="small" round :loading="refreshing" @click="manualRefresh">刷新</el-button>
        </div>
      </header>

      <div v-if="initError" class="state glass-card" data-testid="dashboard-error">
        <el-empty description="数据加载失败">
          <div class="muted" style="margin-bottom: 10px">{{ initError }}</div>
          <el-button type="primary" round :loading="refreshing" @click="manualRefresh">重试</el-button>
        </el-empty>
      </div>

      <div v-if="!initError" class="grid-shell">
        <GridLayout
          v-model:layout="layout"
          :col-num="colNum"
          :row-height="46"
          :margin="[16, 16]"
          :is-draggable="colNum >= 4"
          :is-resizable="colNum >= 4"
          :vertical-compact="true"
          :use-css-transforms="true"
          @layout-updated="onLayoutUpdated"
        >
          <GridItem v-for="item in layout" :key="item.i" :x="item.x" :y="item.y" :w="item.w" :h="item.h" :i="item.i">
            <div class="grid-item-inner glass-card" :class="{ 'no-pad': item.i === 'category' }">
              <template v-if="item.i.startsWith('stat-')">
                <div class="stat-item">
                  <div class="stat-icon" :style="{ color: statById[item.i].color, background: statById[item.i].bg }">
                    <el-icon :size="24"><component :is="statById[item.i].icon" /></el-icon>
                  </div>
                  <div class="stat-info">
                    <span class="label">{{ statById[item.i].label }}</span>
                    <span class="value">{{ statById[item.i].value }}</span>
                  </div>
                </div>
              </template>

              <template v-else-if="item.i === 'trend'">
                <div class="chart-card">
                  <div class="chart-header">
                    <h3 class="chart-title">内容影响力趋势</h3>
                    <el-radio-group v-model="timeRange" size="small" @change="updateTrendChart">
                      <el-radio-button label="7d">近7天</el-radio-button>
                      <el-radio-button label="30d">近30天</el-radio-button>
                    </el-radio-group>
                  </div>
                  <div id="dashboard-trend-chart" class="chart-container"></div>
                </div>
              </template>

              <template v-else-if="item.i === 'category'">
                <div class="category-grid-wrap">
                  <div
                    class="chart-card category-panel"
                    :class="{ 'is-hover': hoveredChart === 'bar' }"
                    @mouseenter="hoveredChart = 'bar'"
                    @mouseleave="hoveredChart = null"
                  >
                    <div class="chart-head">
                      <h3 class="chart-title">创作分类统计</h3>
                      <div class="chart-subtitle">按分类统计文章数量</div>
                    </div>
                    <div v-if="!hasCategoryData" class="chart-empty">
                      <img class="empty-illus" :src="emptyCategorySvg" alt="暂无分类数据" />
                      <div class="empty-text">暂无分类数据</div>
                    </div>
                    <div v-else id="dashboard-bar-chart" class="chart-container"></div>
                  </div>

                  <div
                    class="chart-card category-panel"
                    :class="{ 'is-hover': hoveredChart === 'pie' }"
                    @mouseenter="hoveredChart = 'pie'"
                    @mouseleave="hoveredChart = null"
                  >
                    <div class="chart-head">
                      <h3 class="chart-title">创作分类分布</h3>
                      <div class="chart-subtitle">查看分类占比</div>
                    </div>
                    <div v-if="!hasCategoryData" class="chart-empty">
                      <img class="empty-illus" :src="emptyCategorySvg" alt="暂无分类数据" />
                      <div class="empty-text">暂无分类数据</div>
                    </div>
                    <div v-else id="dashboard-pie-chart" class="chart-container"></div>
                  </div>
                </div>
              </template>

              <template v-else-if="item.i === 'map'">
                <div class="chart-card">
                  <h3>活跃互动地图</h3>
                  <div id="dashboard-map" class="map-container">
                    <div v-if="mapFailed" class="map-placeholder">
                      <div class="pulse-ring"></div>
                      <el-icon :size="32" color="var(--primary)"><MapLocation /></el-icon>
                      <p>高德地图加载失败</p>
                    </div>
                  </div>
                </div>
              </template>

              <template v-else-if="item.i === 'articles'">
                <div class="chart-card">
                  <div class="chart-header">
                    <h3>我的文章</h3>
                    <el-button link type="primary" @click="router.push('/article/mine')">更多</el-button>
                  </div>
                  <div class="article-mini-list" v-loading="articlesLoading">
                    <div
                      v-for="a in recentArticles"
                      :key="a.id"
                      class="mini-row"
                      @click="router.push(`/article/${a.id}`)"
                    >
                      <div class="mini-title">{{ a.title }}</div>
                      <div class="mini-meta muted">{{ a.createTime }}</div>
                    </div>
                    <el-empty v-if="!articlesLoading && recentArticles.length === 0" description="暂无文章" :image-size="60" />
                  </div>
                </div>
              </template>
            </div>
          </GridItem>
        </GridLayout>
      </div>

      <section
        v-if="!initError"
        class="my-actions glass-card"
        v-loading="actionsLoading || initLoading"
        data-testid="dashboard-actions"
      >
        <div class="my-actions-header">
          <div>
            <h2 class="serif-text">最近互动</h2>
            <div class="muted">最近互动时间：{{ mySummary?.lastActionTime || '暂无' }}</div>
          </div>
          <div class="filters">
            <el-select v-model="actionTypeFilter" size="small" placeholder="全部类型" @change="onFilterChanged">
              <el-option label="全部" value="" />
              <el-option label="点赞" value="liked" />
              <el-option label="收藏" value="collected" />
            </el-select>
          </div>
        </div>

        <div class="kpis">
          <div class="kpi">
            <div class="kpi-label muted">点赞数</div>
            <div class="kpi-value">{{ (mySummary?.likeCount ?? 0).toLocaleString() }}</div>
          </div>
          <div class="kpi">
            <div class="kpi-label muted">收藏数</div>
            <div class="kpi-value">{{ (mySummary?.collectCount ?? 0).toLocaleString() }}</div>
          </div>
        </div>

        <el-table :data="actionsPage.records" style="width: 100%" size="small">
          <el-table-column prop="actionTime" label="时间" width="180" />
          <el-table-column prop="actionType" label="类型" width="90">
            <template #default="scope">
              <el-tag v-if="scope.row.actionType === 'liked'" type="warning">点赞</el-tag>
              <el-tag v-else type="success">收藏</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="文章" min-width="220">
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="router.push(`/article/${scope.row.articleId}`)">
                {{ scope.row.title }}
              </el-link>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="actionsQuery.current"
            :page-size="actionsQuery.size"
            :total="actionsPage.total"
            @current-change="onPageChanged"
          />
        </div>

        <el-empty v-if="!actionsLoading && actionsPage.records.length === 0" description="近 30 天暂无互动" :image-size="80" />
      </section>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import * as echarts from 'echarts'
import { loadAMap } from '@/utils/amap'
import type { DashboardStats } from '@/api/stats'
import { dashboardApi, type DashboardAction, type DashboardActionType, type DashboardSummary, type MpPage } from '@/api/dashboard'
import { GridLayout, GridItem } from 'vue-grid-layout-v3'
import { articleApi } from '@/api/article'
import type { Article } from '@/types'
import { createPoller } from '@/utils/poller'
import emptyCategorySvg from '@/assets/empty-category.svg'

const router = useRouter()

const initLoading = ref(true)
const refreshing = ref(false)
const initError = ref('')
const lastUpdated = ref('')

const timeRange = ref('7d')
const hoveredChart = ref<'bar' | 'pie' | null>(null)
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let map: any = null
let markerLayer: any[] = []
const mapFailed = ref(false)

const dashboardData = ref<DashboardStats | null>(null)
const hasCategoryData = computed(() => (dashboardData.value?.categoryDistribution?.length || 0) > 0)

const actionsLoading = ref(false)
const mySummary = ref<DashboardSummary | null>(null)
const actionsPage = ref<MpPage<DashboardAction>>({ records: [], total: 0, size: 10, current: 1, pages: 0 })
const actionsQuery = ref({ current: 1, size: 10, days: 30 })
const actionTypeFilter = ref<DashboardActionType | ''>('')

const colNum = ref(4)

const stats = ref([
  { id: 'stat-views', label: '总阅读量', value: '0', icon: 'View', color: '#0EA5E9', bg: 'rgba(14, 165, 233, 0.12)' },
  { id: 'stat-likes', label: '点赞总数', value: '0', icon: 'Star', color: '#F97316', bg: 'rgba(249, 115, 22, 0.12)' },
  { id: 'stat-comments', label: '评论总数', value: '0', icon: 'ChatLineRound', color: '#10B981', bg: 'rgba(16, 185, 129, 0.12)' },
  { id: 'stat-articles', label: '文章总数', value: '0', icon: 'Document', color: '#8B5CF6', bg: 'rgba(139, 92, 246, 0.12)' }
])

const statById = computed(() => {
  const m: Record<string, any> = {}
  stats.value.forEach((s) => (m[s.id] = s))
  return m
})

type LayoutItem = { x: number; y: number; w: number; h: number; i: string }
const LAYOUT_KEY = 'ssl_dashboard_layout_v1'
const layoutKey = (c: number) => `${LAYOUT_KEY}_${c}`

const defaultLayout4: LayoutItem[] = [
  { x: 0, y: 0, w: 1, h: 2, i: 'stat-views' },
  { x: 1, y: 0, w: 1, h: 2, i: 'stat-likes' },
  { x: 2, y: 0, w: 1, h: 2, i: 'stat-comments' },
  { x: 3, y: 0, w: 1, h: 2, i: 'stat-articles' },
  { x: 0, y: 2, w: 4, h: 6, i: 'trend' },
  { x: 0, y: 8, w: 4, h: 6, i: 'category' },
  { x: 0, y: 14, w: 2, h: 4, i: 'map' },
  { x: 2, y: 14, w: 2, h: 4, i: 'articles' }
]

const defaultLayout2: LayoutItem[] = [
  { x: 0, y: 0, w: 1, h: 2, i: 'stat-views' },
  { x: 1, y: 0, w: 1, h: 2, i: 'stat-likes' },
  { x: 0, y: 2, w: 1, h: 2, i: 'stat-comments' },
  { x: 1, y: 2, w: 1, h: 2, i: 'stat-articles' },
  { x: 0, y: 4, w: 2, h: 6, i: 'trend' },
  { x: 0, y: 10, w: 2, h: 8, i: 'category' },
  { x: 0, y: 18, w: 2, h: 4, i: 'map' },
  { x: 0, y: 22, w: 2, h: 4, i: 'articles' }
]

const defaultLayout1: LayoutItem[] = [
  { x: 0, y: 0, w: 1, h: 2, i: 'stat-views' },
  { x: 0, y: 2, w: 1, h: 2, i: 'stat-likes' },
  { x: 0, y: 4, w: 1, h: 2, i: 'stat-comments' },
  { x: 0, y: 6, w: 1, h: 2, i: 'stat-articles' },
  { x: 0, y: 8, w: 1, h: 6, i: 'trend' },
  { x: 0, y: 14, w: 1, h: 8, i: 'category' },
  { x: 0, y: 22, w: 1, h: 4, i: 'map' },
  { x: 0, y: 26, w: 1, h: 4, i: 'articles' }
]

const defaultLayoutByCol = (c: number) => (c >= 4 ? defaultLayout4 : c >= 2 ? defaultLayout2 : defaultLayout1)

const loadLayout = (c: number): LayoutItem[] => {
  try {
    const raw = localStorage.getItem(layoutKey(c))
    if (!raw) return defaultLayoutByCol(c)
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return defaultLayoutByCol(c)
    const base = defaultLayoutByCol(c)
    const ids = new Set(base.map((d) => d.i))
    const filtered = parsed.filter((it: any) => ids.has(String(it.i)))
    if (filtered.length !== base.length) return base
    return filtered
  } catch {
    return defaultLayoutByCol(c)
  }
}

const layout = ref<LayoutItem[]>(defaultLayout4)

const onLayoutUpdated = (newLayout: LayoutItem[]) => {
  layout.value = newLayout
  if (colNum.value >= 4) {
    localStorage.setItem(layoutKey(colNum.value), JSON.stringify(newLayout))
  }
}

const recentArticles = ref<Article[]>([])
const articlesLoading = ref(false)
const fetchRecentArticles = async () => {
  articlesLoading.value = true
  try {
    const res = await articleApi.getMine({ current: 1, size: 6 })
    recentArticles.value = res?.records || []
  } catch {
    recentArticles.value = []
  } finally {
    articlesLoading.value = false
  }
}

const ensureCharts = () => {
  const trendEl = document.getElementById('dashboard-trend-chart')
  const barEl = document.getElementById('dashboard-bar-chart')
  const pieEl = document.getElementById('dashboard-pie-chart')
  if (!trendChart && trendEl) initTrendChart()
  if (!barChart && barEl) initBarChart()
  if (!categoryChart && pieEl) initCategoryChart()
}

const ensureMap = () => {
  if (map || mapFailed.value) return
  const el = document.getElementById('dashboard-map')
  if (!el) return
  initMap()
}

const renderWidgets = () => {
  ensureCharts()
  ensureMap()
  updateTrendChart()
  updateBarChart()
  updateCategoryChart()
  updateMapMarkers()
  trendChart?.resize()
  barChart?.resize()
  categoryChart?.resize()
}

const scheduleRender = () => {
  nextTick(() => {
    window.setTimeout(() => {
      renderWidgets()
    }, 0)
  })
}

const applyStats = (res: DashboardStats) => {
  dashboardData.value = res
  stats.value[0].value = (res.totalViews || 0).toLocaleString()
  stats.value[1].value = (res.totalLikes || 0).toLocaleString()
  stats.value[2].value = (res.totalComments || 0).toLocaleString()
  stats.value[3].value = (res.totalArticles || 0).toLocaleString()
  scheduleRender()
}

const fetchInit = async (silent = false) => {
  if (!silent) {
    refreshing.value = true
  }
  actionsLoading.value = true
  try {
    const res = await dashboardApi.init({
      current: actionsQuery.value.current,
      size: actionsQuery.value.size,
      days: actionsQuery.value.days,
      actionType: actionTypeFilter.value
    })
    applyStats(res.stats)
    mySummary.value = res.summary
    actionsPage.value = res.actions
    initError.value = ''
    lastUpdated.value = new Date().toLocaleString()
  } catch (e: any) {
    initError.value = e?.message || '加载失败'
  } finally {
    actionsLoading.value = false
    refreshing.value = false
    initLoading.value = false
  }
}

const manualRefresh = async () => {
  await fetchInit(false)
}

const poller = createPoller(() => fetchInit(true), 30000)

const updateColNum = () => {
  const w = window.innerWidth
  colNum.value = w >= 1200 ? 4 : w >= 768 ? 2 : 1
}

const fetchMyActions = async () => {
  actionsLoading.value = true
  try {
    const page = await dashboardApi.actions({
      current: actionsQuery.value.current,
      size: actionsQuery.value.size,
      days: actionsQuery.value.days,
      actionType: actionTypeFilter.value
    })
    actionsPage.value = page
  } catch {
    actionsPage.value = { records: [], total: 0, size: actionsQuery.value.size, current: actionsQuery.value.current, pages: 0 }
  } finally {
    actionsLoading.value = false
  }
}

const onFilterChanged = () => {
  actionsQuery.value.current = 1
  fetchMyActions()
}

const onPageChanged = (p: number) => {
  actionsQuery.value.current = p
  fetchMyActions()
}

const initMap = () => {
  const el = document.getElementById('dashboard-map') as HTMLElement | null
  if (!el) return
  loadAMap()
    .then((AMap) => {
      map = new AMap.Map(el, {
        zoom: 12,
        center: [113.883, 22.895],
        mapStyle: 'amap://styles/whitesmoke',
        resizeEnable: true
      })
      map.addControl(new AMap.ToolBar())
      map.on('complete', () => {
        el.setAttribute('data-map-ready', 'true')
        updateMapMarkers()
      })
    })
    .catch(() => {
      mapFailed.value = true
    })
}

const updateMapMarkers = () => {
  if (!map || !dashboardData.value?.locationPoints) return
  
  // 清除旧标记
  markerLayer.forEach(m => m.setMap && m.setMap(null))
  markerLayer = []

  dashboardData.value.locationPoints.forEach(point => {
    const marker = new (window as any).AMap.Marker({
      position: [point.longitude, point.latitude],
      anchor: 'bottom-center'
    })
    const info = new (window as any).AMap.InfoWindow({
      isCustom: false,
      content: `<div style="font-size:13px;font-weight:600;color:#1d1d1f;">${point.title}</div>`,
      offset: new (window as any).AMap.Pixel(0, -28)
    })
    marker.on('click', () => info.open(map, marker.getPosition()))
    map!.add(marker)
    markerLayer.push(marker)
  })
}

const initTrendChart = () => {
  const el = document.getElementById('dashboard-trend-chart') as HTMLElement | null
  if (!el) return
  trendChart = echarts.init(el, undefined, { renderer: 'canvas' })
}

const updateTrendChart = () => {
  if (!dashboardData.value || !trendChart) return
  
  const all = dashboardData.value.dailyTrend || []
  const sliced = timeRange.value === '7d' ? all.slice(-7) : all
  const dates = sliced.map(d => d.date)
  const counts = sliced.map(d => d.count)
  const axisColor = '#e2e8f0'
  const textColor = '#475569'

  if (counts.length === 0) {
    trendChart.setOption({
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { show: false, type: 'category', data: [] },
      yAxis: { show: false, type: 'value' },
      series: [],
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: { text: '暂无数据', fill: textColor, fontSize: 14 }
      }
    })
    return
  }

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: axisColor } },
      axisLabel: { color: textColor }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { type: 'dashed', color: axisColor } }
    },
    series: [{
      name: '发布数',
      type: 'line',
      smooth: true,
      data: counts,
      itemStyle: { color: '#0EA5E9' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(14, 165, 233, 0.28)' },
          { offset: 1, color: 'rgba(14, 165, 233, 0)' }
        ])
      }
    }],
    graphic: []
  })
}

const updateCategoryChart = () => {
  if (!dashboardData.value || !categoryChart) return
  
  const raw = dashboardData.value.categoryDistribution || []
  const textColor = '#475569'
  const primary = '#0052D9'
  const secondary = '#69C0FF'
  const accent = '#FF6B6B'
  const makeGrad = (a: string, b: string) => new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: a }, { offset: 1, color: b }])

  const data = raw.map(item => ({
    name: item.name === 'tech' ? '技术' : 
          item.name === 'life' ? '生活' : 
          item.name === 'visual' ? '可视化' : '其他',
    value: item.value
  }))

  if (data.length === 0) {
    categoryChart.setOption({
      series: [],
      legend: { show: false },
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: { text: '暂无数据', fill: textColor, fontSize: 14 }
      }
    })
    return
  }

  const colored = data.map((d, idx) => {
    const c = idx % 3 === 0 ? makeGrad(primary, secondary) : idx % 3 === 1 ? makeGrad(secondary, accent) : makeGrad(primary, accent)
    return { ...d, itemStyle: { color: c } }
  })

  categoryChart.setOption({
    tooltip: {
      trigger: 'item',
      extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.15); border-radius: 6px;'
    },
    legend: {
      top: 0,
      left: 'center',
      icon: 'circle',
      itemGap: 8,
      textStyle: { color: textColor, overflow: 'truncate' }
    },
    series: [{
      name: '文章分类',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: 'rgba(255,255,255,0.9)', borderWidth: 2 },
      label: { show: false },
      emphasis: { scale: true, scaleSize: 10 },
      data: colored
    }],
    graphic: []
  })
}

const initCategoryChart = () => {
  const el = document.getElementById('dashboard-pie-chart') as HTMLElement | null
  if (!el) return
  categoryChart = echarts.init(el, undefined, { renderer: 'canvas' })
}

const initBarChart = () => {
  const el = document.getElementById('dashboard-bar-chart') as HTMLElement | null
  if (!el) return
  barChart = echarts.init(el, undefined, { renderer: 'canvas' })
}

const updateBarChart = () => {
  if (!dashboardData.value || !barChart) return
  const raw = dashboardData.value.categoryDistribution || []
  const textColor = '#475569'
  const axisColor = '#e2e8f0'
  const primary = '#0052D9'
  const secondary = '#69C0FF'

  const labels = raw.map((item) =>
    item.name === 'tech' ? '技术' : item.name === 'life' ? '生活' : item.name === 'visual' ? '可视化' : '其他'
  )
  const values = raw.map((item) => item.value)

  if (values.length === 0) {
    barChart.setOption({
      xAxis: { show: false, type: 'category', data: [] },
      yAxis: { show: false, type: 'value' },
      series: [],
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: { text: '暂无数据', fill: textColor, fontSize: 14 }
      }
    })
    return
  }

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.15); border-radius: 6px;'
    },
    legend: {
      top: 0,
      left: 'center',
      itemGap: 8,
      textStyle: { color: textColor, overflow: 'truncate' }
    },
    grid: { top: 56, left: '3%', right: '4%', bottom: 44, containLabel: true },
    dataZoom: [
      {
        type: 'slider',
        show: true,
        height: 32,
        bottom: 0,
        showDetail: false,
        brushSelect: false,
        zoomLock: true
      }
    ],
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: textColor, overflow: 'truncate' },
      axisLine: { lineStyle: { color: axisColor } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { type: 'dashed', color: axisColor } }
    },
    series: [
      {
        name: '文章数量',
        type: 'bar',
        data: values,
        barMaxWidth: '70%',
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: primary },
            { offset: 1, color: secondary }
          ])
        }
      }
    ],
    graphic: []
  })
}

onMounted(async () => {
  updateColNum()
  layout.value = loadLayout(colNum.value)
  await fetchInit()
  await fetchRecentArticles()
  window.addEventListener('resize', handleResize)
  poller.start()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  poller.stop()
  trendChart?.dispose()
  categoryChart?.dispose()
  barChart?.dispose()
  map?.destroy?.()
})

const handleResize = () => {
  updateColNum()
  trendChart?.resize()
  categoryChart?.resize()
  barChart?.resize()
}

watch(colNum, (c) => {
  layout.value = loadLayout(c)
  scheduleRender()
})
</script>

<style scoped lang="scss">
.dashboard-container.site-container {
  padding-top: 110px;
  padding-bottom: 60px;
  max-width: none;
  width: 100%;
  max-width: clamp(64rem, 96vw, 110rem);
  max-inline-size: clamp(64rem, 96vw, 110rem);
  min-inline-size: min(100%, 20rem);
}

.dashboard-header {
  margin-bottom: 18px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  h1 { font-size: 28px; margin-bottom: 6px; color: var(--text); }
  p { color: var(--muted); font-size: 14px; }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.last-updated {
  font-size: 12px;
}

.state {
  padding: 18px;
  margin-bottom: 16px;
}

.grid-shell {
  width: 100%;
  min-block-size: 38rem;
}

.my-actions {
  margin-top: 18px;
  padding: 18px;
  min-block-size: 28rem;
}

.my-actions-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kpis {
  margin: 12px 0 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.kpi {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--border);
}

.kpi-value {
  font-size: 22px;
  font-weight: 600;
  margin-top: 6px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

:deep(.vue-grid-layout) {
  width: 100%;
}

.grid-item-inner {
  height: 100%;
  padding: 16px;
  overflow: visible;
  transition: transform 0.3s var(--ease), box-shadow 0.3s var(--ease);
}

.grid-item-inner.no-pad {
  padding: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 20px;
  
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .stat-info {
    display: flex;
    flex-direction: column;
    .label { font-size: 14px; color: var(--muted); margin-bottom: 4px; }
    .value { font-size: 24px; font-weight: 600; color: var(--text); }
  }
}

.chart-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.chart-head {
  margin-bottom: 16px;
}

.chart-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-subtitle {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-grid-wrap {
  height: 100%;
  display: grid;
  gap: clamp(0.75rem, 1.2vw, 1rem);
  grid-template-columns: 1fr;
  padding: 16px;
  min-inline-size: min(100%, 20rem);
  max-inline-size: 100%;
  overflow: hidden;
}

.category-panel {
  min-inline-size: clamp(14rem, 22vw, 32rem);
  max-inline-size: 100%;
  overflow: hidden;
  transform: translateZ(0) scale(1);
  transform-origin: center;
  transition: transform 0.3s ease-out;
  will-change: transform;
}

.category-panel.is-hover {
  transform: translateZ(0) scale(1.05);
  z-index: 2;
}

.chart-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-block-size: 14rem;
}

.empty-illus {
  inline-size: 15rem;
  block-size: 10rem;
}

.empty-text {
  font-size: 14px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (min-width: 48rem) and (max-width: 74.999rem) {
  .category-grid-wrap {
    grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  }
}

@media (min-width: 75rem) {
  .category-grid-wrap {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.chart-container {
  flex: 1;
  width: 100%;
  min-inline-size: 0;
}

.map-container {
  flex: 1;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.map-placeholder {
  height: 100%;
  background: var(--surface-2);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  
  p { font-size: 13px; color: var(--muted); margin-top: 12px; }
  
  .pulse-ring {
    position: absolute;
    width: 60px;
    height: 60px;
    border: 2px solid rgba(14, 165, 233, 0.22);
    border-radius: 50%;
    animation: pulse 2s infinite;
  }
}

.article-mini-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
}

.mini-row {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(2, 132, 199, 0.12);
  background: rgba(2, 132, 199, 0.04);
  cursor: pointer;
  transition: background 0.3s var(--ease), transform 0.3s var(--ease);
}

.mini-row:hover {
  transform: translateY(-2px);
  background: rgba(2, 132, 199, 0.06);
}

.mini-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mini-meta {
  font-size: 12px;
  margin-top: 6px;
}

:deep(.custom-marker) {
  width: 12px;
  height: 12px;
  background-color: var(--primary);
  border: 2px solid white;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 0 10px rgba(14, 165, 233, 0.35);
  transition: transform 0.2s ease;
  
  &:hover {
    transform: scale(1.5);
    background-color: var(--danger);
  }
}

@keyframes pulse {
  0% { transform: scale(0.5); opacity: 1; }
  100% { transform: scale(2.5); opacity: 0; }
}

.side-charts {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.serif-text { font-family: 'Playfair Display', serif; }

@media (max-width: 1024px) {
  .charts-section { grid-template-columns: 1fr; }
}

@media (max-width: 576px) {
  .dashboard-container {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
