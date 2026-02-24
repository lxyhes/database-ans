<template>
  <div class="alert-monitor">
    <a-tabs v-model:active-key="activeTab">
      <a-tab-pane key="dashboard" title="告警概览">
        <a-row :gutter="20" class="stats-row">
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic title="待处理告警" :value="stats.totalPending || 0" :value-style="{ color: '#f53f3f' }">
                <template #prefix><icon-exclamation-circle /></template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic title="严重告警" :value="stats.criticalPending || 0" :value-style="{ color: '#ff7d00' }">
                <template #prefix><icon-close-circle /></template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic title="今日告警" :value="stats.todayAlerts || 0">
                <template #prefix><icon-calendar /></template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <a-statistic title="活跃规则" :value="activeRuleCount">
                <template #prefix><icon-check-circle /></template>
              </a-statistic>
            </a-card>
          </a-col>
        </a-row>

        <a-card title="最近告警" class="recent-alerts">
          <template #extra>
            <a-button type="primary" size="small" @click="loadRecentAlerts">
              <template #icon><icon-refresh /></template>
              刷新
            </a-button>
          </template>
          <a-table :data="recentAlerts" :pagination="{ pageSize: 10 }">
            <a-table-column title="级别" data-index="alertLevel" :width="80">
              <template #cell="{ record }">
                <a-tag :color="getLevelColor(record.alertLevel)">{{ record.alertLevel }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="标题" data-index="title" :ellipsis="true" />
            <a-table-column title="表名" data-index="tableName" :width="120" />
            <a-table-column title="状态" data-index="status" :width="100">
              <template #cell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="时间" data-index="createdAt" :width="160">
              <template #cell="{ record }">{{ formatTime(record.createdAt) }}</template>
            </a-table-column>
            <a-table-column title="操作" :width="180">
              <template #cell="{ record }">
                <a-space>
                  <a-button size="small" type="primary" @click="confirmAlert(record.id)">确认</a-button>
                  <a-button size="small" status="success" @click="resolveAlert(record.id)">解决</a-button>
                  <a-button size="small" status="warning" @click="ignoreAlert(record.id)">忽略</a-button>
                </a-space>
              </template>
            </a-table-column>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="rules" title="告警规则">
        <a-card>
          <template #title>
            <div class="card-header">
              <span>告警规则管理</span>
              <a-button type="primary" @click="showRuleDialog">
                <template #icon><icon-plus /></template>
                新建规则
              </a-button>
            </div>
          </template>
          <a-table :data="rules" :pagination="{ pageSize: 10 }">
            <a-table-column title="规则名称" data-index="name" :ellipsis="true" />
            <a-table-column title="类型" data-index="ruleType" :width="100">
              <template #cell="{ record }">{{ getRuleTypeText(record.ruleType) }}</template>
            </a-table-column>
            <a-table-column title="数据表" data-index="tableName" :width="120" />
            <a-table-column title="级别" data-index="alertLevel" :width="80">
              <template #cell="{ record }">
                <a-tag :color="getLevelColor(record.alertLevel)">{{ record.alertLevel }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="状态" :width="80">
              <template #cell="{ record }">
                <a-switch :model-value="record.isActive" @change="toggleRule(record.id, $event)" />
              </template>
            </a-table-column>
            <a-table-column title="最后检查" :width="160">
              <template #cell="{ record }">{{ formatTime(record.lastCheckTime) || '-' }}</template>
            </a-table-column>
            <a-table-column title="操作" :width="200">
              <template #cell="{ record }">
                <a-space>
                  <a-button size="small" @click="editRule(record)">编辑</a-button>
                  <a-button size="small" type="primary" @click="manualCheck(record.id)">检查</a-button>
                  <a-button size="small" status="danger" @click="deleteRule(record.id)">删除</a-button>
                </a-space>
              </template>
            </a-table-column>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="records" title="告警记录">
        <a-card>
          <template #title>
            <div class="card-header">
              <span>告警记录</span>
              <div class="filter-bar">
                <a-select v-model="filterStatus" placeholder="状态筛选" style="width: 120px" allow-clear @change="loadAlertRecords">
                  <a-option value="PENDING">待处理</a-option>
                  <a-option value="CONFIRMED">已确认</a-option>
                  <a-option value="RESOLVED">已解决</a-option>
                  <a-option value="IGNORED">已忽略</a-option>
                </a-select>
                <a-select v-model="filterLevel" placeholder="级别筛选" style="width: 120px" allow-clear @change="loadAlertRecords">
                  <a-option value="CRITICAL">严重</a-option>
                  <a-option value="WARNING">警告</a-option>
                  <a-option value="INFO">信息</a-option>
                </a-select>
              </div>
            </div>
          </template>
          <a-table :data="alertRecords" :pagination="{ pageSize: 10 }">
            <a-table-column title="级别" data-index="alertLevel" :width="80">
              <template #cell="{ record }">
                <a-tag :color="getLevelColor(record.alertLevel)">{{ record.alertLevel }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="标题" data-index="title" :ellipsis="true" />
            <a-table-column title="消息" data-index="message" :ellipsis="true" />
            <a-table-column title="实际值" :width="100">
              <template #cell="{ record }">{{ record.actualValue || '-' }}</template>
            </a-table-column>
            <a-table-column title="状态" data-index="status" :width="100">
              <template #cell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="时间" :width="160">
              <template #cell="{ record }">{{ formatTime(record.createdAt) }}</template>
            </a-table-column>
            <a-table-column title="操作" :width="180">
              <template #cell="{ record }">
                <a-space v-if="record.status === 'PENDING'">
                  <a-button size="small" type="primary" @click="confirmAlert(record.id)">确认</a-button>
                  <a-button size="small" status="success" @click="resolveAlert(record.id)">解决</a-button>
                  <a-button size="small" status="warning" @click="ignoreAlert(record.id)">忽略</a-button>
                </a-space>
                <span v-else class="handled-info">
                  {{ record.resolvedBy || record.confirmedBy || '-' }}
                </span>
              </template>
            </a-table-column>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal v-model:visible="ruleDialogVisible" :title="isEditRule ? '编辑规则' : '新建规则'" width="600px" @ok="saveRule" @cancel="resetRuleForm">
      <a-form :model="ruleForm" layout="vertical">
        <a-form-item label="规则名称" required>
          <a-input v-model="ruleForm.name" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="数据源" required>
          <a-select v-model="ruleForm.dataSourceId" placeholder="选择数据源" @change="onDataSourceChange">
            <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据表" required>
          <a-select v-model="ruleForm.tableName" placeholder="选择数据表">
            <a-option v-for="table in tables" :key="table" :value="table">{{ table }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="监控字段">
          <a-input v-model="ruleForm.columnName" placeholder="可选，留空则监控整表" />
        </a-form-item>
        <a-form-item label="规则类型" required>
          <a-select v-model="ruleForm.ruleType" placeholder="选择规则类型">
            <a-option value="THRESHOLD">阈值检测</a-option>
            <a-option value="ANOMALY">异常检测</a-option>
            <a-option value="TREND">趋势检测</a-option>
            <a-option value="MISSING">缺失检测</a-option>
          </a-select>
        </a-form-item>

        <template v-if="ruleForm.ruleType === 'THRESHOLD'">
          <a-form-item label="操作符" required>
            <a-select v-model="ruleForm.operator" placeholder="选择操作符">
              <a-option value=">">大于</a-option>
              <a-option value="<">小于</a-option>
              <a-option value=">=">大于等于</a-option>
              <a-option value="<=">小于等于</a-option>
              <a-option value="==">等于</a-option>
              <a-option value="!=">不等于</a-option>
            </a-select>
          </a-form-item>
          <a-form-item label="阈值" required>
            <a-input-number v-model="ruleForm.thresholdValue" placeholder="输入阈值" style="width: 100%" />
          </a-form-item>
        </template>

        <template v-if="ruleForm.ruleType === 'ANOMALY'">
          <a-form-item label="检测方法" required>
            <a-select v-model="ruleForm.detectionMethod" placeholder="选择检测方法">
              <a-option value="ZSCORE">Z-Score</a-option>
              <a-option value="IQR">四分位距</a-option>
              <a-option value="MOVING_AVERAGE">移动平均</a-option>
            </a-select>
          </a-form-item>
          <a-form-item label="敏感度(标准差倍数)">
            <a-input-number v-model="ruleForm.sensitivity" :min="1" :max="10" :step="0.5" style="width: 100%" />
          </a-form-item>
          <a-form-item label="基线周期(天)">
            <a-input-number v-model="ruleForm.baselinePeriod" :min="1" :max="30" style="width: 100%" />
          </a-form-item>
        </template>

        <a-form-item label="告警级别" required>
          <a-select v-model="ruleForm.alertLevel" placeholder="选择告警级别">
            <a-option value="INFO">信息</a-option>
            <a-option value="WARNING">警告</a-option>
            <a-option value="CRITICAL">严重</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="检查间隔(分钟)">
          <a-input-number v-model="ruleForm.checkInterval" :min="1" :max="1440" style="width: 100%" />
        </a-form-item>
        <a-form-item label="冷却时间(分钟)">
          <a-input-number v-model="ruleForm.cooldownMinutes" :min="0" :max="1440" style="width: 100%" />
        </a-form-item>
        <a-form-item label="告警渠道">
          <a-checkbox-group v-model="alertChannels">
            <a-checkbox value="EMAIL">邮件</a-checkbox>
            <a-checkbox value="WECHAT">企业微信</a-checkbox>
            <a-checkbox value="DINGTALK">钉钉</a-checkbox>
          </a-checkbox-group>
        </a-form-item>
        <a-form-item label="接收人">
          <a-input v-model="ruleForm.alertReceivers" placeholder="多个接收人用逗号分隔" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model="ruleForm.description" placeholder="规则描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconExclamationCircle,
  IconCloseCircle,
  IconCalendar,
  IconCheckCircle,
  IconRefresh,
  IconPlus
} from '@arco-design/web-vue/es/icon'
import request from '@/utils/request'

const activeTab = ref('dashboard')
const stats = ref<any>({})
const recentAlerts = ref<any[]>([])
const rules = ref<any[]>([])
const alertRecords = ref<any[]>([])
const dataSources = ref<any[]>([])
const tables = ref<string[]>([])
const filterStatus = ref<string>('')
const filterLevel = ref<string>('')
const ruleDialogVisible = ref(false)
const isEditRule = ref(false)
const alertChannels = ref<string[]>([])

const ruleForm = ref<any>({
  id: null,
  name: '',
  description: '',
  dataSourceId: null,
  tableName: '',
  columnName: '',
  ruleType: 'THRESHOLD',
  operator: '>',
  thresholdValue: null,
  thresholdValue2: null,
  detectionMethod: 'ZSCORE',
  sensitivity: 3,
  baselinePeriod: 7,
  checkInterval: 5,
  alertLevel: 'WARNING',
  alertChannels: '',
  alertReceivers: '',
  cooldownMinutes: 30
})

const activeRuleCount = computed(() => rules.value.filter(r => r.isActive).length)

const loadStatistics = async () => {
  try {
    const res = await request.get('/api/alerts/statistics')
    if (res.success) stats.value = res.data
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const loadRecentAlerts = async () => {
  try {
    const res = await request.get('/api/alerts/records/recent', { params: { hours: 24 } })
    if (res.success) recentAlerts.value = res.data
  } catch (error) {
    console.error('加载最近告警失败', error)
  }
}

const loadRules = async () => {
  try {
    const res = await request.get('/api/alerts/rules')
    if (res.success) rules.value = res.data
  } catch (error) {
    console.error('加载规则失败', error)
  }
}

const loadAlertRecords = async () => {
  try {
    const params: any = { limit: 100 }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterLevel.value) params.level = filterLevel.value
    const res = await request.get('/api/alerts/records', { params })
    if (res.success) alertRecords.value = res.data
  } catch (error) {
    console.error('加载告警记录失败', error)
  }
}

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data || res
  } catch (error) {
    console.error('加载数据源失败', error)
  }
}

const onDataSourceChange = async () => {
  if (!ruleForm.value.dataSourceId) return
  try {
    const res = await request.get('/api/data/tables', { params: { dataSourceId: ruleForm.value.dataSourceId } })
    tables.value = res.data || res
  } catch (error) {
    console.error('加载表列表失败', error)
  }
}

const showRuleDialog = () => {
  isEditRule.value = false
  resetRuleForm()
  ruleDialogVisible.value = true
}

const editRule = (rule: any) => {
  isEditRule.value = true
  ruleForm.value = { ...rule }
  alertChannels.value = rule.alertChannels ? rule.alertChannels.split(',') : []
  onDataSourceChange()
  ruleDialogVisible.value = true
}

const resetRuleForm = () => {
  ruleForm.value = {
    id: null,
    name: '',
    description: '',
    dataSourceId: null,
    tableName: '',
    columnName: '',
    ruleType: 'THRESHOLD',
    operator: '>',
    thresholdValue: null,
    thresholdValue2: null,
    detectionMethod: 'ZSCORE',
    sensitivity: 3,
    baselinePeriod: 7,
    checkInterval: 5,
    alertLevel: 'WARNING',
    alertChannels: '',
    alertReceivers: '',
    cooldownMinutes: 30
  }
  alertChannels.value = []
  tables.value = []
}

const saveRule = async () => {
  try {
    ruleForm.value.alertChannels = alertChannels.value.join(',')
    const url = isEditRule.value ? `/api/alerts/rules/${ruleForm.value.id}` : '/api/alerts/rules'
    const method = isEditRule.value ? 'put' : 'post'
    const res = await request[method](url, ruleForm.value)
    if (res.success) {
      Message.success(isEditRule.value ? '更新成功' : '创建成功')
      ruleDialogVisible.value = false
      loadRules()
    } else {
      Message.error(res.message || '保存失败')
    }
  } catch (error) {
    Message.error('保存失败')
  }
}

const deleteRule = async (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此规则吗？',
    onOk: async () => {
      try {
        await request.delete(`/api/alerts/rules/${id}`)
        Message.success('删除成功')
        loadRules()
      } catch (error) {
        Message.error('删除失败')
      }
    }
  })
}

const toggleRule = async (id: number, active: boolean) => {
  try {
    await request.put(`/api/alerts/rules/${id}/toggle`, null, { params: { active } })
    Message.success(active ? '已启用' : '已禁用')
    loadRules()
  } catch (error) {
    Message.error('操作失败')
  }
}

const manualCheck = async (id: number) => {
  try {
    const res = await request.post(`/api/alerts/rules/${id}/check`)
    Message.success(res.message || '检查完成')
    loadRecentAlerts()
    loadStatistics()
  } catch (error) {
    Message.error('检查失败')
  }
}

const confirmAlert = async (id: number) => {
  try {
    await request.put(`/api/alerts/records/${id}/confirm`)
    Message.success('已确认')
    loadRecentAlerts()
    loadAlertRecords()
    loadStatistics()
  } catch (error) {
    Message.error('确认失败')
  }
}

const resolveAlert = async (id: number) => {
  try {
    await request.put(`/api/alerts/records/${id}/resolve`)
    Message.success('已解决')
    loadRecentAlerts()
    loadAlertRecords()
    loadStatistics()
  } catch (error) {
    Message.error('解决失败')
  }
}

const ignoreAlert = async (id: number) => {
  try {
    await request.put(`/api/alerts/records/${id}/ignore`)
    Message.success('已忽略')
    loadRecentAlerts()
    loadAlertRecords()
    loadStatistics()
  } catch (error) {
    Message.error('操作失败')
  }
}

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    CRITICAL: 'red',
    WARNING: 'orange',
    INFO: 'blue'
  }
  return colors[level] || 'gray'
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    PENDING: 'orange',
    CONFIRMED: 'blue',
    RESOLVED: 'green',
    IGNORED: 'gray'
  }
  return colors[status] || 'gray'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    PENDING: '待处理',
    CONFIRMED: '已确认',
    RESOLVED: '已解决',
    IGNORED: '已忽略'
  }
  return texts[status] || status
}

const getRuleTypeText = (type: string) => {
  const texts: Record<string, string> = {
    THRESHOLD: '阈值检测',
    ANOMALY: '异常检测',
    TREND: '趋势检测',
    MISSING: '缺失检测'
  }
  return texts[type] || type
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadStatistics()
  loadRecentAlerts()
  loadRules()
  loadAlertRecords()
  loadDataSources()
})
</script>

<style scoped>
.alert-monitor {
  height: 100%;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.recent-alerts {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 12px;
}

.handled-info {
  color: #86909c;
  font-size: 12px;
}
</style>
