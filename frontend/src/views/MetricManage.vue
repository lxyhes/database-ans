<template>
  <div class="metric-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>智能指标库</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新建指标
          </el-button>
        </div>
      </template>

      <!-- 指标概览 -->
      <el-row :gutter="20" class="overview">
        <el-col :span="6" v-for="(metrics, category) in groupedMetrics" :key="category">
          <el-card class="category-card">
            <template #header>
              <span>{{ category }}</span>
              <el-tag size="small">{{ metrics.length }}个指标</el-tag>
            </template>
            <div class="metric-list">
              <div v-for="metric in metrics" :key="metric.id" class="metric-item" @click="viewMetric(metric)">
                <span class="metric-name">{{ metric.name }}</span>
                <el-tag size="small" :type="metric.isActive ? 'success' : 'info'">
                  {{ metric.isActive ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 指标列表 -->
      <el-table :data="metrics" v-loading="loading" class="metric-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="name" label="指标名称" />
        <el-table-column prop="code" label="指标编码" />
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="tableName" label="数据表" />
        <el-table-column prop="columnName" label="字段" />
        <el-table-column prop="aggregationType" label="聚合方式">
          <template #default="{ row }">
            <el-tag size="small">{{ row.aggregationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="calculateMetric(row.id)">
              <el-icon><VideoPlay /></el-icon>
              计算
            </el-button>
            <el-button type="primary" link @click="viewTrend(row)">
              <el-icon><TrendCharts /></el-icon>
              趋势
            </el-button>
            <el-button type="primary" link @click="editMetric(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button type="danger" link @click="deleteMetric(row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑指标' : '新建指标'" width="600px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="指标名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="指标编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一编码，如：total_sales" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="指标描述" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类" allow-create filterable style="width: 100%">
            <el-option label="销售指标" value="销售指标" />
            <el-option label="用户指标" value="用户指标" />
            <el-option label="财务指标" value="财务指标" />
            <el-option label="运营指标" value="运营指标" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" prop="dataSourceId">
          <el-select v-model="form.dataSourceId" placeholder="选择数据源" style="width: 100%">
            <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据表" prop="tableName">
          <el-input v-model="form.tableName" placeholder="数据表名" />
        </el-form-item>
        <el-form-item label="字段名" prop="columnName">
          <el-input v-model="form.columnName" placeholder="字段名" />
        </el-form-item>
        <el-form-item label="聚合方式" prop="aggregationType">
          <el-select v-model="form.aggregationType" style="width: 100%">
            <el-option label="求和 (SUM)" value="SUM" />
            <el-option label="计数 (COUNT)" value="COUNT" />
            <el-option label="平均值 (AVG)" value="AVG" />
            <el-option label="最大值 (MAX)" value="MAX" />
            <el-option label="最小值 (MIN)" value="MIN" />
            <el-option label="去重计数 (DISTINCT_COUNT)" value="DISTINCT_COUNT" />
          </el-select>
        </el-form-item>
        <el-form-item label="过滤条件">
          <el-input v-model="form.filterCondition" placeholder="如：status = 1" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如：元、个、%" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.isActive" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMetric" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 计算结果对话框 -->
    <el-dialog v-model="resultVisible" title="指标计算结果" width="400px">
      <div v-if="calcResult" class="calc-result">
        <div class="result-value">
          {{ formatValue(calcResult.value) }}
          <span class="result-unit">{{ calcResult.unit }}</span>
        </div>
        <div class="result-name">{{ calcResult.metricName }}</div>
        <div class="result-code">{{ calcResult.metricCode }}</div>
      </div>
    </el-dialog>

    <!-- 趋势图对话框 -->
    <el-dialog v-model="trendVisible" title="指标趋势" width="800px">
      <div ref="trendChartRef" class="trend-chart"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay, TrendCharts, Edit, Delete } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const metrics = ref([])
const dataSources = ref([])
const groupedMetrics = ref({})
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const resultVisible = ref(false)
const trendVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const currentMetric = ref(null)
const calcResult = ref(null)
const trendChartRef = ref(null)
let trendChart = null

const form = ref({
  name: '',
  code: '',
  description: '',
  category: '',
  dataSourceId: null,
  tableName: '',
  columnName: '',
  aggregationType: 'SUM',
  filterCondition: '',
  unit: '',
  isActive: true
})

const rules = {
  name: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入指标编码', trigger: 'blur' }],
  dataSourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  tableName: [{ required: true, message: '请输入数据表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  aggregationType: [{ required: true, message: '请选择聚合方式', trigger: 'change' }]
}

const loadMetrics = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/metrics')
    metrics.value = res.data
    groupMetrics()
  } catch (error) {
    ElMessage.error('加载指标失败')
  } finally {
    loading.value = false
  }
}

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

const groupMetrics = () => {
  const grouped = {}
  metrics.value.forEach(metric => {
    const category = metric.category || '未分类'
    if (!grouped[category]) grouped[category] = []
    grouped[category].push(metric)
  })
  groupedMetrics.value = grouped
}

const showCreateDialog = () => {
  isEdit.value = false
  form.value = {
    name: '', code: '', description: '', category: '',
    dataSourceId: null, tableName: '', columnName: '',
    aggregationType: 'SUM', filterCondition: '', unit: '', isActive: true
  }
  dialogVisible.value = true
}

const editMetric = (row) => {
  isEdit.value = true
  currentMetric.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

const saveMetric = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/api/metrics/${currentMetric.value.id}`, form.value)
    } else {
      await request.post('/api/metrics', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadMetrics()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const calculateMetric = async (id) => {
  try {
    const res = await request.post(`/api/metrics/${id}/calculate`)
    calcResult.value = res.data
    resultVisible.value = true
  } catch (error) {
    ElMessage.error('计算失败')
  }
}

const viewTrend = async (row) => {
  trendVisible.value = true
  currentMetric.value = row
  nextTick(() => {
    renderTrendChart(row.id)
  })
}

const renderTrendChart = async (metricId) => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  try {
    const res = await request.get(`/api/metrics/${metricId}/trend`, {
      params: { timeField: 'created_at', days: 30 }
    })
    const data = res.data

    const option = {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date),
        axisLabel: { rotate: 45 }
      },
      yAxis: { type: 'value' },
      series: [{
        data: data.map(d => d.value),
        type: 'line',
        smooth: true,
        areaStyle: {}
      }]
    }
    trendChart.setOption(option)
  } catch (error) {
    ElMessage.error('加载趋势失败')
  }
}

const deleteMetric = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除此指标吗？', '提示', { type: 'warning' })
    await request.delete(`/api/metrics/${id}`)
    ElMessage.success('删除成功')
    loadMetrics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewMetric = (metric) => {
  calculateMetric(metric.id)
}

const formatValue = (value) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'number') {
    return value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
  }
  return value
}

onMounted(() => {
  loadMetrics()
  loadDataSources()
  window.addEventListener('resize', () => trendChart?.resize())
})

onUnmounted(() => {
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.metric-manage {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .overview {
    margin-bottom: 20px;

    .category-card {
      .metric-list {
        max-height: 200px;
        overflow-y: auto;

        .metric-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 0;
          border-bottom: 1px solid #ebeef5;
          cursor: pointer;

          &:hover {
            background: #f5f7fa;
          }

          .metric-name {
            font-size: 14px;
          }
        }
      }
    }
  }

  .metric-table {
    margin-top: 20px;
  }

  .calc-result {
    text-align: center;
    padding: 20px;

    .result-value {
      font-size: 48px;
      font-weight: bold;
      color: #409eff;

      .result-unit {
        font-size: 20px;
        color: #666;
        margin-left: 10px;
      }
    }

    .result-name {
      font-size: 18px;
      color: #333;
      margin-top: 10px;
    }

    .result-code {
      font-size: 14px;
      color: #999;
      margin-top: 5px;
    }
  }

  .trend-chart {
    width: 100%;
    height: 400px;
  }
}
</style>
