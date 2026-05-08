
<template>
  <el-dialog
    v-model="visible"
    title="生成可视化图表"
    width="800px"
    destroy-on-close
    class="chart-generator-dialog"
  >
    <div class="generator-layout">
      <!-- 左侧表单配置 -->
      <div class="config-side">
        <el-form :model="config" label-position="top">
          <el-form-item label="图表标题">
            <el-input v-model="config.title" placeholder="如：松山湖客流分析" />
          </el-form-item>
          
          <el-form-item label="图表类型">
            <el-select v-model="config.type" class="w-full">
              <el-option label="柱状图 (Bar)" value="bar" />
              <el-option label="折线图 (Line)" value="line" />
              <el-option label="饼图 (Pie)" value="pie" />
              <el-option label="散点图 (Scatter)" value="scatter" />
              <el-option label="仪表盘 (Gauge)" value="gauge" />
            </el-select>
          </el-form-item>

          <el-form-item label="数据配置 (JSON 格式)">
            <el-input
              v-model="config.dataJson"
              type="textarea"
              :rows="8"
              placeholder='[{"name": "周一", "value": 120}, ...]'
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 右侧预览区域 -->
      <div class="preview-side">
        <div class="preview-header">实时预览</div>
        <div ref="chartRef" class="chart-container"></div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">插入文章</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { buildChartOption, parseChartJson } from '@/utils/articleChart'

const visible = ref(false)
const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const config = reactive({
  title: '示例图表',
  type: 'bar',
  dataJson: JSON.stringify([
    { name: '一月', value: 400 },
    { name: '二月', value: 300 },
    { name: '三月', value: 600 },
    { name: '四月', value: 800 }
  ], null, 2)
})

const emit = defineEmits(['insert'])

const open = () => {
  visible.value = true
  nextTick(() => {
    initChart()
  })
}

const initChart = () => {
  if (chartRef.value) {
    chartInstance?.dispose()
    chartInstance = echarts.init(chartRef.value)
    updateChart()
  }
}

const updateChart = () => {
  if (!chartInstance) return
  
  try {
    const data = parseChartJson(config.dataJson)
    chartInstance.setOption(
      buildChartOption({
        title: config.title,
        type: config.type as any,
        data
      }),
      true
    )
  } catch (e) {
    // 解析 JSON 出错时不更新图表
  }
}

watch(config, () => {
  updateChart()
}, { deep: true })

const handleConfirm = () => {
  try {
    const data = parseChartJson(config.dataJson)
    emit('insert', {
      title: config.title,
      type: config.type,
      data
    })
    visible.value = false
  } catch (e) {
    ElMessage.error((e as Error)?.message || '数据格式错误，请检查 JSON 语法')
  }
}

watch(visible, (val) => {
  if (!val) {
    chartInstance?.dispose()
    chartInstance = null
  }
})

onBeforeUnmount(() => {
  chartInstance?.dispose()
  chartInstance = null
})

defineExpose({ open })
</script>

<style scoped lang="scss">
.generator-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
}

.preview-side {
  background: var(--surface-2);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  
  .preview-header {
    font-size: 14px;
    font-weight: 600;
    color: var(--muted);
    margin-bottom: 16px;
  }
  
  .chart-container {
    flex: 1;
    min-height: 300px;
  }
}

.w-full { width: 100%; }
</style>
