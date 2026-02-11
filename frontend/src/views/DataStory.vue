<template>
  <div class="data-story">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>一键数据故事生成</span>
          <div class="header-actions">
            <el-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange">
              <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
            <el-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables.length">
              <el-option v-for="table in tables" :key="table" :label="table" :value="table" />
            </el-select>
            <el-button type="primary" @click="generateStory" :loading="generating" :disabled="!selectedTable">
              <el-icon><MagicStick /></el-icon>
              生成数据故事
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="story" class="story-content">
        <!-- 故事标题 -->
        <div class="story-header">
          <h1>{{ story.title }}</h1>
          <p class="story-meta">生成时间：{{ formatDate(story.generatedAt) }}</p>
        </div>

        <!-- 数据概览 -->
        <el-row :gutter="20" class="statistics-row">
          <el-col :span="8">
            <el-statistic title="总记录数" :value="story.statistics?.totalRows || 0" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="字段数量" :value="story.statistics?.columnCount || 0" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="洞察数量" :value="story.insights?.length || 0" />
          </el-col>
        </el-row>

        <!-- 故事正文 -->
        <el-card class="story-text-card">
          <template #header>
            <span>数据洞察报告</span>
            <el-button type="primary" link @click="copyStory" style="float: right;">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </template>
          <div class="story-text" v-html="formattedStoryText"></div>
        </el-card>

        <!-- 关键发现 -->
        <el-card class="insights-card">
          <template #header>
            <span>关键发现</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(insight, index) in story.insights"
              :key="index"
              :type="index === 0 ? 'primary' : index === 1 ? 'success' : 'warning'"
            >
              {{ insight }}
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 字段统计 -->
        <el-card class="columns-card">
          <template #header>
            <span>字段统计信息</span>
          </template>
          <el-table :data="story.statistics?.columns" size="small">
            <el-table-column prop="name" label="字段名" />
            <el-table-column prop="type" label="数据类型" />
            <el-table-column label="统计信息">
              <template #default="{ row }">
                <div v-if="row.statistics">
                  <el-tag size="small">最小: {{ formatNumber(row.statistics.min) }}</el-tag>
                  <el-tag size="small" style="margin-left: 5px;">最大: {{ formatNumber(row.statistics.max) }}</el-tag>
                  <el-tag size="small" style="margin-left: 5px;">平均: {{ formatNumber(row.statistics.avg) }}</el-tag>
                </div>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <el-empty v-else description="选择数据表并点击生成按钮，开始创建数据故事" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, CopyDocument } from '@element-plus/icons-vue'
import request from '@/utils/request'

const dataSources = ref([])
const tables = ref([])
const selectedDataSource = ref(null)
const selectedTable = ref(null)
const story = ref(null)
const generating = ref(false)

const formattedStoryText = computed(() => {
  if (!story.value?.storyText) return ''
  // 将Markdown转换为HTML
  return story.value.storyText
    .replace(/# (.*)/, '<h1>$1</h1>')
    .replace(/## (.*)/g, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/- (.*)/g, '<li>$1</li>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/(\d+)\. (.*)/g, '<p><strong>$1.</strong> $2</p>')
})

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

const onDataSourceChange = async () => {
  selectedTable.value = null
  tables.value = []
  if (!selectedDataSource.value) return
  try {
    const res = await request.get(`/api/datasources/${selectedDataSource.value}/tables`)
    tables.value = res.data
  } catch (error) {
    ElMessage.error('加载数据表失败')
  }
}

const generateStory = async () => {
  if (!selectedDataSource.value || !selectedTable.value) return

  generating.value = true
  try {
    const res = await request.post('/api/data-story/generate', {
      dataSourceId: selectedDataSource.value,
      tableName: selectedTable.value
    })
    story.value = res.data
    ElMessage.success('数据故事生成成功')
  } catch (error) {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const copyStory = () => {
  if (story.value?.storyText) {
    navigator.clipboard.writeText(story.value.storyText)
    ElMessage.success('已复制到剪贴板')
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

const formatNumber = (num) => {
  if (num === null || num === undefined) return '-'
  if (typeof num === 'number') {
    return num.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
  }
  return num
}

loadDataSources()
</script>

<style scoped lang="scss">
.data-story {
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

  .story-content {
    .story-header {
      text-align: center;
      margin-bottom: 30px;

      h1 {
        font-size: 28px;
        color: #303133;
        margin-bottom: 10px;
      }

      .story-meta {
        color: #909399;
        font-size: 14px;
      }
    }

    .statistics-row {
      margin-bottom: 30px;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 4px;
    }

    .story-text-card {
      margin-bottom: 20px;

      .story-text {
        line-height: 1.8;
        color: #606266;

        h1 {
          font-size: 24px;
          color: #303133;
          margin-bottom: 20px;
        }

        h2 {
          font-size: 18px;
          color: #303133;
          margin: 20px 0 10px 0;
        }

        strong {
          color: #409eff;
        }

        li {
          margin: 8px 0;
          padding-left: 20px;
        }
      }
    }

    .insights-card {
      margin-bottom: 20px;
    }

    .columns-card {
      margin-bottom: 20px;
    }
  }
}
</style>
