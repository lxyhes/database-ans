<template>
  <div class="smart-assistant">
    <a-row :gutter="20">
      <a-col :span="6">
        <a-card title="对话历史" class="history-card">
          <template #extra>
            <a-button type="primary" size="small" @click="newConversation">
              <template #icon><icon-plus /></template>
              新对话
            </a-button>
          </template>
          
          <a-list :bordered="false" size="small" class="conversation-list">
            <a-list-item 
              v-for="conv in conversations" 
              :key="conv.id"
              :class="{ active: currentConversationId === conv.id }"
              @click="selectConversation(conv.id)"
            >
              <a-list-item-meta :title="conv.title || '新对话'">
                <template #description>{{ formatTime(conv.updatedAt) }}</template>
              </a-list-item-meta>
              <template #actions>
                <a-button type="text" size="small" @click.stop="deleteConversation(conv.id)">
                  <icon-delete />
                </a-button>
              </template>
            </a-list-item>
          </a-list>
        </a-card>
        
        <a-card title="快捷问题" class="quick-card">
          <a-space direction="column" fill>
            <a-tag v-for="q in quickQuestions" :key="q" color="arcoblue" class="quick-tag" @click="useQuickQuestion(q)">
              {{ q }}
            </a-tag>
          </a-space>
        </a-card>
      </a-col>
      
      <a-col :span="18">
        <a-card class="chat-card">
          <template #title>
            <div class="chat-header">
              <span>智能数据助手</span>
              <a-select v-model="selectedDataSource" placeholder="选择数据源" style="width: 200px">
                <a-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">{{ ds.name }}</a-option>
              </a-select>
            </div>
          </template>
          
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messages.length === 0" class="empty-chat">
              <icon-robot :size="64" :style="{ color: '#c9cdd4' }" />
              <h3>你好，我是智能数据助手</h3>
              <p>你可以用自然语言向我提问，我会帮你分析数据并给出答案</p>
              <div class="example-questions">
                <div class="example-title">试试这些问题：</div>
                <a-tag v-for="q in exampleQuestions" :key="q" color="arcoblue" class="example-tag" @click="useQuickQuestion(q)">
                  {{ q }}
                </a-tag>
              </div>
            </div>
            
            <div v-else>
              <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
                <div class="message-avatar">
                  <icon-user v-if="msg.role === 'user'" />
                  <icon-robot v-else />
                </div>
                <div class="message-content">
                  <div class="message-text" v-html="formatMessage(msg.content)"></div>
                  
                  <div v-if="msg.sqlQuery" class="sql-block">
                    <div class="sql-header">
                      <span>生成的SQL</span>
                      <a-button size="small" @click="copySql(msg.sqlQuery)">
                        <template #icon><icon-copy /></template>
                        复制
                      </a-button>
                    </div>
                    <pre class="sql-code">{{ msg.sqlQuery }}</pre>
                  </div>
                  
                  <div v-if="msg.data && msg.data.length > 0" class="data-block">
                    <a-table :data="msg.data.slice(0, 10)" :pagination="false" size="small" :bordered="true">
                      <a-table-column v-for="key in Object.keys(msg.data[0])" :key="key" :title="key" :data-index="key" :ellipsis="true" />
                    </a-table>
                    <div v-if="msg.data.length > 10" class="more-data">
                      共 {{ msg.data.length }} 条数据，仅显示前 10 条
                    </div>
                  </div>
                  
                  <div v-if="msg.chart" class="chart-block">
                    <v-chart :option="msg.chart" autoresize style="height: 250px" />
                  </div>
                  
                  <div v-if="msg.insight" class="insight-block">
                    <a-alert type="info">
                      <template #title>数据洞察</template>
                      {{ msg.insight }}
                    </a-alert>
                  </div>
                </div>
              </div>
              
              <div v-if="thinking" class="message assistant">
                <div class="message-avatar"><icon-robot /></div>
                <div class="message-content">
                  <a-spin />
                  <span class="thinking-text">正在思考中...</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="chat-input">
            <a-textarea
              v-model="inputMessage"
              placeholder="输入你的问题，例如：上个月销售额最高的产品是什么？"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              @press-enter="handleEnter"
            />
            <div class="input-actions">
              <a-space>
                <a-button @click="clearMessages">
                  <template #icon><icon-delete /></template>
                  清空
                </a-button>
                <a-button type="primary" :loading="thinking" :disabled="!inputMessage.trim()" @click="sendMessage">
                  <template #icon><icon-send /></template>
                  发送
                </a-button>
              </a-space>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconPlus,
  IconDelete,
  IconRobot,
  IconUser,
  IconCopy,
  IconSend
} from '@arco-design/web-vue/es/icon'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '@/utils/request'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const dataSources = ref<any[]>([])
const selectedDataSource = ref<number | null>(null)
const conversations = ref<any[]>([])
const currentConversationId = ref<number | null>(null)
const messages = ref<any[]>([])
const inputMessage = ref('')
const thinking = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

const quickQuestions = [
  '显示总销售额',
  '查询订单数量趋势',
  '找出销售额最高的产品'
]

const exampleQuestions = [
  '上个月销售额最高的产品是什么？',
  '最近7天的订单数量是多少？',
  '各地区的销售占比如何？',
  '哪些客户的复购率最高？'
]

const loadDataSources = async () => {
  try {
    const res = await request.get('/api/datasources')
    dataSources.value = res.data || res
    if (dataSources.value.length > 0) {
      const defaultDs = dataSources.value.find(ds => ds.isDefault || ds.default)
      selectedDataSource.value = defaultDs ? defaultDs.id : dataSources.value[0].id
    }
  } catch (error) {
    console.error('加载数据源失败', error)
  }
}

const loadConversations = async () => {
  try {
    const res = await request.get('/api/assistant/conversations')
    conversations.value = res.data || res
  } catch (error) {
    console.error('加载对话历史失败', error)
  }
}

const newConversation = () => {
  currentConversationId.value = null
  messages.value = []
}

const selectConversation = async (id: number) => {
  currentConversationId.value = id
  try {
    const res = await request.get(`/api/assistant/conversations/${id}/messages`)
    messages.value = res.data || []
    scrollToBottom()
  } catch (error) {
    console.error('加载对话消息失败', error)
  }
}

const deleteConversation = async (id: number) => {
  try {
    await request.delete(`/api/assistant/conversations/${id}`)
    Message.success('删除成功')
    loadConversations()
    if (currentConversationId.value === id) {
      newConversation()
    }
  } catch (error) {
    Message.error('删除失败')
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || thinking.value) return
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMessage
  })
  
  scrollToBottom()
  
  thinking.value = true
  
  try {
    const res = await request.post('/api/assistant/chat', {
      message: userMessage,
      conversationId: currentConversationId.value,
      dataSourceId: selectedDataSource.value
    })
    
    if (res.success) {
      if (!currentConversationId.value && res.conversationId) {
        currentConversationId.value = res.conversationId
        loadConversations()
      }
      
      messages.value.push({
        id: Date.now() + 1,
        role: 'assistant',
        content: res.message || '查询完成',
        sqlQuery: res.sqlQuery,
        data: res.data,
        chart: res.chart,
        insight: res.insight
      })
    } else {
      messages.value.push({
        id: Date.now() + 1,
        role: 'assistant',
        content: res.message || '抱歉，处理您的问题时出现错误'
      })
    }
  } catch (error) {
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，网络错误，请稍后重试'
    })
  } finally {
    thinking.value = false
    scrollToBottom()
  }
}

const handleEnter = (e: KeyboardEvent) => {
  if (!e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const clearMessages = () => {
  messages.value = []
}

const useQuickQuestion = (q: string) => {
  inputMessage.value = q
  sendMessage()
}

const copySql = (sql: string) => {
  navigator.clipboard.writeText(sql)
  Message.success('已复制到剪贴板')
}

const formatMessage = (content: string) => {
  return content.replace(/\n/g, '<br>')
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(() => {
  loadDataSources()
  loadConversations()
})
</script>

<style scoped>
.smart-assistant {
  height: 100%;
}

.history-card {
  margin-bottom: 20px;
}

.conversation-list {
  max-height: 300px;
  overflow-y: auto;
}

.conversation-list :deep(.arco-list-item) {
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-list :deep(.arco-list-item:hover),
.conversation-list :deep(.arco-list-item.active) {
  background: #e8f3ff;
}

.quick-card {
  margin-bottom: 20px;
}

.quick-tag {
  cursor: pointer;
  width: 100%;
  justify-content: flex-start;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  min-height: 400px;
  max-height: 500px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #86909c;
}

.empty-chat h3 {
  margin: 16px 0 8px;
  color: #1d2129;
}

.empty-chat p {
  margin-bottom: 20px;
}

.example-questions {
  text-align: center;
}

.example-title {
  margin-bottom: 12px;
  font-size: 14px;
}

.example-tag {
  cursor: pointer;
  margin: 4px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.message.user .message-avatar {
  background: #165dff;
  margin-right: 12px;
}

.message.assistant .message-avatar {
  background: #00b42a;
  margin-right: 12px;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  background: #f7f8fa;
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
}

.sql-block {
  margin-top: 12px;
  background: #1d2129;
  border-radius: 8px;
  overflow: hidden;
}

.sql-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #272e3b;
  color: #fff;
  font-size: 13px;
}

.sql-code {
  padding: 12px;
  margin: 0;
  color: #d4d4d4;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  overflow-x: auto;
}

.data-block {
  margin-top: 12px;
}

.more-data {
  text-align: center;
  padding: 8px;
  color: #86909c;
  font-size: 12px;
}

.chart-block {
  margin-top: 12px;
}

.insight-block {
  margin-top: 12px;
}

.thinking-text {
  margin-left: 12px;
  color: #86909c;
}

.chat-input {
  border-top: 1px solid #e5e6eb;
  padding-top: 16px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
