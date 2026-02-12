<template>
  <el-dialog
    v-model="visible"
    title="生成图表"
    width="800px"
    destroy-on-close
  >
    <div class="chart-generator">
      <!-- 图表类型选择 -->
      <div class="chart-type-selector">
        <span class="label">选择图表类型：</span>
        <el-radio-group v-model="chartType">
          <el-radio-button label="bar">
            <el-icon><Histogram /></el-icon> 柱状图
          </el-radio-button>
          <el-radio-button label="line">
            <el-icon><TrendCharts /></el-icon> 折线图
          </el-radio-button>
          <el-radio-button label="pie">
            <el-icon><PieChart /></el-icon> 饼图
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 字段选择 -->
      <div class="field-selector">
        <el-form label-width="80px">
          <el-form-item label="X轴字段">
            <el-select v-model="xField" placeholder="选择X轴字段" style="width: 100%">
              <el-option
                v-for="col in columns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Y轴字段">
            <el-select v-model="yField" placeholder="选择Y轴字段" style="width: 100%">
              <el-option
                v-for="col in numericColumns"
                :key="col"
                :label="col"
                :value="col"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="图表标题">
            <el-input v-model="chartTitle" placeholder="输入图表标题" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 图表预览 -->
      <div class="chart-preview">
        <div ref="chartRef" class="chart-container"></div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="exportChart">
        <el-icon><Download /></el-icon> 导出图片
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Histogram, TrendCharts, PieChart, Download } from '@element-plus/icons-vue'
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
            color: '#409eff'
          },
          areaStyle: chartType.value === 'line' ? {} : undefined
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
      color: #606266;
    }
  }

  .field-selector {
    margin-bottom: 20px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
  }

  .chart-preview {
    .chart-container {
      width: 100%;
      height: 400px;
      border: 1px solid #ebeef5;
      border-radius: 8px;
    }
  }
}
</style>
