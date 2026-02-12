<template>
  <div class="data-quality-check">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>数据质量检测</span>
          <div class="header-actions">
            <a-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange" style="width: 200px;">
              <a-option
                v-for="ds in dataSources"
                :key="ds.id"
                :value="ds.id"
              >{{ ds.name }}</a-option>
            </a-select>
            <a-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables?.length" style="width: 200px;">
              <a-option
                v-for="table in tables"
                :key="table"
                :value="table"
              >{{ table }}</a-option>
            </a-select>
            <a-button type="primary" @click="checkQuality" :loading="checking" :disabled="!selectedTable">
              <template #icon><icon-search /></template>
              开始检测
            </a-button>
          </div>
        </div>
      </template>

      <div v-if="report" class="quality-report">
        <!-- 总体评分 -->
        <a-row :gutter="20" class="score-section">
          <a-col :span="6">
            <div class="score-card" :class="getGradeClass(report.grade)">
              <div class="score-value">{{ report.overallScore?.toFixed(1) }}</div>
              <div class="score-grade">{{ report.grade }}级</div>
              <div class="score-label">综合评分</div>
            </div>
          </a-col>
          <a-col :span="18">
            <a-descriptions :column="3" bordered>
              <a-descriptions-item label="表名">{{ report.tableName }}</a-descriptions-item>
              <a-descriptions-item label="总行数">{{ report.totalRows?.toLocaleString() }}</a-descriptions-item>
              <a-descriptions-item label="总列数">{{ report.totalColumns }}</a-descriptions-item>
              <a-descriptions-item label="检测时间">{{ formatDate(report.checkTime) }}</a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>

        <!-- 问题统计 -->
        <a-row :gutter="20" class="issue-stats">
          <a-col :span="6">
            <a-statistic title="缺失值问题" :value="report.missingValues?.length || 0">
              <template #suffix>个字段</template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="重复数据" :value="report.duplicates?.length || 0">
              <template #suffix>处</template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="异常值" :value="report.outliers?.length || 0">
              <template #suffix>个字段</template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="格式问题" :value="report.formatIssues?.length || 0">
              <template #suffix>处</template>
            </a-statistic>
          </a-col>
        </a-row>

        <!-- 详细问题列表 -->
        <a-tabs type="card" class="detail-tabs">
          <a-tab-pane key="missing" title="缺失值">
            <a-table :data="report.missingValues" v-if="report.missingValues?.length" :bordered="true">
              <a-table-column title="字段名" data-index="columnName" />
              <a-table-column title="缺失数量" data-index="missingCount" />
              <a-table-column title="缺失率">
                <template #cell="{ record }">
                  <a-progress :percent="record.missingPercentage" :status="getMissingValueStatus(record.missingPercentage)" />
                </template>
              </a-table-column>
              <a-table-column title="严重程度">
                <template #cell="{ record }">
                  <a-tag :color="getSeverityColor(record.severity)">{{ record.severity }}</a-tag>
                </template>
              </a-table-column>
            </a-table>
            <a-empty v-else description="未发现缺失值问题" />
          </a-tab-pane>

          <a-tab-pane key="duplicates" title="重复数据">
            <a-table :data="report.duplicates" v-if="report.duplicates?.length" :bordered="true">
              <a-table-column title="重复行数" data-index="duplicateRowCount" />
              <a-table-column title="重复率">
                <template #cell="{ record }">
                  <a-progress :percent="record.duplicatePercentage" status="danger" />
                </template>
              </a-table-column>
              <a-table-column title="关键字段">
                <template #cell="{ record }">
                  <a-tag v-for="col in record.keyColumns" :key="col" size="small">{{ col }}</a-tag>
                </template>
              </a-table-column>
            </a-table>
            <a-empty v-else description="未发现重复数据" />
          </a-tab-pane>

          <a-tab-pane key="outliers" title="异常值">
            <a-table :data="report.outliers" v-if="report.outliers?.length" :bordered="true">
              <a-table-column title="字段名" data-index="columnName" />
              <a-table-column title="异常数量" data-index="outlierCount" />
              <a-table-column title="异常率">
                <template #cell="{ record }">
                  <a-progress :percent="record.outlierPercentage" />
                </template>
              </a-table-column>
              <a-table-column title="检测方法" data-index="detectionMethod" />
              <a-table-column title="正常范围">
                <template #cell="{ record }">
                  {{ record.minNormal?.toFixed(2) }} ~ {{ record.maxNormal?.toFixed(2) }}
                </template>
              </a-table-column>
            </a-table>
            <a-empty v-else description="未发现异常值" />
          </a-tab-pane>

          <a-tab-pane key="format" title="格式问题">
            <a-table :data="report.formatIssues" v-if="report.formatIssues?.length" :bordered="true">
              <a-table-column title="字段名" data-index="columnName" />
              <a-table-column title="期望格式" data-index="expectedFormat" />
              <a-table-column title="问题数量" data-index="issueCount" />
            </a-table>
            <a-empty v-else description="未发现格式问题" />
          </a-tab-pane>

          <a-tab-pane key="columns" title="字段质量">
            <a-table :data="report.columnQuality" v-if="report.columnQuality?.length" :bordered="true">
              <a-table-column title="字段名" data-index="columnName" />
              <a-table-column title="数据类型" data-index="dataType" />
              <a-table-column title="完整性">
                <template #cell="{ record }">
                  <a-progress :percent="record.completeness" />
                </template>
              </a-table-column>
              <a-table-column title="等级">
                <template #cell="{ record }">
                  <a-tag :color="getGradeColor(record.grade)">{{ record.grade }}</a-tag>
                </template>
              </a-table-column>
            </a-table>
          </a-tab-pane>
        </a-tabs>

        <!-- 修复建议 -->
        <a-card class="suggestions-card" v-if="report.repairSuggestions?.length">
          <template #title>
            <span>修复建议</span>
          </template>
          <a-timeline>
            <a-timeline-item
              v-for="(suggestion, index) in report.repairSuggestions"
              :key="index"
              :dot-color="getSuggestionColor(suggestion.priority)"
            >
              <template #label>{{ suggestion.issueType }}</template>
              <h4>{{ suggestion.description }}</h4>
              <p>{{ suggestion.suggestion }}</p>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </div>

      <a-empty v-else description="请选择数据表并开始检测" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconSearch } from '@arco-design/web-vue/es/icon'
import request from '@/utils/request'

const dataSources = ref([])
const tables = ref([])
const selectedDataSource = ref(null)
const selectedTable = ref(null)
const checking = ref(false)
const report = ref(null)

// 加载数据源
const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    console.log('Data sources response:', res)
    dataSources.value = res.data
    console.log('Data sources:', dataSources.value)
    if (dataSources.value.length > 0) {
      // 优先选择默认数据源，否则选择第一个
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
      console.log('Selected data source:', selectedDataSource.value)
      onDataSourceChange()
    }
  } catch (error) {
    console.error('加载数据源失败:', error)
    Message.error('加载数据源失败')
  }
}

// 数据源变化
const onDataSourceChange = async () => {
  console.log('onDataSourceChange called, selectedDataSource:', selectedDataSource.value)
  selectedTable.value = null
  tables.value = []
  if (!selectedDataSource.value) return

  try {
    const res = await request.get('/api/data/tables', {
      params: { dataSourceId: selectedDataSource.value }
    })
    console.log('Tables response:', res)
    // 兼容两种返回格式: {success: true, data: [...]} 或 [...]
    tables.value = res.data || res
    console.log('Tables set to:', tables.value)
  } catch (error) {
    console.error('加载数据表失败:', error)
    Message.error('加载数据表失败')
  }
}

// 检测质量
const checkQuality = async () => {
  if (!selectedDataSource.value || !selectedTable.value) return

  checking.value = true
  try {
    const res = await request.get(`/api/data-quality/check/${selectedDataSource.value}`, {
      params: { tableName: selectedTable.value }
    })
    report.value = res.data
    Message.success('检测完成')
  } catch (error) {
    Message.error('检测失败')
  } finally {
    checking.value = false
  }
}

const getGradeClass = (grade) => {
  const classes = {
    'A': 'grade-a',
    'B': 'grade-b',
    'C': 'grade-c',
    'D': 'grade-d'
  }
  return classes[grade] || ''
}

const getGradeColor = (grade) => {
  const colors = {
    'A': 'green',
    'B': 'arcoblue',
    'C': 'orange',
    'D': 'red'
  }
  return colors[grade] || 'gray'
}

const getSeverityColor = (severity) => {
  const colors = {
    '严重': 'red',
    '警告': 'orange',
    '轻微': 'arcoblue'
  }
  return colors[severity] || 'gray'
}

const getMissingValueStatus = (percentage) => {
  if (percentage > 50) return 'danger'
  if (percentage > 20) return 'warning'
  return 'success'
}

const getSuggestionColor = (priority) => {
  const colors = {
    1: 'red',
    2: 'orange',
    3: 'arcoblue'
  }
  return colors[priority] || 'gray'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped lang="scss">
.data-quality-check {
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

  .quality-report {
    .score-section {
      margin-bottom: 20px;

      .score-card {
        text-align: center;
        padding: 20px;
        border-radius: 8px;
        color: white;

        &.grade-a {
          background: linear-gradient(135deg, rgb(var(--success-6)), rgb(var(--success-4)));
        }
        &.grade-b {
          background: linear-gradient(135deg, rgb(var(--primary-6)), rgb(var(--primary-4)));
        }
        &.grade-c {
          background: linear-gradient(135deg, rgb(var(--warning-6)), rgb(var(--warning-4)));
        }
        &.grade-d {
          background: linear-gradient(135deg, rgb(var(--danger-6)), rgb(var(--danger-4)));
        }

        .score-value {
          font-size: 48px;
          font-weight: bold;
        }

        .score-grade {
          font-size: 24px;
          margin: 10px 0;
        }

        .score-label {
          font-size: 14px;
          opacity: 0.9;
        }
      }
    }

    .issue-stats {
      margin-bottom: 20px;
      padding: 20px;
      background: var(--color-fill-2);
      border-radius: 4px;
    }

    .detail-tabs {
      margin-bottom: 20px;
    }

    .suggestions-card {
      margin-top: 20px;

      h4 {
        margin: 0 0 5px 0;
        color: var(--color-text-1);
      }

      p {
        margin: 0;
        color: var(--color-text-2);
        font-size: 14px;
      }
    }
  }
}
</style>
