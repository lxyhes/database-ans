<template>
  <a-drawer
    v-model:visible="visible"
    :title="title"
    width="600px"
    unmountOnClose
    @after-open="handleOpen"
  >
    <div class="table-drawer-content">
      <div class="drawer-header">
        <a-input
          v-model="searchKeyword"
          placeholder="搜索表名"
          allow-clear
          style="width: 250px"
        >
          <template #prefix>
            <icon-search />
          </template>
        </a-input>
        <div class="header-actions">
          <span class="table-count">共 {{ filteredTables.length }} 个表</span>
          <a-button 
            type="primary" 
            @click="refreshTables"
            :loading="loading"
          >
            <template #icon><icon-refresh /></template>
            刷新
          </a-button>
        </div>
      </div>

      <div class="table-list-container">
        <a-empty v-if="filteredTables.length === 0" description="暂无表数据" />
        
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
                <icon-right class="expand-icon" :class="{ 'expanded': expandedTable === table.name }" />
                <icon-storage class="table-icon" />
                <span class="table-name" :title="table.name">{{ table.name }}</span>
                <a-tag v-if="table.rowCount !== undefined && table.rowCount !== null" size="small" color="blue">
                  {{ formatRowCount(table.rowCount) }}
                </a-tag>
              </div>
              <div v-if="table.comment" class="table-comment-row">
                {{ table.comment }}
              </div>
            </div>
            
            <!-- 列信息 - 懒加载渲染 -->
            <div v-if="expandedTable === table.name" class="table-content">
              <div v-if="table.loadingColumns" class="loading-state">
                <icon-loading class="loading-icon" />
                加载中...
              </div>
              <div v-else-if="table.columns.length > 0" class="column-list">
                <a-table
                  :data="table.columns"
                  :pagination="false"
                  size="small"
                  bordered
                  :scroll="{ x: '100%' }"
                >
                  <template #columns>
                    <a-table-column title="列名" data-index="name" :ellipsis="true" />
                    <a-table-column title="类型" data-index="type" :width="80" />
                    <a-table-column title="可空" data-index="nullable" :width="50" align="center">
                      <template #cell="{ record }">
                        <a-tag v-if="record" size="small" :color="record.nullable === 'YES' ? 'orange' : 'green'">
                          {{ record.nullable === 'YES' ? '是' : '否' }}
                        </a-tag>
                        <span v-else>-</span>
                      </template>
                    </a-table-column>
                    <a-table-column title="注释" data-index="comment" :ellipsis="true" />
                  </template>
                </a-table>
              </div>
              <div v-else class="empty-state">暂无列信息</div>
            </div>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="hasMoreTables" class="load-more">
            <a-button type="text" size="small" @click="loadMore">
              加载更多 (还有 {{ filteredTables.length - displayedTables.length }} 个)
            </a-button>
          </div>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { 
  IconSearch, 
  IconRefresh, 
  IconRight, 
  IconStorage, 
  IconLoading 
} from '@arco-design/web-vue/es/icon'
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
const displayLimit = ref(50)

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
  console.log('TableStructureDrawer opened, dataSourceId:', props.dataSourceId)
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
      Message.error(res.message || '加载表列表失败')
    }
  } catch (error) {
    Message.error('加载表列表失败')
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
      Message.error(res.message || '加载列信息失败')
      // 加载失败时也要标记为已加载，避免重复请求
      table.loaded = true
      table.columns = []
    }
  } catch (error) {
    Message.error('加载列信息失败')
    // 加载失败时也要标记为已加载，避免重复请求
    table.loaded = true
    table.columns = []
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
    border-bottom: 1px solid var(--color-neutral-3);

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .table-count {
        color: var(--color-text-2);
        font-size: 13px;
      }
    }
  }

  .table-list-container {
    flex: 1;
    overflow-y: auto;

    .virtual-list-container {
      .table-item {
        border-bottom: 1px solid var(--color-neutral-3);

        &.is-expanded {
          .table-header {
            background: var(--color-fill-2);

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
            background: var(--color-fill-2);
          }

          .table-header-main {
            display: flex;
            align-items: center;
            gap: 6px;

            .expand-icon {
              transition: transform 0.2s;
              color: var(--color-text-2);
              font-size: 12px;
              flex-shrink: 0;
              
              &.expanded {
                transform: rotate(90deg);
              }
            }

            .table-icon {
              color: rgb(var(--primary-6));
              font-size: 14px;
              flex-shrink: 0;
            }

            .table-name {
              font-weight: 500;
              color: var(--color-text-1);
              font-size: 13px;
              flex-shrink: 0;
            }

            :deep(.arco-tag) {
              font-size: 11px;
              height: 18px;
              line-height: 16px;
              padding: 0 6px;
              flex-shrink: 0;
            }
          }

          .table-comment-row {
            color: var(--color-text-2);
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
          background: var(--color-fill-1);

          .loading-state {
            padding: 12px;
            text-align: center;
            color: var(--color-text-2);
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            font-size: 13px;

            .loading-icon {
              animation: arco-loading-circle 1s linear infinite;
            }
          }

          .empty-state {
            padding: 12px;
            text-align: center;
            color: var(--color-text-2);
            font-size: 13px;
          }

          .column-list {
            :deep(.arco-table) {
              font-size: 12px;

              .arco-table-td,
              .arco-table-th {
                padding: 4px 8px;
              }

              :deep(.arco-tag) {
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
</style>
