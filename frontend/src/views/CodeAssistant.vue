<template>
  <div class="code-assistant-page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>代码编辑器</span>
              <el-select v-model="selectedAction" size="small" style="width: 120px;">
                <el-option label="代码生成" value="generate" />
                <el-option label="代码分析" value="analyze" />
                <el-option label="代码优化" value="optimize" />
                <el-option label="代码解释" value="explain" />
                <el-option label="代码修复" value="fix" />
              </el-select>
            </div>
          </template>

          <el-input
            v-model="codeInput"
            type="textarea"
            :rows="20"
            placeholder="在此输入代码或描述..."
            class="code-editor"
            :class="{ 'has-error': hasError }"
          />

          <div class="editor-actions">
            <el-button type="primary" :icon="MagicStick" :loading="processing" @click="processCode">
              {{ actionButtonText }}
            </el-button>
            <el-button :icon="DocumentCopy" @click="copyCode">
              复制
            </el-button>
            <el-button :icon="Delete" @click="clearCode">
              清空
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover" class="result-card">
          <template #header>
            <div class="card-header">
              <span>处理结果</span>
              <el-tag v-if="resultType" :type="resultType">{{ resultTypeText }}</el-tag>
            </div>
          </template>

          <div v-if="result" class="result-content">
            <pre v-if="isCodeResult"><code>{{ result }}</code></pre>
            <div v-else v-html="formatResult(result)"></div>
          </div>

          <el-empty v-else description="等待处理...">
            <template #description>
              <p>在左侧输入代码或描述</p>
              <p style="font-size: 12px; color: #909399; margin-top: 8px;">
                选择功能后点击处理按钮
              </p>
            </template>
          </el-empty>
        </el-card>
      </el-col>
    </el-row>

    <!-- 代码模板 -->
    <el-card shadow="hover" class="templates-card">
      <template #header>
        <span>代码模板</span>
      </template>
      <div class="templates-list">
        <el-button
          v-for="template in codeTemplates"
          :key="template.name"
          size="small"
          @click="loadTemplate(template)"
        >
          {{ template.name }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { MagicStick, DocumentCopy, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const codeInput = ref('')
const selectedAction = ref('generate')
const processing = ref(false)
const result = ref('')
const resultType = ref('')
const hasError = ref(false)

const isCodeResult = computed(() => {
  return ['generate', 'optimize', 'fix'].includes(selectedAction.value)
})

const actionButtonText = computed(() => {
  const map: Record<string, string> = {
    'generate': '生成代码',
    'analyze': '分析代码',
    'optimize': '优化代码',
    'explain': '解释代码',
    'fix': '修复代码'
  }
  return map[selectedAction.value] || '处理'
})

const resultTypeText = computed(() => {
  const map: Record<string, string> = {
    'success': '成功',
    'warning': '警告',
    'danger': '错误',
    'info': '信息'
  }
  return map[resultType.value] || ''
})

const codeTemplates = [
  { name: 'Java 类模板', code: 'public class Example {\n    // 在此输入代码\n}' },
  { name: 'Python 函数', code: 'def example():\n    # 在此输入代码\n    pass' },
  { name: 'SQL 查询', code: 'SELECT * FROM table_name\nWHERE condition;' },
  { name: 'JavaScript', code: 'function example() {\n    // 在此输入代码\n}' }
]

const processCode = async () => {
  if (!codeInput.value.trim()) {
    ElMessage.warning('请输入代码或描述')
    return
  }

  processing.value = true
  result.value = ''

  try {
    const data: any = {}
    
    switch (selectedAction.value) {
      case 'generate':
        data.description = codeInput.value
        break
      case 'analyze':
      case 'explain':
        data.code = codeInput.value
        break
      case 'optimize':
        data.code = codeInput.value
        data.requirements = '优化性能和可读性'
        break
      case 'fix':
        data.code = codeInput.value
        data.errorDescription = '请修复代码中的问题'
        break
    }

    const response = await window.electronAPI.codeAssistant(selectedAction.value, data)
    
    if (response.success) {
      result.value = response.data.result || response.data
      resultType.value = 'success'
      ElMessage.success('处理完成')
    } else {
      result.value = response.message || '处理失败'
      resultType.value = 'danger'
      ElMessage.error('处理失败')
    }
  } catch (error) {
    result.value = '处理过程出错，请稍后重试'
    resultType.value = 'danger'
    ElMessage.error('处理过程出错')
  } finally {
    processing.value = false
  }
}

const copyCode = () => {
  navigator.clipboard.writeText(codeInput.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

const clearCode = () => {
  codeInput.value = ''
  result.value = ''
  resultType.value = ''
}

const loadTemplate = (template: any) => {
  codeInput.value = template.code
}

const formatResult = (text: string) => {
  return text.replace(/\n/g, '<br>')
}
</script>

<style scoped>
.code-assistant-page {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-editor {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.code-editor :deep(textarea) {
  font-family: 'Courier New', monospace;
  background: #1e1e1e;
  color: #d4d4d4;
}

.has-error :deep(textarea) {
  border-color: #f56c6c;
}

.editor-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.result-card {
  height: 100%;
  min-height: 500px;
}

.result-content {
  height: calc(100% - 60px);
  overflow-y: auto;
}

.result-content pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}

.templates-card {
  margin-top: 20px;
}

.templates-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
