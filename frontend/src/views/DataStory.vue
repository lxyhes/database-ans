<template>
  <div class="data-story">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>一键数据故事生成</span>
          <div class="header-actions">
            <a-select v-model="selectedDataSource" placeholder="选择数据源" @change="onDataSourceChange" style="width: 200px;">
              <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
            </a-select>
            <a-select v-model="selectedTable" placeholder="选择数据表" :disabled="!tables?.length" style="width: 200px;">
              <a-option v-for="table in tables" :key="table" :value="table">{{ table }}</a-option>
            </a-select>
            <a-button type="primary" @click="generateStory" :loading="generating" :disabled="!selectedTable">
              <template #icon><icon-bulb /></template>
              生成数据故事
            </a-button>
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
        <a-row :gutter="20" class="statistics-row">
          <a-col :span="8">
            <a-statistic title="总记录数" :value="story.statistics?.totalRows || 0" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="字段数量" :value="story.statistics?.columnCount || 0" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="洞察数量" :value="story.insights?.length || 0" />
          </a-col>
        </a-row>

        <!-- 故事正文 -->
        <a-card class="story-text-card">
          <template #title>
            <div class="card-title-with-action">
              <span>数据洞察报告</span>
              <a-button type="text" @click="copyStory">
                <template #icon><icon-copy /></template>
                复制
              </a-button>
            </div>
          </template>
          <div class="story-text" v-html="formattedStoryText"></div>
        </a-card>

        <!-- 关键发现 -->
        <a-card class="insights-card">
          <template #title>
            <span>关键发现</span>
          </template>
          <a-timeline>
            <a-timeline-item
              v-for="(insight, index) in story.insights"
              :key="index"
              :dot-color="index === 0 ? '#165dff' : index === 1 ? '#00b42a' : '#ff7d00'"
            >
              {{ insight }}
            </a-timeline-item>
          </a-timeline>
        </a-card>

        <!-- 字段统计 -->
        <a-card class="columns-card">
          <template #title>
            <span>字段统计信息</span>
          </template>
          <a-table :data="story.statistics?.columns" size="small" :bordered="true">
            <a-table-column title="字段名" data-index="name" />
            <a-table-column title="数据类型" data-index="type" />
            <a-table-column title="统计信息">
              <template #cell="{ record }">
                <div v-if="record.statistics">
                  <a-tag size="small">最小: {{ formatNumber(record.statistics.min) }}</a-tag>
                  <a-tag size="small" style="margin-left: 5px;">最大: {{ formatNumber(record.statistics.max) }}</a-tag>
                  <a-tag size="small" style="margin-left: 5px;">平均: {{ formatNumber(record.statistics.avg) }}</a-tag>
                </div>
                <span v-else>-</span>
              </template>
            </a-table-column>
          </a-table>
        </a-card>
      </div>

      <a-empty v-else description="选择数据表并点击生成按钮，开始创建数据故事" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconBulb, IconCopy } from '@arco-design/web-vue/es/icon'
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
    if (dataSources.value.length > 0) {
      // 优先选择默认数据源，否则选择第一个
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
      onDataSourceChange()
    }
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

const onDataSourceChange = async () => {
  selectedTable.value = null
  tables.value = []
  if (!selectedDataSource.value) return
  try {
    const res = await request.get('/api/data/tables', {
      params: { dataSourceId: selectedDataSource.value }
    })
    // 兼容两种返回格式: {success: true, data: [...]} 或 [...]
    tables.value = res.data || res
  } catch (error) {
    Message.error('加载数据表失败')
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
    // 兼容两种返回格式: {success: true, data: {...}} 或 {...}
    story.value = res.data || res
    Message.success('数据故事生成成功')
  } catch (error) {
    Message.error('生成失败')
  } finally {
    generating.value = false
  }
}

const copyStory = () => {
  if (story.value?.storyText) {
    navigator.clipboard.writeText(story.value.storyText)
    Message.success('已复制到剪贴板')
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

  .card-title-with-action {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .story-content {
    .story-header {
      text-align: center;
      margin-bottom: 30px;

      h1 {
        font-size: 28px;
        color: var(--color-text-1);
        margin-bottom: 10px;
      }

      .story-meta {
        color: var(--color-text-3);
        font-size: 14px;
      }
    }

    .statistics-row {
      margin-bottom: 30px;
      padding: 20px;
      background: var(--color-fill-2);
      border-radius: 4px;
    }

    .story-text-card {
      margin-bottom: 20px;

      .story-text {
        line-height: 1.8;
        color: var(--color-text-2);

        h1 {
          font-size: 24px;
          color: var(--color-text-1);
          margin-bottom: 20px;
        }

        h2 {
          font-size: 18px;
          color: var(--color-text-1);
          margin: 20px 0 10px 0;
        }

        strong {
          color: rgb(var(--primary-6));
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
