import * as echarts from 'echarts'

export type ArticleChartType = 'bar' | 'line' | 'pie' | 'scatter' | 'gauge'

export interface ArticleChartDatum {
  name: string
  value: number
}

export interface ArticleChartConfig {
  title: string
  type: ArticleChartType
  data: ArticleChartDatum[]
}

const CHART_COLORS = ['#0EA5E9', '#10B981', '#F59E0B', '#F97316', '#8B5CF6']

export function escapeChartHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function escapeChartAttr(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function normalizeChartConfig(input: Partial<ArticleChartConfig>) {
  const rawData = Array.isArray(input.data) ? input.data : []
  return {
    title: String(input.title || '未命名图表').trim(),
    type: (input.type || 'bar') as ArticleChartType,
    data: rawData
      .map((item) => ({
        name: String(item?.name ?? '').trim(),
        value: Number(item?.value ?? 0)
      }))
      .filter((item) => item.name && Number.isFinite(item.value))
  } satisfies ArticleChartConfig
}

export function parseChartJson(dataJson: string) {
  const raw = JSON.parse(dataJson)
  if (!Array.isArray(raw)) {
    throw new Error('图表数据必须是数组')
  }
  return raw.map((item) => {
    const name = String(item?.name ?? '').trim()
    const value = Number(item?.value)
    if (!name || !Number.isFinite(value)) {
      throw new Error('每项数据必须包含 name 和数值 value')
    }
    return { name, value }
  }) as ArticleChartDatum[]
}

export function buildEmbeddedChartHtml(chartId: string, config: ArticleChartConfig) {
  const normalized = normalizeChartConfig(config)
  const configAttr = escapeChartAttr(JSON.stringify(normalized))
  return `
    <div class="embedded-chart-container" data-chart-id="${chartId}" data-config="${configAttr}">
      <div class="embedded-chart-canvas"></div>
    </div>
    <p><br></p>
  `
}

export function buildChartOption(config: ArticleChartConfig): echarts.EChartsOption {
  const normalized = normalizeChartConfig(config)
  const isPie = normalized.type === 'pie'
  const isGauge = normalized.type === 'gauge'
  const isScatter = normalized.type === 'scatter'

  const option: echarts.EChartsOption = {
    color: CHART_COLORS,
    title: {
      text: normalized.title,
      left: 'center',
      textStyle: {
        fontSize: 15,
        fontWeight: 600,
        color: '#0f172a'
      }
    },
    grid: isPie || isGauge ? undefined : { left: 44, right: 18, top: 52, bottom: 32, containLabel: true },
    tooltip: isGauge
      ? { formatter: '{a}<br/>{b}: {c}%' }
      : isPie
        ? { trigger: 'item' }
        : { trigger: isScatter ? 'item' : 'axis' },
    xAxis: isPie || isGauge
      ? undefined
      : {
          type: 'category',
          data: normalized.data.map((item) => item.name),
          axisLine: { lineStyle: { color: 'rgba(15,23,42,0.18)' } },
          axisLabel: { color: 'rgba(15,23,42,0.62)' }
        },
    yAxis: isPie || isGauge
      ? undefined
      : {
          type: 'value',
          splitLine: { lineStyle: { color: 'rgba(15,23,42,0.08)' } },
          axisLabel: { color: 'rgba(15,23,42,0.56)' }
        },
    series: [
      isGauge
        ? {
            name: normalized.title,
            type: 'gauge',
            radius: '82%',
            progress: { show: true, roundCap: true, width: 12 },
            axisLine: { lineStyle: { width: 12 } },
            detail: { formatter: '{value}%', color: '#0f172a', fontSize: 18, fontWeight: 700 },
            data: [{ value: normalized.data[0]?.value ?? 0, name: normalized.data[0]?.name || '当前值' }]
          }
        : isPie
          ? {
              name: normalized.title,
              type: 'pie',
              radius: ['36%', '64%'],
              center: ['50%', '56%'],
              data: normalized.data,
              label: { color: '#334155' },
              emphasis: {
                itemStyle: {
                  shadowBlur: 12,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(15, 23, 42, 0.16)'
                }
              }
            }
          : isScatter
            ? {
                name: normalized.title,
                type: 'scatter',
                symbolSize: 14,
                data: normalized.data.map((item, index) => [index + 1, item.value, item.name])
              }
            : {
                name: normalized.title,
                type: normalized.type,
                smooth: normalized.type === 'line',
                barMaxWidth: normalized.type === 'bar' ? 34 : undefined,
                areaStyle: normalized.type === 'line' ? { opacity: 0.12 } : undefined,
                data: normalized.data.map((item) => item.value)
              }
    ]
  }

  if (isScatter) {
    ;(option.xAxis as echarts.XAXisComponentOption) = {
      type: 'category',
      data: normalized.data.map((item) => item.name),
      axisLine: { lineStyle: { color: 'rgba(15,23,42,0.18)' } },
      axisLabel: { color: 'rgba(15,23,42,0.62)' }
    }
    ;(option.series as echarts.SeriesOption[])[0] = {
      name: normalized.title,
      type: 'scatter',
      symbolSize: 16,
      data: normalized.data.map((item) => ({
        name: item.name,
        value: [item.name, item.value]
      }))
    }
  }

  return option
}
