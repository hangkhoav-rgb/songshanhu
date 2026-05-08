<template>
  <div class="article-map-container glass-card" :style="{ height: `${props.height}px`, borderRadius: `${props.radius}px` }">
    <div class="map-header">
      <el-icon><Location /></el-icon>
      <span>松山湖地理位置</span>
      <span class="coords" v-if="lng && lat">{{ lng.toFixed(4) }}, {{ lat.toFixed(4) }}</span>
    </div>
    <div ref="mapRef" class="map-canvas" :data-map-ready="mapReady ? 'true' : 'false'"></div>
    <div v-if="!lng || !lat" class="map-placeholder">
      <el-empty description="该文章未关联地理位置" :image-size="60" />
    </div>
    <div v-else-if="!sdkReady" class="map-placeholder">
      <el-empty description="地图加载失败，请检查高德 Key 与安全密钥配置" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { loadAMap } from '@/utils/amap'

const props = withDefaults(defineProps<{
  lng?: number
  lat?: number
  title?: string
  height?: number
  radius?: number
}>(), {
  height: 400,
  radius: 12
})

const mapRef = ref<HTMLElement>()
let map: any = null
let marker: any = null
let infoWindow: any = null
const sdkReady = ref(true)
const mapReady = ref(false)

const initMap = () => {
  if (!mapRef.value || !props.lng || !props.lat) return

  loadAMap()
    .then((AMap) => {
      map = new AMap.Map(mapRef.value, {
        zoom: 13,
        center: [props.lng, props.lat],
        mapStyle: 'amap://styles/whitesmoke',
        resizeEnable: true
      })

      marker = new AMap.Marker({
        position: [props.lng, props.lat],
        anchor: 'bottom-center'
      })
      map.add(marker)

      infoWindow = new AMap.InfoWindow({
        isCustom: false,
        content: `<div style="font-size:13px;font-weight:600;color:#1d1d1f;">${props.title || '标记点'}</div>`,
        offset: new AMap.Pixel(0, -28)
      })

      marker.on('click', () => {
        infoWindow.open(map, marker.getPosition())
      })

      map.addControl(new AMap.ToolBar())
      map.addControl(new AMap.Scale())

      map.on('complete', () => {
        mapReady.value = true
      })
    })
    .catch(() => {
      sdkReady.value = false
    })
}

onMounted(() => {
  if (props.lng && props.lat) {
    initMap()
  }
})

onUnmounted(() => {
  map?.destroy?.()
  map = null
  marker = null
  infoWindow = null
})

watch(() => [props.lng, props.lat], () => {
  if (props.lng && props.lat && !map) {
    initMap()
  } else if (map && props.lng && props.lat) {
    map.setCenter([props.lng, props.lat])
    marker?.setPosition?.([props.lng, props.lat])
  }
})
</script>

<style scoped lang="scss">
.article-map-container {
  margin-top: 40px;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  
  .map-header {
    padding: 12px 20px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid var(--border-soft);
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text);
    z-index: 10;
    
    .coords {
      margin-left: auto;
      font-family: monospace;
      font-weight: 400;
      color: var(--muted);
    }
  }
  
  .map-canvas {
    flex: 1;
    width: 100%;
  }
  
  .map-placeholder {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--surface-2);
  }
}
</style>
