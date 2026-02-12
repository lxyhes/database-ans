<template>
  <el-drawer
    v-model="visible"
    :title="title"
    size="600px"
    destroy-on-close
    @open="handleOpen"
  >
    <div class="table-drawer-content">
      <div class="drawer-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索表名"
          clearable
          style="width: 250px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="header-actions">
          <span class="table-count">共 {{ filteredTables.length }} 个表</span>
          <el-button 
            type="primary" 
            @click="refreshTables"
            :loading="loading"
          >
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <div class="table-list-container" v-loading="loading">
        <el-empty v-if="filteredTables.length === 0" description="暂无表数据" />
        
        <!-- 使用虚拟滚动列表 -->
        <div v-else class="virtual-list-container" ref="listContainerRef">
          <div
            v-for="table in displayedTables"
            :key="table.name"
            class="table-item"
            :class="{ 'is-expanded': expandedTable === table.name }"
          >
            <!-- 表头 -->
            <div class="table-header" @click="toggleTable(table.name)">
              <div class="table-header-main">
                <el-icon class="expand-icon"><ArrowRight /></el-icon>
                <el-icon class="table-icon"><Grid /></el-icon>
                <span class="table-name" :title="table.name">{{ table.name }}</span>
                <el-tag v-if="table.rowCount !== undefined && table.rowCount !== null" size="small" type="info" effect="plain">
                  {{ formatRowCount(table.rowCount) }}
                </el-tag>
              </div>
              <div v-if="table.comment" class="table-comment-row">
                {{ table.comment }}
              </div>
            </div>
            
            <!-- 列信息 - 懒加载渲染 -->
            <div v-if="expandedTable === table.name" class="table-content">
              <div v-if="table.loadingColumns" class="loading-state">
                <el-icon class="is-loading"><Loading /></el-icon>
                加载中...
              </div>
              <div v-else-if="table.columns.length > 0" class="column-list">
                <el-table
                  :data="table.columns"
                  size="small"
                  border
                  style="width: 100%"
                  :header-cell-style="{ background: '#f5f7fa', fontWeight: 500 }"
                >
                  <el-table-column prop="name" label="列名" min-width="100" show-overflow-tooltip />
                  <el-table-column prop="type" label="类型" width="80" show-overflow-tooltip />
                  <el-table-column prop="nullable" label="可空" width="50" align="center">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.nullable === 'YES' ? 'info' : 'success'">
                        {{ row.nullable === 'YES' ? '是' : '否' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="comment" label="注释" min-width="120" show-overflow-tooltip />
                </el-table>
              </div>
              <div v-else class="empty-state">暂无列信息</div>
            </div>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="hasMoreTables" class="load-more">
            <el-button link size="small" @click="loadMore">
              加载更多 (还有 {{ filteredTables.length - displayedTables.length }} 个)
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Grid, Refresh, Search, ArrowRight, Loading } from '@element-plus/icons-vue'
import { getDataSourceTables, getTableColumns } from '@/api/datasource'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  dataSourceId: {
    type: [Number, String],
    default: null
  },
  title: {
    type: String,
    default: '表结构'
  }
})

const emit = defineEmits(['update:modelValue'])

// 内部状态
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const tableList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const expandedTable = ref(null)
const displayLimit = ref(50) // 初始显示50个表
const listContainerRef = ref(null)

// 防抖搜索
let searchTimeout = null
const debouncedSearch = (val) => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    searchKeyword.value = val
    displayLimit.value = 50 // 重置显示数量
  }, 300)
}

// 监听搜索输入
watch(() => searchKeyword.value, (val) => {
  displayLimit.value = 50
})

// 过滤后的表列表
const filteredTables = computed(() => {
  if (!searchKeyword.value) return tableList.value
  const keyword = searchKeyword.value.toLowerCase()
  return tableList.value.filter(table => 
    table.name.toLowerCase().includes(keyword) ||
    (table.comment && table.comment.toLowerCase().includes(keyword))
  )
})

// 分页显示的表
const displayedTables = computed(() => {
  return filteredTables.value.slice(0, displayLimit.value)
})

// 是否还有更多
const hasMoreTables = computed(() => {
  return filteredTables.value.length > displayLimit.value
})

// 格式化行数
const formatRowCount = (count) => {
  if (count >= 1000000) {
    return (count / 1000000).toFixed(1) + 'M'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'K'
  }
  return count
}

// 加载更多
const loadMore = () => {
  displayLimit.value += 50
}

// 处理抽屉打开
const handleOpen = () => {
  searchKeyword.value = ''
  expandedTable.value = null
  displayLimit.value = 50
  loadTableList()
}

// 加载表列表
const loadTableList = async () => {
  if (!props.dataSourceId) return
  
  loading.value = true
  try {
    const res = await getDataSourceTables(props.dataSourceId)
    if (res.success) {
      tableList.value = res.data.map(table => ({
        ...table,
        columns: [],
        loadingColumns: false,
        loaded: false
      }))
    } else {
      ElMessage.error(res.message || '加载表列表失败')
    }
  } catch (error) {
    ElMessage.error('加载表列表失败')
  } finally {
    loading.value = false
  }
}

// 刷新表列表
const refreshTables = () => {
  loadTableList()
}

// 切换表展开/收起
const toggleTable = async (tableName) => {
  if (expandedTable.value === tableName) {
    expandedTable.value = null
    return
  }
  
  expandedTable.value = tableName
  
  const table = tableList.value.find(t => t.name === tableName)
  if (!table || table.loaded) return
  
  table.loadingColumns = true
  try {
    const res = await getTableColumns(props.dataSourceId, tableName)
    if (res.success) {
      table.columns = res.data
      table.loaded = true
    } else {
      ElMessage.error(res.message || '加载列信息失败')
    }
  } catch (error) {
    ElMessage.error('加载列信息失败')
  } finally {
    table.loadingColumns = false
  }
}

// 监听数据源ID变化，自动刷新
watch(() => props.dataSourceId, () => {
  if (visible.value && props.dataSourceId) {
    loadTableList()
  }
})
</script>

<style scoped lang="scss">
.table-drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;

  .drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    margin-bottom: 16px;
    border-bottom: 1px solid #ebeef5;

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .table-count {
        color: #909399;
        font-size: 13px;
      }
    }
  }

  .table-list-container {
    flex: 1;
    overflow-y: auto;

    .virtual-list-container {
      .table-item {
        border-bottom: 1px solid #f0f0f0;

        &.is-expanded {
          .table-header {
            background: #f5f7fa;

            .expand-icon {
              transform: rotate(90deg);
            }
          }
        }

        .table-header {
          display: flex;
          flex-direction: column;
          padding: 5px 8px;
          cursor: pointer;
          transition: background 0.2s;

          &:hover {
            background: #f5f7fa;
          }

          .table-header-main {
            display: flex;
            align-items: center;
            gap: 6px;

            .expand-icon {
              transition: transform 0.2s;
              color: #909399;
              font-size: 12px;
              flex-shrink: 0;
            }

            .table-icon {
              color: #409eff;
              font-size: 14px;
              flex-shrink: 0;
            }

            .table-name {
              font-weight: 500;
              color: #303133;
              font-size: 13px;
              flex-shrink: 0;
            }

            .el-tag {
              font-size: 11px;
              height: 18px;
              line-height: 16px;
              padding: 0 6px;
              flex-shrink: 0;
            }
          }

          .table-comment-row {
            color: #909399;
            font-size: 12px;
            padding-left: 24px;
            margin-top: 2px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .table-content {
          padding: 0 6px 8px 24px;
          background: #fafafa;

          .loading-state {
            padding: 12px;
            text-align: center;
            color: #909399;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            font-size: 13px;
          }

          .empty-state {
            padding: 12px;
            text-align: center;
            color: #909399;
            font-size: 13px;
          }

          .column-list {
            :deep(.el-table) {
              font-size: 12px;

              .el-table__cell {
                padding: 4px 0;
              }

              .el-tag {
                font-size: 11px;
                height: 18px;
                line-height: 16px;
                padding: 0 4px;
              }
            }
          }
        }
      }

      .load-more {
        padding: 12px;
        text-align: center;
      }
    }
  }
}

:deep(.el-drawer__body) {
  padding: 16px;
}
</style>
