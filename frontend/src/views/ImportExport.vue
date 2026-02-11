<template>
  <div class="import-export-page">
    <el-row :gutter="20">
      <!-- 数据导入 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据导入</span>
              <el-tag type="success">支持 Excel / CSV</el-tag>
            </div>
          </template>

          <el-upload
            class="upload-area"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".xlsx,.xls,.csv"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .xlsx, .xls, .csv 格式文件
              </div>
            </template>
          </el-upload>

          <el-form :model="importForm" label-width="100px" class="import-form">
            <el-form-item label="目标表名">
              <el-input v-model="importForm.tableName" placeholder="默认为 imported_data" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="importing" @click="importData">
                开始导入
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 数据导出 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>数据导出</span>
              <el-tag type="primary">自定义查询</el-tag>
            </div>
          </template>

          <el-form :model="exportForm" label-width="100px">
            <el-form-item label="SQL 查询">
              <el-input
                v-model="exportForm.sql"
                type="textarea"
                :rows="6"
                placeholder="输入 SQL 查询语句，例如：SELECT * FROM sales"
              />
            </el-form-item>

            <el-form-item label="导出格式">
              <el-radio-group v-model="exportForm.format">
                <el-radio label="excel">Excel (.xlsx)</el-radio>
                <el-radio label="csv">CSV (.csv)</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :icon="Download" :loading="exporting" @click="exportData">
                导出数据
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导入进度 -->
    <el-dialog v-model="progressVisible" title="导入进度" width="400px" :close-on-click-modal="false">
      <el-progress :percentage="progress" :status="progressStatus" />
      <p class="progress-text">{{ progressText }}</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const importForm = ref({
  tableName: '',
  file: null as File | null
})

const exportForm = ref({
  sql: '',
  format: 'excel'
})

const importing = ref(false)
const exporting = ref(false)
const progressVisible = ref(false)
const progress = ref(0)
const progressStatus = ref('')
const progressText = ref('准备导入...')

const handleFileChange = (file: any) => {
  importForm.value.file = file.raw
}

const importData = async () => {
  if (!importForm.value.file) {
    ElMessage.warning('请先选择文件')
    return
  }

  importing.value = true
  progressVisible.value = true
  progress.value = 0
  progressStatus.value = ''
  progressText.value = '正在上传文件...'

  // 模拟进度
  const interval = setInterval(() => {
    if (progress.value < 90) {
      progress.value += 10
      if (progress.value === 30) progressText.value = '正在解析数据...'
      if (progress.value === 60) progressText.value = '正在导入数据库...'
    }
  }, 500)

  try {
    const formData = new FormData()
    formData.append('file', importForm.value.file)
    formData.append('tableName', importForm.value.tableName || 'imported_data')

    const result = await window.electronAPI.importFile(formData)
    
    clearInterval(interval)
    progress.value = 100
    progressStatus.value = 'success'
    progressText.value = '导入完成！'

    if (result.success) {
      ElMessage.success('数据导入成功')
      setTimeout(() => {
        progressVisible.value = false
      }, 1500)
    } else {
      ElMessage.error(result.message || '导入失败')
    }
  } catch (error) {
    clearInterval(interval)
    progressStatus.value = 'exception'
    progressText.value = '导入失败'
    ElMessage.error('导入过程出错')
  } finally {
    importing.value = false
  }
}

const exportData = async () => {
  if (!exportForm.value.sql.trim()) {
    ElMessage.warning('请输入 SQL 查询语句')
    return
  }

  exporting.value = true
  try {
    const result = await window.electronAPI.exportData(
      exportForm.value.sql,
      exportForm.value.format as 'excel' | 'csv'
    )
    if (result.success) {
      ElMessage.success('导出成功')
    } else {
      ElMessage.error(result.message || '导出失败')
    }
  } catch (error) {
    ElMessage.error('导出过程出错')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  width: 100%;
}

.import-form {
  margin-top: 20px;
}

.progress-text {
  text-align: center;
  margin-top: 16px;
  color: #606266;
}
</style>
