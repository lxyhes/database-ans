<template>
  <div class="metric-manage">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>智能指标库</span>
          <a-button type="primary" @click="showCreateDialog">
            <template #icon><icon-plus /></template>
            新建指标
          </a-button>
        </div>
      </template>

      <!-- 指标概览 -->
      <a-row :gutter="20" class="overview">
        <a-col :span="6" v-for="(metrics, category) in groupedMetrics" :key="category">
          <a-card class="category-card">
            <template #title>
              <div class="category-title">
                <span>{{ category }}</span>
                <a-tag size="small">{{ metrics.length }}个指标</a-tag>
              </div>
            </template>
            <div class="metric-list">
              <div v-for="metric in metrics" :key="metric.id" class="metric-item" @click="viewMetric(metric)">
                <span class="metric-name">{{ metric.name }}</span>
                <a-tag size="small" :color="metric.isActive ? 'green' : 'gray'">
                  {{ metric.isActive ? '启用' : '禁用' }}
                </a-tag>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 指标列表 -->
      <a-table :data="metrics" :loading="loading" class="metric-table">
        <a-table-column title="#" data-index="index" :width="50">
          <template #cell="{ rowIndex }">{{ rowIndex + 1 }}</template>
        </a-table-column>
        <a-table-column title="指标名称" data-index="name" />
        <a-table-column title="指标编码" data-index="code" />
        <a-table-column title="分类" data-index="category" />
        <a-table-column title="数据表" data-index="tableName" />
        <a-table-column title="字段" data-index="columnName" />
        <a-table-column title="聚合方式">
          <template #cell="{ record }">
            <a-tag size="small">{{ record.aggregationType }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="单位" data-index="unit" />
        <a-table-column title="操作" :width="200">
          <template #cell="{ record }">
            <a-space>
              <a-button type="text" @click="calculateMetric(record.id)">
                <template #icon><icon-play-circle /></template>
                计算
              </a-button>
              <a-button type="text" @click="viewTrend(record)">
                <template #icon><icon-dashboard /></template>
                趋势
              </a-button>
              <a-button type="text" @click="editMetric(record)">
                <template #icon><icon-edit /></template>
              </a-button>
              <a-button type="text" status="danger" @click="deleteMetric(record.id)">
                <template #icon><icon-delete /></template>
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <!-- 创建/编辑对话框 -->
    <a-modal v-model:visible="dialogVisible" :title="isEdit ? '编辑指标' : '新建指标'" width="600px">
      <a-form :model="form" layout="vertical" ref="formRef">
        <a-form-item label="指标名称" field="name" required>
          <a-input v-model="form.name" placeholder="请输入指标名称" />
        </a-form-item>
        <a-form-item label="指标编码" field="code" required>
          <a-input v-model="form.code" placeholder="唯一编码，如：total_sales" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model="form.description" placeholder="指标描述" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model="form.category" placeholder="选择分类" allow-create allow-search style="width: 100%">
            <a-option value="销售指标">销售指标</a-option>
            <a-option value="用户指标">用户指标</a-option>
            <a-option value="财务指标">财务指标</a-option>
            <a-option value="运营指标">运营指标</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据源" field="dataSourceId" required>
          <a-select v-model="form.dataSourceId" placeholder="选择数据源" style="width: 100%">
            <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据表" field="tableName" required>
          <a-input v-model="form.tableName" placeholder="数据表名" />
        </a-form-item>
        <a-form-item label="字段名" field="columnName" required>
          <a-input v-model="form.columnName" placeholder="字段名" />
        </a-form-item>
        <a-form-item label="聚合方式" field="aggregationType" required>
          <a-select v-model="form.aggregationType" style="width: 100%">
            <a-option value="SUM">求和 (SUM)</a-option>
            <a-option value="COUNT">计数 (COUNT)</a-option>
            <a-option value="AVG">平均值 (AVG)</a-option>
            <a-option value="MAX">最大值 (MAX)</a-option>
            <a-option value="MIN">最小值 (MIN)</a-option>
            <a-option value="DISTINCT_COUNT">去重计数 (DISTINCT_COUNT)</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="过滤条件">
          <a-input v-model="form.filterCondition" placeholder="如：status = 1" />
        </a-form-item>
        <a-form-item label="单位">
          <a-input v-model="form.unit" placeholder="如：元、个、%" />
        </a-form-item>
        <a-form-item label="启用状态">
          <a-switch v-model="form.isActive">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </a-switch>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="dialogVisible = false">取消</a-button>
          <a-button type="primary" @click="saveMetric" :loading="saving">保存</a-button>
        </a-space>
      </template>
    </a-modal>

    <!-- 计算结果对话框 -->
    <a-modal v-model:visible="resultVisible" title="指标计算结果" width="400px">
      <div v-if="calcResult" class="calc-result">
        <div class="result-value">
          {{ formatValue(calcResult.value) }}
          <span class="result-unit">{{ calcResult.unit }}</span>
        </div>
        <div class="result-name">{{ calcResult.metricName }}</div>
        <div class="result-code">{{ calcResult.metricCode }}</div>
      </div>
    </a-modal>

    <!-- 趋势图对话框 -->
    <a-modal v-model:visible="trendVisible" title="指标趋势" width="800px">
      <div ref="trendChartRef" class="trend-chart"></div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus, IconPlayCircle, IconDashboard, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon'
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

const loadMetrics = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/metrics')
    metrics.value = res.data
    groupMetrics()
  } catch (error) {
    Message.error('加载指标失败')
  } finally {
    loading.value = false
  }
}

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
  } catch (error) {
    Message.error('加载数据源失败')
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
  const valid = await formRef.value.validate()
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/api/metrics/${currentMetric.value.id}`, form.value)
    } else {
      await request.post('/api/metrics', form.value)
    }
    Message.success('保存成功')
    dialogVisible.value = false
    loadMetrics()
  } catch (error) {
    Message.error('保存失败')
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
    Message.error('计算失败')
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
    const data = res.data || []

    if (!Array.isArray(data) || data.length === 0) {
      return
    }

    const option = {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map(d => d?.date || '').filter(Boolean),
        axisLabel: { rotate: 45 }
      },
      yAxis: { type: 'value' },
      series: [{
        data: data.map(d => d?.value ?? 0),
        type: 'line',
        smooth: true,
        areaStyle: {}
      }]
    }
    trendChart.setOption(option)
  } catch (error) {
    Message.error('加载趋势失败')
  }
}

const deleteMetric = async (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定删除此指标吗？',
    onOk: async () => {
      try {
        await request.delete(`/api/metrics/${id}`)
        Message.success('删除成功')
        loadMetrics()
      } catch (error) {
        Message.error('删除失败')
      }
    }
  })
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
      .category-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .metric-list {
        max-height: 200px;
        overflow-y: auto;

        .metric-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 0;
          border-bottom: 1px solid var(--color-neutral-3);
          cursor: pointer;

          &:hover {
            background: var(--color-fill-2);
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
      color: rgb(var(--primary-6));

      .result-unit {
        font-size: 20px;
        color: var(--color-text-2);
        margin-left: 10px;
      }
    }

    .result-name {
      font-size: 18px;
      color: var(--color-text-1);
      margin-top: 10px;
    }

    .result-code {
      font-size: 14px;
      color: var(--color-text-3);
      margin-top: 5px;
    }
  }

  .trend-chart {
    width: 100%;
    height: 400px;
  }
}
</style>
