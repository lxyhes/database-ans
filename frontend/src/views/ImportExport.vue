<template>
  <div class="import-export-page">
    <a-row :gutter="20">
      <!-- 数据导入 -->
      <a-col :span="12">
        <a-card hoverable>
          <template #title>
            <div class="card-header">
              <span>数据导入</span>
              <a-tag color="green">支持 Excel / CSV</a-tag>
            </div>
          </template>

          <a-upload
            class="upload-area"
            draggable
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".xlsx,.xls,.csv"
          >
            <template #upload-button>
              <div class="upload-trigger">
                <icon-upload :size="48" />
                <div class="upload-text">
                  拖拽文件到此处或 <em>点击上传</em>
                </div>
                <div class="upload-hint">
                  支持 .xlsx, .xls, .csv 格式文件
                </div>
              </div>
            </template>
          </a-upload>

          <a-form :model="importForm" layout="vertical" class="import-form">
            <a-form-item label="目标表名">
              <a-input v-model="importForm.tableName" placeholder="默认为 imported_data" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="importing" @click="importData">
                开始导入
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <!-- 数据导出 -->
      <a-col :span="12">
        <a-card hoverable>
          <template #title>
            <div class="card-header">
              <span>数据导出</span>
              <a-tag color="arcoblue">自定义查询</a-tag>
            </div>
          </template>

          <a-form :model="exportForm" layout="vertical">
            <a-form-item label="SQL 查询">
              <a-textarea
                v-model="exportForm.sql"
                :rows="6"
                placeholder="输入 SQL 查询语句，例如：SELECT * FROM sales"
                allow-clear
              />
            </a-form-item>

            <a-form-item label="导出格式">
              <a-radio-group v-model="exportForm.format">
                <a-radio value="excel">Excel (.xlsx)</a-radio>
                <a-radio value="csv">CSV (.csv)</a-radio>
              </a-radio-group>
            </a-form-item>

            <a-form-item>
              <a-button type="primary" :loading="exporting" @click="exportData">
                <template #icon><icon-download /></template>
                导出数据
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>

    <!-- 导入进度 -->
    <a-modal v-model:visible="progressVisible" title="导入进度" width="400px" :mask-closable="false">
      <a-progress :percent="progress" :status="progressStatus" />
      <p class="progress-text">{{ progressText }}</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { IconUpload, IconDownload } from '@arco-design/web-vue/es/icon'
import { Message } from '@arco-design/web-vue'

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
const progressStatus = ref('normal')
const progressText = ref('准备导入...')

const handleFileChange = (fileList: any) => {
  if (fileList && fileList.length > 0) {
    importForm.value.file = fileList[0].file
  }
}

const importData = async () => {
  if (!importForm.value.file) {
    Message.warning('请先选择文件')
    return
  }

  importing.value = true
  progressVisible.value = true
  progress.value = 0
  progressStatus.value = 'normal'
  progressText.value = '准备导入...'

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
      Message.success('数据导入成功')
      setTimeout(() => {
        progressVisible.value = false
      }, 1500)
    } else {
      Message.error(result.message || '导入失败')
    }
  } catch (error) {
    clearInterval(interval)
    progressStatus.value = 'error'
    progressText.value = '导入失败'
    Message.error('导入过程出错')
  } finally {
    importing.value = false
  }
}

const exportData = async () => {
  if (!exportForm.value.sql.trim()) {
    Message.warning('请输入 SQL 查询语句')
    return
  }

  exporting.value = true
  try {
    const result = await window.electronAPI.exportData(
      exportForm.value.sql,
      exportForm.value.format as 'excel' | 'csv'
    )
    if (result.success) {
      Message.success('导出成功')
    } else {
      Message.error(result.message || '导出失败')
    }
  } catch (error) {
    Message.error('导出过程出错')
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

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: var(--color-fill-2);
  border: 2px dashed var(--color-neutral-4);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-trigger:hover {
  border-color: rgb(var(--primary-6));
  background: var(--color-fill-3);
}

.upload-text {
  margin-top: 16px;
  color: var(--color-text-2);
}

.upload-text em {
  color: rgb(var(--primary-6));
  font-style: normal;
}

.upload-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-3);
}

.import-form {
  margin-top: 20px;
}

.progress-text {
  text-align: center;
  margin-top: 16px;
  color: var(--color-text-2);
}
</style>
