<template>
  <div class="ai-summary glass-card">
    <div class="ai-head">
      <div class="ai-title">AI 智能摘要</div>
      <el-button
        size="small"
        type="primary"
        round
        :disabled="!enabled || !userStore.isLoggedIn || !content"
        :loading="loading"
        @click="generate"
      >
        生成
      </el-button>
    </div>

    <div v-if="!enabled" class="ai-hint muted">未开启</div>
    <div v-else-if="!userStore.isLoggedIn" class="ai-hint">
      <span class="muted">登录后可用</span>
      <el-button link type="primary" @click="goLogin">去登录</el-button>
    </div>
    <div v-else-if="!content" class="ai-hint muted">暂无内容</div>

    <el-collapse-transition>
      <div v-if="summary" class="ai-result">
        <div class="ai-result-head">
          <div class="ai-result-title">摘要</div>
          <el-button link type="primary" @click="expanded = !expanded">{{ expanded ? '收起' : '展开' }}</el-button>
        </div>
        <div class="ai-text" :class="{ clamp: !expanded }">{{ summary }}</div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api/ai'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  title?: string
  content: string
}>()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const enabled = computed(() => String(import.meta.env.VITE_AI_SUMMARY_ENABLED || '').toLowerCase() === 'true')

const loading = ref(false)
const summary = ref('')
const expanded = ref(false)

const goLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

const generate = async () => {
  if (!enabled.value) return
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  const raw = (props.content || '').trim()
  if (!raw) {
    ElMessage.warning('暂无可摘要内容')
    return
  }
  loading.value = true
  try {
    const res = await aiApi.summarize({
      title: props.title,
      content: raw.length > 8000 ? raw.slice(0, 8000) : raw
    })
    summary.value = res.summary || ''
    expanded.value = false
    if (!summary.value) {
      ElMessage.warning('未生成摘要，请重试')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || 'AI 摘要生成失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.ai-summary {
  padding: 12px;
  border: 1px solid var(--border-soft);
  background:
    linear-gradient(135deg, rgba(139, 92, 246, 0.12) 0%, rgba(30, 58, 138, 0.10) 100%),
    rgba(255, 255, 255, 0.70);
  overflow: hidden;
}

.ai-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ai-title {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}

.ai-hint {
  margin-top: 10px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-result {
  margin-top: 10px;
  padding: 10px 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 23, 42, 0.10);
}

.ai-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ai-result-title {
  font-size: 12px;
  font-weight: 800;
  color: #334155;
}

.ai-text {
  font-size: 13px;
  line-height: 1.7;
  color: #0f172a;
  white-space: pre-wrap;
}

.clamp {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
