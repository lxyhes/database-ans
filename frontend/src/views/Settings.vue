<template>
  <div class="settings-page">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>应用设置</span>
            </div>
          </template>

          <el-form :model="settings" label-width="150px">
            <el-divider>后端连接</el-divider>
            
            <el-form-item label="后端地址">
              <el-input v-model="settings.backendUrl" placeholder="http://localhost:9090" />
            </el-form-item>

            <el-divider>AI 配置</el-divider>

            <el-form-item label="默认 AI 模型">
              <el-select v-model="settings.defaultModel" style="width: 100%;">
                <el-option label="iFlow AI" value="iflow" />
                <el-option label="Qwen Code" value="qwencode" />
                <el-option label="通义千问" value="qwen" />
              </el-select>
            </el-form-item>

            <el-form-item label="API Key">
              <el-input
                v-model="settings.apiKey"
                type="password"
                placeholder="输入你的 API Key"
                show-password
              />
            </el-form-item>

            <el-divider>界面设置</el-divider>

            <el-form-item label="主题">
              <el-radio-group v-model="settings.theme">
                <el-radio-button label="light">浅色</el-radio-button>
                <el-radio-button label="dark">深色</el-radio-button>
                <el-radio-button label="auto">自动</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="语言">
              <el-select v-model="settings.language">
                <el-option label="简体中文" value="zh-CN" />
                <el-option label="English" value="en-US" />
              </el-select>
            </el-form-item>

            <el-form-item label="每页显示条数">
              <el-slider v-model="settings.pageSize" :min="10" :max="100" :step="10" show-stops />
            </el-form-item>

            <el-divider>数据设置</el-divider>

            <el-form-item label="自动保存查询">
              <el-switch v-model="settings.autoSaveQuery" />
            </el-form-item>

            <el-form-item label="查询历史保留">
              <el-select v-model="settings.historyRetention">
                <el-option label="7 天" :value="7" />
                <el-option label="30 天" :value="30" />
                <el-option label="90 天" :value="90" />
                <el-option label="永久" :value="-1" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveSettings">保存设置</el-button>
              <el-button @click="resetSettings">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>关于</span>
          </template>
          <div class="about-content">
            <h3>数据分析助手</h3>
            <p class="version">版本 2.0.0</p>
            <p class="description">
              基于 Vue 3 + Electron 构建的智能数据分析桌面应用
            </p>
            <el-divider />
            <div class="info-list">
              <div class="info-item">
                <span class="label">Vue:</span>
                <span class="value">3.4.0</span>
              </div>
              <div class="info-item">
                <span class="label">Electron:</span>
                <span class="value">28.0.0</span>
              </div>
              <div class="info-item">
                <span class="label">Element Plus:</span>
                <span class="value">2.5.0</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" style="margin-top: 20px;">
          <template #header>
            <span>快捷键</span>
          </template>
          <div class="shortcuts-list">
            <div class="shortcut-item">
              <span class="key">Ctrl + Q</span>
              <span class="desc">打开智能查询</span>
            </div>
            <div class="shortcut-item">
              <span class="key">Ctrl + D</span>
              <span class="desc">打开仪表盘</span>
            </div>
            <div class="shortcut-item">
              <span class="key">Ctrl + H</span>
              <span class="desc">打开历史记录</span>
            </div>
            <div class="shortcut-item">
              <span class="key">Ctrl + Enter</span>
              <span class="desc">执行查询</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const settings = reactive({
  backendUrl: 'http://localhost:9090',
  defaultModel: 'iflow',
  apiKey: '',
  theme: 'light',
  language: 'zh-CN',
  pageSize: 20,
  autoSaveQuery: true,
  historyRetention: 30
})

const saveSettings = () => {
  // 保存到本地存储
  localStorage.setItem('app-settings', JSON.stringify(settings))
  ElMessage.success('设置已保存')
}

const resetSettings = () => {
  settings.backendUrl = 'http://localhost:9090'
  settings.defaultModel = 'iflow'
  settings.apiKey = ''
  settings.theme = 'light'
  settings.language = 'zh-CN'
  settings.pageSize = 20
  settings.autoSaveQuery = true
  settings.historyRetention = 30
  ElMessage.info('设置已重置')
}

// 加载设置
const loadSettings = () => {
  const saved = localStorage.getItem('app-settings')
  if (saved) {
    Object.assign(settings, JSON.parse(saved))
  }
}

loadSettings()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.about-content {
  text-align: center;
}

.about-content h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.version {
  color: #909399;
  font-size: 14px;
  margin: 0 0 16px 0;
}

.description {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.info-list {
  text-align: left;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: #909399;
}

.value {
  color: #606266;
  font-weight: 500;
}

.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shortcut-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.key {
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #606266;
}

.desc {
  color: #909399;
  font-size: 13px;
}
</style>
