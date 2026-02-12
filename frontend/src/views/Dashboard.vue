<template>
  <div class="dashboard">
    <!-- 指标卡片 -->
    <div class="metrics-row">
      <div class="metric-card">
        <div class="metric-icon" style="background: #165dff;">
          <icon-tag :size="24" />
        </div>
        <div class="metric-content">
          <div class="metric-value">¥{{ formatNumber(metrics.totalSales) }}</div>
          <div class="metric-label">总销售额</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon" style="background: #00b42a;">
          <icon-gift :size="24" />
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(metrics.orderCount) }}</div>
          <div class="metric-label">订单数量</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon" style="background: #ff7d00;">
          <icon-user :size="24" />
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(metrics.customerCount) }}</div>
          <div class="metric-label">客户数量</div>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon" style="background: #f53f3f;">
          <icon-apps :size="24" />
        </div>
        <div class="metric-content">
          <div class="metric-value">{{ formatNumber(metrics.productCount) }}</div>
          <div class="metric-label">产品数量</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">
          <span>销售趋势</span>
          <a-tag color="arcoblue">近7天</a-tag>
        </div>
        <v-chart class="chart" :option="salesTrendOption" autoresize />
      </div>
      
      <div class="chart-card">
        <div class="card-header">
          <span>各地区销售对比</span>
        </div>
        <v-chart class="chart" :option="regionSalesOption" autoresize />
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">
          <span>产品销售占比</span>
        </div>
        <v-chart class="chart" :option="productSalesOption" autoresize />
      </div>
      
      <div class="chart-card">
        <div class="card-header">
          <span>客户购买频次</span>
        </div>
        <v-chart class="chart" :option="customerPurchaseOption" autoresize />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  ToolboxComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { Message } from '@arco-design/web-vue'
import { IconTag, IconGift, IconUser, IconApps } from '@arco-design/web-vue/es/icon'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  ToolboxComponent
])

const metrics = ref({
  totalSales: 0,
  orderCount: 0,
  customerCount: 0,
  productCount: 0
})

const salesTrendOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value' },
  series: [{
    data: [820, 932, 901, 934, 1290, 1330, 1320],
    type: 'line',
    smooth: true,
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(22, 93, 255, 0.3)' },
          { offset: 1, color: 'rgba(22, 93, 255, 0.05)' }
        ]
      }
    }
  }]
})

const regionSalesOption = ref({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: {
    type: 'category',
    data: ['北京', '上海', '广州', '深圳', '杭州', '成都']
  },
  yAxis: { type: 'value' },
  series: [{
    data: [320, 302, 301, 334, 390, 330],
    type: 'bar',
    itemStyle: { color: '#165dff' }
  }]
})

const productSalesOption = ref({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: {
      borderRadius: 10,
      borderColor: '#fff',
      borderWidth: 2
    },
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 16, fontWeight: 'bold' }
    },
    data: [
      { value: 1048, name: '笔记本电脑' },
      { value: 735, name: '无线鼠标' },
      { value: 580, name: '机械键盘' },
      { value: 484, name: '显示器' },
      { value: 300, name: '移动硬盘' }
    ]
  }]
})

const customerPurchaseOption = ref({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: {
    type: 'category',
    data: ['1次', '2次', '3次', '4次', '5次及以上']
  },
  yAxis: { type: 'value' },
  series: [{
    data: [150, 230, 224, 218, 135],
    type: 'bar',
    itemStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: '#00b42a' },
          { offset: 1, color: '#7be188' }
        ]
      }
    }
  }]
})

const formatNumber = (num: number) => {
  return num.toLocaleString('zh-CN')
}

const loadDashboardData = async () => {
  try {
    const result = await window.electronAPI.getDashboardData()
    if (result.success) {
      metrics.value = result.data.metrics || metrics.value
    }
  } catch (error) {
    Message.error('加载仪表盘数据失败')
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  flex-shrink: 0;
}

.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: #fff;
  flex-shrink: 0;
}

.metric-content {
  flex: 1;
  min-width: 0;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-1);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.metric-label {
  font-size: 14px;
  color: var(--color-text-3);
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  flex: 1;
  min-height: 0;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 500;
  color: var(--color-text-1);
  flex-shrink: 0;
}

.chart {
  flex: 1;
  min-height: 250px;
}
</style>
