<template>
  <div class="data-lineage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据血缘追踪</span>
          <div class="header-actions">
            <el-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange">
              <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
            <el-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables.length" @change="loadLineageGraph">
              <el-option v-for="table in tables" :key="table" :label="table" :value="table" />
            </el-select>
            <el-button type="primary" @click="showAddDialog">
              <el-icon><Plus /></el-icon>
              添加血缘
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="16">
          <div ref="chartRef" class="lineage-chart"></div>
        </el-col>
        <el-col :span="8">
          <el-card class="trace-card">
            <template #header>
              <span>血缘追溯</span>
            </template>
            <el-form label-width="80px">
              <el-form-item label="表名">
                <el-input v-model="traceTable" placeholder="输入表名" />
              </el-form-item>
              <el-form-item label="字段名">
                <el-input v-model="traceColumn" placeholder="输入字段名" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="traceUpstream">
                  <el-icon><Top /></el-icon>
                  向上追溯
                </el-button>
                <el-button type="primary" @click="traceDownstream">
                  <el-icon><Bottom /></el-icon>
                  向下追溯
                </el-button>
              </el-form-item>
            </el-form>

            <el-divider />

            <div v-if="traceResults.length" class="trace-results">
              <el-timeline>
                <el-timeline-item v-for="(item, index) in traceResults" :key="index" :type="item.type">
                  <h4>{{ item.table }}.{{ item.column }}</h4>
                  <p>{{ item.transformationType }} - {{ item.transformationLogic }}</p>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 添加血缘对话框 -->
    <el-dialog v-model="dialogVisible" title="添加血缘关系" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="源表">
          <el-input v-model="form.sourceTable" placeholder="源表名" />
        </el-form-item>
        <el-form-item label="源字段">
          <el-input v-model="form.sourceColumn" placeholder="源字段名" />
        </el-form-item>
        <el-form-item label="目标表">
          <el-input v-model="form.targetTable" placeholder="目标表名" />
        </el-form-item>
        <el-form-item label="目标字段">
          <el-input v-model="form.targetColumn" placeholder="目标字段名" />
        </el-form-item>
        <el-form-item label="转换类型">
          <el-select v-model="form.transformationType" style="width: 100%">
            <el-option label="SELECT" value="SELECT" />
            <el-option label="JOIN" value="JOIN" />
            <el-option label="AGGREGATION" value="AGGREGATION" />
            <el-option label="CALCULATION" value="CALCULATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="转换逻辑">
          <el-input v-model="form.transformationLogic" type="textarea" placeholder="转换逻辑描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLineage">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Top, Bottom } from '@element-plus/icons-vue'
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
  } catch (error) {
    ElMessage.error('加载数据源失败')
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
    ElMessage.error('加载数据表失败')
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
    ElMessage.error('加载血缘图谱失败')
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
        itemStyle: { color: node.category === 'current' ? '#409eff' : '#67c23a' }
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
    ElMessage.success('添加成功')
    dialogVisible.value = false
    loadLineageGraph()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const traceUpstream = async () => {
  if (!traceTable.value || !traceColumn.value) {
    ElMessage.warning('请输入表名和字段名')
    return
  }
  try {
    const res = await request.get(`/api/lineage/${selectedDataSource.value}/upstream`, {
      params: { tableName: traceTable.value, columnName: traceColumn.value }
    })
    traceResults.value = res.data.map(item => ({ ...item, type: 'primary' }))
  } catch (error) {
    ElMessage.error('追溯失败')
  }
}

const traceDownstream = async () => {
  if (!traceTable.value || !traceColumn.value) {
    ElMessage.warning('请输入表名和字段名')
    return
  }
  try {
    const res = await request.get(`/api/lineage/${selectedDataSource.value}/downstream`, {
      params: { tableName: traceTable.value, columnName: traceColumn.value }
    })
    traceResults.value = res.data.map(item => ({ ...item, type: 'success' }))
  } catch (error) {
    ElMessage.error('追溯失败')
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
