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
              <a-tag color="green">AI 驱动</a-tag>
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
                <a-tag v-if="queryResult.sql" color="arcoblue" class="sql-tag">{{ queryResult.sql }}</a-tag>
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
import { ref, onMounted, h } from 'vue'
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

const queryText = ref('')
const loading = ref(false)
const analyzing = ref(false)
const loadingTables = ref(false)
const queryResult = ref<any>(null)
const tableTree = ref<any[]>([])

const querySuggestions = [
  '显示总销售额',
  '查询每个地区的订单数量',
  '找出销售额最高的前10个客户',
  '统计每个产品类别的销售情况',
  '显示最近30天的销售趋势',
  '查询复购率最高的客户'
]

const loadTables = async () => {
  loadingTables.value = true
  try {
    const result = await window.electronAPI.getAllTables()
    if (result.success) {
      tableTree.value = result.data.tables?.map((table: string) => ({
        name: table,
        type: 'table',
        children: []
      })) || []
    }
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
      const result = await window.electronAPI.getTableSchema(node.name)
      if (result.success) {
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

  loading.value = true
  try {
    const result = await window.electronAPI.queryNaturalLanguage(queryText.value)
    if (result.success) {
      queryResult.value = result.data
      Message.success('查询成功')
    } else {
      Message.error(result.message || '查询失败')
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

  analyzing.value = true
  try {
    const result = await window.electronAPI.analyzeData(queryText.value)
    if (result.success) {
      queryResult.value = result.data
      Message.success('分析完成')
    } else {
      Message.error(result.message || '分析失败')
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
    const result = await window.electronAPI.saveQuery({
      queryText: queryText.value,
      sqlQuery: queryResult.value?.sql || '',
      queryType: 'natural_language'
    })
    if (result.success) {
      Message.success('查询已收藏')
    }
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
  Message.info('导出功能开发中')
}

onMounted(() => {
  loadTables()
})
</script>

<style scoped>
.query-page {
  height: 100%;
}

.schema-card {
  height: calc(100vh - 140px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
}

.result-card {
  margin-bottom: 20px;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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
