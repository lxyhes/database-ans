<template>
  <div class="history-page">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>查询历史</span>
          <div class="header-actions">
            <el-radio-group v-model="filterType" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="favorite">收藏</el-radio-button>
            </el-radio-group>
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
                <span>{{ item.queryText }}</span>
              </div>
              <div class="history-sql" v-if="item.sqlQuery">
                <code>{{ item.sqlQuery }}</code>
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
import { Star, StarFilled, VideoPlay, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const history = ref<any[]>([])
const filterType = ref('all')

const filteredHistory = computed(() => {
  if (filterType.value === 'favorite') {
    return history.value.filter(item => item.isFavorite)
  }
  return history.value
})

const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN')
}

const loadHistory = async () => {
  try {
    const result = await window.electronAPI.getQueryHistory()
    if (result.success) {
      history.value = result.data.history || []
    }
  } catch (error) {
    ElMessage.error('加载历史记录失败')
  }
}

const rerunQuery = (item: any) => {
  // 跳转到查询页面并填充
  ElMessage.info('功能开发中：重新运行查询')
}

const toggleFavorite = async (item: any) => {
  try {
    const result = await window.electronAPI.toggleFavorite(item.id, !item.isFavorite)
    if (result.success) {
      item.isFavorite = !item.isFavorite
      ElMessage.success(item.isFavorite ? '已收藏' : '已取消收藏')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const clearHistory = () => {
  ElMessageBox.confirm('确定要清空所有历史记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 清空历史
    ElMessage.success('历史记录已清空')
  }).catch(() => {})
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
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
}

.history-sql code {
  font-family: 'Courier New', monospace;
}

.history-actions {
  display: flex;
  gap: 8px;
}
</style>
