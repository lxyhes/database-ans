<template>
  <div class="sql-optimization">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>SQL 性能优化</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>原始 SQL</span>
              <el-button type="primary" size="small" @click="analyzeSql" :loading="analyzing" style="float: right;">
                <el-icon><Search /></el-icon>
                分析
              </el-button>
            </template>
            <el-input
              v-model="originalSql"
              type="textarea"
              :rows="15"
              placeholder="请输入需要分析的SQL语句"
            />
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card v-if="analysisResult">
            <template #header>
              <span>优化建议</span>
              <div class="score-badge" :class="getGradeClass(analysisResult.grade)">
                {{ analysisResult.score }}分
              </div>
            </template>

            <div class="suggestions-list">
              <el-empty v-if="!analysisResult.suggestions?.length" description="暂无优化建议" />
              <el-timeline v-else>
                <el-timeline-item
                  v-for="(suggestion, index) in analysisResult.suggestions"
                  :key="index"
                  :type="getSeverityType(suggestion.severity)"
                >
                  <h4>{{ suggestion.title }}</h4>
                  <p class="description">{{ suggestion.description }}</p>
                  <p class="solution">
                    <el-icon><InfoFilled /></el-icon>
                    建议：{{ suggestion.solution }}
                  </p>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>

          <el-card v-if="optimizedSql" style="margin-top: 20px;">
            <template #header>
              <span>优化后的 SQL</span>
              <el-button type="success" size="small" @click="copyOptimizedSql" style="float: right;">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </template>
            <pre class="sql-code">{{ optimizedSql }}</pre>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, InfoFilled, CopyDocument } from '@element-plus/icons-vue'
import request from '@/utils/request'

const originalSql = ref('')
const analysisResult = ref(null)
const optimizedSql = ref('')
const analyzing = ref(false)

const analyzeSql = async () => {
  if (!originalSql.value.trim()) {
    ElMessage.warning('请输入SQL语句')
    return
  }

  analyzing.value = true
  try {
    const res = await request.post('/api/sql-optimization/analyze', {
      sql: originalSql.value
    })
    analysisResult.value = res.data

    // 同时获取优化后的SQL
    const optimizeRes = await request.post('/api/sql-optimization/optimize', {
      sql: originalSql.value
    })
    optimizedSql.value = optimizeRes.data.optimized
  } catch (error) {
    ElMessage.error('分析失败')
  } finally {
    analyzing.value = false
  }
}

const copyOptimizedSql = () => {
  navigator.clipboard.writeText(optimizedSql.value)
  ElMessage.success('已复制到剪贴板')
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

const getSeverityType = (severity) => {
  const types = {
    'critical': 'danger',
    'high': 'warning',
    'medium': 'primary',
    'low': 'info'
  }
  return types[severity] || 'info'
}
</script>

<style scoped lang="scss">
.sql-optimization {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .score-badge {
    float: right;
    padding: 5px 15px;
    border-radius: 15px;
    color: white;
    font-weight: bold;

    &.grade-a {
      background: #67c23a;
    }
    &.grade-b {
      background: #e6a23c;
    }
    &.grade-c {
      background: #f56c6c;
    }
    &.grade-d {
      background: #909399;
    }
  }

  .suggestions-list {
    max-height: 400px;
    overflow-y: auto;

    h4 {
      margin: 0 0 8px 0;
      color: #303133;
    }

    .description {
      margin: 0 0 8px 0;
      color: #606266;
      font-size: 14px;
    }

    .solution {
      margin: 0;
      color: #409eff;
      font-size: 13px;
    }
  }

  .sql-code {
    background: #f5f7fa;
    padding: 15px;
    border-radius: 4px;
    overflow-x: auto;
    font-family: monospace;
    font-size: 13px;
    line-height: 1.6;
  }
}
</style>
