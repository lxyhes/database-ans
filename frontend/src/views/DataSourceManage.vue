<template>
  <div class="datasource-manage">
    <a-card class="page-header" :bordered="false">
      <template #title>
        <div class="card-header">
          <span class="title">数据源管理</span>
          <a-button type="primary" @click="handleAdd">
            <template #icon><icon-plus /></template>
            添加数据源
          </a-button>
        </div>
      </template>
      
      <a-alert
        v-if="dataSources.length === 0"
        type="info"
        :show-icon="true"
      >
        <template #title>暂无数据源</template>
        <template #content>请先添加一个数据源以开始使用 NL2SQL 功能</template>
      </a-alert>
    </a-card>

    <!-- 数据源列表 -->
    <a-row :gutter="20" class="datasource-list">
      <a-col 
        v-for="ds in dataSources" 
        :key="ds.id" 
        :xs="24" 
        :sm="12" 
        :md="8" 
        :lg="8"
      >
        <a-card 
          class="datasource-card" 
          :class="{ 'is-default': ds.isDefault, 'is-disconnected': ds.connectionStatus === 'disconnected' }"
          hoverable
        >
          <div class="datasource-header">
            <div class="datasource-icon">
              <div class="status-indicator" :class="ds.connectionStatus === 'connected' ? 'connected' : 'disconnected'"></div>
              <icon-storage v-if="ds.type === 'mysql'" :size="32" :style="{ color: '#165DFF' }" />
              <icon-storage v-else-if="ds.type === 'postgresql'" :size="32" :style="{ color: '#165DFF' }" />
              <icon-storage v-else :size="32" :style="{ color: '#165DFF' }" />
            </div>
            <div class="datasource-info">
              <h3 class="datasource-name">
                {{ ds.name }}
                <a-tag v-if="ds.isDefault" color="green" size="small">默认</a-tag>
              </h3>
              <p class="datasource-type">{{ getDatabaseTypeName(ds.type) }}</p>
            </div>
            <a-dropdown @select="(key) => handleCommand(key, ds)">
              <a-button type="text">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption value="edit">
                  <template #icon><icon-edit /></template>
                  编辑
                </a-doption>
                <a-doption value="test">
                  <template #icon><icon-link /></template>
                  测试连接
                </a-doption>
                <a-doption value="refreshStatus">
                  <template #icon><icon-refresh /></template>
                  刷新状态
                </a-doption>
                <a-doption v-if="!ds.isDefault" value="setDefault">
                  <template #icon><icon-star /></template>
                  设为默认
                </a-doption>
                <a-divider style="margin: 4px 0" />
                <a-doption value="delete" style="color: #f53f3f">
                  <template #icon><icon-delete /></template>
                  删除
                </a-doption>
              </template>
            </a-dropdown>
          </div>

          <div class="datasource-body">
            <div class="info-item">
              <span class="label">主机:</span>
              <span class="value">{{ ds.host }}:{{ ds.port }}</span>
            </div>
            <div class="info-item">
              <span class="label">数据库:</span>
              <span class="value">{{ ds.database }}</span>
            </div>
            <div class="info-item">
              <span class="label">用户名:</span>
              <span class="value">{{ ds.username }}</span>
            </div>
            <div class="info-item">
              <span class="label">状态:</span>
              <a-tag 
                :color="ds.connectionStatus === 'connected' ? 'green' : 'red'"
                size="small"
              >
                {{ ds.connectionStatus === 'connected' ? '已连接' : '未连接' }}
              </a-tag>
            </div>
          </div>

          <div class="datasource-footer">
            <span class="update-time">更新于 {{ formatDate(ds.updatedAt) }}</span>
            <a-button 
              v-if="ds.connectionStatus === 'connected'"
              type="text" 
              size="small"
              @click="openTableDrawer(ds)"
            >
              <template #icon><icon-apps /></template>
              查看表结构
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 表结构抽屉组件 -->
    <TableStructureDrawer
      v-model="tableDrawerVisible"
      :data-source-id="currentDataSource?.id"
      :title="currentDataSource ? `${currentDataSource.name} - 表结构` : '表结构'"
    />

    <!-- 添加/编辑对话框 -->
    <a-modal
      v-model:visible="dialogVisible"
      :title="isEdit ? '编辑数据源' : '添加数据源'"
      width="600px"
      @cancel="dialogVisible = false"
      @before-ok="handleBeforeOk"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        class="datasource-form"
      >
        <a-form-item label="数据源名称" field="name">
          <a-input v-model="form.name" placeholder="请输入数据源名称" />
        </a-form-item>

        <a-form-item label="数据库类型" field="type">
          <a-select v-model="form.type" placeholder="请选择数据库类型">
            <a-option value="mysql">MySQL</a-option>
            <a-option value="postgresql">PostgreSQL</a-option>
            <a-option value="h2">H2 Database</a-option>
          </a-select>
        </a-form-item>

        <a-row :gutter="20">
          <a-col :span="16">
            <a-form-item label="主机地址" field="host">
              <a-input v-model="form.host" placeholder="localhost" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="端口" field="port">
              <a-input-number 
                v-model="form.port" 
                placeholder="3306"
                :min="1"
                :max="65535"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="数据库名" field="database">
          <a-input v-model="form.database" placeholder="请输入数据库名称" />
        </a-form-item>

        <a-form-item label="用户名" field="username">
          <a-input v-model="form.username" placeholder="请输入用户名" />
        </a-form-item>

        <a-form-item label="密码" field="password">
          <a-input-password 
            v-model="form.password" 
            placeholder="请输入密码"
          />
        </a-form-item>

        <a-form-item label="描述" field="description">
          <a-textarea 
            v-model="form.description" 
            :rows="2"
            placeholder="请输入数据源描述（可选）"
          />
        </a-form-item>

        <a-form-item label="设为默认" field="isDefault">
          <a-switch v-model="form.isDefault" />
        </a-form-item>
      </a-form>

      <template #footer>
        <div class="dialog-footer">
          <a-button @click="dialogVisible = false">取消</a-button>
          <a-button @click="handleTestConnection" :loading="testing">
            测试连接
          </a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { 
  IconPlus, 
  IconStorage, 
  IconMore,
  IconEdit, 
  IconDelete, 
  IconLink, 
  IconStar, 
  IconApps, 
  IconRefresh 
} from '@arco-design/web-vue/es/icon'
import { Message, Modal } from '@arco-design/web-vue'
import type { FormInstance } from '@arco-design/web-vue'
import {
  getDataSources,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection,
  setDefaultDataSource,
  getSupportedDatabaseTypes
} from '@/api/datasource'
import TableStructureDrawer from '@/components/TableStructureDrawer.vue'

const dataSources = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const testing = ref(false)
const formRef = ref<FormInstance>()
const databaseTypes = ref({})

// 表结构抽屉相关
const tableDrawerVisible = ref(false)
const currentDataSource = ref<any>(null)

const defaultPorts: Record<string, number> = {
  mysql: 3306,
  postgresql: 5432,
  h2: 0
}

const form = reactive({
  id: null as number | null,
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  database: '',
  username: '',
  password: '',
  description: '',
  isDefault: false
})

const rules = {
  name: [{ required: true, message: '请输入数据源名称' }],
  type: [{ required: true, message: '请选择数据库类型' }],
  host: [{ required: true, message: '请输入主机地址' }],
  port: [{ required: true, message: '请输入端口' }],
  database: [{ required: true, message: '请输入数据库名' }],
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

// 监听数据库类型变化，自动设置默认端口
watch(() => form.type, (newType) => {
  if (defaultPorts[newType] !== undefined) {
    form.port = defaultPorts[newType]
  }
})

const loadDataSources = async () => {
  try {
    const res = await getDataSources()
    if (res.success) {
      dataSources.value = res.data
    }
  } catch (error) {
    Message.error('加载数据源失败')
  }
}

const loadDatabaseTypes = async () => {
  try {
    const res = await getSupportedDatabaseTypes()
    if (res.success) {
      databaseTypes.value = res.data
    }
  } catch (error) {
    console.error('加载数据库类型失败', error)
  }
}

const getDatabaseTypeName = (type: string) => {
  const names: Record<string, string> = {
    mysql: 'MySQL',
    postgresql: 'PostgreSQL',
    h2: 'H2 Database'
  }
  return names[type] || type
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await new Promise((resolve, reject) => {
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除数据源 "${row.name}" 吗？`,
        onOk: () => resolve(true),
        onCancel: () => reject('cancel')
      })
    })
    const res = await deleteDataSource(row.id)
    if (res.success) {
      Message.success('删除成功')
      loadDataSources()
    } else {
      Message.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      Message.error('删除失败')
    }
  }
}

const handleTestConnection = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  testing.value = true
  try {
    const res = await testDataSourceConnection(form)
    if (res.success) {
      Message.success('连接成功')
    } else {
      Message.error(res.message || '连接失败')
    }
  } catch (error) {
    Message.error('连接测试失败')
  } finally {
    testing.value = false
  }
}

const handleBeforeOk = async () => {
  const valid = await formRef.value?.validate()
  return valid !== false
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateDataSource(form.id!, form)
    } else {
      res = await createDataSource(form)
    }

    if (res.success) {
      Message.success(isEdit.value ? '保存成功' : '创建成功')
      dialogVisible.value = false
      loadDataSources()
    } else {
      Message.error(res.message || (isEdit.value ? '保存失败' : '创建失败'))
    }
  } catch (error) {
    Message.error(isEdit.value ? '保存失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const handleSetDefault = async (row: any) => {
  try {
    const res = await setDefaultDataSource(row.id)
    if (res.success) {
      Message.success('已设为默认数据源')
      loadDataSources()
    } else {
      Message.error(res.message || '设置失败')
    }
  } catch (error) {
    Message.error('设置失败')
  }
}

const handleCommand = (command: string, row: any) => {
  switch (command) {
    case 'edit':
      handleEdit(row)
      break
    case 'delete':
      handleDelete(row)
      break
    case 'test':
      handleTestSingleConnection(row)
      break
    case 'refreshStatus':
      handleRefreshStatus(row)
      break
    case 'setDefault':
      handleSetDefault(row)
      break
  }
}

const handleRefreshStatus = async (row: any) => {
  try {
    row.connectionStatus = 'checking'
    const res = await testDataSourceConnection(row)
    row.connectionStatus = res.success ? 'connected' : 'disconnected'
    Message.success(res.success ? '连接正常' : '连接失败')
  } catch (error) {
    row.connectionStatus = 'disconnected'
    Message.error('状态刷新失败')
  }
}

const handleTestSingleConnection = async (row: any) => {
  try {
    const res = await testDataSourceConnection(row)
    if (res.success) {
      Message.success('连接成功')
      loadDataSources()
    } else {
      Message.error(res.message || '连接失败')
    }
  } catch (error) {
    Message.error('连接测试失败')
  }
}

// 打开表结构抽屉
const openTableDrawer = (ds: any) => {
  console.log('Opening table drawer for data source:', ds)
  currentDataSource.value = ds
  tableDrawerVisible.value = true
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.type = 'mysql'
  form.host = 'localhost'
  form.port = 3306
  form.database = ''
  form.username = ''
  form.password = ''
  form.description = ''
  form.isDefault = false
}

onMounted(() => {
  loadDataSources()
  loadDatabaseTypes()
})
</script>

<style scoped lang="scss">
.datasource-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  margin-bottom: 20px;
  flex-shrink: 0;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .title {
      font-size: 18px;
      font-weight: 600;
    }
  }
}

.datasource-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;

  .datasource-card {
    margin-bottom: 20px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    &.is-default {
      border: 2px solid #00b42a;
    }

    &.is-disconnected {
      opacity: 0.7;
    }

    .datasource-header {
      display: flex;
      align-items: center;
      margin-bottom: 16px;

      .datasource-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        background: #f2f3f5;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
        position: relative;
        flex-shrink: 0;

        .status-indicator {
          position: absolute;
          top: -2px;
          right: -2px;
          width: 12px;
          height: 12px;
          border-radius: 50%;
          border: 2px solid #fff;
          
          &.connected {
            background: #00b42a;
            box-shadow: 0 0 6px #00b42a;
          }
          
          &.disconnected {
            background: #f53f3f;
            box-shadow: 0 0 6px #f53f3f;
          }
        }
      }

      .datasource-info {
        flex: 1;
        min-width: 0;

        .datasource-name {
          margin: 0 0 4px 0;
          font-size: 16px;
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
        }

        .datasource-type {
          margin: 0;
          color: #86909c;
          font-size: 13px;
        }
      }
    }

    .datasource-body {
      .info-item {
        display: flex;
        justify-content: space-between;
        padding: 8px 0;
        border-bottom: 1px solid #e5e6eb;

        &:last-child {
          border-bottom: none;
        }

        .label {
          color: #4e5969;
          font-size: 13px;
        }

        .value {
          color: #1d2129;
          font-size: 13px;
          font-weight: 500;
          word-break: break-all;
        }
      }
    }

    .datasource-footer {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #e5e6eb;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .update-time {
        color: #86909c;
        font-size: 12px;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
