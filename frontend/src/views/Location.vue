<template>
  <ArtLayout force-navbar-scrolled>
    <div class="container loc">
      <div class="glass-card loc-card">
        <header class="header">
          <h1 class="serif-text">定位</h1>
          <p>展示定位精度，并支持手动刷新（已启用缓存）</p>
        </header>

        <div class="panel" v-loading="loading">
          <el-alert
            v-if="errorMessage"
            type="error"
            show-icon
            :closable="false"
            :title="errorMessage"
            class="mb"
          />

          <div class="grid" v-if="loc">
            <div class="item">
              <div class="k">经度</div>
              <div class="v">{{ loc.lng.toFixed(6) }}</div>
            </div>
            <div class="item">
              <div class="k">纬度</div>
              <div class="v">{{ loc.lat.toFixed(6) }}</div>
            </div>
            <div class="item">
              <div class="k">精度(米)</div>
              <div class="v">{{ (loc.accuracy || 0).toFixed(0) }}</div>
            </div>
            <div class="item">
              <div class="k">来源</div>
              <div class="v">{{ loc.source }}</div>
            </div>
            <div class="item">
              <div class="k">更新时间</div>
              <div class="v">{{ new Date(loc.timestamp).toLocaleString() }}</div>
            </div>
          </div>

          <div class="actions">
            <el-button type="primary" :loading="loading" @click="refresh(true)">手动刷新</el-button>
            <el-button :loading="loading" @click="refresh(false)">使用缓存</el-button>
          </div>
        </div>
      </div>
    </div>
  </ArtLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ArtLayout from '@/components/Layout/ArtLayout.vue'
import { getCurrentLocation, type LocationResult } from '@/utils/location'

const loading = ref(false)
const errorMessage = ref('')
const loc = ref<LocationResult | null>(null)

const refresh = async (force: boolean) => {
  loading.value = true
  errorMessage.value = ''
  try {
    loc.value = await getCurrentLocation(force)
    ElMessage.success('定位成功')
  } catch (e: any) {
    errorMessage.value = e?.message || '定位失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => refresh(false))
</script>

<style scoped lang="scss">
.loc {
  padding-top: 110px;
  padding-bottom: 60px;
  max-width: 900px;
}
.loc-card {
  padding: 40px;
  border-radius: 20px;
}
.header {
  text-align: center;
  margin-bottom: 22px;
  h1 { font-size: 34px; color: var(--text); margin-bottom: 6px; }
  p { color: var(--muted); }
}
.panel { margin-top: 16px; }
.mb { margin-bottom: 16px; }
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.item {
  padding: 14px 16px;
  border: 1px solid var(--border-soft);
  border-radius: 14px;
  background: #fff;
  .k { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
  .v { font-size: 16px; font-weight: 700; color: var(--text); font-family: ui-monospace, Consolas, monospace; }
}
.actions {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
.serif-text { font-family: 'Playfair Display', serif; }
@media (max-width: 576px) {
  .glass-card { padding: 22px; }
  .grid { grid-template-columns: 1fr; }
}
</style>
