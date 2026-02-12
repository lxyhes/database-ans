<template>
  <div class="history-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>查询历史</span>
          <div class="header-actions">
            <el-input
              v-model="searchText"
              placeholder="搜索查询内容..."
              :prefix-icon="Search"
              clearable
              style="width: 200px"
              size="small"
            />
            <el-select v-model="filterDate" placeholder="时间筛选" size="small" style="width: 120px" clearable>
              <el-option label="今天" value="today" />
              <el-option label="近7天" value="week" />
              <el-option label="近30天" value="month" />
            </el-select>
            <el-select v-model="filterType" placeholder="类型筛选" size="small" style="width: 100px" clearable>
              <el-option label="全部" value="all" />
              <el-option label="收藏" value="favorite" />
              <el-option label="NL查询" value="nl" />
              <el-option label="SQL查询" value="sql" />
            </el-select>
            <el-button type="danger" :icon="Delete" size="small" @click="clearHistory">
              清空
            </el-button>
          </div>
        </div>
      </template>

      <el-timeline v-if="filteredHistory.length > 0">
        <el-timeline-item
          v-for="item in filteredHistory"
          :key="item.id"
          :type="item.isFavorite ? 'primary' : 'info'"
          :timestamp="formatTime(item.createdAt)"
        >
          <el-card shadow="hover" class="history-item">
            <div class="history-content">
              <div class="history-query">
                <el-icon v-if="item.isFavorite" color="#E6A23C"><StarFilled /></el-icon>
                <el-tag v-if="item.queryType" size="small" :type="item.queryType === 'nl' ? 'success' : 'info'" style="margin-right: 8px">
                  {{ item.queryType === 'nl' ? '自然语言' : 'SQL' }}
                </el-tag>
                <span>{{ item.queryText }}</span>
              </div>
              <div class="history-sql" v-if="item.sqlQuery">
                <code>{{ item.sqlQuery }}</code>
              </div>
              <div class="history-tags" v-if="item.tags">
                <el-tag v-for="tag in item.tags.split(',')" :key="tag" size="small" style="margin-right: 4px">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
            <div class="history-actions">
              <el-button
                type="primary"
                :icon="VideoPlay"
                size="small"
                @click="rerunQuery(item)"
              >
                重新运行
              </el-button>
              <el-button
                :type="item.isFavorite ? 'warning' : 'default'"
                :icon="item.isFavorite ? StarFilled : Star"
                size="small"
                @click="toggleFavorite(item)"
              >
                {{ item.isFavorite ? '取消收藏' : '收藏' }}
              </el-button>
              <el-button
                type="danger"
                :icon="Delete"
                size="small"
                @click="deleteItem(item)"
              >
                删除
              </el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-else description="暂无查询历史" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Star, StarFilled, VideoPlay, Delete, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
    ElMessage.error('加载历史记录失败')
  }
}

const rerunQuery = (item: any) => {
  ElMessage.info('功能开发中：重新运行查询')
}

const toggleFavorite = async (item: any) => {
  try {
    await request.post(`/api/query-history/${item.id}/toggle-favorite`)
    item.isFavorite = !item.isFavorite
    ElMessage.success(item.isFavorite ? '已收藏' : '已取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteItem = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/api/query-history/${item.id}`)
    history.value = history.value.filter(h => h.id !== item.id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const clearHistory = () => {
  ElMessageBox.confirm('确定要清空所有历史记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    history.value = []
    ElMessage.success('历史记录已清空')
  }).catch(() => {})
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.history-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
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
  color: #303133;
  margin-bottom: 8px;
}

.history-sql {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
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
}
</style>
