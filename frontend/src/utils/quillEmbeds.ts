import Quill from 'quill'
import { normalizeChartConfig } from '@/utils/articleChart'

let registered = false

export function registerArticleEmbeds() {
  if (registered) return
  registered = true

  const BlockEmbed = Quill.import('blots/block/embed')

  class EmbeddedChartBlot extends BlockEmbed {
    static blotName = 'embeddedChart'
    static tagName = 'div'
    static className = 'embedded-chart-container'

    static create(value: { id?: string; config?: unknown }) {
      const node = super.create() as HTMLDivElement
      const chartId = String(value?.id || `chart-${Date.now()}`)
      const config = normalizeChartConfig((value?.config || {}) as never)
      node.setAttribute('data-chart-id', chartId)
      node.setAttribute('data-config', JSON.stringify(config))
      node.setAttribute('contenteditable', 'false')
      node.innerHTML = `
        <div class="embedded-chart-canvas"></div>
      `
      return node
    }
  }

  class EmbeddedMapBlot extends BlockEmbed {
    static blotName = 'embeddedMap'
    static tagName = 'div'
    static className = 'embedded-map-placeholder'

    static create(value: { lng?: number; lat?: number }) {
      const node = super.create() as HTMLDivElement
      const lng = Number(value?.lng ?? 0)
      const lat = Number(value?.lat ?? 0)
      node.setAttribute('data-lng', String(lng))
      node.setAttribute('data-lat', String(lat))
      node.setAttribute('contenteditable', 'false')
      node.innerHTML = `
        <div class="embedded-block-title">📍 已绑定地理位置</div>
        <div class="embedded-block-hint">坐标：${lng.toFixed(4)}, ${lat.toFixed(4)}（发布后显示动态地图）</div>
      `
      return node
    }
  }

  Quill.register(EmbeddedChartBlot)
  Quill.register(EmbeddedMapBlot)
}
