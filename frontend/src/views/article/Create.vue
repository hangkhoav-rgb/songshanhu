<template>
  <ArtLayout force-navbar-scrolled>
    <div class="create-article-container site-container">
      <div class="card editor-card">
        <header class="editor-header">
          <div class="header-main">
            <h1 class="serif-text">{{ isPreview ? '博文预览' : '创建新博文' }}</h1>
            <p>{{ isPreview ? '确认您的创作呈现效果。' : '记录您的灵感，让创作成为一种艺术。' }}</p>
          </div>
          <div class="header-actions">
            <el-button @click="isPreview = !isPreview">
              <el-icon><View v-if="!isPreview" /><Edit v-else /></el-icon>
              {{ isPreview ? '返回编辑' : '预览效果' }}
            </el-button>
          </div>
        </header>

        <div v-if="isPreview" class="preview-mode">
          <h2 class="preview-title">{{ articleForm.title || '无标题' }}</h2>
          <div class="preview-content-wrapper" ref="previewContentRef">
            <div class="content-body" v-html="articleForm.content"></div>
          </div>
        </div>

        <el-form v-show="!isPreview" :model="articleForm" label-position="top">
          <div class="create-layout">
            <div class="top-row">
              <el-form-item label="文章标题" class="title-item">
                <el-input
                  v-model="articleForm.title"
                  placeholder="请输入富有吸引力的标题..."
                  size="large"
                  class="title-input"
                />
              </el-form-item>

              <el-form-item label="分类" class="category-item">
                <el-select v-model="articleForm.category" placeholder="选择分类" size="large" class="category-control">
                  <el-option label="技术" value="tech" />
                  <el-option label="生活" value="life" />
                  <el-option label="可视化" value="visual" />
                  <el-option label="其他" value="other" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="封面图片">
              <div class="cover-panel">
                <el-tabs v-model="coverMode" class="cover-tabs">
                  <el-tab-pane label="外链" name="url">
                    <el-input
                      v-model="coverUrlInput"
                      placeholder="输入图片 URL（将校验并转存）"
                      size="large"
                      clearable
                    >
                      <template #prefix>
                        <el-icon><Picture /></el-icon>
                      </template>
                      <template #append>
                        <el-button :loading="coverUploading" @click="applyCoverFromUrl">转存</el-button>
                      </template>
                    </el-input>
                  </el-tab-pane>
                  <el-tab-pane label="本地上传" name="file">
                    <div class="cover-upload-row">
                      <input ref="coverFileRef" class="file-input" type="file" accept="image/*" @change="onPickCoverFile" />
                      <el-button plain @click="coverFileRef?.click()">选择图片</el-button>
                      <el-button type="primary" :disabled="!coverFile" :loading="coverUploading" @click="uploadCoverFile">上传</el-button>
                      <div class="tips">支持 JPG/PNG/WebP，最大 2MB</div>
                    </div>
                  </el-tab-pane>
                </el-tabs>

                <div v-if="articleForm.coverImage" class="cover-preview">
                  <img :src="articleForm.coverImage" :alt="articleForm.title || '封面预览'" @error="(e) => ((e.target as HTMLImageElement).src = '/favicon.svg')" />
                </div>
              </div>
            </el-form-item>

              <el-form-item class="compose-item">
              <template #label>
                <div class="compose-label">
                  <span>内容创作</span>
                  <span class="compose-meta muted">
                    {{ contentCharCount }} 字
                    <span class="dot">·</span>
                    自动保存
                  </span>
                </div>
              </template>

              <div class="editor-shell">
                <div class="editor-toolbar">
                  <div class="tool-left">
                    <el-tooltip content="插入图表" placement="top" :show-after="250">
                      <el-button size="small" plain aria-label="插入图表" @click="insertChart">
                        <el-icon><Histogram /></el-icon>
                        图表
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="绑定位置" placement="top" :show-after="250">
                      <el-button size="small" plain aria-label="绑定位置" @click="insertMap">
                        <el-icon><MapLocation /></el-icon>
                        位置
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="插入外链图片（自动转存）" placement="top" :show-after="250">
                      <el-button size="small" plain aria-label="插入外链图片" @click="insertExternalImage">
                        <el-icon><Picture /></el-icon>
                        图片
                      </el-button>
                    </el-tooltip>
                  </div>

                  <div class="tool-right">
                    <span class="tool-hint muted">支持粘贴图片/链接，审核通过后会公开展示</span>
                  </div>
                </div>

                <div v-if="articleForm.charts.length" class="chart-summary">
                  <div class="chart-summary__title">已插入图表</div>
                  <div class="chart-summary__list">
                    <div v-for="chart in articleForm.charts" :key="chart.id" class="chart-chip">
                      <span class="chart-chip__name">{{ chart.title }}</span>
                      <span class="chart-chip__meta">{{ chart.type }} · {{ chart.data.length }} 项</span>
                    </div>
                  </div>
                </div>

                <QuillEditor
                  ref="quillRef"
                  v-model:content="articleForm.content"
                  content-type="html"
                  theme="snow"
                  :options="editorOptions"
                  class="quill-editor quill-editor-large"
                  @focus="handleEditorFocus"
                />
              </div>
            </el-form-item>

            <div class="submit-actions">
              <el-button size="large" round @click="router.back()">取消</el-button>
              <el-button type="primary" size="large" round :loading="publishing" @click="handlePublish">
                提交审核
              </el-button>
            </div>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 图表生成器组件 -->
    <ChartGenerator ref="chartGenRef" @insert="handleInsertChart" />
    <MapPicker ref="mapPickerRef" @select="handleSelectLocation" />
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, Edit, Histogram, MapLocation, Picture } from '@element-plus/icons-vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import ChartGenerator from '@/components/Chart/ChartGenerator.vue'
import MapPicker from '@/components/Map/MapPicker.vue'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { articleApi } from '@/api/article'
import * as echarts from 'echarts'
import { uploadApi } from '@/api/upload'
import { buildChartOption, normalizeChartConfig, type ArticleChartConfig } from '@/utils/articleChart'
import { registerArticleEmbeds } from '@/utils/quillEmbeds'

registerArticleEmbeds()

const router = useRouter()
const publishing = ref(false)
const isPreview = ref(false)
const quillRef = ref()
const chartGenRef = ref()
const mapPickerRef = ref()
const previewChartInstances = ref<echarts.ECharts[]>([])

const handleEditorFocus = () => {
  let tries = 0
  const run = () => {
    const quill = quillRef.value?.getQuill?.()
    if (quill) {
      if (import.meta.env.DEV) {
        ;(window as any).__createQuill = quill
      }
      return
    }
    tries += 1
    if (tries < 8) {
      setTimeout(run, 50)
    }
  }
  run()
}

const articleForm = reactive({
  title: '',
  content: '',
  category: 'visual',
  coverImage: '',
  location: null as [number, number] | null,
  charts: [] as any[]
})

const contentCharCount = computed(() => {
  const html = String(articleForm.content || '')
  if (!html) return 0
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const text = (doc.body.textContent || '').replace(/\s+/g, ' ').trim()
  return text.length
})

const coverMode = ref<'url' | 'file'>('url')
const coverUrlInput = ref('')
const coverUploading = ref(false)
const coverFileRef = ref<HTMLInputElement>()
const coverFile = ref<File | null>(null)

// 本地草稿逻辑
const DRAFT_KEY = 'ssl_blog_draft'
const saveDraft = () => {
  localStorage.setItem(DRAFT_KEY, JSON.stringify(articleForm))
}

const loadDraft = () => {
  const draft = localStorage.getItem(DRAFT_KEY)
  if (draft) {
    Object.assign(articleForm, JSON.parse(draft))
    syncChartsFromContent(articleForm.content)
  }
}

onMounted(() => {
  loadDraft()
  // 每 30 秒自动保存
  const timer = setInterval(saveDraft, 30000)
  onUnmounted(() => clearInterval(timer))
  nextTick(() => {
    window.scrollTo({ top: 0 })
  })
})

const editorOptions = {
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{ 'header': 1 }, { 'header': 2 }],
      [{ 'list': 'ordered' }, { 'list': 'bullet' }],
      [{ 'color': [] }, { 'background': [] }],
      ['link', 'image'],
      ['clean']
    ]
  },
  placeholder: '在这里开始您的创作...'
}

const insertChart = () => {
  chartGenRef.value?.open()
}

const handleInsertChart = (chartConfig: any) => {
  const quill = quillRef.value?.getQuill?.()
  if (!quill) {
    ElMessage.warning('编辑器还未准备好，请稍后再试')
    return
  }
  const range = quill.getSelection(true)
  const chartId = `chart-${Date.now()}`
  const normalized = normalizeChartConfig(chartConfig)
  const insertAt = range ? range.index : quill.getLength()
  quill.insertEmbed(insertAt, 'embeddedChart', { id: chartId, config: normalized }, 'user')
  quill.insertText(insertAt + 1, '\n', 'user')
  quill.setSelection(insertAt + 2, 0, 'silent')
  articleForm.content = quill.root.innerHTML
  syncChartsFromContent(articleForm.content)
  ElMessage.success('图表已插入编辑器')
}

const insertMap = () => {
  mapPickerRef.value?.open()
}

const applyCoverFromUrl = async () => {
  if (!coverUrlInput.value) {
    ElMessage.warning('请输入图片 URL')
    return
  }
  coverUploading.value = true
  try {
    const res = await uploadApi.fetchImage(coverUrlInput.value)
    articleForm.coverImage = res.url
    ElMessage.success('封面已转存')
  } catch (e: any) {
    ElMessage.error(e?.message || '转存失败')
  } finally {
    coverUploading.value = false
  }
}

const onPickCoverFile = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    input.value = ''
    return
  }
  coverFile.value = file
}

const uploadCoverFile = async () => {
  if (!coverFile.value) return
  coverUploading.value = true
  try {
    const res = await uploadApi.uploadImage(coverFile.value)
    articleForm.coverImage = res.url
    ElMessage.success('封面上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    coverUploading.value = false
  }
}

const insertExternalImage = async () => {
  try {
    const url = prompt('输入图片 URL（将进行可访问性校验并转存）：')
    if (!url) return
    const res = await uploadApi.fetchImage(url)
    if (!res?.url) {
      ElMessage.error('图片转存失败，请检查链接是否可访问')
      return
    }
    const quill = quillRef.value.getQuill()
    const range = quill.getSelection()
    const html = `<p><img src="${res.url}" alt="图"/></p>`
    quill.clipboard.dangerouslyPasteHTML(range ? range.index : 0, html)
    ElMessage.success('图片已插入')
  } catch (e) {
    ElMessage.error('转存失败')
  }
}
const handleSelectLocation = (coords: [number, number]) => {
  articleForm.location = coords
  ElMessage.success(`位置已绑定: ${coords[0].toFixed(4)}, ${coords[1].toFixed(4)}`)

  const quill = quillRef.value?.getQuill?.()
  if (!quill) return
  const range = quill.getSelection(true)
  const insertAt = range ? range.index : quill.getLength()
  quill.insertEmbed(insertAt, 'embeddedMap', { lng: coords[0], lat: coords[1] }, 'user')
  quill.insertText(insertAt + 1, '\n', 'user')
  quill.setSelection(insertAt + 2, 0, 'silent')
  articleForm.content = quill.root.innerHTML
}

function syncChartsFromContent(content: string) {
  const html = String(content || '')
  if (!html) {
    articleForm.charts = []
    return
  }

  const doc = new DOMParser().parseFromString(html, 'text/html')
  const nodes = Array.from(doc.querySelectorAll('.embedded-chart-container'))
  articleForm.charts = nodes
    .map((node) => {
      const id = node.getAttribute('data-chart-id') || `chart-${Date.now()}`
      const configStr = node.getAttribute('data-config') || '{}'
      try {
        const config = normalizeChartConfig(JSON.parse(configStr) as Partial<ArticleChartConfig>)
        return { id, ...config }
      } catch {
        return null
      }
    })
    .filter(Boolean) as any[]
}

const handlePublish = async () => {
  if (!articleForm.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!articleForm.content || articleForm.content === '<p><br></p>') {
    ElMessage.warning('文章内容不能为空')
    return
  }

  saveDraft()
  publishing.value = true
  try {
    syncChartsFromContent(articleForm.content)
    const articleId = await articleApi.publish({
      title: articleForm.title,
      content: articleForm.content,
      category: articleForm.category,
      coverImage: articleForm.coverImage,
      longitude: articleForm.location ? articleForm.location[0] : undefined,
      latitude: articleForm.location ? articleForm.location[1] : undefined,
      status: 1
    })
    ElMessage.success({ message: '已提交审核，请等待审核结果', duration: 3000 })
    localStorage.removeItem(DRAFT_KEY)
    router.push({ path: '/article/mine', query: { highlight: String(articleId), published: '1' } })
  } catch (error: any) {
    const msg = error?.message || '提交失败'
    ElMessage.error(msg)
  } finally {
    publishing.value = false
  }
}

// 预览图表渲染逻辑
const previewContentRef = ref<HTMLElement>()
const renderPreviewCharts = () => {
  if (!previewContentRef.value) return

  previewChartInstances.value.forEach((chart) => chart.dispose())
  previewChartInstances.value = []

  const containers = previewContentRef.value.querySelectorAll('.embedded-chart-container')
  containers.forEach((el: any) => {
    const configStr = el.getAttribute('data-config')
    if (configStr) {
      try {
        const config = normalizeChartConfig(JSON.parse(configStr))
        const target = el.querySelector('.embedded-chart-canvas') || el
        target.style.height = '350px'
        target.style.width = '100%'
        const tryInit = (triesLeft: number) => {
          if (target.clientWidth > 0 && target.clientHeight > 0) {
            const chart = echarts.init(target)
            previewChartInstances.value.push(chart)
            chart.setOption(buildChartOption(config), true)
            return
          }
          if (triesLeft <= 0) return
          requestAnimationFrame(() => tryInit(triesLeft - 1))
        }
        tryInit(6)
      } catch {
      }
    }
  })
}

watch(
  () => articleForm.content,
  (content) => {
    syncChartsFromContent(content)
    saveDraft()
  }
)

watch(isPreview, (newVal) => {
  if (newVal) {
    nextTick(() => {
      renderPreviewCharts()
    })
  }
})
</script>

<style scoped lang="scss">
.create-article-container {
  padding-top: 100px;
  padding-bottom: 60px;
  max-width: 1080px;
}

.editor-card {
  padding: 24px;
  background: var(--surface);
  border-radius: var(--radius-card);
  max-width: 960px;
  margin: 0 auto;
}

.editor-header {
  margin-bottom: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .header-main {
    h1 { font-size: 36px; margin-bottom: 8px; color: var(--text); }
    p { color: var(--muted); font-size: 16px; }
  }
}

.preview-mode {
  padding: 20px 0;
  
  .preview-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 30px;
    color: var(--text);
    text-align: center;
  }
  
  .preview-content-wrapper {
    background: #fff;
    padding: 40px;
    border-radius: 12px;
    min-height: 400px;
    border: 1px solid var(--border-soft);
  }


  .content-body {
    :deep(.embedded-chart-container),
    :deep(.embedded-map-placeholder) {
      margin: 18px 0;
      padding: 16px;
      border-radius: 12px;
      border: 1px solid var(--border);
      background: rgba(14, 165, 233, 0.06);
    }
    :deep(.embedded-map-placeholder) {
      border-style: dashed;
      border-color: rgba(14, 165, 233, 0.45);
    }
    :deep(.embedded-block-title) {
      font-weight: 800;
      color: var(--text);
      margin-bottom: 6px;
    }
    :deep(.embedded-block-hint) {
      font-size: 12px;
      color: var(--muted);
    }
    :deep(.embedded-chart-container .embedded-block-title),
    :deep(.embedded-chart-container .embedded-block-hint) {
      display: none;
    }
  }
}

.title-input {
  :deep(.el-input__wrapper) {
    box-shadow: none !important;
    border-bottom: 2px solid var(--border-soft);
    border-radius: 0;
    padding: 0;
    &:hover, &.is-focus {
      border-bottom-color: var(--primary);
    }
    .el-input__inner {
      font-size: 24px;
      font-weight: 700;
      height: 60px;
    }
  }
}

.create-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.top-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 200px;
  gap: 16px;
  align-items: end;
}

.category-control {
  width: 100%;
}

.cover-panel {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.70);
}

.compose-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.compose-meta {
  font-size: 12px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.editor-shell {
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 10px 24px rgba(15, 23, 42, 0.06);
  transition: box-shadow 0.3s var(--ease), border-color 0.3s var(--ease);
}

.editor-shell:focus-within {
  border-color: rgba(14, 165, 233, 0.55);
  box-shadow: 0 18px 44px rgba(14, 165, 233, 0.12), 0 10px 26px rgba(15, 23, 42, 0.12);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92) 0%, rgba(255, 255, 255, 0.88) 100%);
  border-bottom: 1px solid var(--border-soft);
}

.tool-left {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-right {
  min-width: 0;
  display: flex;
  justify-content: flex-end;
}

.tool-hint {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-left :deep(.el-button) {
  border-radius: 10px;
  border-color: rgba(15, 23, 42, 0.10);
}

.tool-left :deep(.el-button > span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tool-left :deep(.el-button:hover) {
  border-color: rgba(14, 165, 233, 0.40);
}

.tool-left :deep(.el-button:focus-visible) {
  outline: 2px solid rgba(14, 165, 233, 0.55);
  outline-offset: 2px;
}

.chart-summary {
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-soft);
  background: rgba(14, 165, 233, 0.04);
}

.chart-summary__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 10px;
}

.chart-summary__list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chart-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(14, 165, 233, 0.14);
  background: rgba(255, 255, 255, 0.86);
}

.chart-chip__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.chart-chip__meta {
  font-size: 12px;
  color: var(--muted);
}

.quill-editor {
  min-height: 400px;
  :deep(.ql-toolbar.ql-snow) {
    border: none;
    border-bottom: 1px solid var(--border-soft);
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.86);
  }
  :deep(.ql-container.ql-snow) {
    border: none;
    font-size: 16px;
    font-family: inherit;
  }
  :deep(.ql-editor) {
    min-height: 400px;
    padding: 24px;
    line-height: 1.8;
    caret-color: var(--primary);
  }
  :deep(.embedded-chart-container),
  :deep(.embedded-map-placeholder) {
    margin: 18px 0;
    padding: 16px;
    border-radius: 12px;
    border: 1px solid var(--border);
    background: rgba(14, 165, 233, 0.06);
  }
  :deep(.embedded-map-placeholder) {
    border-style: dashed;
    border-color: rgba(14, 165, 233, 0.45);
  }
  :deep(.embedded-block-title) {
    font-weight: 800;
    color: var(--text);
    margin-bottom: 6px;
  }
  :deep(.embedded-block-hint) {
    font-size: 12px;
    color: var(--muted);
  }
}

.quill-editor-large {
  min-height: clamp(560px, 76vh, 980px);
}

.quill-editor-large {
  :deep(.ql-editor) {
    min-height: clamp(560px, 76vh, 980px);
    padding: 26px;
  }
}

.submit-actions {
  margin-top: 28px;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.cover-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  .file-input { display: none; }
  .tips { color: var(--muted); font-size: 12px; }
}

.cover-preview {
  margin-top: 14px;
  img {
    width: 100%;
    height: 220px;
    object-fit: cover;
    border-radius: 12px;
    border: 1px solid var(--border);
  }
}

.serif-text { font-family: 'Playfair Display', serif; }

@media (max-width: 992px) {
  .top-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .create-article-container {
    padding-left: 16px;
    padding-right: 16px;
  }
  .editor-card {
    padding: 18px;
  }
}
</style>
