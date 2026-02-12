<template>
  <div class="nl2sql-page">
    <el-row :gutter="20">
      <!-- 左侧：查询区域 -->
      <el-col :span="16">
        <el-card class="query-card">
          <template #header>
            <div class="card-header">
              <span class="title">
                <el-icon><ChatDotRound /></el-icon>
                智能查询
              </span>
              <div class="header-actions">
                <!-- 数据源选择 -->
                <el-select 
                  v-model="selectedDataSource" 
                  placeholder="选择数据源" 
                  style="width: 200px; margin-right: 10px"
                >
                  <el-option
                    v-for="ds in dataSources"
                    :key="ds.id"
                    :label="ds.name + (ds.isDefault ? ' (默认)' : '')"
                    :value="ds.id"
                  >
                    <span>{{ ds.name }}</span>
                    <el-tag v-if="ds.isDefault" type="success" size="small" style="margin-left: 8px">默认</el-tag>
                  </el-option>
                </el-select>
                
                <!-- AI 提供商选择 -->
                <el-select 
                  v-model="selectedProvider" 
                  placeholder="AI 提供商" 
                  style="width: 140px"
                >
                  <el-option label="DashScope (推荐)" value="dashscope" />
                  <el-option label="iFlow" value="iflow" />
                  <el-option label="Mock (演示)" value="mock" />
                  <el-option label="自动选择" value="" />
                </el-select>
              </div>
            </div>
          </template>

          <!-- 对话历史 -->
          <div class="chat-history" ref="chatHistoryRef">
            <div v-if="chatHistory.length === 0" class="empty-state">
              <el-empty description="开始你的第一个查询">
                <template #image>
                  <el-icon :size="64" color="#909399"><ChatLineRound /></el-icon>
                </template>
                <p class="hint">输入自然语言，AI 会自动转换为 SQL 并执行</p>
                <div class="examples">
                  <el-tag 
                    v-for="example in examples" 
                    :key="example"
                    class="example-tag"
                    @click="useExample(example)"
                  >
                    {{ example }}
                  </el-tag>
                </div>
              </el-empty>
            </div>

            <div v-else class="messages">
              <div 
                v-for="(msg, index) in chatHistory" 
                :key="index"
                class="message"
                :class="msg.type"
              >
                <!-- 用户消息 -->
                <div v-if="msg.type === 'user'" class="user-message">
                  <div class="avatar">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="content">
                    <div class="text">{{ msg.content }}</div>
                  </div>
                </div>

                <!-- AI 消息 -->
                <div v-else class="ai-message">
                  <div class="avatar">
                    <el-icon><Cpu /></el-icon>
                  </div>
                  <div class="content">
                    <!-- 生成的 SQL -->
                    <div v-if="msg.sql" class="sql-block">
                      <div class="sql-header">
                        <span>生成的 SQL</span>
                        <div class="actions">
                          <el-button type="primary" text size="small" @click="copySQL(msg.sql)">
                            <el-icon><CopyDocument /></el-icon> 复制
                          </el-button>
                          <el-button type="info" text size="small" @click="toggleFormat(msg)" v-if="!msg.formatted">
                            <el-icon><View /></el-icon> 格式化
                          </el-button>
                          <el-button type="info" text size="small" @click="toggleFormat(msg)" v-else>
                            <el-icon><View /></el-icon> 原始
                          </el-button>
                          <el-button type="info" text size="small" @click="explainSQL(msg.sql)">
                            <el-icon><QuestionFilled /></el-icon> 解释
                          </el-button>
                          <el-button type="warning" text size="small" @click="optimizeSQL(msg.sql)">
                            <el-icon><MagicStick /></el-icon> 优化
                          </el-button>
                        </div>
                      </div>
                      <pre class="sql-code"><code v-html="getFormattedSql(msg)"></code></pre>
                    </div>

                    <!-- 查询描述 -->
                    <div v-if="msg.description" class="description">
                      <el-icon><InfoFilled /></el-icon>
                      {{ msg.description }}
                    </div>

                    <!-- 数据表格 -->
                    <div v-if="msg.data && msg.data.length > 0" class="data-table">
                      <el-table 
                        :data="getPagedData(msg)" 
                        border 
                        stripe
                        max-height="400"
                        size="small"
                      >
                        <el-table-column 
                          v-for="col in getColumns(msg.data)" 
                          :key="col"
                          :prop="col"
                          :label="col"
                          show-overflow-tooltip
                        />
                      </el-table>
                      <div class="table-footer">
                        <div class="stats-info">
                          <span class="record-count">共 {{ msg.data.length }} 条记录</span>
                          <span v-if="msg.executionTime" class="execution-time">
                            <el-icon><Timer /></el-icon>
                            耗时 {{ msg.executionTime < 1000 ? msg.executionTime + ' ms' : (msg.executionTime / 1000).toFixed(2) + ' s' }}
                          </span>
                        </div>
                        <div class="footer-right">
                          <el-pagination
                            v-if="msg.data.length > 20"
                            v-model:current-page="msg.currentPage"
                            :page-size="20"
                            :total="msg.data.length"
                            layout="prev, pager, next"
                            small
                          />
                          <div class="action-buttons">
                            <el-button type="success" text size="small" @click="openChartGenerator(msg.data)">
                              <el-icon><TrendCharts /></el-icon> 生成图表
                            </el-button>
                            <el-button type="primary" text size="small" @click="exportData(msg.data)">
                              <el-icon><Download /></el-icon> 导出
                            </el-button>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 错误信息 -->
                    <div v-if="msg.error" class="error-message">
                      <el-icon><CircleClose /></el-icon>
                      {{ msg.error }}
                    </div>

                    <!-- 加载状态 -->
                    <div v-if="msg.loading" class="loading">
                      <el-icon class="is-loading"><Loading /></el-icon>
                      AI 正在思考...
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <el-input
              v-model="inputQuery"
              type="textarea"
              :rows="3"
              placeholder="输入你的查询，例如：查询最近7天的销售额"
              :disabled="loading"
              @keydown.enter.prevent="handleEnter"
            />
            <div class="input-actions">
              <el-button 
                type="primary" 
                :loading="loading"
                :disabled="!inputQuery.trim()"
                @click="sendQuery"
              >
                <el-icon><Promotion /></el-icon>
                发送查询
              </el-button>
              <el-button @click="clearHistory">
                <el-icon><Delete /></el-icon>
                清空对话
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：辅助信息 -->
      <el-col :span="8">
        <!-- 当前数据源信息 -->
        <el-card class="info-card">
          <template #header>
            <span>当前数据源</span>
          </template>
          <div v-if="currentDataSource" class="datasource-info">
            <div class="info-item">
              <span class="label">名称:</span>
              <span class="value">{{ currentDataSource.name }}</span>
            </div>
            <div class="info-item">
              <span class="label">类型:</span>
              <el-tag size="small">{{ currentDataSource.type }}</el-tag>
            </div>
            <div class="info-item">
              <span class="label">主机:</span>
              <span class="value">{{ currentDataSource.host }}:{{ currentDataSource.port }}</span>
            </div>
            <div class="info-item">
              <span class="label">数据库:</span>
              <span class="value">{{ currentDataSource.database }}</span>
            </div>
          </div>
          <el-empty v-else description="未选择数据源" />
        </el-card>

        <!-- 查询模板库 -->
        <el-card class="template-card">
          <template #header>
            <div class="template-header">
              <span>查询模板</span>
              <el-tag size="small" type="info">点击使用</el-tag>
            </div>
          </template>
          <el-collapse v-model="activeTemplateCategory">
            <el-collapse-item title="📊 统计分析" name="stats">
              <div class="template-list">
                <div class="template-item" @click="useTemplate('统计表中记录总数')">
                  <span class="template-text">统计表中记录总数</span>
                </div>
                <div class="template-item" @click="useTemplate('统计每个分类的数量')">
                  <span class="template-text">统计每个分类的数量</span>
                </div>
                <div class="template-item" @click="useTemplate('计算某字段的平均值')">
                  <span class="template-text">计算某字段的平均值</span>
                </div>
                <div class="template-item" @click="useTemplate('计算某字段的总和')">
                  <span class="template-text">计算某字段的总和</span>
                </div>
              </div>
            </el-collapse-item>
            <el-collapse-item title="📈 趋势分析" name="trend">
              <div class="template-list">
                <div class="template-item" @click="useTemplate('按日期统计每天的数量')">
                  <span class="template-text">按日期统计每天的数量</span>
                </div>
                <div class="template-item" @click="useTemplate('按月份统计趋势')">
                  <span class="template-text">按月份统计趋势</span>
                </div>
                <div class="template-item" @click="useTemplate('对比本月和上月的数据')">
                  <span class="template-text">对比本月和上月的数据</span>
                </div>
              </div>
            </el-collapse-item>
            <el-collapse-item title="🔍 数据查询" name="query">
              <div class="template-list">
                <div class="template-item" @click="useTemplate('查询最近10条记录')">
                  <span class="template-text">查询最近10条记录</span>
                </div>
                <div class="template-item" @click="useTemplate('查找重复数据')">
                  <span class="template-text">查找重复数据</span>
                </div>
                <div class="template-item" @click="useTemplate('查询空值记录')">
                  <span class="template-text">查询空值记录</span>
                </div>
                <div class="template-item" @click="useTemplate('多表关联查询')">
                  <span class="template-text">多表关联查询</span>
                </div>
              </div>
            </el-collapse-item>
            <el-collapse-item title="📋 排名对比" name="rank">
              <div class="template-list">
                <div class="template-item" @click="useTemplate('按某字段降序排列前10名')">
                  <span class="template-text">按某字段降序排列前10名</span>
                </div>
                <div class="template-item" @click="useTemplate('按分组统计并排序')">
                  <span class="template-text">按分组统计并排序</span>
                </div>
                <div class="template-item" @click="useTemplate('对比两个时间段的数据')">
                  <span class="template-text">对比两个时间段的数据</span>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <!-- 查询提示 -->
        <el-card class="tips-card">
          <template #header>
            <span>查询提示</span>
          </template>
          <ul class="tips-list">
            <li>使用自然语言描述你的查询需求</li>
            <li>可以追问或修正之前的查询</li>
            <li>支持聚合、排序、筛选等操作</li>
            <li>可以要求生成图表</li>
          </ul>
        </el-card>

        <!-- 快捷操作 -->
        <el-card class="quick-actions">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="action-buttons">
            <el-button @click="goToDataSourceManage">
              <el-icon><Setting /></el-icon>
              管理数据源
            </el-button>
            <el-button @click="showSchema">
              <el-icon><View /></el-icon>
              查看表结构
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- SQL 解释对话框 -->
    <el-dialog v-model="explainDialogVisible" title="SQL 解释" width="600px">
      <div class="sql-explanation">
        <pre class="sql-code">{{ currentSQL }}</pre>
        <el-divider />
        <div class="explanation-content">{{ sqlExplanation }}</div>
      </div>
    </el-dialog>

    <!-- SQL 优化对话框 -->
    <el-dialog v-model="optimizeDialogVisible" title="SQL 优化建议" width="600px">
      <div class="sql-optimization">
        <pre class="sql-code">{{ currentSQL }}</pre>
        <el-divider />
        <div class="optimization-content">{{ sqlOptimization }}</div>
      </div>
    </el-dialog>

    <!-- 表结构抽屉组件 -->
    <TableStructureDrawer
      v-model="tableDrawerVisible"
      :data-source-id="selectedDataSource"
      :title="currentDataSource ? `${currentDataSource.name} - 表结构` : '表结构'"
    />

    <!-- 图表生成器 -->
    <ChartGenerator
      v-model="chartGeneratorVisible"
      :data="chartData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, ChatLineRound, User, Cpu, CopyDocument,
  QuestionFilled, MagicStick, InfoFilled, Download,
  CircleClose, Loading, Promotion, Delete, Setting, View, Timer, TrendCharts
} from '@element-plus/icons-vue'
import { naturalLanguageToSQL, explainSQL as apiExplainSQL, optimizeSQL as apiOptimizeSQL } from '@/api/nl2sql'
import { getDataSources } from '@/api/datasource'
import { exportToExcel } from '@/utils/export'
import { formatSql, highlightSql } from '@/utils/sqlFormatter'
import TableStructureDrawer from '@/components/TableStructureDrawer.vue'
import ChartGenerator from '@/components/ChartGenerator.vue'

const router = useRouter()

// 状态
const inputQuery = ref('')
const loading = ref(false)
const chatHistory = ref<any[]>([])
const dataSources = ref<any[]>([])
const selectedDataSource = ref<number | null>(null)
const selectedProvider = ref('')
const chatHistoryRef = ref<HTMLElement | null>(null)

// 对话框状态
const explainDialogVisible = ref(false)
const optimizeDialogVisible = ref(false)
const currentSQL = ref('')
const sqlExplanation = ref('')
const sqlOptimization = ref('')

// 表结构抽屉状态
const tableDrawerVisible = ref(false)
const chartGeneratorVisible = ref(false)
const chartData = ref<any[]>([])

// 示例查询
const examples = [
  '查询所有表',
  '统计每个表的数据量',
  '查询最近7天的数据',
  '销售额最高的前10个产品'
]

// 计算当前选中的数据源
const currentDataSource = computed(() => {
  return dataSources.value.find(ds => ds.id === selectedDataSource.value)
})

// 加载数据源列表
const loadDataSources = async () => {
  try {
    const res = await getDataSources()
    if (res.success) {
      dataSources.value = res.data
      // 默认选择默认数据源
      const defaultDS = res.data.find(ds => ds.isDefault)
      if (defaultDS) {
        selectedDataSource.value = defaultDS.id
      } else if (res.data.length > 0) {
        selectedDataSource.value = res.data[0].id
      }
    }
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

// 使用示例
const useExample = (example) => {
  inputQuery.value = example
}

// 查询模板相关
const activeTemplateCategory = ref('stats')

const useTemplate = (template) => {
  inputQuery.value = template
  ElMessage.success('已填充模板，可修改后发送')
}

// 发送查询
const sendQuery = async () => {
  if (!inputQuery.value.trim()) return
  if (!selectedDataSource.value) {
    ElMessage.warning('请先选择一个数据源')
    return
  }

  const query = inputQuery.value.trim()
  
  // 添加用户消息
  chatHistory.value.push({
    type: 'user',
    content: query
  })

  // 添加 AI 消息（加载中）
  const aiMessageIndex = chatHistory.value.push({
    type: 'ai',
    loading: true
  }) - 1

  inputQuery.value = ''
  loading.value = true
  scrollToBottom()

  try {
    // 构建历史上下文
    const history = chatHistory.value
      .filter(msg => !msg.loading)
      .map(msg => `${msg.type === 'user' ? '用户' : 'AI'}: ${msg.content || msg.sql || ''}`)
      .join('\n')

    const res = await naturalLanguageToSQL({
      query,
      provider: selectedProvider.value,
      dataSourceId: selectedDataSource.value,
      history: history.length > 1000 ? history.slice(-1000) : history
    })

    // 更新 AI 消息
    const aiMessage = chatHistory.value[aiMessageIndex]
    aiMessage.loading = false

    if (res.success) {
      aiMessage.sql = res.sql
      aiMessage.data = res.data
      aiMessage.description = res.description
      aiMessage.intent = res.intent
      aiMessage.suggestedChart = res.suggestedChart
      aiMessage.executionTime = res.executionTime
      aiMessage.rowCount = res.rowCount
      aiMessage.currentPage = 1
    } else {
      aiMessage.error = res.message || '查询失败'
    }
  } catch (error) {
    const aiMessage = chatHistory.value[aiMessageIndex]
    aiMessage.loading = false
    aiMessage.error = '请求失败: ' + error.message
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 处理回车键
const handleEnter = (e) => {
  if (!e.shiftKey) {
    sendQuery()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
    }
  })
}

// 切换格式化状态
const toggleFormat = (msg) => {
  msg.formatted = !msg.formatted
}

// 获取格式化后的 SQL
const getFormattedSql = (msg) => {
  if (!msg.sql) return ''
  const sql = msg.formatted ? formatSql(msg.sql) : msg.sql
  return highlightSql(sql)
}

// 复制 SQL
const copySQL = (sql) => {
  navigator.clipboard.writeText(sql).then(() => {
    ElMessage.success('SQL 已复制到剪贴板')
  })
}

// 解释 SQL
const explainSQL = async (sql) => {
  currentSQL.value = sql
  sqlExplanation.value = '正在分析...'
  explainDialogVisible.value = true
  
  try {
    const res = await apiExplainSQL({
      sql,
      provider: selectedProvider.value
    })
    if (res.success) {
      sqlExplanation.value = res.explanation
    } else {
      sqlExplanation.value = res.message || '解释失败'
    }
  } catch (error) {
    sqlExplanation.value = '解释失败: ' + error.message
  }
}

// 优化 SQL
const optimizeSQL = async (sql) => {
  currentSQL.value = sql
  sqlOptimization.value = '正在优化...'
  optimizeDialogVisible.value = true
  
  try {
    const res = await apiOptimizeSQL({
      sql,
      provider: selectedProvider.value
    })
    if (res.success) {
      sqlOptimization.value = res.optimization
    } else {
      sqlOptimization.value = res.message || '优化失败'
    }
  } catch (error) {
    sqlOptimization.value = '优化失败: ' + error.message
  }
}

// 导出数据
const exportData = (data) => {
  exportToExcel(data, 'query_result')
  ElMessage.success('数据导出成功')
}

// 打开图表生成器
const openChartGenerator = (data) => {
  chartData.value = data
  chartGeneratorVisible.value = true
}

// 获取表格列
const getColumns = (data) => {
  if (!data || data.length === 0) return []
  return Object.keys(data[0])
}

// 获取分页数据
const getPagedData = (msg) => {
  if (!msg.data || msg.data.length === 0) return []
  const page = msg.currentPage || 1
  const start = (page - 1) * 20
  const end = start + 20
  return msg.data.slice(start, end)
}

// 清空历史
const clearHistory = () => {
  chatHistory.value = []
}

// 跳转到数据源管理
const goToDataSourceManage = () => {
  router.push('/datasource')
}

// 显示表结构抽屉
const showSchema = () => {
  if (!selectedDataSource.value) {
    ElMessage.warning('请先选择一个数据源')
    return
  }
  tableDrawerVisible.value = true
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped lang="scss">
.nl2sql-page {
  padding: 20px;
  height: calc(100vh - 100px);

  .query-card {
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: 16px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .header-actions {
        display: flex;
        align-items: center;
      }
    }

    .chat-history {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      background: #f5f7fa;

      .empty-state {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;

        .hint {
          color: #909399;
          margin: 16px 0;
        }

        .examples {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          justify-content: center;

          .example-tag {
            cursor: pointer;

            &:hover {
              background: #409eff;
              color: white;
            }
          }
        }
      }

      .messages {
        .message {
          margin-bottom: 20px;

          &.user {
            .user-message {
              display: flex;
              justify-content: flex-end;

              .avatar {
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background: #409eff;
                color: white;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-left: 12px;
              }

              .content {
                background: #409eff;
                color: white;
                padding: 12px 16px;
                border-radius: 12px 12px 0 12px;
                max-width: 70%;
              }
            }
          }

          &.ai {
            .ai-message {
              display: flex;

              .avatar {
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background: #67c23a;
                color: white;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 12px;
                flex-shrink: 0;
              }

              .content {
                flex: 1;
                background: white;
                padding: 16px;
                border-radius: 0 12px 12px 12px;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

                .sql-block {
                  margin-bottom: 12px;

                  .sql-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 8px;
                    font-weight: 500;

                    .actions {
                      display: flex;
                      gap: 8px;
                    }
                  }

                  .sql-code {
                    background: #1e1e1e;
                    color: #d4d4d4;
                    padding: 12px;
                    border-radius: 6px;
                    overflow-x: auto;
                    margin: 0;
                    font-family: 'Courier New', monospace;
                    font-size: 13px;
                    line-height: 1.5;

                    :deep(.sql-keyword) {
                      color: #569cd6;
                      font-weight: 500;
                    }

                    :deep(.sql-string) {
                      color: #ce9178;
                    }

                    :deep(.sql-number) {
                      color: #b5cea8;
                    }

                    :deep(.sql-comment) {
                      color: #6a9955;
                      font-style: italic;
                    }
                  }
                }

                .description {
                  color: #606266;
                  font-size: 13px;
                  margin-bottom: 12px;
                  display: flex;
                  align-items: center;
                  gap: 6px;
                }

                .data-table {
                  margin-top: 12px;

                  .table-footer {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-top: 8px;
                    padding-top: 8px;
                    border-top: 1px solid #ebeef5;

                    .stats-info {
                      display: flex;
                      align-items: center;
                      gap: 16px;
                    }

                    .record-count {
                      color: #909399;
                      font-size: 13px;
                    }

                    .execution-time {
                      color: #67c23a;
                      font-size: 13px;
                      display: flex;
                      align-items: center;
                      gap: 4px;
                    }

                    .footer-right {
                      display: flex;
                      align-items: center;
                      gap: 16px;
                    }
                  }
                }

                .error-message {
                  color: #f56c6c;
                  display: flex;
                  align-items: center;
                  gap: 6px;
                }

                .loading {
                  color: #909399;
                  display: flex;
                  align-items: center;
                  gap: 8px;
                }
              }
            }
          }
        }
      }
    }

    .input-area {
      padding: 20px;
      border-top: 1px solid #ebeef5;
      background: white;

      .input-actions {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        margin-top: 12px;
      }
    }
  }

  .info-card,
  .tips-card,
  .quick-actions,
  .template-card {
    margin-bottom: 20px;

    .template-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .template-list {
      .template-item {
        padding: 8px 12px;
        margin: 4px 0;
        background: #f5f7fa;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #e6f0ff;
          color: #409eff;
        }

        .template-text {
          font-size: 13px;
        }
      }
    }

    .datasource-info {
      .info-item {
        display: flex;
        justify-content: space-between;
        padding: 8px 0;
        border-bottom: 1px solid #ebeef5;

        &:last-child {
          border-bottom: none;
        }

        .label {
          color: #606266;
        }

        .value {
          color: #303133;
          font-weight: 500;
        }
      }
    }

    .tips-list {
      margin: 0;
      padding-left: 20px;
      color: #606266;

      li {
        margin-bottom: 8px;
      }
    }

    .action-buttons {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .el-button {
        justify-content: flex-start;
      }
    }
  }
}

.sql-explanation,
.sql-optimization {
  .sql-code {
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 16px;
    border-radius: 6px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 14px;
    margin: 0;
  }

  .explanation-content,
  .optimization-content {
    line-height: 1.8;
    color: #303133;
    white-space: pre-wrap;
  }
}
</style>
