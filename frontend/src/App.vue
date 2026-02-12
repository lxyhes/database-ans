<template>
  <div class="app-container">
    <!-- 后端状态提示条 -->
    <BackendStatusBar />
    
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <el-icon size="32" color="#409EFF"><DataAnalysis /></el-icon>
          <span class="logo-text">数据分析助手</span>
        </div>
        
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/">
            <el-icon><Odometer /></el-icon>
            <span>数据仪表盘</span>
          </el-menu-item>

          <el-sub-menu index="/data">
            <template #title>
              <el-icon><Coin /></el-icon>
              <span>数据管理</span>
            </template>
            <el-menu-item index="/datasource">
              <el-icon><DataLine /></el-icon>
              <span>数据源管理</span>
            </el-menu-item>
            <el-menu-item index="/nl2sql">
              <el-icon><ChatDotRound /></el-icon>
              <span>智能查询 (NL2SQL)</span>
            </el-menu-item>
            <el-menu-item index="/query">
              <el-icon><Search /></el-icon>
              <span>SQL 查询</span>
            </el-menu-item>
            <el-menu-item index="/data-relation">
              <el-icon><Share /></el-icon>
              <span>数据关系图谱</span>
            </el-menu-item>
            <el-menu-item index="/data-quality">
              <el-icon><CircleCheck /></el-icon>
              <span>数据质量检测</span>
            </el-menu-item>
            <el-menu-item index="/sensitive-data">
              <el-icon><Lock /></el-icon>
              <span>敏感数据管理</span>
            </el-menu-item>
            <el-menu-item index="/reports">
              <el-icon><Document /></el-icon>
              <span>报表管理</span>
            </el-menu-item>
            <el-menu-item index="/lineage">
              <el-icon><Share /></el-icon>
              <span>数据血缘</span>
            </el-menu-item>
            <el-menu-item index="/sql-optimization">
              <el-icon><MagicStick /></el-icon>
              <span>SQL优化</span>
            </el-menu-item>
            <el-menu-item index="/metrics">
              <el-icon><TrendCharts /></el-icon>
              <span>指标库</span>
            </el-menu-item>
            <el-menu-item index="/data-story">
              <el-icon><Reading /></el-icon>
              <span>数据故事</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon>
            <span>查询历史</span>
          </el-menu-item>

          <el-menu-item index="/import-export">
            <el-icon><Upload /></el-icon>
            <span>导入导出</span>
          </el-menu-item>

          <el-sub-menu index="/ai">
            <template #title>
              <el-icon><MagicStick /></el-icon>
              <span>AI 助手</span>
            </template>
            <el-menu-item index="/ai/iflow">
              <el-icon><ChatDotRound /></el-icon>
              <span>iFlow AI</span>
            </el-menu-item>
            <el-menu-item index="/ai/code">
              <el-icon><Code /></el-icon>
              <span>代码助手</span>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <el-header class="header">
          <div class="header-left">
            <breadcrumb />
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Connection" @click="checkConnection">
              连接状态
            </el-button>
          </div>
        </el-header>
        
        <el-main class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { Connection, DataLine, Coin, Share, CircleCheck, Lock, Document, MagicStick, TrendCharts, Reading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Breadcrumb from './components/Breadcrumb.vue'
import BackendStatusBar from './components/BackendStatusBar.vue'
import { useBackendStatus } from './composables/useBackendStatus'

const { startMonitoring, stopMonitoring, checkStatus } = useBackendStatus()

onMounted(() => {
  startMonitoring(30000)
})

onUnmounted(() => {
  stopMonitoring()
})

const checkConnection = async () => {
  await checkStatus()
  ElMessage.success('已检测后端连接状态')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-bottom: 1px solid #1f2d3d;
}

.logo-text {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.sidebar-menu {
  border-right: none;
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
