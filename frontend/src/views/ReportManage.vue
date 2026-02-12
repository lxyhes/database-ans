<template>
  <div class="report-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>报表管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新建报表
          </el-button>
        </div>
      </template>

      <!-- 报表列表 -->
      <el-table :data="templates" v-loading="loading">
        <el-table-column type="index" width="50" />
        <el-table-column prop="name" label="报表名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="naturalLanguageQuery" label="查询描述" show-overflow-tooltip />
        <el-table-column prop="cronExpression" label="定时规则">
          <template #default="{ row }">
            <el-tag v-if="row.cronExpression" size="small" type="success">{{ row.cronExpression }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastExecuteStatus" label="最后执行">
          <template #default="{ row }">
            <el-tag v-if="row.lastExecuteStatus" :type="getStatusType(row.lastExecuteStatus)" size="small">
              {{ getStatusText(row.lastExecuteStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastExecuteTime" label="执行时间">
          <template #default="{ row }">
            {{ formatDate(row.lastExecuteTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" link @click="executeReport(row.id)" :loading="executing === row.id">
              <el-icon><VideoPlay /></el-icon>
              执行
            </el-button>
            <el-button type="primary" link @click="viewHistory(row)">
              <el-icon><Clock /></el-icon>
              历史
            </el-button>
            <el-button type="primary" link @click="editTemplate(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" link @click="deleteTemplate(row.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑报表' : '新建报表'" width="600px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="报表名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入报表名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="数据源" prop="dataSourceId">
          <el-select v-model="form.dataSourceId" placeholder="选择数据源" style="width: 100%">
            <el-option
              v-for="ds in dataSources"
              :key="ds.id"
              :label="ds.name"
              :value="ds.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="查询描述" prop="naturalLanguageQuery">
          <el-input
            v-model="form.naturalLanguageQuery"
            type="textarea"
            :rows="3"
            placeholder="用自然语言描述报表需求，例如：统计每天的销售金额和订单数"
          />
        </el-form-item>
        <el-form-item label="生成的SQL">
          <el-input
            v-model="form.generatedSql"
            type="textarea"
            :rows="3"
            placeholder="点击转换按钮生成SQL"
            readonly
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="convertToSql" :loading="converting">
            <el-icon><MagicStick /></el-icon>
            转换为SQL
          </el-button>
        </el-form-item>
        <el-form-item label="定时规则">
          <el-input v-model="form.cronExpression" placeholder="Cron表达式，如：0 0 8 * * ?（每天8点）" />
          <div class="cron-hint">
            <el-link type="primary" @click="showCronHelp">查看Cron表达式帮助</el-link>
          </div>
        </el-form-item>
        <el-form-item label="输出格式">
          <el-radio-group v-model="form.outputFormat">
            <el-radio label="EXCEL">Excel</el-radio>
            <el-radio label="CSV">CSV</el-radio>
            <el-radio label="PDF">PDF</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.isActive" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 执行历史对话框 -->
    <el-dialog v-model="historyVisible" title="执行历史" width="800px">
      <el-table :data="instances" v-loading="historyLoading">
        <el-table-column prop="executeTime" label="执行时间">
          <template #default="{ row }">
            {{ formatDate(row.executeTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getInstanceStatusType(row.status)">{{ getInstanceStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rowCount" label="数据行数" />
        <el-table-column prop="executeDuration" label="执行耗时(ms)" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUCCESS'" type="primary" link @click="downloadReport(row.id)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-tooltip v-if="row.errorMessage" :content="row.errorMessage" placement="top">
              <el-button type="danger" link>
                <el-icon><Warning /></el-icon>
                错误
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Cron帮助对话框 -->
    <el-dialog v-model="cronHelpVisible" title="Cron表达式帮助" width="500px">
      <el-table :data="cronExamples" size="small">
        <el-table-column prop="expression" label="表达式" width="150" />
        <el-table-column prop="description" label="说明" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay, Clock, Edit, Delete, MagicStick, Download, Warning } from '@element-plus/icons-vue'
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

const rules = {
  name: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  dataSourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  naturalLanguageQuery: [{ required: true, message: '请输入查询描述', trigger: 'blur' }]
}

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
    ElMessage.error('加载报表列表失败')
  } finally {
    loading.value = false
  }
}

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data
  } catch (error) {
    ElMessage.error('加载数据源失败')
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
    ElMessage.warning('请先输入查询描述并选择数据源')
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
    ElMessage.success('转换成功')
  } catch (error) {
    ElMessage.error('转换失败')
  } finally {
    converting.value = false
  }
}

const saveTemplate = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/api/reports/templates/${currentTemplate.value.id}`, form.value)
    } else {
      await request.post('/api/reports/templates', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadTemplates()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const executeReport = async (id) => {
  executing.value = id
  try {
    await request.post(`/api/reports/templates/${id}/execute`)
    ElMessage.success('报表执行中，请稍后查看结果')
    setTimeout(loadTemplates, 2000)
  } catch (error) {
    ElMessage.error('执行失败')
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
    ElMessage.error('加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

const deleteTemplate = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除此报表模板吗？', '提示', { type: 'warning' })
    await request.delete(`/api/reports/templates/${id}`)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const downloadReport = (instanceId) => {
  window.open(`/api/reports/download/${instanceId}`)
}

const showCronHelp = () => {
  cronHelpVisible.value = true
}

const getStatusType = (status) => {
  const types = {
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'RUNNING': 'warning',
    'PENDING': 'info'
  }
  return types[status] || 'info'
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

const getInstanceStatusType = (status) => {
  const types = {
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'RUNNING': 'warning'
  }
  return types[status] || 'info'
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
