<template>
  <el-dialog
    v-model="visible"
    title="选择地理位置"
    width="800px"
    destroy-on-close
    class="map-picker-dialog"
  >
    <div class="map-picker-body">
      <div class="search-box">
        <el-input 
          v-model="searchKey" 
          placeholder="搜索松山湖区域地点..." 
          @keyup.enter="searchLocation"
        >
          <template #append>
            <el-button @click="searchLocation"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </div>
      <div ref="mapRef" class="picker-map-canvas"></div>
      <div class="coords-info" v-if="selectedCoords">
        已选择: <span>{{ selectedCoords[0].toFixed(6) }}, {{ selectedCoords[1].toFixed(6) }}</span>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedCoords" @click="confirmSelection">
          确认绑定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onUnmounted, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { loadAMap } from '@/utils/amap'
import { getGaodeMapEnv } from '../../../gaode-map.config'

const visible = ref(false)
const searchKey = ref('')
const mapRef = ref<HTMLElement>()
const selectedCoords = ref<[number, number] | null>(null)
let map: any = null
let marker: any = null

const open = () => {
  visible.value = true
  nextTick(() => {
    initMap()
  })
}

const initMap = () => {
  if (!mapRef.value) return
  map?.destroy?.()
  map = null
  marker = null

  loadAMap()
    .then((AMap) => {
      map = new AMap.Map(mapRef.value, {
        zoom: 13,
        center: [113.882, 22.884],
        mapStyle: 'amap://styles/normal',
        resizeEnable: true
      })

      map.addControl(new AMap.ToolBar())

      map.on('click', (e: any) => {
        const lng = e.lnglat.getLng()
        const lat = e.lnglat.getLat()
        selectedCoords.value = [lng, lat]

        if (marker) {
          marker.setPosition([lng, lat])
        } else {
          marker = new AMap.Marker({
            position: [lng, lat],
            draggable: true,
            anchor: 'bottom-center'
          })
          map.add(marker)

          marker.on('dragend', (ev: any) => {
            const p = ev.lnglat
            selectedCoords.value = [p.getLng(), p.getLat()]
          })
        }
      })
    })
    .catch(() => {
      const { key, securityJsCode } = getGaodeMapEnv()
      const host = location.host
      if (!key || !securityJsCode) {
        ElMessage.warning('未配置高德 Key/安全密钥，请先在 .env.development 填写 VITE_AMAP_KEY 和 VITE_AMAP_SECURITY_JS_CODE')
        return
      }
      ElMessage.warning(`高德地图加载失败：请在高德控制台将域名白名单加入 ${host}，并确认 securityJsCode 与该 Key 匹配`)
    })
}

const searchLocation = () => {
  if (searchKey.value.includes('松山湖')) {
    map?.setZoomAndCenter?.(15, [113.882, 22.884])
  }
}

const emit = defineEmits(['select'])
const confirmSelection = () => {
  emit('select', selectedCoords.value)
  visible.value = false
}

defineExpose({ open })

onUnmounted(() => {
  map?.destroy?.()
})
</script>

<style scoped lang="scss">
.map-picker-body {
  height: 500px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  
  .search-box {
    position: absolute;
    top: 16px;
    left: 16px;
    right: 16px;
    z-index: 100;
    max-width: 400px;
  }
  
  .picker-map-canvas {
    flex: 1;
    border-radius: 8px;
    border: 1px solid var(--border);
  }
  
  .coords-info {
    font-size: 13px;
    color: var(--muted);
    span { color: var(--text); font-weight: 600; font-family: monospace; }
  }
}
</style>
