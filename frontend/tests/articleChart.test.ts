import { describe, expect, it } from 'vitest'
import { buildChartOption, buildEmbeddedChartHtml, normalizeChartConfig, parseChartJson } from '@/utils/articleChart'

describe('articleChart utils', () => {
  it('parses valid chart json', () => {
    const data = parseChartJson('[{"name":"周一","value":12},{"name":"周二","value":18}]')
    expect(data).toHaveLength(2)
    expect(data[0]).toEqual({ name: '周一', value: 12 })
  })

  it('rejects invalid chart json items', () => {
    expect(() => parseChartJson('[{"name":"","value":"abc"}]')).toThrow()
  })

  it('normalizes chart config safely', () => {
    const config = normalizeChartConfig({
      title: '客流趋势',
      type: 'line',
      data: [{ name: '一月', value: 100 }, { name: '二月', value: Number.NaN }]
    })
    expect(config.title).toBe('客流趋势')
    expect(config.data).toHaveLength(1)
  })

  it('builds embedded chart html with config attrs', () => {
    const html = buildEmbeddedChartHtml('chart-1', {
      title: '热度',
      type: 'bar',
      data: [{ name: '阅读', value: 10 }]
    })
    expect(html).toContain('data-chart-id="chart-1"')
    expect(html).toContain('data-config=')
    expect(html).toContain('可视化图表：热度')
  })

  it('builds gauge option', () => {
    const option = buildChartOption({
      title: '完成度',
      type: 'gauge',
      data: [{ name: '完成度', value: 72 }]
    })
    expect(option.series).toBeTruthy()
  })
})

