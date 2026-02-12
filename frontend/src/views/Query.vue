<template>
  <div class="query-page">
    <a-row :gutter="20">
      <!-- 左侧：数据库结构 -->
      <a-col :span="6">
        <a-card class="schema-card" hoverable>
          <template #title>
            <div class="card-header">
              <span>数据库结构</span>
              <a-button type="primary" shape="circle" size="small" @click="loadTables">
                <template #icon><icon-refresh /></template>
              </a-button>
            </div>
          </template>
          
          <a-tree
            :data="tableTree"
            :field-names="{ key: 'name', title: 'name', children: 'children' }"
            @select="handleNodeSelect"
            :loading="loadingTables"
          >
            <template #icon="{ node }">
              <icon-apps v-if="node.type === 'table'" />
              <icon-file v-else />
            </template>
          </a-tree>
        </a-card>
      </a-col>

      <!-- 右侧：查询区域 -->
      <a-col :span="18">
        <a-card hoverable class="query-card">
          <template #title>
            <div class="card-header">
              <span>自然语言查询</span>
              <div class="header-right">
                <a-select v-model="selectedDataSource" placeholder="选择数据源" style="width: 200px;" @change="onDataSourceChange">
                  <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
                </a-select>
                <a-tag color="green">AI 驱动</a-tag>
              </div>
            </div>
          </template>

          <div class="query-input-section">
            <a-textarea
              v-model="queryText"
              :rows="4"
              placeholder="用自然语言描述你的查询需求，例如：
1. 显示总销售额
2. 查询每个地区的订单数量
3. 找出销售额最高的前10个客户"
              class="query-input"
              allow-clear
            />
            
            <div class="query-actions">
              <a-button 
                type="primary" 
                :loading="loading"
                @click="executeQuery"
              >
                <template #icon><icon-search /></template>
                执行查询
              </a-button>
              <a-button 
                type="primary" 
                status="success"
                :loading="analyzing"
                @click="analyzeData"
              >
                <template #icon><icon-bar-chart /></template>
                数据分析
              </a-button>
              <a-button 
                type="outline" 
                status="warning"
                @click="saveCurrentQuery"
              >
                <template #icon><icon-star /></template>
                收藏查询
              </a-button>
            </div>
          </div>
        </a-card>

        <!-- 查询结果 -->
        <a-card v-if="queryResult" hoverable class="result-card">
          <template #title>
            <div class="card-header">
              <span>查询结果</span>
              <div class="result-actions">
                <a-tag v-if="queryResult.sqlQuery" color="arcoblue" class="sql-tag">{{ queryResult.sqlQuery }}</a-tag>
                <a-button type="primary" size="small" @click="exportResult">
                  <template #icon><icon-download /></template>
                  导出
                </a-button>
              </div>
            </div>
          </template>

          <!-- 数据表格 -->
          <a-table
            v-if="queryResult.data && queryResult.data.length > 0"
            :data="queryResult.data"
            :bordered="true"
            :stripe="true"
            :scroll="{ y: 400 }"
            page-position="bottom"
            :pagination="{ pageSize: 10 }"
          >
            <a-table-column
              v-for="(key, index) in Object.keys(queryResult.data[0])"
              :key="index"
              :title="key"
              :data-index="key"
              :width="120"
            />
          </a-table>

          <!-- 分析结果 -->
          <div v-else-if="queryResult.analysis" class="analysis-result">
            <a-alert type="success" :show-icon="true">
              <template #title>分析完成</template>
            </a-alert>
            <div class="analysis-content" v-html="formatAnalysis(queryResult.analysis)"></div>
          </div>

          <!-- 空结果 -->
          <a-empty v-else description="暂无数据" />
        </a-card>

        <!-- 查询建议 -->
        <a-card hoverable class="suggestions-card">
          <template #title>
            <span>查询示例</span>
          </template>
          <div class="suggestions-list">
            <a-tag
              v-for="(suggestion, index) in querySuggestions"
              :key="index"
              class="suggestion-tag"
              color="arcoblue"
              @click="useSuggestion(suggestion)"
            >
              {{ suggestion }}
            </a-tag>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
  IconSearch, 
  IconBarChart, 
  IconStar, 
  IconRefresh, 
  IconApps, 
  IconFile, 
  IconDownload 
} from '@arco-design/web-vue/es/icon'
import { Message } from '@arco-design/web-vue'
import request from '@/utils/request'

const queryText = ref('')
const loading = ref(false)
const analyzing = ref(false)
const loadingTables = ref(false)
const queryResult = ref<any>(null)
const tableTree = ref<any[]>([])
const dataSources = ref<any[]>([])
const selectedDataSource = ref<number | null>(null)

const querySuggestions = [
  '显示总销售额',
  '查询每个地区的订单数量',
  '找出销售额最高的前10个客户',
  '统计每个产品类别的销售情况',
  '显示最近30天的销售趋势',
  '查询复购率最高的客户'
]

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data || res
    if (dataSources.value.length > 0) {
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
      loadTables()
    }
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

const onDataSourceChange = () => {
  tableTree.value = []
  loadTables()
}

const loadTables = async () => {
  if (!selectedDataSource.value) {
    Message.warning('请先选择数据源')
    return
  }
  
  loadingTables.value = true
  try {
    const res = await request.get('/api/data/tables', {
      params: { dataSourceId: selectedDataSource.value }
    })
    const tables = res.data || res
    tableTree.value = tables.map((table: string) => ({
      name: table,
      type: 'table',
      children: []
    }))
  } catch (error) {
    Message.error('加载表结构失败')
  } finally {
    loadingTables.value = false
  }
}

const handleNodeSelect = async (selectedKeys: string[], data: any) => {
  const node = data.node
  if (node.type === 'table') {
    try {
      const res = await request.get(`/api/data/schema/${node.name}`, {
        params: { dataSourceId: selectedDataSource.value }
      })
      if (res.data) {
        Message.success(`已选择表: ${node.name}`)
      }
    } catch (error) {
      Message.error('获取表结构失败')
    }
  }
}

const executeQuery = async () => {
  if (!queryText.value.trim()) {
    Message.warning('请输入查询内容')
    return
  }

  if (!selectedDataSource.value) {
    Message.warning('请先选择数据源')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/api/query/natural', {
      naturalLanguageQuery: queryText.value,
      dataSourceId: selectedDataSource.value
    })
    if (res.success) {
      queryResult.value = res
      Message.success('查询成功')
    } else {
      Message.error(res.message || '查询失败')
    }
  } catch (error) {
    Message.error('查询执行失败')
  } finally {
    loading.value = false
  }
}

const analyzeData = async () => {
  if (!queryText.value.trim()) {
    Message.warning('请输入分析内容')
    return
  }

  if (!selectedDataSource.value) {
    Message.warning('请先选择数据源')
    return
  }

  analyzing.value = true
  try {
    const res = await request.post('/api/query/analyze', {
      naturalLanguageQuery: queryText.value,
      dataSourceId: selectedDataSource.value
    })
    if (res.success) {
      queryResult.value = {
        ...res,
        analysis: res.message
      }
      Message.success('分析完成')
    } else {
      Message.error(res.message || '分析失败')
    }
  } catch (error) {
    Message.error('分析执行失败')
  } finally {
    analyzing.value = false
  }
}

const saveCurrentQuery = async () => {
  if (!queryText.value.trim()) {
    Message.warning('没有可保存的查询')
    return
  }

  try {
    await request.post('/api/query-history', {
      queryText: queryText.value,
      sqlQuery: queryResult.value?.sqlQuery || '',
      queryType: 'natural_language'
    })
    Message.success('查询已收藏')
  } catch (error) {
    Message.error('保存失败')
  }
}

const useSuggestion = (suggestion: string) => {
  queryText.value = suggestion
}

const formatAnalysis = (analysis: string) => {
  return analysis.replace(/\n/g, '<br>')
}

const exportResult = () => {
  if (!queryResult.value?.data) {
    Message.warning('没有可导出的数据')
    return
  }
  
  try {
    const data = queryResult.value.data
    const headers = Object.keys(data[0])
    const csvContent = [
      headers.join(','),
      ...data.map((row: any) => headers.map(h => row[h]).join(','))
    ].join('\n')
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = 'query_result.csv'
    link.click()
    Message.success('导出成功')
  } catch (error) {
    Message.error('导出失败')
  }
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped>
.query-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.schema-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.query-card {
  margin-bottom: 20px;
}

.query-input-section {
  padding: 10px 0;
}

.query-input {
  margin-bottom: 16px;
}

.query-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.result-card {
  margin-bottom: 20px;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.sql-tag {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.analysis-result {
  padding: 20px;
}

.analysis-content {
  margin-top: 16px;
  line-height: 1.8;
  color: var(--color-text-2);
}

.suggestions-card {
  margin-bottom: 20px;
}

.suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.suggestion-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.suggestion-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
