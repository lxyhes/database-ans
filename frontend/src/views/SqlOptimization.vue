<template>
  <div class="sql-optimization">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>SQL 性能优化</span>
        </div>
      </template>

      <a-row :gutter="20">
        <a-col :span="12">
          <a-card>
            <template #title>
              <div class="card-title-with-action">
                <span>原始 SQL</span>
                <a-button type="primary" size="small" @click="analyzeSql" :loading="analyzing">
                  <template #icon><icon-search /></template>
                  分析
                </a-button>
              </div>
            </template>
            <a-textarea
              v-model="originalSql"
              :rows="15"
              placeholder="请输入需要分析的SQL语句"
              allow-clear
            />
          </a-card>
        </a-col>

        <a-col :span="12">
          <a-card v-if="analysisResult">
            <template #title>
              <div class="card-title-with-action">
                <span>优化建议</span>
                <a-tag :color="getGradeColor(analysisResult.grade)" class="score-badge">
                  {{ analysisResult.score }}分
                </a-tag>
              </div>
            </template>

            <div class="suggestions-list">
              <a-empty v-if="!analysisResult.suggestions?.length" description="暂无优化建议" />
              <a-timeline v-else>
                <a-timeline-item
                  v-for="(suggestion, index) in analysisResult.suggestions"
                  :key="index"
                  :dot-color="getSeverityColor(suggestion.severity)"
                >
                  <h4>{{ suggestion.title }}</h4>
                  <p class="description">{{ suggestion.description }}</p>
                  <p class="solution">
                    <icon-info-circle />
                    建议：{{ suggestion.solution }}
                  </p>
                </a-timeline-item>
              </a-timeline>
            </div>
          </a-card>

          <a-card v-if="optimizedSql" style="margin-top: 20px;">
            <template #title>
              <div class="card-title-with-action">
                <span>优化后的 SQL</span>
                <a-button type="primary" status="success" size="small" @click="copyOptimizedSql">
                  <template #icon><icon-copy /></template>
                  复制
                </a-button>
              </div>
            </template>
            <pre class="sql-code">{{ optimizedSql }}</pre>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconSearch, IconInfoCircle, IconCopy } from '@arco-design/web-vue/es/icon'
import request from '@/utils/request'

const originalSql = ref('')
const analysisResult = ref(null)
const optimizedSql = ref('')
const analyzing = ref(false)

const analyzeSql = async () => {
  if (!originalSql.value.trim()) {
    Message.warning('请输入SQL语句')
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
    Message.error('分析失败')
  } finally {
    analyzing.value = false
  }
}

const copyOptimizedSql = () => {
  navigator.clipboard.writeText(optimizedSql.value)
  Message.success('已复制到剪贴板')
}

const getGradeColor = (grade) => {
  const colors = {
    'A': 'green',
    'B': 'orange',
    'C': 'red',
    'D': 'gray'
  }
  return colors[grade] || 'gray'
}

const getSeverityColor = (severity) => {
  const colors = {
    'critical': 'red',
    'high': 'orange',
    'medium': 'arcoblue',
    'low': 'gray'
  }
  return colors[severity] || 'gray'
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

  .card-title-with-action {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .score-badge {
    font-weight: bold;
  }

  .suggestions-list {
    max-height: 400px;
    overflow-y: auto;

    h4 {
      margin: 0 0 8px 0;
      color: var(--color-text-1);
    }

    .description {
      margin: 0 0 8px 0;
      color: var(--color-text-2);
      font-size: 14px;
    }

    .solution {
      margin: 0;
      color: rgb(var(--primary-6));
      font-size: 13px;
    }
  }

  .sql-code {
    background: var(--color-fill-2);
    padding: 15px;
    border-radius: 4px;
    overflow-x: auto;
    font-family: monospace;
    font-size: 13px;
    line-height: 1.6;
  }
}
</style>
