<template>
  <div class="settings-page">
    <a-row :gutter="20">
      <a-col :span="16">
        <a-card hoverable>
          <template #title>
            <div class="card-header">
              <span>应用设置</span>
            </div>
          </template>

          <a-form :model="settings" layout="vertical">
            <a-divider>后端连接</a-divider>
            
            <a-form-item label="后端地址">
              <a-input v-model="settings.backendUrl" placeholder="http://localhost:9090" />
            </a-form-item>

            <a-divider>AI 配置</a-divider>

            <a-form-item label="默认 AI 模型">
              <a-select v-model="settings.defaultModel" style="width: 100%;">
                <a-option value="iflow">iFlow AI</a-option>
                <a-option value="qwencode">Qwen Code</a-option>
                <a-option value="qwen">通义千问</a-option>
              </a-select>
            </a-form-item>

            <a-form-item label="API Key">
              <a-input-password
                v-model="settings.apiKey"
                placeholder="输入你的 API Key"
                allow-clear
              />
            </a-form-item>

            <a-divider>界面设置</a-divider>

            <a-form-item label="主题">
              <a-radio-group v-model="settings.theme" type="button">
                <a-radio value="light">浅色</a-radio>
                <a-radio value="dark">深色</a-radio>
                <a-radio value="auto">自动</a-radio>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="语言">
              <a-select v-model="settings.language">
                <a-option value="zh-CN">简体中文</a-option>
                <a-option value="en-US">English</a-option>
              </a-select>
            </a-form-item>

            <a-form-item label="每页显示条数">
              <a-slider v-model="settings.pageSize" :min="10" :max="100" :step="10" show-ticks />
            </a-form-item>

            <a-divider>数据设置</a-divider>

            <a-form-item label="自动保存查询">
              <a-switch v-model="settings.autoSaveQuery" />
            </a-form-item>

            <a-form-item label="查询历史保留">
              <a-select v-model="settings.historyRetention">
                <a-option :value="7">7 天</a-option>
                <a-option :value="30">30 天</a-option>
                <a-option :value="90">90 天</a-option>
                <a-option :value="-1">永久</a-option>
              </a-select>
            </a-form-item>

            <a-form-item>
              <a-space>
                <a-button type="primary" @click="saveSettings">保存设置</a-button>
                <a-button @click="resetSettings">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card hoverable>
          <template #title>
            <span>关于</span>
          </template>
          <div class="about-content">
            <h3>数据分析助手</h3>
            <p class="version">版本 2.0.0</p>
            <p class="description">
              基于 Vue 3 + Electron 构建的智能数据分析桌面应用
            </p>
            <a-divider />
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
                <span class="label">Arco Design Vue:</span>
                <span class="value">2.55.0</span>
              </div>
            </div>
          </div>
        </a-card>

        <a-card hoverable style="margin-top: 20px;">
          <template #title>
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
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Message } from '@arco-design/web-vue'

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
  Message.success('设置已保存')
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
  Message.info('设置已重置')
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
  color: var(--color-text-1);
}

.version {
  color: var(--color-text-3);
  font-size: 14px;
  margin: 0 0 16px 0;
}

.description {
  color: var(--color-text-2);
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
  border-bottom: 1px solid var(--color-neutral-3);
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: var(--color-text-3);
}

.value {
  color: var(--color-text-2);
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
  background: var(--color-fill-2);
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: var(--color-text-2);
}

.desc {
  color: var(--color-text-3);
  font-size: 13px;
}
</style>
