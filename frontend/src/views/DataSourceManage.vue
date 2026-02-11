<template>
  <div class="datasource-manage">
    <el-card class="page-header">
      <template #header>
        <div class="card-header">
          <span class="title">数据源管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加数据源
          </el-button>
        </div>
      </template>
      
      <el-alert
        v-if="dataSources.length === 0"
        title="暂无数据源"
        description="请先添加一个数据源以开始使用 NL2SQL 功能"
        type="info"
        :closable="false"
        show-icon
      />
    </el-card>

    <!-- 数据源列表 -->
    <el-row :gutter="20" class="datasource-list">
      <el-col 
        v-for="ds in dataSources" 
        :key="ds.id" 
        :xs="24" 
        :sm="12" 
        :md="8" 
        :lg="8"
      >
        <el-card 
          class="datasource-card" 
          :class="{ 'is-default': ds.isDefault, 'is-disconnected': ds.connectionStatus === 'disconnected' }"
        >
          <div class="datasource-header">
            <div class="datasource-icon">
              <el-icon :size="32">
                <Coin v-if="ds.type === 'mysql'" />
                <DataLine v-else-if="ds.type === 'postgresql'" />
                <Collection v-else />
              </el-icon>
            </div>
            <div class="datasource-info">
              <h3 class="datasource-name">
                {{ ds.name }}
                <el-tag v-if="ds.isDefault" type="success" size="small">默认</el-tag>
              </h3>
              <p class="datasource-type">{{ getDatabaseTypeName(ds.type) }}</p>
            </div>
            <el-dropdown @command="handleCommand($event, ds)">
              <el-button type="primary" text>
                <el-icon><More /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="test">
                    <el-icon><Connection /></el-icon> 测试连接
                  </el-dropdown-item>
                  <el-dropdown-item v-if="!ds.isDefault" command="setDefault">
                    <el-icon><Star /></el-icon> 设为默认
                  </el-dropdown-item>
                  <el-dropdown-item divided command="delete" type="danger">
                    <el-icon><Delete /></el-icon> 删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
              <el-tag 
                :type="ds.connectionStatus === 'connected' ? 'success' : 'danger'"
                size="small"
              >
                {{ ds.connectionStatus === 'connected' ? '已连接' : '未连接' }}
              </el-tag>
            </div>
          </div>

          <div class="datasource-footer">
            <span class="update-time">更新于 {{ formatDate(ds.updatedAt) }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑数据源' : '添加数据源'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="datasource-form"
      >
        <el-form-item label="数据源名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>

        <el-form-item label="数据库类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择数据库类型" style="width: 100%">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="H2 Database" value="h2" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="主机地址" prop="host">
              <el-input v-model="form.host" placeholder="localhost" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口" prop="port">
              <el-input 
                v-model.number="form.port" 
                placeholder="3306"
                type="number"
                :min="1"
                :max="65535"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="数据库名" prop="database">
          <el-input v-model="form.database" placeholder="请输入数据库名称" />
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入数据源描述（可选）"
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="info" @click="handleTestConnection" :loading="testing">
            测试连接
          </el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Coin, DataLine, Collection, More,
  Edit, Delete, Connection, Star
} from '@element-plus/icons-vue'
import {
  getDataSources,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection,
  setDefaultDataSource,
  getSupportedDatabaseTypes
} from '@/api/datasource'

const dataSources = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const testing = ref(false)
const formRef = ref(null)
const databaseTypes = ref({})

const defaultPorts = {
  mysql: 3306,
  postgresql: 5432,
  h2: 0
}

const form = reactive({
  id: null,
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
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
    ElMessage.error('加载数据源失败')
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

const getDatabaseTypeName = (type) => {
  const names = {
    mysql: 'MySQL',
    postgresql: 'PostgreSQL',
    h2: 'H2 Database'
  }
  return names[type] || type
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据源 "${row.name}" 吗？`,
      '确认删除',
      { type: 'warning' }
    )
    const res = await deleteDataSource(row.id)
    if (res.success) {
      ElMessage.success('删除成功')
      loadDataSources()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleTestConnection = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  testing.value = true
  try {
    const res = await testDataSourceConnection(form)
    if (res.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(res.message || '连接失败')
    }
  } catch (error) {
    ElMessage.error('连接测试失败')
  } finally {
    testing.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateDataSource(form.id, form)
    } else {
      res = await createDataSource(form)
    }

    if (res.success) {
      ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
      dialogVisible.value = false
      loadDataSources()
    } else {
      ElMessage.error(res.message || (isEdit.value ? '保存失败' : '创建失败'))
    }
  } catch (error) {
    ElMessage.error(isEdit.value ? '保存失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const handleSetDefault = async (row) => {
  try {
    const res = await setDefaultDataSource(row.id)
    if (res.success) {
      ElMessage.success('已设为默认数据源')
      loadDataSources()
    } else {
      ElMessage.error(res.message || '设置失败')
    }
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const handleCommand = (command, row) => {
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
    case 'setDefault':
      handleSetDefault(row)
      break
  }
}

const handleTestSingleConnection = async (row) => {
  try {
    const res = await testDataSourceConnection(row)
    if (res.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(res.message || '连接失败')
    }
  } catch (error) {
    ElMessage.error('连接测试失败')
  }
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
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 18px;
      font-weight: 600;
    }
  }
}

.datasource-list {
  .datasource-card {
    margin-bottom: 20px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    &.is-default {
      border: 2px solid #67c23a;
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
        background: #f0f2f5;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
        color: #409eff;
      }

      .datasource-info {
        flex: 1;

        .datasource-name {
          margin: 0 0 4px 0;
          font-size: 16px;
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .datasource-type {
          margin: 0;
          color: #909399;
          font-size: 13px;
        }
      }
    }

    .datasource-body {
      .info-item {
        display: flex;
        justify-content: space-between;
        padding: 8px 0;
        border-bottom: 1px solid #ebeef5;

        &:last-child {
          border-bottom: none;
        }

        .label {
          color: #606266;
          font-size: 13px;
        }

        .value {
          color: #303133;
          font-size: 13px;
          font-weight: 500;
        }
      }
    }

    .datasource-footer {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #ebeef5;

      .update-time {
        color: #909399;
        font-size: 12px;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
