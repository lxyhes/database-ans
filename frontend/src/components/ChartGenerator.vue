<template>
  <a-modal
    v-model:visible="visible"
    title="生成图表"
    width="800px"
    :mask-closable="false"
    @cancel="visible = false"
  >
    <div class="chart-generator">
      <!-- 图表类型选择 -->
      <div class="chart-type-selector">
        <span class="label">选择图表类型：</span>
        <a-radio-group v-model="chartType" type="button">
          <a-radio value="bar">
            <icon-bar-chart /> 柱状图
          </a-radio>
          <a-radio value="line">
            <icon-dashboard /> 折线图
          </a-radio>
          <a-radio value="pie">
            <icon-tags /> 饼图
          </a-radio>
        </a-radio-group>
      </div>

      <!-- 字段选择 -->
      <div class="field-selector">
        <a-form layout="vertical">
          <a-form-item label="X轴字段">
            <a-select v-model="xField" placeholder="选择X轴字段" style="width: 100%">
              <a-option
                v-for="col in columns"
                :key="col"
                :value="col"
              >{{ col }}</a-option>
            </a-select>
          </a-form-item>
          <a-form-item label="Y轴字段">
            <a-select v-model="yField" placeholder="选择Y轴字段" style="width: 100%">
              <a-option
                v-for="col in numericColumns"
                :key="col"
                :value="col"
              >{{ col }}</a-option>
            </a-select>
          </a-form-item>
          <a-form-item label="图表标题">
            <a-input v-model="chartTitle" placeholder="输入图表标题" />
          </a-form-item>
        </a-form>
      </div>

      <!-- 图表预览 -->
      <div class="chart-preview">
        <div ref="chartRef" class="chart-container"></div>
      </div>
    </div>

    <template #footer>
      <a-space>
        <a-button @click="visible = false">关闭</a-button>
        <a-button type="primary" @click="exportChart">
          <template #icon><icon-download /></template>
          导出图片
        </a-button>
      </a-space>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { IconBarChart, IconDashboard, IconTags, IconDownload } from '@arco-design/web-vue/es/icon'
import * as echarts from 'echarts'

const props = defineProps<{
  modelValue: boolean
  data: any[]
}>()

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const chartType = ref('bar')
const xField = ref('')
const yField = ref('')
const chartTitle = ref('')
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 获取所有列
const columns = computed(() => {
  if (!props.data || props.data.length === 0) return []
  return Object.keys(props.data[0])
})

// 获取数值列
const numericColumns = computed(() => {
  if (!props.data || props.data.length === 0) return []
  return Object.keys(props.data[0]).filter(col => {
    const val = props.data[0][col]
    return typeof val === 'number'
  })
})

// 自动推荐字段
watch(columns, (cols) => {
  if (cols.length > 0 && !xField.value) {
    xField.value = cols[0]
  }
})

watch(numericColumns, (cols) => {
  if (cols.length > 0 && !yField.value) {
    yField.value = cols[0]
  }
})

// 渲染图表
const renderChart = () => {
  if (!chartRef.value || !props.data || !xField.value || !yField.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)

  // 准备数据
  const xData = props.data.map(item => item[xField.value])
  const yData = props.data.map(item => item[yField.value])

  let option: echarts.EChartsOption = {}

  if (chartType.value === 'pie') {
    option = {
      title: {
        text: chartTitle.value || '数据图表',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          type: 'pie',
          radius: '50%',
          data: props.data.map(item => ({
            name: item[xField.value],
            value: item[yField.value]
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    }
  } else {
    option = {
      title: {
        text: chartTitle.value || '数据图表',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: {
          rotate: xData.length > 10 ? 45 : 0
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: yData,
          type: chartType.value,
          smooth: chartType.value === 'line',
          itemStyle: {
            color: '#165dff'
          },
          areaStyle: chartType.value === 'line' ? {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(22, 93, 255, 0.3)' },
                { offset: 1, color: 'rgba(22, 93, 255, 0.05)' }
              ]
            }
          } : undefined
        }
      ]
    }
  }

  chartInstance.setOption(option)
}

// 监听变化重新渲染
watch([chartType, xField, yField, chartTitle, () => props.data], () => {
  nextTick(() => {
    renderChart()
  })
})

// 监听对话框打开
watch(visible, (val) => {
  if (val) {
    nextTick(() => {
      renderChart()
    })
  }
})

// 导出图表
const exportChart = () => {
  if (!chartInstance) return
  
  const url = chartInstance.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  })
  
  const link = document.createElement('a')
  link.download = `${chartTitle.value || 'chart'}.png`
  link.href = url
  link.click()
}

onMounted(() => {
  if (visible.value) {
    nextTick(() => {
      renderChart()
    })
  }
})
</script>

<style scoped lang="scss">
.chart-generator {
  .chart-type-selector {
    margin-bottom: 20px;
    
    .label {
      margin-right: 12px;
      color: var(--color-text-2);
    }
  }

  .field-selector {
    margin-bottom: 20px;
    padding: 16px;
    background: var(--color-fill-2);
    border-radius: 8px;
  }

  .chart-preview {
    .chart-container {
      width: 100%;
      height: 400px;
      border: 1px solid var(--color-neutral-3);
      border-radius: 8px;
    }
  }
}
</style>
