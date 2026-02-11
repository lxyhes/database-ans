<template>
  <div class="sensitive-data-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>敏感数据管理</span>
          <div class="header-actions">
            <el-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange">
              <el-option
                v-for="ds in dataSources"
                :key="ds.id"
                :label="ds.name"
                :value="ds.id"
              />
            </el-select>
            <el-select v-model="selectedTable" placeholder="选择数据表" clearable @change="loadSensitiveColumns">
              <el-option
                v-for="table in tables"
                :key="table"
                :label="table"
                :value="table"
              />
            </el-select>
            <el-button type="primary" @click="scanTable" :loading="scanning" :disabled="!selectedTable">
              <el-icon><Search /></el-icon>
              扫描敏感数据
            </el-button>
          </div>
        </div>
      </template>

      <!-- 概览统计 -->
      <el-row :gutter="20" class="overview" v-if="overview">
        <el-col :span="6">
          <el-statistic title="敏感字段总数" :value="overview.totalSensitiveColumns || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="已脱敏字段" :value="overview.maskedColumns || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="涉及表数" :value="Object.keys(overview.tableDistribution || {}).length" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="敏感类型数" :value="Object.keys(overview.typeDistribution || {}).length" />
        </el-col>
      </el-row>

      <!-- 敏感字段列表 -->
      <el-table :data="sensitiveColumns" v-loading="loading" class="sensitive-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="tableName" label="表名" />
        <el-table-column prop="columnName" label="字段名" />
        <el-table-column prop="sensitiveType" label="敏感类型">
          <template #default="{ row }">
            <el-tag :type="getSensitiveTypeTag(row.sensitiveType)">
              {{ getSensitiveTypeText(row.sensitiveType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detectionMethod" label="检测方式">
          <template #default="{ row }">
            <el-tag size="small" :type="row.detectionMethod === 'COLUMN_NAME' ? 'primary' : 'success'">
              {{ row.detectionMethod === 'COLUMN_NAME' ? '字段名' : '内容采样' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.confidence * 100)" />
          </template>
        </el-table-column>
        <el-table-column prop="isMasked" label="脱敏状态">
          <template #default="{ row }">
            <el-switch
              v-model="row.isMasked"
              @change="updateMaskConfig(row)"
              active-text="已脱敏"
              inactive-text="未脱敏"
            />
          </template>
        </el-table-column>
        <el-table-column label="脱敏示例">
          <template #default="{ row }">
            <span class="mask-example">{{ getMaskExample(row.sensitiveType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" link @click="deleteColumn(row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 类型分布图表 -->
      <el-row :gutter="20" class="charts" v-if="overview?.typeDistribution">
        <el-col :span="12">
          <div ref="typeChartRef" class="chart"></div>
        </el-col>
        <el-col :span="12">
          <div ref="tableChartRef" class="chart"></div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Delete } from '@element-plus/icons-vue'
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
  'PHONE': { text: '手机号', tag: 'primary' },
  'ID_CARD': { text: '身份证号', tag: 'danger' },
  'BANK_CARD': { text: '银行卡号', tag: 'warning' },
  'EMAIL': { text: '邮箱', tag: 'success' },
  'NAME': { text: '姓名', tag: 'info' },
  'ADDRESS': { text: '地址', tag: 'info' },
  'PASSWORD': { text: '密码', tag: 'danger' }
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
  } catch (error) {
    ElMessage.error('加载数据源失败')
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
    const res = await request.get(`/api/datasources/${selectedDataSource.value}/tables`)
    tables.value = res.data
    loadOverview()
  } catch (error) {
    ElMessage.error('加载数据表失败')
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
    ElMessage.error('加载敏感字段失败')
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
    ElMessage.success('扫描完成')
    loadSensitiveColumns()
    loadOverview()
  } catch (error) {
    ElMessage.error('扫描失败')
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
    ElMessage.success('更新成功')
  } catch (error) {
    ElMessage.error('更新失败')
    row.isMasked = !row.isMasked
  }
}

// 删除字段
const deleteColumn = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除此敏感字段标记吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/api/sensitive-data/${id}`)
    ElMessage.success('删除成功')
    loadSensitiveColumns()
    loadOverview()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getSensitiveTypeTag = (type) => {
  return sensitiveTypeMap[type]?.tag || 'info'
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
        itemStyle: { color: '#409eff' }
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
    background: #f5f7fa;
    border-radius: 4px;
  }

  .sensitive-table {
    margin-bottom: 20px;

    .mask-example {
      color: #909399;
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
