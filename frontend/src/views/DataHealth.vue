<template>
  <div class="data-health">
    <a-row :gutter="20">
      <a-col :span="6">
        <a-card title="数据源选择" class="source-card">
          <a-list :bordered="false" size="small">
            <a-list-item 
              v-for="ds in dataSources" 
              :key="ds.id"
              :class="{ active: selectedDataSource === ds.id }"
              @click="selectDataSource(ds.id)"
            >
              <a-list-item-meta :title="ds.name" :description="ds.type" />
              <template #actions>
                <a-tag v-if="ds.healthScore" :color="getHealthColor(ds.healthScore)">
                  {{ ds.healthScore }}分
                </a-tag>
              </template>
            </a-list-item>
          </a-list>
        </a-card>
        
        <a-card v-if="tables.length > 0" title="数据表列表" class="table-card">
          <a-input-search v-model="tableFilter" placeholder="搜索表名" allow-clear />
          <a-list :bordered="false" size="small" class="table-list">
            <a-list-item 
              v-for="table in filteredTables" 
              :key="table.name"
              :class="{ active: selectedTable === table.name }"
              @click="selectTable(table.name)"
            >
              <a-list-item-meta :title="table.name">
                <template #avatar>
                  <icon-apps />
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-tag v-if="table.healthScore" :color="getHealthColor(table.healthScore)" size="small">
                  {{ table.healthScore }}
                </a-tag>
              </template>
            </a-list-item>
          </a-list>
        </a-card>
      </a-col>
      
      <a-col :span="18">
        <a-card v-if="!selectedDataSource" class="empty-card">
          <a-empty description="请选择数据源开始健康体检" />
        </a-card>
        
        <template v-else>
          <a-card class="overview-card">
            <template #title>
              <div class="card-header">
                <span>健康概览</span>
                <a-space>
                  <a-button type="primary" :loading="checking" @click="runHealthCheck">
                    <template #icon><icon-refresh /></template>
                    执行体检
                  </a-button>
                  <a-button @click="exportReport">
                    <template #icon><icon-download /></template>
                    导出报告
                  </a-button>
                </a-space>
              </div>
            </template>
            
            <a-row :gutter="20" class="health-stats">
              <a-col :span="6">
                <div class="health-score-card">
                  <a-progress 
                    type="circle" 
                    :percent="healthOverview.score || 0" 
                    :color="getHealthColor(healthOverview.score)"
                    :size="100"
                  />
                  <div class="score-label">综合健康评分</div>
                </div>
              </a-col>
              <a-col :span="6">
                <a-statistic title="完整性" :value="healthOverview.completeness || 0" suffix="%" :value-style="{ color: '#00b42a' }" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="准确性" :value="healthOverview.accuracy || 0" suffix="%" :value-style="{ color: '#165dff' }" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="一致性" :value="healthOverview.consistency || 0" suffix="%" :value-style="{ color: '#ff7d00' }" />
              </a-col>
            </a-row>
          </a-card>
          
          <a-card v-if="selectedTable" title="字段健康详情" class="detail-card">
            <a-table :data="fieldHealth" :pagination="false">
              <a-table-column title="字段名" data-index="name" :width="150" />
              <a-table-column title="类型" data-index="type" :width="100" />
              <a-table-column title="完整率" :width="120">
                <template #cell="{ record }">
                  <a-progress :percent="record.completeness" :color="getHealthColor(record.completeness)" size="small" />
                </template>
              </a-table-column>
              <a-table-column title="唯一值" data-index="distinctCount" :width="100" />
              <a-table-column title="空值数" data-index="nullCount" :width="100">
                <template #cell="{ record }">
                  <span :class="{ warning: record.nullCount > 0 }">{{ record.nullCount }}</span>
                </template>
              </a-table-column>
              <a-table-column title="异常值" data-index="anomalyCount" :width="100">
                <template #cell="{ record }">
                  <span :class="{ danger: record.anomalyCount > 0 }">{{ record.anomalyCount }}</span>
                </template>
              </a-table-column>
              <a-table-column title="问题" :ellipsis="true">
                <template #cell="{ record }">
                  <a-tag v-for="issue in record.issues" :key="issue" color="red" size="small">{{ issue }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column title="建议" :ellipsis="true">
                <template #cell="{ record }">
                  <span class="suggestion">{{ record.suggestion }}</span>
                </template>
              </a-table-column>
            </a-table>
          </a-card>
          
          <a-card v-else title="问题列表" class="issues-card">
            <a-table :data="issues" :pagination="{ pageSize: 10 }">
              <a-table-column title="级别" :width="80">
                <template #cell="{ record }">
                  <a-tag :color="getIssueColor(record.level)">{{ record.level }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column title="表名" data-index="tableName" :width="150" />
              <a-table-column title="字段" data-index="columnName" :width="120" />
              <a-table-column title="问题类型" data-index="issueType" :width="120" />
              <a-table-column title="问题描述" data-index="description" :ellipsis="true" />
              <a-table-column title="影响范围" :width="100">
                <template #cell="{ record }">{{ record.affectedRows }} 行</template>
              </a-table-column>
              <a-table-column title="建议" :ellipsis="true">
                <template #cell="{ record }">
                  <span class="suggestion">{{ record.suggestion }}</span>
                </template>
              </a-table-column>
            </a-table>
          </a-card>
          
          <a-card title="健康趋势" class="trend-card">
            <v-chart :option="trendChartOption" autoresize style="height: 300px" />
          </a-card>
        </template>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconRefresh,
  IconDownload,
  IconApps
} from '@arco-design/web-vue/es/icon'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '@/utils/request'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const dataSources = ref<any[]>([])
const tables = ref<any[]>([])
const selectedDataSource = ref<number | null>(null)
const selectedTable = ref<string | null>(null)
const tableFilter = ref('')
const checking = ref(false)
const healthOverview = ref<any>({})
const fieldHealth = ref<any[]>([])
const issues = ref<any[]>([])
const trendData = ref<any[]>([])

const filteredTables = computed(() => {
  if (!tableFilter.value) return tables.value
  return tables.value.filter(t => t.name.toLowerCase().includes(tableFilter.value.toLowerCase()))
})

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['完整性', '准确性', '一致性'] },
  xAxis: {
    type: 'category',
    data: trendData.value.map(d => d.date)
  },
  yAxis: { type: 'value', min: 0, max: 100 },
  series: [
    {
      name: '完整性',
      type: 'line',
      data: trendData.value.map(d => d.completeness),
      smooth: true,
      itemStyle: { color: '#00b42a' }
    },
    {
      name: '准确性',
      type: 'line',
      data: trendData.value.map(d => d.accuracy),
      smooth: true,
      itemStyle: { color: '#165dff' }
    },
    {
      name: '一致性',
      type: 'line',
      data: trendData.value.map(d => d.consistency),
      smooth: true,
      itemStyle: { color: '#ff7d00' }
    }
  ]
}))

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data || res
  } catch (error) {
    console.error('加载数据源失败', error)
  }
}

const selectDataSource = async (id: number) => {
  selectedDataSource.value = id
  selectedTable.value = null
  
  try {
    const res = await request.get('/api/data/tables', { params: { dataSourceId: id } })
    tables.value = (res.data || res).map((name: string) => ({ name, healthScore: null }))
    loadHealthOverview()
  } catch (error) {
    console.error('加载表列表失败', error)
  }
}

const selectTable = async (name: string) => {
  selectedTable.value = name
  await loadFieldHealth()
}

const loadHealthOverview = async () => {
  if (!selectedDataSource.value) return
  
  try {
    const res = await request.get(`/api/health/overview/${selectedDataSource.value}`)
    if (res.success) {
      healthOverview.value = res.data.overview || {}
      issues.value = res.data.issues || []
      
      tables.value = tables.value.map(t => ({
        ...t,
        healthScore: res.data.tableScores?.[t.name] || null
      }))
    }
  } catch (error) {
    console.error('加载健康概览失败', error)
  }
}

const loadFieldHealth = async () => {
  if (!selectedDataSource.value || !selectedTable.value) return
  
  try {
    const res = await request.get(`/api/health/table/${selectedDataSource.value}/${selectedTable.value}`)
    if (res.success) {
      fieldHealth.value = res.data.fields || []
    }
  } catch (error) {
    console.error('加载字段健康失败', error)
  }
}

const runHealthCheck = async () => {
  if (!selectedDataSource.value) return
  
  checking.value = true
  try {
    const params: any = { dataSourceId: selectedDataSource.value }
    if (selectedTable.value) params.tableName = selectedTable.value
    
    const res = await request.post('/api/health/check', params)
    if (res.success) {
      Message.success('体检完成')
      loadHealthOverview()
      if (selectedTable.value) loadFieldHealth()
      loadTrendData()
    } else {
      Message.error(res.message || '体检失败')
    }
  } catch (error) {
    Message.error('体检失败')
  } finally {
    checking.value = false
  }
}

const loadTrendData = async () => {
  if (!selectedDataSource.value) return
  
  try {
    const res = await request.get(`/api/health/trend/${selectedDataSource.value}`)
    if (res.success) {
      trendData.value = res.data || []
    }
  } catch (error) {
    console.error('加载趋势数据失败', error)
  }
}

const exportReport = async () => {
  if (!selectedDataSource.value) return
  
  try {
    const res = await request.get(`/api/health/export/${selectedDataSource.value}`, {
      responseType: 'blob'
    })
    
    const blob = new Blob([res], { type: 'application/pdf' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `健康报告_${new Date().toISOString().split('T')[0]}.pdf`
    link.click()
    Message.success('导出成功')
  } catch (error) {
    Message.error('导出失败')
  }
}

const getHealthColor = (score: number) => {
  if (score >= 80) return '#00b42a'
  if (score >= 60) return '#ff7d00'
  return '#f53f3f'
}

const getIssueColor = (level: string) => {
  const colors: Record<string, string> = {
    HIGH: 'red',
    MEDIUM: 'orange',
    LOW: 'blue'
  }
  return colors[level] || 'gray'
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped>
.data-health {
  height: 100%;
}

.source-card {
  margin-bottom: 20px;
}

.source-card :deep(.arco-list-item),
.table-card :deep(.arco-list-item) {
  cursor: pointer;
  transition: background 0.2s;
}

.source-card :deep(.arco-list-item:hover),
.table-card :deep(.arco-list-item:hover),
.source-card :deep(.arco-list-item.active),
.table-card :deep(.arco-list-item.active) {
  background: #e8f3ff;
}

.table-card {
  margin-bottom: 20px;
}

.table-list {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 12px;
}

.empty-card {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overview-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.health-stats {
  margin-top: 20px;
}

.health-score-card {
  text-align: center;
}

.score-label {
  margin-top: 12px;
  font-size: 14px;
  color: #4e5969;
}

.detail-card,
.issues-card {
  margin-bottom: 20px;
}

.warning {
  color: #ff7d00;
}

.danger {
  color: #f53f3f;
}

.suggestion {
  font-size: 12px;
  color: #86909c;
}

.trend-card {
  margin-bottom: 20px;
}
</style>
