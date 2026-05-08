<template>
  <ArtLayout force-navbar-scrolled>
    <div class="my-article-container site-container">
      <header class="header">
        <h1 class="serif-text">我的文章</h1>
        <p>管理你的草稿与已发布内容</p>
      </header>

      <div class="toolbar glass-card">
        <el-input
          v-model="keyword"
          placeholder="搜索标题/摘要..."
          clearable
          class="kw"
          @keyup.enter="refresh"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="status" placeholder="状态" class="st" @change="refresh">
          <el-option label="全部" value="all" />
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>

        <el-button type="primary" round @click="refresh">搜索</el-button>
      </div>

      <div class="list" v-loading="loading" v-infinite-scroll="loadMore" :infinite-scroll-disabled="disabled">
        <div
          v-for="a in items"
          :key="a.id"
          class="item glass-card"
          :id="`article-${a.id}`"
          :class="{ highlight: highlightId === String(a.id) }"
          @click="router.push(`/article/${a.id}`)"
        >
          <div class="left">
            <div class="title">{{ a.title }}</div>
            <div class="meta">
              <span class="tag">{{ a.status === 1 ? '已发布' : '草稿' }}</span>
              <span class="dot">·</span>
              <span class="cat">{{ a.category }}</span>
              <span class="dot">·</span>
              <span class="views">{{ a.views || 0 }} 阅读</span>
            </div>
            <div v-if="cleanArticleSummary(String(a.summary || ''))" class="summary">{{ cleanArticleSummary(String(a.summary || '')) }}</div>
          </div>
          <div class="right">
            <img
              v-if="a.coverImage"
              class="cover"
              :src="normalizeCoverUrl(a.coverImage) || '/favicon.svg'"
              :alt="a.title || '文章封面'"
              loading="lazy"
              decoding="async"
              @error="(e) => ((e.target as HTMLImageElement).src = '/favicon.svg')"
            />
          </div>
        </div>

        <el-empty v-if="!loading && items.length === 0" description="暂无内容" />
        <div v-if="finished && items.length > 0" class="end">没有更多了</div>
      </div>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { articleApi } from '@/api/article'
import type { Article } from '@/types'
import { Search } from '@element-plus/icons-vue'
import { normalizeCoverUrl } from '@/utils/image'
import { cleanArticleSummary } from '@/utils/articleSummary'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const status = ref<'all' | 0 | 1>('all')
const items = ref<Article[]>([])
const current = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

const highlightId = ref<string>((route.query.highlight as string) || '')

const disabled = ref(false)

const fetchPage = async (page: number) => {
  loading.value = true
  try {
    const res = await articleApi.getMine({
      current: page,
      size: pageSize,
      status: status.value === 'all' ? undefined : status.value,
      keyword: keyword.value
    })
    total.value = res.total
    const records = res.records || []
    if (page === 1) {
      items.value = records
    } else {
      items.value = items.value.concat(records)
    }
    finished.value = items.value.length >= total.value
    disabled.value = finished.value
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  current.value = 1
  finished.value = false
  disabled.value = false
  await fetchPage(1)
  await nextTick()
  scrollToHighlight()
}

const loadMore = async () => {
  if (loading.value || finished.value) return
  current.value += 1
  await fetchPage(current.value)
}

const scrollToHighlight = () => {
  if (!highlightId.value) return
  const el = document.getElementById(`article-${highlightId.value}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setTimeout(() => {
      highlightId.value = ''
    }, 3500)
  }
}

onMounted(async () => {
  if (route.query.published === '1') {
    ElMessage.success({ message: '发布成功', duration: 3000 })
  }
  await fetchPage(1)
  await nextTick()
  scrollToHighlight()
})
</script>

<style scoped lang="scss">
.my-article-container {
  padding-top: 110px;
  padding-bottom: 70px;
  max-width: 1200px;
}
.header {
  text-align: center;
  margin-bottom: 24px;
  h1 { font-size: 38px; margin-bottom: 8px; color: var(--text); }
  p { color: var(--muted); }
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 18px;
  margin-bottom: 18px;
  .kw { flex: 1; }
  .st { width: 140px; }
}
.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  cursor: pointer;
  border: 1px solid var(--border);
  .left { flex: 1; min-width: 0; }
  .right { width: 160px; }
  .title { font-size: 18px; font-weight: 800; color: var(--text); margin-bottom: 8px; }
  .meta { font-size: 12px; color: var(--muted); display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
  .tag { color: var(--primary); font-weight: 700; }
  .dot { opacity: 0.6; }
  .summary {
    color: var(--muted);
    font-size: 14px;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .cover {
    width: 160px;
    height: 96px;
    object-fit: cover;
    border-radius: 12px;
    border: 1px solid var(--border);
  }
}
.highlight {
  border-color: rgba(52, 199, 89, 0.55) !important;
  box-shadow: 0 10px 24px rgba(52, 199, 89, 0.12);
}
.end {
  text-align: center;
  color: var(--muted);
  padding: 16px 0;
}
.serif-text { font-family: 'Playfair Display', serif; }
@media (max-width: 768px) {
  .item { flex-direction: column; }
  .right { width: 100%; }
  .cover { width: 100% !important; height: 180px !important; }
}
</style>
