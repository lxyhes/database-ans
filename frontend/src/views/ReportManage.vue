<template>
  <div class="report-manage">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>报表管理</span>
          <a-button type="primary" @click="showCreateDialog">
            <template #icon><icon-plus /></template>
            新建报表
          </a-button>
        </div>
      </template>

      <!-- 报表列表 -->
      <a-table :data="templates" :loading="loading">
        <a-table-column title="#" :width="50">
          <template #cell="{ rowIndex }">{{ rowIndex + 1 }}</template>
        </a-table-column>
        <a-table-column title="报表名称" data-index="name" />
        <a-table-column title="描述" data-index="description" ellipsis tooltip />
        <a-table-column title="查询描述" data-index="naturalLanguageQuery" ellipsis tooltip />
        <a-table-column title="定时规则">
          <template #cell="{ record }">
            <a-tag v-if="record.cronExpression" size="small" color="green">{{ record.cronExpression }}</a-tag>
            <span v-else>-</span>
          </template>
        </a-table-column>
        <a-table-column title="最后执行">
          <template #cell="{ record }">
            <a-tag v-if="record.lastExecuteStatus" :color="getStatusColor(record.lastExecuteStatus)" size="small">
              {{ getStatusText(record.lastExecuteStatus) }}
            </a-tag>
            <span v-else>-</span>
          </template>
        </a-table-column>
        <a-table-column title="执行时间">
          <template #cell="{ record }">
            {{ formatDate(record.lastExecuteTime) }}
          </template>
        </a-table-column>
        <a-table-column title="操作" :width="280">
          <template #cell="{ record }">
            <a-space>
              <a-button type="text" @click="executeReport(record.id)" :loading="executing === record.id">
                <template #icon><icon-play-circle /></template>
                执行
              </a-button>
              <a-button type="text" @click="viewHistory(record)">
                <template #icon><icon-clock-circle /></template>
                历史
              </a-button>
              <a-button type="text" @click="editTemplate(record)">
                <template #icon><icon-edit /></template>
                编辑
              </a-button>
              <a-button type="text" status="danger" @click="deleteTemplate(record.id)">
                <template #icon><icon-delete /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <!-- 创建/编辑对话框 -->
    <a-modal v-model:visible="dialogVisible" :title="isEdit ? '编辑报表' : '新建报表'" width="600px">
      <a-form :model="form" layout="vertical" ref="formRef">
        <a-form-item label="报表名称" field="name" required>
          <a-input v-model="form.name" placeholder="请输入报表名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model="form.description" placeholder="请输入描述" />
        </a-form-item>
        <a-form-item label="数据源" field="dataSourceId" required>
          <a-select v-model="form.dataSourceId" placeholder="选择数据源" style="width: 100%">
            <a-option
              v-for="ds in dataSources"
              :key="ds.id"
              :value="ds.id"
            >{{ ds.name }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="查询描述" field="naturalLanguageQuery" required>
          <a-textarea
            v-model="form.naturalLanguageQuery"
            :rows="3"
            placeholder="用自然语言描述报表需求，例如：统计每天的销售金额和订单数"
          />
        </a-form-item>
        <a-form-item label="生成的SQL">
          <a-textarea
            v-model="form.generatedSql"
            :rows="3"
            placeholder="点击转换按钮生成SQL"
            readonly
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="convertToSql" :loading="converting">
            <template #icon><icon-bulb /></template>
            转换为SQL
          </a-button>
        </a-form-item>
        <a-form-item label="定时规则">
          <a-input v-model="form.cronExpression" placeholder="Cron表达式，如：0 0 8 * * ?（每天8点）" />
          <div class="cron-hint">
            <a-link @click="showCronHelp">查看Cron表达式帮助</a-link>
          </div>
        </a-form-item>
        <a-form-item label="输出格式">
          <a-radio-group v-model="form.outputFormat">
            <a-radio value="EXCEL">Excel</a-radio>
            <a-radio value="CSV">CSV</a-radio>
            <a-radio value="PDF">PDF</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="启用状态">
          <a-switch v-model="form.isActive">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </a-switch>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="dialogVisible = false">取消</a-button>
          <a-button type="primary" @click="saveTemplate" :loading="saving">保存</a-button>
        </a-space>
      </template>
    </a-modal>

    <!-- 执行历史对话框 -->
    <a-modal v-model:visible="historyVisible" title="执行历史" width="800px">
      <a-table :data="instances" :loading="historyLoading">
        <a-table-column title="执行时间">
          <template #cell="{ record }">
            {{ formatDate(record.executeTime) }}
          </template>
        </a-table-column>
        <a-table-column title="状态">
          <template #cell="{ record }">
            <a-tag :color="getInstanceStatusColor(record.status)">{{ getInstanceStatusText(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="数据行数" data-index="rowCount" />
        <a-table-column title="执行耗时(ms)" data-index="executeDuration" />
        <a-table-column title="操作">
          <template #cell="{ record }">
            <a-button v-if="record.status === 'SUCCESS'" type="text" @click="downloadReport(record.id)">
              <template #icon><icon-download /></template>
              下载
            </a-button>
            <a-tooltip v-if="record.errorMessage" :content="record.errorMessage" position="top">
              <a-button type="text" status="danger">
                <template #icon><icon-exclamation-circle /></template>
                错误
              </a-button>
            </a-tooltip>
          </template>
        </a-table-column>
      </a-table>
    </a-modal>

    <!-- Cron帮助对话框 -->
    <a-modal v-model:visible="cronHelpVisible" title="Cron表达式帮助" width="500px">
      <a-table :data="cronExamples" size="small">
        <a-table-column title="表达式" data-index="expression" :width="150" />
        <a-table-column title="说明" data-index="description" />
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus, IconPlayCircle, IconClockCircle, IconEdit, IconDelete, IconBulb, IconDownload, IconExclamationCircle } from '@arco-design/web-vue/es/icon'
import request from '@/utils/request'

const templates = ref([])
const dataSources = ref([])
const instances = ref([])
const loading = ref(false)
const executing = ref(null)
const saving = ref(false)
const converting = ref(false)
const historyLoading = ref(false)
const dialogVisible = ref(false)
const historyVisible = ref(false)
const cronHelpVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const currentTemplate = ref(null)

const form = ref({
  name: '',
  description: '',
  dataSourceId: null,
  naturalLanguageQuery: '',
  generatedSql: '',
  cronExpression: '',
  outputFormat: 'EXCEL',
  isActive: true
})

const cronExamples = [
  { expression: '0 0 * * * ?', description: '每小时执行' },
  { expression: '0 0 8 * * ?', description: '每天8点执行' },
  { expression: '0 0 8,18 * * ?', description: '每天8点和18点执行' },
  { expression: '0 0/30 * * * ?', description: '每30分钟执行' },
  { expression: '0 0 8 ? * MON', description: '每周一8点执行' },
  { expression: '0 0 8 1 * ?', description: '每月1号8点执行' }
]

const loadTemplates = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/reports/templates')
    templates.value = res.data
  } catch (error) {
    Message.error('加载报表列表失败')
  } finally {
    loading.value = false
  }
}

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  form.value = {
    name: '',
    description: '',
    dataSourceId: null,
    naturalLanguageQuery: '',
    generatedSql: '',
    cronExpression: '',
    outputFormat: 'EXCEL',
    isActive: true
  }
  dialogVisible.value = true
}

const editTemplate = (row) => {
  isEdit.value = true
  currentTemplate.value = row
  form.value = { ...row }
  dialogVisible.value = true
}

const convertToSql = async () => {
  if (!form.value.naturalLanguageQuery || !form.value.dataSourceId) {
    Message.warning('请先输入查询描述并选择数据源')
    return
  }

  converting.value = true
  try {
    const res = await request.post('/api/nl2sql', {
      dataSourceId: form.value.dataSourceId,
      question: form.value.naturalLanguageQuery,
      provider: 'mock'
    })
    form.value.generatedSql = res.data.sql
    Message.success('转换成功')
  } catch (error) {
    Message.error('转换失败')
  } finally {
    converting.value = false
  }
}

const saveTemplate = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/api/reports/templates/${currentTemplate.value.id}`, form.value)
    } else {
      await request.post('/api/reports/templates', form.value)
    }
    Message.success('保存成功')
    dialogVisible.value = false
    loadTemplates()
  } catch (error) {
    Message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const executeReport = async (id) => {
  executing.value = id
  try {
    await request.post(`/api/reports/templates/${id}/execute`)
    Message.success('报表执行中，请稍后查看结果')
    setTimeout(loadTemplates, 2000)
  } catch (error) {
    Message.error('执行失败')
  } finally {
    executing.value = null
  }
}

const viewHistory = async (row) => {
  currentTemplate.value = row
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await request.get(`/api/reports/templates/${row.id}/instances`)
    instances.value = res.data
  } catch (error) {
    Message.error('加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

const deleteTemplate = async (id) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定删除此报表模板吗？',
    onOk: async () => {
      try {
        await request.delete(`/api/reports/templates/${id}`)
        Message.success('删除成功')
        loadTemplates()
      } catch (error) {
        Message.error('删除失败')
      }
    }
  })
}

const downloadReport = (instanceId) => {
  window.open(`/api/reports/download/${instanceId}`)
}

const showCronHelp = () => {
  cronHelpVisible.value = true
}

const getStatusColor = (status) => {
  const colors = {
    'SUCCESS': 'green',
    'FAILED': 'red',
    'RUNNING': 'orange',
    'PENDING': 'arcoblue'
  }
  return colors[status] || 'gray'
}

const getStatusText = (status) => {
  const texts = {
    'SUCCESS': '成功',
    'FAILED': '失败',
    'RUNNING': '执行中',
    'PENDING': '待执行'
  }
  return texts[status] || status
}

const getInstanceStatusColor = (status) => {
  const colors = {
    'SUCCESS': 'green',
    'FAILED': 'red',
    'RUNNING': 'orange'
  }
  return colors[status] || 'gray'
}

const getInstanceStatusText = (status) => {
  const texts = {
    'SUCCESS': '成功',
    'FAILED': '失败',
    'RUNNING': '执行中'
  }
  return texts[status] || status
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadTemplates()
  loadDataSources()
})
</script>

<style scoped lang="scss">
.report-manage {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .cron-hint {
    margin-top: 5px;
    font-size: 12px;
  }
}
</style>
