<template>
  <div class="query-page">
    <el-row :gutter="20">
      <!-- 左侧：数据库结构 -->
      <el-col :span="6">
        <el-card class="schema-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据库结构</span>
              <el-button type="primary" :icon="Refresh" circle size="small" @click="loadTables" />
            </div>
          </template>
          
          <el-tree
            :data="tableTree"
            :props="{ label: 'name', children: 'children' }"
            @node-click="handleNodeClick"
            v-loading="loadingTables"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 'table'"><Grid /></el-icon>
                <el-icon v-else><Document /></el-icon>
                <span>{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：查询区域 -->
      <el-col :span="18">
        <el-card shadow="hover" class="query-card">
          <template #header>
            <div class="card-header">
              <span>自然语言查询</span>
              <el-tag type="success">AI 驱动</el-tag>
            </div>
          </template>

          <div class="query-input-section">
            <el-input
              v-model="queryText"
              type="textarea"
              :rows="4"
              placeholder="用自然语言描述你的查询需求，例如：&#10;1. 显示总销售额&#10;2. 查询每个地区的订单数量&#10;3. 找出销售额最高的前10个客户"
              class="query-input"
            />
            
            <div class="query-actions">
              <el-button 
                type="primary" 
                :icon="Search" 
                :loading="loading"
                @click="executeQuery"
              >
                执行查询
              </el-button>
              <el-button 
                type="success" 
                :icon="DataAnalysis"
                :loading="analyzing"
                @click="analyzeData"
              >
                数据分析
              </el-button>
              <el-button 
                type="warning" 
                :icon="Star"
                @click="saveCurrentQuery"
              >
                收藏查询
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 查询结果 -->
        <el-card v-if="queryResult" shadow="hover" class="result-card">
          <template #header>
            <div class="card-header">
              <span>查询结果</span>
              <div>
                <el-tag v-if="queryResult.sql" type="info" class="sql-tag">{{ queryResult.sql }}</el-tag>
                <el-button type="primary" :icon="Download" size="small" @click="exportResult">
                  导出
                </el-button>
              </div>
            </div>
          </template>

          <!-- 数据表格 -->
          <el-table
            v-if="queryResult.data && queryResult.data.length > 0"
            :data="queryResult.data"
            border
            stripe
            style="width: 100%"
            max-height="400"
          >
            <el-table-column
              v-for="(key, index) in Object.keys(queryResult.data[0])"
              :key="index"
              :prop="key"
              :label="key"
              min-width="120"
            />
          </el-table>

          <!-- 分析结果 -->
          <div v-else-if="queryResult.analysis" class="analysis-result">
            <el-alert
              :title="'分析完成'"
              type="success"
              :closable="false"
            />
            <div class="analysis-content" v-html="formatAnalysis(queryResult.analysis)"></div>
          </div>

          <!-- 空结果 -->
          <el-empty v-else description="暂无数据" />
        </el-card>

        <!-- 查询建议 -->
        <el-card shadow="hover" class="suggestions-card">
          <template #header>
            <span>查询示例</span>
          </template>
          <div class="suggestions-list">
            <el-tag
              v-for="(suggestion, index) in querySuggestions"
              :key="index"
              class="suggestion-tag"
              @click="useSuggestion(suggestion)"
            >
              {{ suggestion }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, DataAnalysis, Star, Refresh, Grid, Document, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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
    ElMessage.error('加载表结构失败')
  } finally {
    loadingTables.value = false
  }
}

const handleNodeClick = async (data: any) => {
  if (data.type === 'table') {
    try {
      const result = await window.electronAPI.getTableSchema(data.name)
      if (result.success) {
        // 可以在这里显示表结构详情
        ElMessage.success(`已选择表: ${data.name}`)
      }
    } catch (error) {
      ElMessage.error('获取表结构失败')
    }
  }
}

const executeQuery = async () => {
  if (!queryText.value.trim()) {
    ElMessage.warning('请输入查询内容')
    return
  }

  loading.value = true
  try {
    const result = await window.electronAPI.queryNaturalLanguage(queryText.value)
    if (result.success) {
      queryResult.value = result.data
      ElMessage.success('查询成功')
    } else {
      ElMessage.error(result.message || '查询失败')
    }
  } catch (error) {
    ElMessage.error('查询执行失败')
  } finally {
    loading.value = false
  }
}

const analyzeData = async () => {
  if (!queryText.value.trim()) {
    ElMessage.warning('请输入分析内容')
    return
  }

  analyzing.value = true
  try {
    const result = await window.electronAPI.analyzeData(queryText.value)
    if (result.success) {
      queryResult.value = result.data
      ElMessage.success('分析完成')
    } else {
      ElMessage.error(result.message || '分析失败')
    }
  } catch (error) {
    ElMessage.error('分析执行失败')
  } finally {
    analyzing.value = false
  }
}

const saveCurrentQuery = async () => {
  if (!queryText.value.trim()) {
    ElMessage.warning('没有可保存的查询')
    return
  }

  try {
    const result = await window.electronAPI.saveQuery({
      queryText: queryText.value,
      sqlQuery: queryResult.value?.sql || '',
      queryType: 'natural_language'
    })
    if (result.success) {
      ElMessage.success('查询已收藏')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const useSuggestion = (suggestion: string) => {
  queryText.value = suggestion
}

const formatAnalysis = (analysis: string) => {
  return analysis.replace(/\n/g, '<br>')
}

const exportResult = () => {
  // 导出功能实现
  ElMessage.info('导出功能开发中')
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

.sql-tag {
  margin-right: 12px;
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
  color: #606266;
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
