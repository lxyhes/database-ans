<template>
  <div class="data-lineage">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>数据血缘追踪</span>
          <div class="header-actions">
            <a-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange" style="width: 200px;">
              <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
            </a-select>
            <a-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables?.length" @change="loadLineageGraph" style="width: 200px;">
              <a-option v-for="table in tables" :key="table" :value="table">{{ table }}</a-option>
            </a-select>
            <a-button type="primary" @click="showAddDialog">
              <template #icon><icon-plus /></template>
              添加血缘
            </a-button>
          </div>
        </div>
      </template>

      <a-row :gutter="20">
        <a-col :span="16">
          <div ref="chartRef" class="lineage-chart"></div>
        </a-col>
        <a-col :span="8">
          <a-card class="trace-card">
            <template #title>
              <span>血缘追溯</span>
            </template>
            <a-form layout="vertical">
              <a-form-item label="表名">
                <a-input v-model="traceTable" placeholder="输入表名" />
              </a-form-item>
              <a-form-item label="字段名">
                <a-input v-model="traceColumn" placeholder="输入字段名" />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button type="primary" @click="traceUpstream">
                    <template #icon><icon-arrow-up /></template>
                    向上追溯
                  </a-button>
                  <a-button type="primary" @click="traceDownstream">
                    <template #icon><icon-arrow-down /></template>
                    向下追溯
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>

            <a-divider />

            <div v-if="traceResults.length" class="trace-results">
              <a-timeline>
                <a-timeline-item v-for="(item, index) in traceResults" :key="index" :dot-color="item.type === 'primary' ? '#165dff' : '#00b42a'">
                  <h4>{{ item.table }}.{{ item.column }}</h4>
                  <p>{{ item.transformationType }} - {{ item.transformationLogic }}</p>
                </a-timeline-item>
              </a-timeline>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <!-- 添加血缘对话框 -->
    <a-modal v-model:visible="dialogVisible" title="添加血缘关系" width="500px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="源表">
          <a-input v-model="form.sourceTable" placeholder="源表名" />
        </a-form-item>
        <a-form-item label="源字段">
          <a-input v-model="form.sourceColumn" placeholder="源字段名" />
        </a-form-item>
        <a-form-item label="目标表">
          <a-input v-model="form.targetTable" placeholder="目标表名" />
        </a-form-item>
        <a-form-item label="目标字段">
          <a-input v-model="form.targetColumn" placeholder="目标字段名" />
        </a-form-item>
        <a-form-item label="转换类型">
          <a-select v-model="form.transformationType" style="width: 100%">
            <a-option value="SELECT">SELECT</a-option>
            <a-option value="JOIN">JOIN</a-option>
            <a-option value="AGGREGATION">AGGREGATION</a-option>
            <a-option value="CALCULATION">CALCULATION</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="转换逻辑">
          <a-textarea v-model="form.transformationLogic" placeholder="转换逻辑描述" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="dialogVisible = false">取消</a-button>
          <a-button type="primary" @click="saveLineage">保存</a-button>
        </a-space>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconArrowUp, IconArrowDown } from '@arco-design/web-vue/es/icon'
import * as echarts from 'echarts'
import request from '@/utils/request'

const chartRef = ref(null)
let chart = null

const dataSources = ref([])
const tables = ref([])
const selectedDataSource = ref(null)
const selectedTable = ref(null)
const traceTable = ref('')
const traceColumn = ref('')
const traceResults = ref([])
const dialogVisible = ref(false)

const form = ref({
  sourceTable: '',
  sourceColumn: '',
  targetTable: '',
  targetColumn: '',
  transformationType: 'SELECT',
  transformationLogic: ''
})

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

const onDataSourceChange = async () => {
  selectedTable.value = null
  tables.value = []
  if (!selectedDataSource.value) return
  try {
    const res = await request.get('/api/data/tables', {
      params: { dataSourceId: selectedDataSource.value }
    })
    tables.value = res.data
  } catch (error) {
    Message.error('加载数据表失败')
  }
}

const loadLineageGraph = async () => {
  if (!selectedDataSource.value || !selectedTable.value) return
  try {
    const res = await request.get(`/api/lineage/${selectedDataSource.value}/graph`, {
      params: { tableName: selectedTable.value }
    })
    renderGraph(res.data)
  } catch (error) {
    Message.error('加载血缘图谱失败')
  }
}

const renderGraph = (data) => {
  if (!chartRef.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)

  const option = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'graph',
      layout: 'force',
      data: data.nodes?.map(node => ({
        name: node.name,
        symbolSize: node.category === 'current' ? 60 : 40,
        itemStyle: { color: node.category === 'current' ? '#165dff' : '#00b42a' }
      })) || [],
      links: data.links?.map(link => ({
        source: link.source,
        target: link.target,
        label: { show: true, formatter: link.transformationType }
      })) || [],
      roam: true,
      label: { show: true },
      force: { repulsion: 300, edgeLength: 150 }
    }]
  }
  chart.setOption(option)
}

const showAddDialog = () => {
  form.value = {
    sourceTable: '', sourceColumn: '', targetTable: '', targetColumn: '',
    transformationType: 'SELECT', transformationLogic: ''
  }
  dialogVisible.value = true
}

const saveLineage = async () => {
  try {
    await request.post('/api/lineage', { ...form.value, dataSourceId: selectedDataSource.value })
    Message.success('添加成功')
    dialogVisible.value = false
    loadLineageGraph()
  } catch (error) {
    Message.error('添加失败')
  }
}

const traceUpstream = async () => {
  if (!traceTable.value || !traceColumn.value) {
    Message.warning('请输入表名和字段名')
    return
  }
  try {
    const res = await request.get(`/api/lineage/${selectedDataSource.value}/upstream`, {
      params: { tableName: traceTable.value, columnName: traceColumn.value }
    })
    traceResults.value = res.data.map(item => ({ ...item, type: 'primary' }))
  } catch (error) {
    Message.error('追溯失败')
  }
}

const traceDownstream = async () => {
  if (!traceTable.value || !traceColumn.value) {
    Message.warning('请输入表名和字段名')
    return
  }
  try {
    const res = await request.get(`/api/lineage/${selectedDataSource.value}/downstream`, {
      params: { tableName: traceTable.value, columnName: traceColumn.value }
    })
    traceResults.value = res.data.map(item => ({ ...item, type: 'success' }))
  } catch (error) {
    Message.error('追溯失败')
  }
}

onMounted(() => {
  loadDataSources()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => chart?.dispose())
</script>

<style scoped lang="scss">
.data-lineage {
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

  .lineage-chart {
    width: 100%;
    height: 500px;
  }

  .trace-card {
    height: 500px;
    overflow-y: auto;
  }

  .trace-results {
    max-height: 300px;
    overflow-y: auto;
  }
}
</style>
