<template>
  <div class="data-quality-check">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据质量检测</span>
          <div class="header-actions">
            <el-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange">
              <el-option
                v-for="ds in dataSources"
                :key="ds.id"
                :label="ds.name"
                :value="ds.id"
              />
            </el-select>
            <el-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables?.length">
              <el-option
                v-for="table in tables"
                :key="table"
                :label="table"
                :value="table"
              />
            </el-select>
            <el-button type="primary" @click="checkQuality" :loading="checking" :disabled="!selectedTable">
              <el-icon><Search /></el-icon>
              开始检测
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="report" class="quality-report">
        <!-- 总体评分 -->
        <el-row :gutter="20" class="score-section">
          <el-col :span="6">
            <div class="score-card" :class="getGradeClass(report.grade)">
              <div class="score-value">{{ report.overallScore?.toFixed(1) }}</div>
              <div class="score-grade">{{ report.grade }}级</div>
              <div class="score-label">综合评分</div>
            </div>
          </el-col>
          <el-col :span="18">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="表名">{{ report.tableName }}</el-descriptions-item>
              <el-descriptions-item label="总行数">{{ report.totalRows?.toLocaleString() }}</el-descriptions-item>
              <el-descriptions-item label="总列数">{{ report.totalColumns }}</el-descriptions-item>
              <el-descriptions-item label="检测时间">{{ formatDate(report.checkTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>

        <!-- 问题统计 -->
        <el-row :gutter="20" class="issue-stats">
          <el-col :span="6">
            <el-statistic title="缺失值问题" :value="report.missingValues?.length || 0">
              <template #suffix>个字段</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="重复数据" :value="report.duplicates?.length || 0">
              <template #suffix>处</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="异常值" :value="report.outliers?.length || 0">
              <template #suffix>个字段</template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="格式问题" :value="report.formatIssues?.length || 0">
              <template #suffix>处</template>
            </el-statistic>
          </el-col>
        </el-row>

        <!-- 详细问题列表 -->
        <el-tabs type="border-card" class="detail-tabs">
          <el-tab-pane label="缺失值">
            <el-table :data="report.missingValues" v-if="report.missingValues?.length">
              <el-table-column prop="columnName" label="字段名" />
              <el-table-column prop="missingCount" label="缺失数量" />
              <el-table-column prop="missingPercentage" label="缺失率">
                <template #default="{ row }">
                  <el-progress :percentage="row.missingPercentage" :status="getMissingValueStatus(row.missingPercentage)" />
                </template>
              </el-table-column>
              <el-table-column prop="severity" label="严重程度">
                <template #default="{ row }">
                  <el-tag :type="getSeverityType(row.severity)">{{ row.severity }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="未发现缺失值问题" />
          </el-tab-pane>

          <el-tab-pane label="重复数据">
            <el-table :data="report.duplicates" v-if="report.duplicates?.length">
              <el-table-column prop="duplicateRowCount" label="重复行数" />
              <el-table-column prop="duplicatePercentage" label="重复率">
                <template #default="{ row }">
                  <el-progress :percentage="row.duplicatePercentage" status="exception" />
                </template>
              </el-table-column>
              <el-table-column prop="keyColumns" label="关键字段">
                <template #default="{ row }">
                  <el-tag v-for="col in row.keyColumns" :key="col" size="small">{{ col }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="未发现重复数据" />
          </el-tab-pane>

          <el-tab-pane label="异常值">
            <el-table :data="report.outliers" v-if="report.outliers?.length">
              <el-table-column prop="columnName" label="字段名" />
              <el-table-column prop="outlierCount" label="异常数量" />
              <el-table-column prop="outlierPercentage" label="异常率">
                <template #default="{ row }">
                  <el-progress :percentage="row.outlierPercentage" />
                </template>
              </el-table-column>
              <el-table-column prop="detectionMethod" label="检测方法" />
              <el-table-column label="正常范围">
                <template #default="{ row }">
                  {{ row.minNormal?.toFixed(2) }} ~ {{ row.maxNormal?.toFixed(2) }}
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="未发现异常值" />
          </el-tab-pane>

          <el-tab-pane label="格式问题">
            <el-table :data="report.formatIssues" v-if="report.formatIssues?.length">
              <el-table-column prop="columnName" label="字段名" />
              <el-table-column prop="expectedFormat" label="期望格式" />
              <el-table-column prop="issueCount" label="问题数量" />
            </el-table>
            <el-empty v-else description="未发现格式问题" />
          </el-tab-pane>

          <el-tab-pane label="字段质量">
            <el-table :data="report.columnQuality" v-if="report.columnQuality?.length">
              <el-table-column prop="columnName" label="字段名" />
              <el-table-column prop="dataType" label="数据类型" />
              <el-table-column prop="completeness" label="完整性">
                <template #default="{ row }">
                  <el-progress :percentage="row.completeness" />
                </template>
              </el-table-column>
              <el-table-column prop="grade" label="等级">
                <template #default="{ row }">
                  <el-tag :type="getGradeType(row.grade)">{{ row.grade }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>

        <!-- 修复建议 -->
        <el-card class="suggestions-card" v-if="report.repairSuggestions?.length">
          <template #header>
            <span>修复建议</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(suggestion, index) in report.repairSuggestions"
              :key="index"
              :type="getSuggestionType(suggestion.priority)"
              :timestamp="suggestion.issueType"
            >
              <h4>{{ suggestion.description }}</h4>
              <p>{{ suggestion.suggestion }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>

      <el-empty v-else description="请选择数据表并开始检测" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
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
    dataSources.value = res.data
    if (dataSources.value.length > 0) {
      // 优先选择默认数据源，否则选择第一个
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
      onDataSourceChange()
    }
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

// 数据源变化
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

// 检测质量
const checkQuality = async () => {
  if (!selectedDataSource.value || !selectedTable.value) return

  checking.value = true
  try {
    const res = await request.get(`/api/data-quality/check/${selectedDataSource.value}`, {
      params: { tableName: selectedTable.value }
    })
    report.value = res.data
    ElMessage.success('检测完成')
  } catch (error) {
    ElMessage.error('检测失败')
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

const getGradeType = (grade) => {
  const types = {
    'A': 'success',
    'B': 'primary',
    'C': 'warning',
    'D': 'danger'
  }
  return types[grade] || 'info'
}

const getSeverityType = (severity) => {
  const types = {
    '严重': 'danger',
    '警告': 'warning',
    '轻微': 'info'
  }
  return types[severity] || 'info'
}

const getMissingValueStatus = (percentage) => {
  if (percentage > 50) return 'exception'
  if (percentage > 20) return 'warning'
  return 'success'
}

const getSuggestionType = (priority) => {
  const types = {
    1: 'danger',
    2: 'warning',
    3: 'primary'
  }
  return types[priority] || 'info'
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

      .el-select {
        width: 200px;
      }
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
          background: linear-gradient(135deg, #67c23a, #85ce61);
        }
        &.grade-b {
          background: linear-gradient(135deg, #409eff, #66b1ff);
        }
        &.grade-c {
          background: linear-gradient(135deg, #e6a23c, #ebb563);
        }
        &.grade-d {
          background: linear-gradient(135deg, #f56c6c, #f78989);
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
      background: #f5f7fa;
      border-radius: 4px;
    }

    .detail-tabs {
      margin-bottom: 20px;
    }

    .suggestions-card {
      margin-top: 20px;

      h4 {
        margin: 0 0 5px 0;
        color: #303133;
      }

      p {
        margin: 0;
        color: #606266;
        font-size: 14px;
      }
    }
  }
}
</style>
