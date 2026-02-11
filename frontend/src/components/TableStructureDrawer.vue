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
        <el-button 
          type="primary" 
          @click="refreshTables"
          :loading="loading"
        >
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div class="table-list-container" v-loading="loading">
        <el-empty v-if="filteredTables.length === 0" description="暂无表数据" />
        
        <el-collapse v-else v-model="expandedTables" accordion @change="handleTableExpand">
          <el-collapse-item 
            v-for="table in filteredTables" 
            :key="table.name"
            :name="table.name"
          >
            <template #title>
              <div class="table-header-item">
                <div class="table-title">
                  <el-icon><Grid /></el-icon>
                  <span class="table-name">{{ table.name }}</span>
                  <el-tag v-if="table.rowCount !== undefined && table.rowCount !== null" size="small" type="info">
                    {{ table.rowCount }} 行
                  </el-tag>
                </div>
                <span v-if="table.comment" class="table-comment">{{ table.comment }}</span>
              </div>
            </template>
            
            <!-- 列信息表格 -->
            <div class="column-table-wrapper" v-loading="table.loadingColumns">
              <el-table 
                :data="table.columns" 
                size="small"
                border
                style="width: 100%"
              >
                <el-table-column prop="name" label="列名" min-width="120" show-overflow-tooltip />
                <el-table-column prop="type" label="数据类型" width="120" show-overflow-tooltip />
                <el-table-column prop="nullable" label="可空" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.nullable === 'YES' ? 'info' : 'success'">
                      {{ row.nullable === 'YES' ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="defaultValue" label="默认值" width="100" show-overflow-tooltip />
                <el-table-column prop="comment" label="注释" min-width="150" show-overflow-tooltip />
              </el-table>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Grid, Refresh, Search } from '@element-plus/icons-vue'
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
const expandedTables = ref([])

// 过滤后的表列表
const filteredTables = computed(() => {
  if (!searchKeyword.value) return tableList.value
  const keyword = searchKeyword.value.toLowerCase()
  return tableList.value.filter(table => 
    table.name.toLowerCase().includes(keyword) ||
    (table.comment && table.comment.toLowerCase().includes(keyword))
  )
})

// 处理抽屉打开
const handleOpen = () => {
  searchKeyword.value = ''
  expandedTables.value = []
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
        loadingColumns: false
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

// 监听展开事件加载列信息
const handleTableExpand = async (tableName) => {
  const table = tableList.value.find(t => t.name === tableName)
  if (!table || table.columns.length > 0) return
  
  table.loadingColumns = true
  try {
    const res = await getTableColumns(props.dataSourceId, tableName)
    if (res.success) {
      table.columns = res.data
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
  }

  .table-list-container {
    flex: 1;
    overflow-y: auto;

    .table-header-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding: 4px 0;

      .table-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #409eff;
        }

        .table-name {
          font-weight: 500;
          color: #303133;
        }
      }

      .table-comment {
        color: #909399;
        font-size: 12px;
        padding-left: 24px;
      }
    }

    .column-table-wrapper {
      padding: 8px 0;
    }
  }
}

:deep(.el-drawer__body) {
  padding: 20px;
}
</style>
