<template>
  <div class="report-generator">
    <a-row :gutter="20">
      <a-col :span="8">
        <a-card title="报告配置" class="config-card">
          <a-form :model="reportConfig" layout="vertical">
            <a-form-item label="数据源" required>
              <a-select v-model="reportConfig.dataSourceId" placeholder="选择数据源" @change="onDataSourceChange">
                <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="数据表" required>
              <a-select v-model="reportConfig.tableName" placeholder="选择数据表" multiple>
                <a-option v-for="table in tables" :key="table" :value="table">{{ table }}</a-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="报告标题">
              <a-input v-model="reportConfig.title" placeholder="输入报告标题" />
            </a-form-item>
            
            <a-form-item label="分析维度">
              <a-checkbox-group v-model="reportConfig.dimensions">
                <a-checkbox value="summary">数据概览</a-checkbox>
                <a-checkbox value="distribution">分布分析</a-checkbox>
                <a-checkbox value="trend">趋势分析</a-checkbox>
                <a-checkbox value="comparison">对比分析</a-checkbox>
                <a-checkbox value="anomaly">异常检测</a-checkbox>
                <a-checkbox value="correlation">相关性分析</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
            
            <a-form-item label="时间范围">
              <a-range-picker v-model="reportConfig.dateRange" style="width: 100%" />
            </a-form-item>
            
            <a-form-item label="报告模板">
              <a-select v-model="reportConfig.templateId" placeholder="选择模板(可选)" allow-clear>
                <a-option v-for="tpl in templates" :key="tpl.id" :value="tpl.id">{{ tpl.name }}</a-option>
              </a-select>
            </a-form-item>
            
            <a-form-item>
              <a-space>
                <a-button type="primary" :loading="generating" @click="generateReport">
                  <template #icon><icon-file /></template>
                  生成报告
                </a-button>
                <a-button @click="saveAsTemplate">
                  <template #icon><icon-save /></template>
                  保存为模板
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      
      <a-col :span="16">
        <a-card title="报告预览" class="preview-card">
          <template #extra>
            <a-space>
              <a-button @click="exportReport('pdf')" :disabled="!generatedReport">
                <template #icon><icon-download /></template>
                导出PDF
              </a-button>
              <a-button @click="exportReport('word')" :disabled="!generatedReport">
                <template #icon><icon-file /></template>
                导出Word
              </a-button>
            </a-space>
          </template>
          
          <div v-if="generating" class="loading-container">
            <a-spin size="32" />
            <p>正在生成报告，请稍候...</p>
          </div>
          
          <div v-else-if="generatedReport" class="report-content">
            <h1 class="report-title">{{ generatedReport.title }}</h1>
            <p class="report-meta">生成时间: {{ formatTime(generatedReport.createdAt) }}</p>
            
            <a-divider />
            
            <section v-if="generatedReport.summary" class="report-section">
              <h2>数据概览</h2>
              <a-row :gutter="16">
                <a-col :span="6" v-for="(item, idx) in generatedReport.summary.stats" :key="idx">
                  <a-statistic :title="item.label" :value="item.value" :suffix="item.suffix" />
                </a-col>
              </a-row>
              <div class="insight-text">{{ generatedReport.summary.insight }}</div>
            </section>
            
            <section v-if="generatedReport.distribution" class="report-section">
              <h2>分布分析</h2>
              <a-row :gutter="16">
                <a-col :span="12" v-for="(chart, idx) in generatedReport.distribution.charts" :key="idx">
                  <div class="chart-container">
                    <v-chart :option="chart.option" autoresize style="height: 250px" />
                  </div>
                </a-col>
              </a-row>
              <div class="insight-text">{{ generatedReport.distribution.insight }}</div>
            </section>
            
            <section v-if="generatedReport.trend" class="report-section">
              <h2>趋势分析</h2>
              <div class="chart-container">
                <v-chart :option="generatedReport.trend.chart" autoresize style="height: 300px" />
              </div>
              <div class="insight-text">{{ generatedReport.trend.insight }}</div>
            </section>
            
            <section v-if="generatedReport.comparison" class="report-section">
              <h2>对比分析</h2>
              <a-table :data="generatedReport.comparison.data" :bordered="true" size="small">
                <a-table-column title="维度" data-index="dimension" />
                <a-table-column title="当前值" data-index="current" />
                <a-table-column title="对比值" data-index="compare" />
                <a-table-column title="变化率" data-index="changeRate">
                  <template #cell="{ record }">
                    <span :class="record.changeRate > 0 ? 'positive' : 'negative'">
                      {{ record.changeRate > 0 ? '+' : '' }}{{ record.changeRate }}%
                    </span>
                  </template>
                </a-table-column>
              </a-table>
              <div class="insight-text">{{ generatedReport.comparison.insight }}</div>
            </section>
            
            <section v-if="generatedReport.anomaly" class="report-section">
              <h2>异常检测</h2>
              <a-alert v-if="generatedReport.anomaly.hasAnomaly" type="warning">
                检测到 {{ generatedReport.anomaly.count }} 个异常点
              </a-alert>
              <a-alert v-else type="success">未检测到明显异常</a-alert>
              <div class="insight-text">{{ generatedReport.anomaly.insight }}</div>
            </section>
            
            <section v-if="generatedReport.correlation" class="report-section">
              <h2>相关性分析</h2>
              <div class="chart-container">
                <v-chart :option="generatedReport.correlation.chart" autoresize style="height: 300px" />
              </div>
              <div class="insight-text">{{ generatedReport.correlation.insight }}</div>
            </section>
            
            <section class="report-section">
              <h2>总结与建议</h2>
              <div class="conclusion">{{ generatedReport.conclusion }}</div>
            </section>
          </div>
          
          <a-empty v-else description="请配置报告参数并点击生成" />
        </a-card>
      </a-col>
    </a-row>
    
    <a-modal v-model:visible="templateDialogVisible" title="保存为模板" @ok="confirmSaveTemplate">
      <a-form :model="templateForm" layout="vertical">
        <a-form-item label="模板名称" required>
          <a-input v-model="templateForm.name" placeholder="输入模板名称" />
        </a-form-item>
        <a-form-item label="模板描述">
          <a-textarea v-model="templateForm.description" placeholder="模板描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconFile, IconSave, IconDownload } from '@arco-design/web-vue/es/icon'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '@/utils/request'

use([CanvasRenderer, BarChart, LineChart, PieChart, ScatterChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const dataSources = ref<any[]>([])
const tables = ref<string[]>([])
const templates = ref<any[]>([])
const generating = ref(false)
const generatedReport = ref<any>(null)
const templateDialogVisible = ref(false)

const reportConfig = ref<any>({
  dataSourceId: null,
  tableName: [],
  title: '',
  dimensions: ['summary', 'distribution', 'trend'],
  dateRange: [],
  templateId: null
})

const templateForm = ref({
  name: '',
  description: ''
})

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data || res
  } catch (error) {
    console.error('加载数据源失败', error)
  }
}

const onDataSourceChange = async () => {
  if (!reportConfig.value.dataSourceId) return
  try {
    const res = await request.get('/api/data/tables', { params: { dataSourceId: reportConfig.value.dataSourceId } })
    tables.value = res.data || res
  } catch (error) {
    console.error('加载表列表失败', error)
  }
}

const loadTemplates = async () => {
  try {
    const res = await request.get('/api/reports/templates')
    templates.value = res.data || res
  } catch (error) {
    console.error('加载模板失败', error)
  }
}

const generateReport = async () => {
  if (!reportConfig.value.dataSourceId) {
    Message.warning('请选择数据源')
    return
  }
  if (!reportConfig.value.tableName?.length) {
    Message.warning('请选择数据表')
    return
  }
  
  generating.value = true
  try {
    const res = await request.post('/api/reports/generate', {
      dataSourceId: reportConfig.value.dataSourceId,
      tableNames: reportConfig.value.tableName,
      title: reportConfig.value.title || `${reportConfig.value.tableName.join(', ')} 数据分析报告`,
      dimensions: reportConfig.value.dimensions,
      dateRange: reportConfig.value.dateRange,
      templateId: reportConfig.value.templateId
    })
    
    if (res.success) {
      generatedReport.value = res.data
      Message.success('报告生成成功')
    } else {
      Message.error(res.message || '生成失败')
    }
  } catch (error) {
    Message.error('生成报告失败')
  } finally {
    generating.value = false
  }
}

const saveAsTemplate = () => {
  templateForm.value = { name: '', description: '' }
  templateDialogVisible.value = true
}

const confirmSaveTemplate = async () => {
  if (!templateForm.value.name) {
    Message.warning('请输入模板名称')
    return
  }
  
  try {
    await request.post('/api/reports/templates', {
      ...templateForm.value,
      config: reportConfig.value
    })
    Message.success('模板保存成功')
    templateDialogVisible.value = false
    loadTemplates()
  } catch (error) {
    Message.error('保存失败')
  }
}

const exportReport = async (format: string) => {
  if (!generatedReport.value) return
  
  try {
    const res = await request.get(`/api/reports/${generatedReport.value.id}/export`, {
      params: { format },
      responseType: 'blob'
    })
    
    const blob = new Blob([res], { type: format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `${generatedReport.value.title}.${format}`
    link.click()
    Message.success('导出成功')
  } catch (error) {
    Message.error('导出失败')
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadDataSources()
  loadTemplates()
})
</script>

<style scoped>
.report-generator {
  height: 100%;
}

.config-card {
  height: 100%;
}

.preview-card {
  height: 100%;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #86909c;
}

.loading-container p {
  margin-top: 16px;
}

.report-content {
  padding: 20px;
}

.report-title {
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 8px;
}

.report-meta {
  text-align: center;
  color: #86909c;
  font-size: 14px;
}

.report-section {
  margin-bottom: 30px;
}

.report-section h2 {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #165dff;
}

.chart-container {
  margin: 16px 0;
}

.insight-text {
  background: #f7f8fa;
  padding: 12px 16px;
  border-radius: 4px;
  margin-top: 12px;
  color: #4e5969;
  line-height: 1.6;
}

.conclusion {
  background: linear-gradient(135deg, #e8f3ff 0%, #f2f3f5 100%);
  padding: 20px;
  border-radius: 8px;
  line-height: 1.8;
  color: #1d2129;
}

.positive {
  color: #00b42a;
}

.negative {
  color: #f53f3f;
}
</style>
