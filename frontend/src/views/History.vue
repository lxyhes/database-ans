<template>
  <div class="history-page">
    <a-card :bordered="false">
      <template #title>
        <div class="card-header">
          <span>查询历史</span>
          <div class="header-actions">
            <a-input-search
              v-model="searchText"
              placeholder="搜索查询内容..."
              :style="{ width: '200px' }"
              size="small"
              allow-clear
            />
            <a-select v-model="filterDate" placeholder="时间筛选" :style="{ width: '120px' }" size="small" allow-clear>
              <a-option value="today">今天</a-option>
              <a-option value="week">近7天</a-option>
              <a-option value="month">近30天</a-option>
            </a-select>
            <a-select v-model="filterType" placeholder="类型筛选" :style="{ width: '100px' }" size="small" allow-clear>
              <a-option value="all">全部</a-option>
              <a-option value="favorite">收藏</a-option>
              <a-option value="nl">NL查询</a-option>
              <a-option value="sql">SQL查询</a-option>
            </a-select>
            <a-button status="danger" size="small" @click="clearHistory">
              <template #icon><icon-delete /></template>
              清空
            </a-button>
          </div>
        </div>
      </template>

      <a-timeline v-if="filteredHistory.length > 0" mode="left">
        <a-timeline-item
          v-for="item in filteredHistory"
          :key="item.id"
          :label="formatTime(item.createdAt)"
        >
          <a-card :bordered="true" class="history-item" hoverable>
            <div class="history-content">
              <div class="history-query">
                <icon-star-fill v-if="item.isFavorite" :style="{ color: '#ff7d00' }" />
                <a-tag v-if="item.queryType" :color="item.queryType === 'nl' ? 'green' : 'blue'" size="small" style="margin-right: 8px">
                  {{ item.queryType === 'nl' ? '自然语言' : 'SQL' }}
                </a-tag>
                <span>{{ item.queryText }}</span>
              </div>
              <div class="history-sql" v-if="item.sqlQuery">
                <code>{{ item.sqlQuery }}</code>
              </div>
              <div class="history-tags" v-if="item.tags">
                <a-tag v-for="tag in item.tags.split(',')" :key="tag" size="small" style="margin-right: 4px">
                  {{ tag }}
                </a-tag>
              </div>
            </div>
            <div class="history-actions">
              <a-button
                type="primary"
                size="small"
                @click="rerunQuery(item)"
              >
                <template #icon><icon-play-circle /></template>
                重新运行
              </a-button>
              <a-button
                :status="item.isFavorite ? 'warning' : 'default'"
                size="small"
                @click="toggleFavorite(item)"
              >
                <template #icon>
                  <icon-star-fill v-if="item.isFavorite" />
                  <icon-star v-else />
                </template>
                {{ item.isFavorite ? '取消收藏' : '收藏' }}
              </a-button>
              <a-button
                status="danger"
                size="small"
                @click="deleteItem(item)"
              >
                <template #icon><icon-delete /></template>
                删除
              </a-button>
            </div>
          </a-card>
        </a-timeline-item>
      </a-timeline>

      <a-empty v-else description="暂无查询历史" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { 
  IconStar, 
  IconStarFill, 
  IconPlayCircle, 
  IconDelete,
  IconSearch 
} from '@arco-design/web-vue/es/icon'
import { Message, Modal } from '@arco-design/web-vue'
import request from '@/utils/request'

const history = ref<any[]>([])
const searchText = ref('')
const filterDate = ref('')
const filterType = ref('all')

const filteredHistory = computed(() => {
  let result = history.value

  // 搜索过滤
  if (searchText.value) {
    const search = searchText.value.toLowerCase()
    result = result.filter(item => 
      item.queryText?.toLowerCase().includes(search) ||
      item.sqlQuery?.toLowerCase().includes(search)
    )
  }

  // 时间过滤
  if (filterDate.value) {
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    
    result = result.filter(item => {
      const itemDate = new Date(item.createdAt)
      switch (filterDate.value) {
        case 'today':
          return itemDate >= today
        case 'week':
          const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)
          return itemDate >= weekAgo
        case 'month':
          const monthAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000)
          return itemDate >= monthAgo
        default:
          return true
      }
    })
  }

  // 类型过滤
  if (filterType.value && filterType.value !== 'all') {
    switch (filterType.value) {
      case 'favorite':
        result = result.filter(item => item.isFavorite)
        break
      case 'nl':
        result = result.filter(item => item.queryType === 'nl')
        break
      case 'sql':
        result = result.filter(item => item.queryType === 'sql')
        break
    }
  }

  return result
})

const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN')
}

const loadHistory = async () => {
  try {
    const res = await request.get('/api/query-history/all')
    history.value = res.data || []
  } catch (error) {
    Message.error('加载历史记录失败')
  }
}

const rerunQuery = (item: any) => {
  Message.info('功能开发中：重新运行查询')
}

const toggleFavorite = async (item: any) => {
  try {
    await request.post(`/api/query-history/${item.id}/toggle-favorite`)
    item.isFavorite = !item.isFavorite
    Message.success(item.isFavorite ? '已收藏' : '已取消收藏')
  } catch (error) {
    Message.error('操作失败')
  }
}

const deleteItem = async (item: any) => {
  try {
    await new Promise((resolve, reject) => {
      Modal.confirm({
        title: '提示',
        content: '确定要删除这条记录吗？',
        onOk: () => resolve(true),
        onCancel: () => reject('cancel')
      })
    })
    await request.delete(`/api/query-history/${item.id}`)
    history.value = history.value.filter(h => h.id !== item.id)
    Message.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      Message.error('删除失败')
    }
  }
}

const clearHistory = () => {
  Modal.confirm({
    title: '提示',
    content: '确定要清空所有历史记录吗？',
    onOk: () => {
      history.value = []
      Message.success('历史记录已清空')
    }
  })
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.history-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-page :deep(.arco-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.history-page :deep(.arco-card-body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.history-item {
  margin-bottom: 10px;
}

.history-content {
  margin-bottom: 12px;
}

.history-query {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1d2129;
  margin-bottom: 8px;
}

.history-sql {
  background: #f2f3f5;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #4e5969;
  margin-bottom: 8px;
  word-break: break-word;
}

.history-sql code {
  font-family: 'Courier New', monospace;
}

.history-tags {
  margin-top: 8px;
}

.history-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
