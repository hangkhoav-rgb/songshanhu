<template>
  <ArtLayout force-navbar-scrolled>
    <div class="article-list-page">
      <div class="site-container">
        <header class="list-header">
          <h1 class="serif-text">文章广场</h1>
          <p class="muted">发现松山湖的每一个精彩瞬间</p>
        </header>

        <div class="content-grid">
          <section class="main">
            <div class="filter-section glass-card">
              <div class="filter-group categories">
                <el-radio-group v-model="category" size="large" @change="fetchArticles">
                  <el-radio-button label="all">全部</el-radio-button>
                  <el-radio-button label="tech">技术</el-radio-button>
                  <el-radio-button label="life">生活</el-radio-button>
                  <el-radio-button label="visual">可视化</el-radio-button>
                  <el-radio-button label="other">其他</el-radio-button>
                </el-radio-group>
              </div>

              <div class="filter-group search">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索标题/摘要..."
                  class="search-input"
                  clearable
                  @keyup.enter="runSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" round @click="runSearch">搜索</el-button>
              </div>
            </div>

            <div class="article-list" v-loading="loading">
              <div
                v-for="article in articles"
                :key="article.id"
                class="glass-card card-hover article-row"
                @click="router.push(`/article/${article.id}`)"
              >
                <div class="row-main">
                  <div class="row-title">{{ article.title }}</div>
                  <div class="row-meta">
                    <span class="pill">{{ getCategoryLabel(article.category) }}</span>
                    <span class="dot">·</span>
                    <span class="muted">{{ article.authorName }}</span>
                    <span class="dot">·</span>
                    <span class="muted"><el-icon><View /></el-icon> {{ article.views }}</span>
                    <span class="dot">·</span>
                    <span class="muted"><el-icon><ChatLineRound /></el-icon> {{ article.comments }}</span>
                    <span class="dot">·</span>
                    <span class="muted"><el-icon><Pointer /></el-icon> {{ article.likes || 0 }}</span>
                  </div>
                  <div v-if="cleanArticleSummary(String(article.summary || ''))" class="row-summary">{{ cleanArticleSummary(String(article.summary || '')) }}</div>
                </div>

                <div class="row-cover">
                  <img
                    v-if="article.coverImage"
                    :src="normalizeCoverUrl(article.coverImage) || '/favicon.svg'"
                    :alt="article.title || '文章封面'"
                    loading="lazy"
                    decoding="async"
                    @error="(e) => ((e.target as HTMLImageElement).src = '/favicon.svg')"
                  />
                  <div v-else class="placeholder-img">
                    <el-icon :size="34" color="#c0c4cc"><Picture /></el-icon>
                  </div>
                </div>
              </div>

              <el-empty v-if="articles.length === 0 && !loading" description="暂无相关文章" />
            </div>

            <div class="pagination">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="pageSize"
                v-model:current-page="currentPage"
                @current-change="handlePageChange"
              />
            </div>
          </section>

          <aside class="sidebar">
            <div class="glass-card side-card">
              <div class="side-title section-title"><span>写文章</span></div>
              <p class="muted side-desc">把今天的灵感写下来</p>
              <el-button type="primary" round class="side-btn" @click="handleCreate">去创作</el-button>
            </div>

            <div class="glass-card side-card">
              <div class="side-title section-title"><span>小贴士</span></div>
              <ul class="tips">
                <li>封面建议 16:9，正文插图尽量贴近段落内容。</li>
                <li>可在发布页绑定定位，在详情页展示地图。</li>
              </ul>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { articleApi } from '@/api/article'
import type { Article } from '@/types'
import { useUserStore } from '@/stores/user'
import { normalizeCoverUrl } from '@/utils/image'
import { cleanArticleSummary } from '@/utils/articleSummary'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const searchQuery = ref('')
const category = ref('all')
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

const articles = ref<Article[]>([])

const fetchArticles = async () => {
  loading.value = true
  try {
    const res = await articleApi.getList({
      current: currentPage.value,
      size: pageSize.value,
      category: category.value,
      keyword: searchQuery.value
    })
    if (res && res.records) {
      articles.value = res.records
      total.value = res.total
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const runSearch = async () => {
  currentPage.value = 1
  const kw = searchQuery.value.trim()
  const nextQuery: any = { ...route.query }
  if (kw) nextQuery.keyword = kw
  else delete nextQuery.keyword
  await router.replace({ path: route.path, query: nextQuery })
  await fetchArticles()
}

onMounted(async () => {
  searchQuery.value = (route.query.keyword as string) || ''
  await fetchArticles()
})

watch([category], () => {
  currentPage.value = 1
  fetchArticles()
})

watch(
  () => route.query.keyword,
  (v) => {
    const kw = (v as string) || ''
    if (kw !== searchQuery.value) {
      searchQuery.value = kw
      fetchArticles()
    }
  }
)

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchArticles()
}

const handleCreate = () => {
  if (userStore.isLoggedIn) {
    router.push('/article/create')
    return
  }
  router.push({ path: '/login', query: { redirect: '/article/create' } })
}

const getCategoryLabel = (cat: string) => {
  const map: Record<string, string> = {
    'tech': '技术',
    'life': '生活',
    'visual': '可视化',
    'other': '其他'
  }
  return map[cat] || cat
}
</script>

<style scoped lang="scss">
.list-header {
  padding-top: 110px;
  margin-bottom: 18px;
  h1 { font-size: 28px; margin-bottom: 6px; }
}


.filter-section {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 14px;
}

.filter-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  &.search {
    justify-content: flex-start;
  }
}

.search-input {
  flex: 1;
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: none !important;
    border: 1px solid var(--border);
    background: #fff;
  }
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.article-row {
  padding: 14px;
  display: flex;
  gap: 14px;
  cursor: pointer;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-title {
  font-size: 16px;
  font-weight: 800;
  color: #1d1d1f;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 8px;
  :deep(.el-icon) { margin-right: 4px; }
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff;
  color: #1d1d1f;
}

.dot { opacity: 0.6; }

.row-summary {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-cover {
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

.pagination {
  display: flex;
  justify-content: center;
  padding: 16px 0 40px;
}

.serif-text { font-family: 'Playfair Display', serif; }
</style>
