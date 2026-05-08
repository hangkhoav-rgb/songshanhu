export function cleanArticleSummary(input: string) {
  return String(input || '')
    .replace(/^\s*(?:📊\s*)?可视化图表：.*?(?:图表将在发布后渲染\.{0,3})?\s*/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}
