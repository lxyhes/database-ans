<template>
  <div class="dashboard">
    <!-- 指标卡片 -->
    <el-row :gutter="20" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-icon" style="background: #409EFF;">
            <el-icon size="24" color="#fff"><Money /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">¥{{ formatNumber(metrics.totalSales) }}</div>
            <div class="metric-label">总销售额</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-icon" style="background: #67C23A;">
            <el-icon size="24" color="#fff"><ShoppingCart /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ formatNumber(metrics.orderCount) }}</div>
            <div class="metric-label">订单数量</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-icon" style="background: #E6A23C;">
            <el-icon size="24" color="#fff"><User /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ formatNumber(metrics.customerCount) }}</div>
            <div class="metric-label">客户数量</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-icon" style="background: #F56C6C;">
            <el-icon size="24" color="#fff"><Goods /></el-icon>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ formatNumber(metrics.productCount) }}</div>
            <div class="metric-label">产品数量</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>销售趋势</span>
              <el-tag type="info">近7天</el-tag>
            </div>
          </template>
          <v-chart class="chart" :option="salesTrendOption" autoresize />
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>各地区销售对比</span>
            </div>
          </template>
          <v-chart class="chart" :option="regionSalesOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>产品销售占比</span>
            </div>
          </template>
          <v-chart class="chart" :option="productSalesOption" autoresize />
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>客户购买频次</span>
            </div>
          </template>
          <v-chart class="chart" :option="customerPurchaseOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
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
import { ElMessage } from 'element-plus'

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

// 销售趋势图配置
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
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ]
      }
    }
  }]
})

// 地区销售对比图配置
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
    itemStyle: { color: '#409EFF' }
  }]
})

// 产品销售占比图配置
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

// 客户购买频次图配置
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
          { offset: 0, color: '#67C23A' },
          { offset: 1, color: '#95D475' }
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
      // 更新数据
      metrics.value = result.data.metrics || metrics.value
      // 可以在这里更新图表数据
    }
  } catch (error) {
    ElMessage.error('加载仪表盘数据失败')
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.metrics-row {
  margin-bottom: 20px;
}

.metric-card {
  display: flex;
  align-items: center;
  padding: 10px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.metric-content {
  flex: 1;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart {
  height: 320px;
}
</style>
