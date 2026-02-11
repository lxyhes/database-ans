<template>
  <div class="iflow-page">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover" class="chat-card">
          <template #header>
            <div class="card-header">
              <span>iFlow AI 智能助手</span>
              <el-tag type="success">Powered by AI</el-tag>
            </div>
          </template>

          <!-- 对话区域 -->
          <div class="chat-container" ref="chatContainer">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', msg.type]"
            >
              <div class="message-avatar">
                <el-avatar
                  :size="40"
                  :src="msg.type === 'user' ? userAvatar : aiAvatar"
                />
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-author">{{ msg.type === 'user' ? '我' : 'iFlow AI' }}</span>
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                </div>
                <div class="message-body">
                  <div v-if="msg.type === 'loading'" class="loading-dots">
                    <span></span><span></span><span></span>
                  </div>
                  <div v-else-if="msg.type === 'ai'">
                    <markdown-renderer :content="String(msg.content)" />
                  </div>
                  <div v-else>{{ msg.content }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="chat-input-area">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入你的问题，iFlow AI 会帮你解答..."
              @keydown.enter.prevent="sendMessage"
            />
            <div class="input-actions">
              <el-select v-model="selectedFunction" size="small" style="width: 150px;">
                <el-option label="智能对话" value="query" />
                <el-option label="数据分析" value="analyze" />
                <el-option label="SQL生成" value="generate-sql" />
                <el-option label="代码生成" value="generate-code" />
                <el-option label="代码解释" value="explain-code" />
                <el-option label="数据洞察" value="insights" />
              </el-select>
              <el-button
                type="primary"
                :icon="Position"
                :loading="loading"
                @click="sendMessage"
              >
                发送
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="function-card">
          <template #header>
            <span>功能说明</span>
          </template>
          <el-timeline>
            <el-timeline-item type="primary">
              <h4>智能对话</h4>
              <p class="function-desc">与 AI 进行自然语言对话，获取数据相关帮助</p>
            </el-timeline-item>
            <el-timeline-item type="success">
              <h4>数据分析</h4>
              <p class="function-desc">对数据进行深度分析，发现隐藏的模式和趋势</p>
            </el-timeline-item>
            <el-timeline-item type="warning">
              <h4>SQL 生成</h4>
              <p class="function-desc">用自然语言描述需求，自动生成 SQL 语句</p>
            </el-timeline-item>
            <el-timeline-item type="info">
              <h4>代码助手</h4>
              <p class="function-desc">生成、解释、优化代码</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card shadow="hover" class="tips-card">
          <template #header>
            <span>使用提示</span>
          </template>
          <ul class="tips-list">
            <li>描述越详细，回答越准确</li>
            <li>可以追问以获取更多信息</li>
            <li>支持代码高亮和复制</li>
            <li>可以切换不同功能模式</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'

const messages = ref<any[]>([
  {
    type: 'ai',
    content: '你好！我是 iFlow AI 智能助手。我可以帮你进行数据分析、生成 SQL、编写代码等。请问有什么可以帮你的？',
    time: new Date()
  }
])

const inputMessage = ref('')
const loading = ref(false)
const selectedFunction = ref('query')
const chatContainer = ref<HTMLElement>()

const userAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
const aiAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const formatTime = (time: Date) => {
  return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}



const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  const userMsg = inputMessage.value
  messages.value.push({
    type: 'user',
    content: userMsg,
    time: new Date()
  })

  inputMessage.value = ''
  loading.value = true

  // 添加加载消息
  messages.value.push({
    type: 'loading',
    time: new Date()
  })

  await scrollToBottom()

  try {
    const result = await window.electronAPI.iflowQuery(userMsg, selectedFunction.value)
    
    // 移除加载消息
    messages.value.pop()

    if (result.success) {
      // 处理响应数据，提取实际内容
      let content = result.data
      if (typeof result.data === 'object' && result.data !== null) {
        content = result.data.result || result.data.response || result.data.message || JSON.stringify(result.data)
      }
      messages.value.push({
        type: 'ai',
        content: content,
        time: new Date()
      })
    } else {
      messages.value.push({
        type: 'ai',
        content: '抱歉，处理请求时出现错误：' + result.message,
        time: new Date()
      })
    }
  } catch (error) {
    messages.value.pop()
    messages.value.push({
      type: 'ai',
      content: '抱歉，服务暂时不可用，请稍后重试。',
      time: new Date()
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}


</script>

<style scoped>
.chat-card {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 12px;
}

.message-content {
  max-width: 70%;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message.user .message-content {
  background: #409EFF;
  color: #fff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.message-author {
  font-weight: 600;
}

.message-time {
  color: #909399;
  margin-left: 12px;
}

.message.user .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.message-body {
  line-height: 1.6;
}

.loading-dots {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: #409EFF;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input-area {
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.function-card,
.tips-card {
  margin-bottom: 20px;
}

.function-desc {
  color: #606266;
  font-size: 13px;
  margin: 4px 0 0 0;
}

.tips-list {
  padding-left: 20px;
  color: #606266;
  line-height: 2;
}

.tips-list li {
  margin-bottom: 8px;
}
</style>
