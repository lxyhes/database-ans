<template>
  <div class="sensitive-data-manage">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>敏感数据管理</span>
          <div class="header-actions">
            <a-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange" style="width: 200px;">
              <a-option
                v-for="ds in dataSources"
                :key="ds.id"
                :value="ds.id"
              >{{ ds.name }}</a-option>
            </a-select>
            <a-select v-model="selectedTable" placeholder="选择数据表" allow-clear @change="loadSensitiveColumns" style="width: 200px;">
              <a-option
                v-for="table in tables"
                :key="table"
                :value="table"
              >{{ table }}</a-option>
            </a-select>
            <a-button type="primary" @click="scanTable" :loading="scanning" :disabled="!selectedTable">
              <template #icon><icon-search /></template>
              扫描敏感数据
            </a-button>
          </div>
        </div>
      </template>

      <!-- 概览统计 -->
      <a-row :gutter="20" class="overview" v-if="overview">
        <a-col :span="6">
          <a-statistic title="敏感字段总数" :value="overview.totalSensitiveColumns || 0" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已脱敏字段" :value="overview.maskedColumns || 0" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="涉及表数" :value="Object.keys(overview.tableDistribution || {}).length" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="敏感类型数" :value="Object.keys(overview.typeDistribution || {}).length" />
        </a-col>
      </a-row>

      <!-- 敏感字段列表 -->
      <a-table :data="sensitiveColumns" :loading="loading" class="sensitive-table">
        <a-table-column title="#" :width="50">
          <template #cell="{ rowIndex }">{{ rowIndex + 1 }}</template>
        </a-table-column>
        <a-table-column title="表名" data-index="tableName" />
        <a-table-column title="字段名" data-index="columnName" />
        <a-table-column title="敏感类型">
          <template #cell="{ record }">
            <a-tag :color="getSensitiveTypeColor(record.sensitiveType)">
              {{ getSensitiveTypeText(record.sensitiveType) }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="检测方式">
          <template #cell="{ record }">
            <a-tag size="small" :color="record.detectionMethod === 'COLUMN_NAME' ? 'arcoblue' : 'green'">
              {{ record.detectionMethod === 'COLUMN_NAME' ? '字段名' : '内容采样' }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="置信度">
          <template #cell="{ record }">
            <a-progress :percent="Math.round(record.confidence * 100)" />
          </template>
        </a-table-column>
        <a-table-column title="脱敏状态">
          <template #cell="{ record }">
            <a-switch
              v-model="record.isMasked"
              @change="updateMaskConfig(record)"
            >
              <template #checked>已脱敏</template>
              <template #unchecked>未脱敏</template>
            </a-switch>
          </template>
        </a-table-column>
        <a-table-column title="脱敏示例">
          <template #cell="{ record }">
            <span class="mask-example">{{ getMaskExample(record.sensitiveType) }}</span>
          </template>
        </a-table-column>
        <a-table-column title="操作" :width="120">
          <template #cell="{ record }">
            <a-button type="text" status="danger" @click="deleteColumn(record.id)">
              <template #icon><icon-delete /></template>
            </a-button>
          </template>
        </a-table-column>
      </a-table>

      <!-- 类型分布图表 -->
      <a-row :gutter="20" class="charts" v-if="overview?.typeDistribution">
        <a-col :span="12">
          <div ref="typeChartRef" class="chart"></div>
        </a-col>
        <a-col :span="12">
          <div ref="tableChartRef" class="chart"></div>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconSearch, IconDelete } from '@arco-design/web-vue/es/icon'
import * as echarts from 'echarts'
import request from '@/utils/request'

const dataSources = ref([])
const tables = ref([])
const selectedDataSource = ref(null)
const selectedTable = ref(null)
const sensitiveColumns = ref([])
const overview = ref(null)
const loading = ref(false)
const scanning = ref(false)
const typeChartRef = ref(null)
const tableChartRef = ref(null)
let typeChart = null
let tableChart = null

// 敏感类型映射
const sensitiveTypeMap = {
  'PHONE': { text: '手机号', color: 'arcoblue' },
  'ID_CARD': { text: '身份证号', color: 'red' },
  'BANK_CARD': { text: '银行卡号', color: 'orange' },
  'EMAIL': { text: '邮箱', color: 'green' },
  'NAME': { text: '姓名', color: 'gray' },
  'ADDRESS': { text: '地址', color: 'gray' },
  'PASSWORD': { text: '密码', color: 'red' }
}

const maskExamples = {
  'PHONE': '138****8888',
  'ID_CARD': '110101********1234',
  'BANK_CARD': '6222 **** **** 1234',
  'EMAIL': 'a***@example.com',
  'NAME': '*某某',
  'ADDRESS': '北京市****',
  'PASSWORD': '********'
}

// 加载数据源
const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
    if (dataSources.value.length > 0) {
      // 优先选择默认数据源，否则选择第一个
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
      onDataSourceChange()
    }
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

// 数据源变化
const onDataSourceChange = async () => {
  selectedTable.value = null
  tables.value = []
  sensitiveColumns.value = []
  overview.value = null

  if (!selectedDataSource.value) return

  try {
    const res = await request.get('/api/data/tables', {
      params: { dataSourceId: selectedDataSource.value }
    })
    tables.value = res.data
    loadOverview()
  } catch (error) {
    Message.error('加载数据表失败')
  }
}

// 加载概览
const loadOverview = async () => {
  if (!selectedDataSource.value) return

  try {
    const res = await request.get(`/api/sensitive-data/${selectedDataSource.value}/overview`)
    overview.value = res.data
    nextTick(() => renderCharts())
  } catch (error) {
    console.error('加载概览失败', error)
  }
}

// 加载敏感字段
const loadSensitiveColumns = async () => {
  if (!selectedDataSource.value) return

  loading.value = true
  try {
    let url = `/api/sensitive-data/${selectedDataSource.value}`
    if (selectedTable.value) {
      url = `/api/sensitive-data/${selectedDataSource.value}/table/${selectedTable.value}`
    }
    const res = await request.get(url)
    sensitiveColumns.value = res.data
  } catch (error) {
    Message.error('加载敏感字段失败')
  } finally {
    loading.value = false
  }
}

// 扫描表
const scanTable = async () => {
  if (!selectedDataSource.value || !selectedTable.value) return

  scanning.value = true
  try {
    await request.post(`/api/sensitive-data/scan/${selectedDataSource.value}`, {
      tableName: selectedTable.value
    })
    Message.success('扫描完成')
    loadSensitiveColumns()
    loadOverview()
  } catch (error) {
    Message.error('扫描失败')
  } finally {
    scanning.value = false
  }
}

// 更新脱敏配置
const updateMaskConfig = async (row) => {
  try {
    await request.put(`/api/sensitive-data/config/${row.id}`, {
      isMasked: row.isMasked
    })
    Message.success('更新成功')
  } catch (error) {
    Message.error('更新失败')
    row.isMasked = !row.isMasked
  }
}

// 删除字段
const deleteColumn = async (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定删除此敏感字段标记吗？',
    onOk: async () => {
      try {
        await request.delete(`/api/sensitive-data/${id}`)
        Message.success('删除成功')
        loadSensitiveColumns()
        loadOverview()
      } catch (error) {
        Message.error('删除失败')
      }
    }
  })
}

const getSensitiveTypeColor = (type) => {
  return sensitiveTypeMap[type]?.color || 'gray'
}

const getSensitiveTypeText = (type) => {
  return sensitiveTypeMap[type]?.text || type
}

const getMaskExample = (type) => {
  return maskExamples[type] || '***'
}

// 渲染图表
const renderCharts = () => {
  if (!overview.value) return

  // 类型分布饼图
  if (typeChartRef.value) {
    if (typeChart) typeChart.dispose()
    typeChart = echarts.init(typeChartRef.value)

    const typeData = Object.entries(overview.value.typeDistribution || {}).map(([name, value]) => ({
      name,
      value
    }))

    typeChart.setOption({
      title: { text: '敏感类型分布', left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: typeData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
  }

  // 表分布柱状图
  if (tableChartRef.value) {
    if (tableChart) tableChart.dispose()
    tableChart = echarts.init(tableChartRef.value)

    const tableData = Object.entries(overview.value.tableDistribution || {})

    tableChart.setOption({
      title: { text: '各表敏感字段数', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: tableData.map(([name]) => name),
        axisLabel: { rotate: 45 }
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: tableData.map(([, value]) => value),
        itemStyle: { color: '#165dff' }
      }]
    })
  }
}

onMounted(() => {
  loadDataSources()
  window.addEventListener('resize', () => {
    typeChart?.resize()
    tableChart?.resize()
  })
})

onUnmounted(() => {
  typeChart?.dispose()
  tableChart?.dispose()
})
</script>

<style scoped lang="scss">
.sensitive-data-manage {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .overview {
    margin-bottom: 20px;
    padding: 20px;
    background: var(--color-fill-2);
    border-radius: 4px;
  }

  .sensitive-table {
    margin-bottom: 20px;

    .mask-example {
      color: var(--color-text-3);
      font-family: monospace;
    }
  }

  .charts {
    margin-top: 20px;

    .chart {
      height: 300px;
    }
  }
}
</style>
