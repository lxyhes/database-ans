<template>
  <div class="global-search">
    <a-card class="search-card">
      <div class="search-header">
        <a-input-search
          v-model="searchKeyword"
          placeholder="搜索数据表、字段、报表、查询历史..."
          :style="{ width: '600px' }"
          size="large"
          allow-clear
          @search="doSearch"
          @press-enter="doSearch"
        >
          <template #prefix><icon-search /></template>
        </a-input-search>
        
        <div class="search-filters">
          <a-checkbox-group v-model="searchTypes" @change="doSearch">
            <a-checkbox value="table">数据表</a-checkbox>
            <a-checkbox value="column">字段</a-checkbox>
            <a-checkbox value="report">报表</a-checkbox>
            <a-checkbox value="query">查询历史</a-checkbox>
          </a-checkbox-group>
        </div>
      </div>
      
      <div class="search-suggestions" v-if="suggestions.length > 0 && !searchKeyword">
        <div class="suggestion-title">热门搜索</div>
        <a-space wrap>
          <a-tag v-for="sug in suggestions" :key="sug" color="arcoblue" class="suggestion-tag" @click="useSuggestion(sug)">
            {{ sug }}
          </a-tag>
        </a-space>
      </div>
      
      <div class="search-history" v-if="searchHistory.length > 0 && !searchKeyword">
        <div class="history-title">
          <span>搜索历史</span>
          <a-button type="text" size="small" @click="clearHistory">清空</a-button>
        </div>
        <a-list :bordered="false" size="small">
          <a-list-item v-for="item in searchHistory" :key="item.keyword">
            <a-list-item-meta :title="item.keyword" @click="useSuggestion(item.keyword)" />
            <template #actions>
              <a-button type="text" size="small" @click="removeHistory(item.keyword)">
                <icon-delete />
              </a-button>
            </template>
          </a-list-item>
        </a-list>
      </div>
    </a-card>
    
    <a-card v-if="searching" class="results-card">
      <div class="loading-container">
        <a-spin size="32" />
        <p>正在搜索...</p>
      </div>
    </a-card>
    
    <a-card v-else-if="searchResults.length > 0" class="results-card">
      <template #title>
        <div class="results-header">
          <span>搜索结果 ({{ totalResults }} 条)</span>
          <a-select v-model="sortBy" style="width: 150px" @change="sortResults">
            <a-option value="relevance">相关度</a-option>
            <a-option value="time">时间</a-option>
            <a-option value="name">名称</a-option>
          </a-select>
        </div>
      </template>
      
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="all" :title="`全部 (${totalResults})`">
          <div class="results-list">
            <div v-for="result in searchResults" :key="result.id + result.type" class="result-item" @click="viewResult(result)">
              <div class="result-icon">
                <icon-file v-if="result.type === 'table'" />
                <icon-file v-else-if="result.type === 'column'" />
                <icon-bar-chart v-else-if="result.type === 'report'" />
                <icon-history v-else-if="result.type === 'query'" />
              </div>
              <div class="result-content">
                <div class="result-title" v-html="highlightKeyword(result.name)"></div>
                <div class="result-desc" v-if="result.description" v-html="highlightKeyword(result.description)"></div>
                <div class="result-meta">
                  <a-tag :color="getTypeColor(result.type)" size="small">{{ getTypeText(result.type) }}</a-tag>
                  <span class="meta-item">{{ result.dataSourceName || '-' }}</span>
                  <span class="meta-item" v-if="result.tableName">{{ result.tableName }}</span>
                </div>
              </div>
              <div class="result-time">{{ formatTime(result.updatedAt || result.createdAt) }}</div>
            </div>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="table" :title="`数据表 (${tableResults.length})`">
          <div class="results-list">
            <div v-for="result in tableResults" :key="result.id" class="result-item" @click="viewResult(result)">
              <div class="result-icon"><icon-apps /></div>
              <div class="result-content">
                <div class="result-title" v-html="highlightKeyword(result.name)"></div>
                <div class="result-desc" v-if="result.description" v-html="highlightKeyword(result.description)"></div>
                <div class="result-meta">
                  <a-tag color="arcoblue" size="small">数据表</a-tag>
                  <span class="meta-item">{{ result.dataSourceName }}</span>
                  <span class="meta-item">{{ result.columnCount }} 字段</span>
                  <span class="meta-item">{{ result.rowCount }} 行</span>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="column" :title="`字段 (${columnResults.length})`">
          <div class="results-list">
            <div v-for="result in columnResults" :key="result.id" class="result-item" @click="viewResult(result)">
              <div class="result-icon"><icon-file /></div>
              <div class="result-content">
                <div class="result-title" v-html="highlightKeyword(result.name)"></div>
                <div class="result-meta">
                  <a-tag color="green" size="small">字段</a-tag>
                  <span class="meta-item">{{ result.tableName }}</span>
                  <span class="meta-item">{{ result.dataType }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="report" :title="`报表 (${reportResults.length})`">
          <div class="results-list">
            <div v-for="result in reportResults" :key="result.id" class="result-item" @click="viewResult(result)">
              <div class="result-icon"><icon-bar-chart /></div>
              <div class="result-content">
                <div class="result-title" v-html="highlightKeyword(result.name)"></div>
                <div class="result-desc" v-if="result.description" v-html="highlightKeyword(result.description)"></div>
                <div class="result-meta">
                  <a-tag color="orange" size="small">报表</a-tag>
                  <span class="meta-item">{{ result.dataSourceName }}</span>
                </div>
              </div>
              <div class="result-time">{{ formatTime(result.createdAt) }}</div>
            </div>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="query" :title="`查询历史 (${queryResults.length})`">
          <div class="results-list">
            <div v-for="result in queryResults" :key="result.id" class="result-item" @click="viewResult(result)">
              <div class="result-icon"><icon-history /></div>
              <div class="result-content">
                <div class="result-title" v-html="highlightKeyword(result.queryText)"></div>
                <div class="result-meta">
                  <a-tag color="purple" size="small">查询历史</a-tag>
                  <span class="meta-item">{{ result.dataSourceName }}</span>
                </div>
              </div>
              <div class="result-time">{{ formatTime(result.createdAt) }}</div>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    
    <a-card v-else-if="hasSearched" class="results-card">
      <a-empty description="未找到相关结果">
        <template #image>
          <icon-search :size="64" :style="{ color: '#c9cdd4' }" />
        </template>
      </a-empty>
    </a-card>
    
    <a-modal v-model:visible="detailDialogVisible" :title="currentResult?.name" width="800px" :footer="false">
      <div v-if="currentResult" class="result-detail">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="类型">
            <a-tag :color="getTypeColor(currentResult.type)">{{ getTypeText(currentResult.type) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="数据源">{{ currentResult.dataSourceName }}</a-descriptions-item>
          <a-descriptions-item label="名称">{{ currentResult.name }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(currentResult.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="2">{{ currentResult.description || '-' }}</a-descriptions-item>
        </a-descriptions>
        
        <div class="detail-actions">
          <a-button type="primary" @click="goToDetail(currentResult)">
            <template #icon><icon-eye /></template>
            查看详情
          </a-button>
          <a-button v-if="currentResult.type === 'query'" @click="useQuery(currentResult)">
            <template #icon><icon-play-arrow /></template>
            使用此查询
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import {
  IconSearch,
  IconFile,
  IconBarChart,
  IconHistory,
  IconDelete,
  IconEye,
  IconPlayArrow
} from '@arco-design/web-vue/es/icon'
import request from '@/utils/request'

const router = useRouter()

const searchKeyword = ref('')
const searchTypes = ref(['table', 'column', 'report', 'query'])
const sortBy = ref('relevance')
const searching = ref(false)
const hasSearched = ref(false)
const searchResults = ref<any[]>([])
const suggestions = ref<string[]>(['销售', '订单', '客户', '产品', '库存'])
const searchHistory = ref<any[]>([])
const activeTab = ref('all')
const detailDialogVisible = ref(false)
const currentResult = ref<any>(null)

const totalResults = computed(() => searchResults.value.length)

const tableResults = computed(() => searchResults.value.filter(r => r.type === 'table'))
const columnResults = computed(() => searchResults.value.filter(r => r.type === 'column'))
const reportResults = computed(() => searchResults.value.filter(r => r.type === 'report'))
const queryResults = computed(() => searchResults.value.filter(r => r.type === 'query'))

const doSearch = async () => {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    hasSearched.value = false
    return
  }
  
  searching.value = true
  hasSearched.value = true
  
  try {
    const res = await request.get('/api/search/global', {
      params: {
        keyword: searchKeyword.value,
        types: searchTypes.value.join(',')
      }
    })
    
    if (res.success) {
      searchResults.value = res.data || []
      saveSearchHistory(searchKeyword.value)
    }
  } catch (error) {
    console.error('搜索失败', error)
    Message.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const saveSearchHistory = (keyword: string) => {
  const history = JSON.parse(localStorage.getItem('searchHistory') || '[]')
  const existing = history.findIndex((h: any) => h.keyword === keyword)
  if (existing >= 0) {
    history.splice(existing, 1)
  }
  history.unshift({ keyword, time: new Date().toISOString() })
  if (history.length > 10) history.pop()
  localStorage.setItem('searchHistory', JSON.stringify(history))
  loadSearchHistory()
}

const loadSearchHistory = () => {
  try {
    searchHistory.value = JSON.parse(localStorage.getItem('searchHistory') || '[]')
  } catch {
    searchHistory.value = []
  }
}

const clearHistory = () => {
  localStorage.removeItem('searchHistory')
  searchHistory.value = []
  Message.success('历史已清空')
}

const removeHistory = (keyword: string) => {
  const history = searchHistory.value.filter(h => h.keyword !== keyword)
  searchHistory.value = history
  localStorage.setItem('searchHistory', JSON.stringify(history))
}

const useSuggestion = (keyword: string) => {
  searchKeyword.value = keyword
  doSearch()
}

const highlightKeyword = (text: string) => {
  if (!text || !searchKeyword.value) return text
  const regex = new RegExp(`(${searchKeyword.value})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

const sortResults = () => {
  if (sortBy.value === 'name') {
    searchResults.value.sort((a, b) => a.name.localeCompare(b.name))
  } else if (sortBy.value === 'time') {
    searchResults.value.sort((a, b) => {
      const timeA = new Date(a.updatedAt || a.createdAt).getTime()
      const timeB = new Date(b.updatedAt || b.createdAt).getTime()
      return timeB - timeA
    })
  }
}

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    table: 'arcoblue',
    column: 'green',
    report: 'orange',
    query: 'purple'
  }
  return colors[type] || 'gray'
}

const getTypeText = (type: string) => {
  const texts: Record<string, string> = {
    table: '数据表',
    column: '字段',
    report: '报表',
    query: '查询历史'
  }
  return texts[type] || type
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const viewResult = (result: any) => {
  currentResult.value = result
  detailDialogVisible.value = true
}

const goToDetail = (result: any) => {
  detailDialogVisible.value = false
  
  switch (result.type) {
    case 'table':
      router.push({ path: '/query', query: { table: result.name, dataSourceId: result.dataSourceId } })
      break
    case 'column':
      router.push({ path: '/query', query: { table: result.tableName, column: result.name, dataSourceId: result.dataSourceId } })
      break
    case 'report':
      router.push({ path: '/reports', query: { id: result.id } })
      break
    case 'query':
      router.push({ path: '/nl2sql', query: { queryId: result.id } })
      break
  }
}

const useQuery = (result: any) => {
  detailDialogVisible.value = false
  router.push({ path: '/nl2sql', query: { queryId: result.id } })
}

onMounted(() => {
  loadSearchHistory()
})
</script>

<style scoped>
.global-search {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-card {
  flex-shrink: 0;
}

.search-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
}

.search-filters {
  display: flex;
  justify-content: center;
}

.search-suggestions,
.search-history {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e6eb;
}

.suggestion-title,
.history-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #1d2129;
}

.suggestion-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.suggestion-tag:hover {
  transform: translateY(-2px);
}

.results-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #86909c;
}

.results-list {
  max-height: 500px;
  overflow-y: auto;
}

.result-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid #e5e6eb;
  cursor: pointer;
  transition: background 0.2s;
}

.result-item:hover {
  background: #f7f8fa;
}

.result-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #e8f3ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #165dff;
  margin-right: 16px;
  flex-shrink: 0;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
  margin-bottom: 4px;
}

.result-title :deep(mark) {
  background: #fff7e8;
  color: #ff7d00;
  padding: 0 2px;
  border-radius: 2px;
}

.result-desc {
  font-size: 14px;
  color: #86909c;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-desc :deep(mark) {
  background: #fff7e8;
  color: #ff7d00;
  padding: 0 2px;
  border-radius: 2px;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-item {
  font-size: 12px;
  color: #86909c;
}

.result-time {
  font-size: 12px;
  color: #c9cdd4;
  flex-shrink: 0;
}

.result-detail {
  padding: 20px 0;
}

.detail-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
