<template>
  <div class="nl2sql-page">
    <a-row :gutter="20" class="main-row">
      <!-- 左侧：查询区域 -->
      <a-col :span="16" class="left-col">
        <a-card class="query-card" :bordered="false">
          <template #title>
            <div class="card-header">
              <span class="title">
                <icon-message /> 智能查询
              </span>
              <div class="header-actions">
                <!-- 数据源选择 -->
                <a-select 
                  v-model="selectedDataSource" 
                  placeholder="选择数据源" 
                  :style="{ width: '200px', marginRight: '10px' }"
                >
                  <a-option
                    v-for="ds in dataSources"
                    :key="ds.id"
                    :value="ds.id"
                  >
                    {{ ds.name }}{{ ds.isDefault ? ' (默认)' : '' }}
                  </a-option>
                </a-select>
                
                <!-- AI 提供商选择 -->
                <a-select 
                  v-model="selectedProvider" 
                  placeholder="AI 提供商" 
                  :style="{ width: '140px' }"
                >
                  <a-option value="dashscope">DashScope (推荐)</a-option>
                  <a-option value="iflow">iFlow</a-option>
                  <a-option value="mock">Mock (演示)</a-option>
                  <a-option value="">自动选择</a-option>
                </a-select>
              </div>
            </div>
          </template>

          <!-- 对话历史 -->
          <div class="chat-history" ref="chatHistoryRef">
            <div v-if="chatHistory.length === 0" class="empty-state">
              <a-empty description="开始你的第一个查询">
                <template #image>
                  <icon-message :size="64" :style="{ color: '#86909c' }" />
                </template>
                <p class="hint">输入自然语言，AI 会自动转换为 SQL 并执行</p>
                <div class="examples">
                  <a-tag 
                    v-for="example in examples" 
                    :key="example"
                    class="example-tag"
                    @click="useExample(example)"
                  >
                    {{ example }}
                  </a-tag>
                </div>
              </a-empty>
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
                    <icon-user />
                  </div>
                  <div class="content">
                    <div class="text">{{ msg.content }}</div>
                  </div>
                </div>

                <!-- AI 消息 -->
                <div v-else class="ai-message">
                  <div class="avatar">
                    <icon-robot />
                  </div>
                  <div class="content">
                    <!-- 生成的 SQL -->
                    <div v-if="msg.sql" class="sql-block">
                      <div class="sql-header">
                        <span>生成的 SQL</span>
                        <div class="actions">
                          <a-button type="text" size="small" @click="copySQL(msg.sql)">
                            <template #icon><icon-copy /></template> 复制
                          </a-button>
                          <a-button type="text" size="small" @click="toggleFormat(msg)">
                            <template #icon><icon-eye /></template>
                            {{ msg.formatted ? '原始' : '格式化' }}
                          </a-button>
                          <a-button type="text" size="small" @click="explainSQL(msg.sql)">
                            <template #icon><icon-question-circle /></template> 解释
                          </a-button>
                          <a-button type="text" size="small" status="warning" @click="optimizeSQL(msg.sql)">
                            <template #icon><icon-bulb /></template> 优化
                          </a-button>
                        </div>
                      </div>
                      <pre class="sql-code"><code v-html="getFormattedSql(msg)"></code></pre>
                    </div>

                    <!-- 查询描述 -->
                    <div v-if="msg.description" class="description">
                      <icon-info-circle />
                      {{ msg.description }}
                    </div>

                    <!-- 数据表格 -->
                    <div v-if="msg.data && msg.data.length > 0" class="data-table">
                      <a-table 
                        :data="getPagedData(msg)" 
                        :bordered="true"
                        :stripe="true"
                        :scroll="{ maxHeight: 400 }"
                        size="small"
                      >
                        <template #columns>
                          <a-table-column 
                            v-for="col in getColumns(msg.data)" 
                            :key="col"
                            :title="col"
                            :data-index="col"
                            :ellipsis="true"
                            :tooltip="true"
                          />
                        </template>
                      </a-table>
                      <div class="table-footer">
                        <div class="stats-info">
                          <span class="record-count">共 {{ msg.data.length }} 条记录</span>
                          <span v-if="msg.executionTime" class="execution-time">
                            <icon-clock-circle />
                            耗时 {{ msg.executionTime < 1000 ? msg.executionTime + ' ms' : (msg.executionTime / 1000).toFixed(2) + ' s' }}
                          </span>
                        </div>
                        <div class="footer-right">
                          <a-pagination
                            v-if="msg.data.length > 20"
                            v-model:current="msg.currentPage"
                            :page-size="20"
                            :total="msg.data.length"
                            size="small"
                          />
                          <div class="action-buttons">
                            <a-button type="text" status="success" size="small" @click="openChartGenerator(msg.data)">
                              <template #icon><icon-bar-chart /></template> 生成图表
                            </a-button>
                            <a-button type="text" size="small" @click="exportData(msg.data)">
                              <template #icon><icon-download /></template> 导出
                            </a-button>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 错误信息 -->
                    <div v-if="msg.error" class="error-message">
                      <icon-close-circle />
                      {{ msg.error }}
                    </div>

                    <!-- 加载状态 -->
                    <div v-if="msg.loading" class="loading">
                      <a-spin size="small" />
                      AI 正在思考...
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <a-textarea
              v-model="inputQuery"
              :rows="3"
              placeholder="输入你的查询，例如：查询最近7天的销售额"
              :disabled="loading"
              @keydown.enter.prevent="handleEnter"
            />
            <div class="input-actions">
              <a-button 
                type="primary" 
                :loading="loading"
                :disabled="!inputQuery.trim()"
                @click="sendQuery"
              >
                <template #icon><icon-send /></template>
                发送查询
              </a-button>
              <a-button @click="clearHistory">
                <template #icon><icon-delete /></template>
                清空对话
              </a-button>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧：辅助信息 -->
      <a-col :span="8" class="right-col">
        <!-- 当前数据源信息 -->
        <a-card class="info-card" :bordered="false">
          <template #title>当前数据源</template>
          <div v-if="currentDataSource" class="datasource-info">
            <div class="info-item">
              <span class="label">名称:</span>
              <span class="value">{{ currentDataSource.name }}</span>
            </div>
            <div class="info-item">
              <span class="label">类型:</span>
              <a-tag size="small">{{ currentDataSource.type }}</a-tag>
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
          <a-empty v-else description="未选择数据源" />
        </a-card>

        <!-- 查询模板库 -->
        <a-card class="template-card" :bordered="false">
          <template #title>
            <div class="template-header">
              <span>查询模板</span>
              <a-tag size="small" color="arcoblue">点击使用</a-tag>
            </div>
          </template>
          <a-collapse :default-active-key="['stats']">
            <a-collapse-item title="📊 统计分析" key="stats">
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
            </a-collapse-item>
            <a-collapse-item title="📈 趋势分析" key="trend">
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
            </a-collapse-item>
            <a-collapse-item title="🔍 数据查询" key="query">
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
            </a-collapse-item>
            <a-collapse-item title="📋 排名对比" key="rank">
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
            </a-collapse-item>
          </a-collapse>
        </a-card>

        <!-- 查询提示 -->
        <a-card class="tips-card" :bordered="false">
          <template #title>查询提示</template>
          <ul class="tips-list">
            <li>使用自然语言描述你的查询需求</li>
            <li>可以追问或修正之前的查询</li>
            <li>支持聚合、排序、筛选等操作</li>
            <li>可以要求生成图表</li>
          </ul>
        </a-card>

        <!-- 快捷操作 -->
        <a-card class="quick-actions" :bordered="false">
          <template #title>快捷操作</template>
          <div class="action-buttons">
            <a-button @click="goToDataSourceManage">
              <template #icon><icon-settings /></template>
              管理数据源
            </a-button>
            <a-button @click="showSchema">
              <template #icon><icon-eye /></template>
              查看表结构
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- SQL 解释对话框 -->
    <a-modal v-model:visible="explainDialogVisible" title="SQL 解释" width="600px">
      <div class="sql-explanation">
        <pre class="sql-code">{{ currentSQL }}</pre>
        <a-divider />
        <div class="explanation-content">{{ sqlExplanation }}</div>
      </div>
    </a-modal>

    <!-- SQL 优化对话框 -->
    <a-modal v-model:visible="optimizeDialogVisible" title="SQL 优化建议" width="600px">
      <div class="sql-optimization">
        <pre class="sql-code">{{ currentSQL }}</pre>
        <a-divider />
        <div class="optimization-content">{{ sqlOptimization }}</div>
      </div>
    </a-modal>

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
import { 
  IconMessage, 
  IconUser, 
  IconRobot, 
  IconCopy,
  IconEye, 
  IconQuestionCircle, 
  IconBulb, 
  IconInfoCircle, 
  IconDownload,
  IconCloseCircle, 
  IconSend, 
  IconDelete, 
  IconSettings, 
  IconBarChart,
  IconClockCircle
} from '@arco-design/web-vue/es/icon'
import { Message } from '@arco-design/web-vue'
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
      const defaultDS = res.data.find((ds: any) => ds.isDefault)
      if (defaultDS) {
        selectedDataSource.value = defaultDS.id
      } else if (res.data.length > 0) {
        selectedDataSource.value = res.data[0].id
      }
    }
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

// 使用示例
const useExample = (example: string) => {
  inputQuery.value = example
}

// 查询模板相关
const useTemplate = (template: string) => {
  inputQuery.value = template
  Message.success('已填充模板，可修改后发送')
}

// 发送查询
const sendQuery = async () => {
  if (!inputQuery.value.trim()) return
  if (!selectedDataSource.value) {
    Message.warning('请先选择一个数据源')
    return
  }

  const query = inputQuery.value.trim()
  inputQuery.value = ''
  loading.value = true

  // 添加用户消息
  chatHistory.value.push({
    type: 'user',
    content: query
  })

  // 添加 AI 消息（加载中）
  const aiMessageIndex = chatHistory.value.push({
    type: 'ai',
    loading: true,
    sql: null,
    data: null,
    error: null
  }) - 1

  scrollToBottom()

  try {
    const history = chatHistory.value
      .slice(0, -1)
      .map((msg: any) => ({
        role: msg.type === 'user' ? 'user' : 'assistant',
        content: msg.content || msg.sql || ''
      }))

    const res = await naturalLanguageToSQL({
      query,
      dataSourceId: selectedDataSource.value,
      provider: selectedProvider.value || undefined,
      history
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
  } catch (error: any) {
    const aiMessage = chatHistory.value[aiMessageIndex]
    aiMessage.loading = false
    aiMessage.error = '请求失败: ' + error.message
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 处理回车键
const handleEnter = (e: KeyboardEvent) => {
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
const toggleFormat = (msg: any) => {
  msg.formatted = !msg.formatted
}

// 获取格式化后的 SQL
const getFormattedSql = (msg: any) => {
  if (!msg.sql) return ''
  const sql = msg.formatted ? formatSql(msg.sql) : msg.sql
  return highlightSql(sql)
}

// 复制 SQL
const copySQL = (sql: string) => {
  navigator.clipboard.writeText(sql)
  Message.success('SQL 已复制到剪贴板')
}

// 解释 SQL
const explainSQL = async (sql: string) => {
  currentSQL.value = sql
  explainDialogVisible.value = true
  sqlExplanation.value = '正在分析...'

  try {
    const res = await apiExplainSQL({ sql })
    if (res.success) {
      sqlExplanation.value = res.explanation
    } else {
      sqlExplanation.value = res.message || '解释失败'
    }
  } catch (error) {
    sqlExplanation.value = '解释请求失败'
  }
}

// 优化 SQL
const optimizeSQL = async (sql: string) => {
  currentSQL.value = sql
  optimizeDialogVisible.value = true
  sqlOptimization.value = '正在优化...'

  try {
    const res = await apiOptimizeSQL({ sql })
    if (res.success) {
      sqlOptimization.value = res.optimization
    } else {
      sqlOptimization.value = res.message || '优化失败'
    }
  } catch (error) {
    sqlOptimization.value = '优化请求失败'
  }
}

// 导出数据
const exportData = (data: any[]) => {
  exportToExcel(data, 'query_result')
  Message.success('数据导出成功')
}

// 打开图表生成器
const openChartGenerator = (data: any[]) => {
  chartData.value = data
  chartGeneratorVisible.value = true
}

// 获取表格列
const getColumns = (data: any[]) => {
  if (!data || data.length === 0) return []
  return Object.keys(data[0])
}

// 获取分页数据
const getPagedData = (msg: any) => {
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

// 显示表结构
const showSchema = () => {
  if (!selectedDataSource.value) {
    Message.warning('请先选择一个数据源')
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
  overflow: hidden;

  .main-row {
    height: 100%;
    margin: 0 !important;

    .left-col,
    .right-col {
      height: 100%;
      padding: 0 10px !important;
    }
  }

  .query-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.arco-card-body) {
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
    }

    .chat-history {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      background: #f7f8fa;

      .empty-state {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;

        .hint {
          color: #86909c;
          margin: 16px 0;
        }

        .examples {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          justify-content: center;
          max-width: 500px;

          .example-tag {
            cursor: pointer;

            &:hover {
              color: #165DFF;
              background: #e8f3ff;
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
              gap: 12px;

              .avatar {
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background: #165DFF;
                color: #fff;
                display: flex;
                align-items: center;
                justify-content: center;
                flex-shrink: 0;
              }

              .content {
                flex: 1;
                background: #fff;
                padding: 12px 16px;
                border-radius: 8px;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

                .text {
                  color: #1d2129;
                  line-height: 1.6;
                }
              }
            }
          }

          &.ai {
            .ai-message {
              display: flex;
              gap: 12px;

              .avatar {
                width: 36px;
                height: 36px;
                border-radius: 50%;
                background: #00b42a;
                color: #fff;
                display: flex;
                align-items: center;
                justify-content: center;
                flex-shrink: 0;
              }

              .content {
                flex: 1;

                .sql-block {
                  background: #1d2129;
                  border-radius: 8px;
                  overflow: hidden;
                  margin-bottom: 12px;

                  .sql-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 8px 12px;
                    background: #272e3b;
                    color: #fff;
                    font-size: 13px;

                    .actions {
                      display: flex;
                      gap: 4px;

                      :deep(.arco-btn) {
                        color: #c9cdd4;

                        &:hover {
                          color: #fff;
                        }
                      }
                    }
                  }

                  .sql-code {
                    background: #1d2129;
                    color: #d4d4d4;
                    padding: 12px;
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
                  background: #e8f3ff;
                  border: 1px solid #bedaff;
                  border-radius: 6px;
                  padding: 10px 12px;
                  margin-bottom: 12px;
                  color: #165DFF;
                  font-size: 13px;
                  display: flex;
                  align-items: center;
                  gap: 8px;
                }

                .data-table {
                  background: #fff;
                  border-radius: 8px;
                  padding: 12px;
                  margin-bottom: 12px;
                  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

                  .table-footer {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-top: 8px;
                    padding-top: 8px;
                    border-top: 1px solid #e5e6eb;

                    .stats-info {
                      display: flex;
                      align-items: center;
                      gap: 16px;
                    }

                    .record-count {
                      color: #86909c;
                      font-size: 13px;
                    }

                    .execution-time {
                      color: #00b42a;
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
                  background: #ffece8;
                  border: 1px solid #ffccc7;
                  border-radius: 6px;
                  padding: 10px 12px;
                  color: #f53f3f;
                  font-size: 13px;
                  display: flex;
                  align-items: center;
                  gap: 8px;
                }

                .loading {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  color: #86909c;
                  padding: 12px;
                }
              }
            }
          }
        }
      }
    }

    .input-area {
      padding: 16px 20px;
      border-top: 1px solid #e5e6eb;
      background: #fff;

      .input-actions {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        margin-top: 10px;
      }
    }
  }

  .info-card,
  .tips-card,
  .quick-actions,
  .template-card {
    margin-bottom: 16px;
    max-height: calc(50vh - 60px);
    overflow-y: auto;

    :deep(.arco-card-body) {
      max-height: calc(50vh - 110px);
      overflow-y: auto;
    }

    .template-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .template-list {
      .template-item {
        padding: 8px 12px;
        margin: 4px 0;
        background: #f2f3f5;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #e8f3ff;
          color: #165DFF;
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
        border-bottom: 1px solid #e5e6eb;

        &:last-child {
          border-bottom: none;
        }

        .label {
          color: #4e5969;
        }

        .value {
          color: #1d2129;
          font-weight: 500;
        }
      }
    }

    .tips-list {
      padding-left: 20px;
      margin: 0;

      li {
        padding: 4px 0;
        color: #4e5969;
      }
    }

    .action-buttons {
      display: flex;
      flex-direction: column;
      gap: 8px;

      :deep(.arco-btn) {
        justify-content: flex-start;
      }
    }
  }
}

.sql-explanation,
.sql-optimization {
  .sql-code {
    background: #1d2129;
    color: #d4d4d4;
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 13px;
  }

  .explanation-content,
  .optimization-content {
    color: #1d2129;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}
</style>
