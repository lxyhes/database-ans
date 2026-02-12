<template>
  <div class="data-relation-graph">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>数据表关系图谱</span>
          <div class="header-actions">
            <a-select v-model="selectedDataSource" placeholder="选择数据源" @change="loadGraphData" style="width: 200px;">
              <a-option
                v-for="ds in dataSources"
                :key="ds.id"
                :value="ds.id"
              >{{ ds.name }}</a-option>
            </a-select>
            <a-button type="primary" @click="analyzeRelations" :loading="analyzing">
              <template #icon><icon-refresh /></template>
              分析关系
            </a-button>
          </div>
        </div>
      </template>

      <div ref="chartRef" class="relation-chart"></div>

      <div class="legend">
        <div class="legend-item">
          <span class="legend-color foreign-key"></span>
          <span>外键关系</span>
        </div>
        <div class="legend-item">
          <span class="legend-color inferred"></span>
          <span>推断关系</span>
        </div>
        <div class="legend-item">
          <span class="legend-color manual"></span>
          <span>手动配置</span>
        </div>
      </div>

      <div class="stats" v-if="graphData">
        <a-row :gutter="20">
          <a-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ graphData.totalTables }}</div>
              <div class="stat-label">数据表</div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ graphData.totalRelations }}</div>
              <div class="stat-label">关联关系</div>
            </div>
          </a-col>
        </a-row>
      </div>
    </a-card>

    <!-- 关系详情对话框 -->
    <a-modal v-model:visible="detailVisible" title="关系详情" width="600px">
      <a-descriptions :column="1" bordered v-if="selectedRelation">
        <a-descriptions-item label="源表">{{ selectedRelation.source }}</a-descriptions-item>
        <a-descriptions-item label="源字段">{{ selectedRelation.sourceColumn }}</a-descriptions-item>
        <a-descriptions-item label="目标表">{{ selectedRelation.target }}</a-descriptions-item>
        <a-descriptions-item label="目标字段">{{ selectedRelation.targetColumn }}</a-descriptions-item>
        <a-descriptions-item label="关系类型">
          <a-tag :color="getRelationTypeColor(selectedRelation.relationType)">
            {{ getRelationTypeText(selectedRelation.relationType) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="置信度">
          <a-progress :percent="Math.round(selectedRelation.confidence * 100)" />
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconRefresh } from '@arco-design/web-vue/es/icon'
import * as echarts from 'echarts'
import request from '@/utils/request'

const chartRef = ref(null)
const chart = ref(null)
const dataSources = ref([])
const selectedDataSource = ref(null)
const graphData = ref(null)
const analyzing = ref(false)
const detailVisible = ref(false)
const selectedRelation = ref(null)

// 加载数据源列表
const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
    if (dataSources.value.length > 0) {
      // 优先选择默认数据源，否则选择第一个
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
      loadGraphData()
    }
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

// 加载图谱数据
const loadGraphData = async () => {
  if (!selectedDataSource.value) return

  try {
    const res = await request.get(`/api/table-relations/${selectedDataSource.value}/graph`)
    // 兼容两种返回格式: {success: true, data: {...}} 或 {...}
    graphData.value = res.data || res
    renderGraph()
  } catch (error) {
    console.error('加载关系图谱失败:', error)
    Message.error('加载关系图谱失败')
  }
}

// 分析关系
const analyzeRelations = async () => {
  if (!selectedDataSource.value) {
    Message.warning('请先选择数据源')
    return
  }

  analyzing.value = true
  try {
    await request.post(`/api/table-relations/analyze/${selectedDataSource.value}`)
    Message.success('关系分析完成')
    loadGraphData()
  } catch (error) {
    Message.error('分析失败')
  } finally {
    analyzing.value = false
  }
}

// 渲染图谱
const renderGraph = () => {
  if (!chartRef.value || !graphData.value) return

  // 检查是否有数据
  const nodes = graphData.value.nodes || []
  const links = graphData.value.links || []
  
  if (nodes.length === 0) {
    Message.info('未检测到表关系，请确保数据库中存在外键约束或符合命名规范的关联字段（如 user_id 关联 users.id）')
    return
  }

  if (chart.value) {
    chart.value.dispose()
  }

  chart.value = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'edge') {
          return `${params.data.source}.${params.data.sourceColumn} → ${params.data.target}.${params.data.targetColumn}`
        }
        return params.data.name
      }
    },
    animationDuration: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes.filter(node => node && node.id).map(node => ({
          ...node,
          symbolSize: 50,
          itemStyle: {
            color: getNodeColor(node.category)
          }
        })),
        links: links.filter(link => link && link.source && link.target).map(link => ({
          ...link,
          lineStyle: {
            color: getLinkColor(link.relationType),
            type: (link.relationType === 'FOREIGN_KEY' || link.relationType?.toString() === 'FOREIGN_KEY') ? 'solid' : 'dashed',
            width: link.confidence > 0.8 ? 3 : 1
          }
        })),
        categories: [
          { name: '用户' },
          { name: '订单' },
          { name: '商品' },
          { name: '分类' },
          { name: '日志' },
          { name: '其他' }
        ],
        roam: true,
        label: {
          show: true,
          position: 'bottom'
        },
        force: {
          repulsion: 300,
          edgeLength: 150
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        }
      }
    ]
  }

  chart.value.setOption(option)

  // 点击事件
  chart.value.on('click', (params) => {
    if (params.dataType === 'edge') {
      selectedRelation.value = params.data
      detailVisible.value = true
    }
  })
}

const getNodeColor = (category) => {
  const colors = {
    '用户': '#5470c6',
    '订单': '#91cc75',
    '商品': '#fac858',
    '分类': '#ee6666',
    '日志': '#73c0de',
    '其他': '#3ba272'
  }
  return colors[category] || '#999'
}

const getLinkColor = (relationType) => {
  const colors = {
    'FOREIGN_KEY': '#67c23a',
    'INFERRED': '#e6a23c',
    'MANUAL': '#409eff'
  }
  const type = relationType?.toString() || ''
  return colors[type] || '#999'
}

const getRelationTypeColor = (type) => {
  const colors = {
    'FOREIGN_KEY': 'green',
    'INFERRED': 'orange',
    'MANUAL': 'arcoblue'
  }
  const typeStr = type?.toString() || ''
  return colors[typeStr] || 'gray'
}

const getRelationTypeText = (type) => {
  const texts = {
    'FOREIGN_KEY': '外键关系',
    'INFERRED': '推断关系',
    'MANUAL': '手动配置'
  }
  const typeStr = type?.toString() || ''
  return texts[typeStr] || typeStr
}

onMounted(() => {
  loadDataSources()
  window.addEventListener('resize', () => {
    chart.value?.resize()
  })
})

onUnmounted(() => {
  chart.value?.dispose()
})
</script>

<style scoped lang="scss">
.data-relation-graph {
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

  .relation-chart {
    width: 100%;
    height: 500px;
  }

  .legend {
    display: flex;
    gap: 20px;
    margin-top: 20px;
    padding: 15px;
    background: var(--color-fill-2);
    border-radius: 4px;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 8px;

      .legend-color {
        width: 20px;
        height: 3px;

        &.foreign-key {
          background: #67c23a;
        }
        &.inferred {
          background: #e6a23c;
          border-top: 2px dashed #e6a23c;
          height: 0;
        }
        &.manual {
          background: #409eff;
        }
      }
    }
  }

  .stats {
    margin-top: 20px;
    padding: 20px;
    background: var(--color-fill-2);
    border-radius: 4px;

    .stat-item {
      text-align: center;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: rgb(var(--primary-6));
      }

      .stat-label {
        margin-top: 5px;
        color: var(--color-text-2);
      }
    }
  }
}
</style>
