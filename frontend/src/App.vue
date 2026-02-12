<template>
  <div class="app-container">
    <!-- 后端状态提示条 -->
    <BackendStatusBar />
    
    <a-layout class="main-layout">
      <!-- 侧边栏 -->
      <a-layout-sider
        :width="240"
        :collapsed-width="64"
        collapsible
        :collapsed="collapsed"
        @collapse="handleCollapse"
        class="sidebar"
        theme="dark"
      >
        <div class="logo">
          <icon-apps size="32" :style="{ color: '#165DFF' }" />
          <span v-if="!collapsed" class="logo-text">数据分析助手</span>
        </div>
        
        <a-menu
          :selected-keys="[activeKey]"
          :open-keys="openKeys"
          @menu-item-click="handleMenuSelect"
          @sub-menu-click="handleSubMenuClick"
          theme="dark"
          :collapsed="collapsed"
        >
          <a-menu-item key="/">
            <template #icon><icon-dashboard /></template>
            数据仪表盘
          </a-menu-item>
          
          <a-sub-menu key="/data">
            <template #icon><icon-storage /></template>
            <template #title>数据管理</template>
            <a-menu-item key="/datasource">数据源管理</a-menu-item>
            <a-menu-item key="/nl2sql">智能查询 (NL2SQL)</a-menu-item>
            <a-menu-item key="/query">SQL 查询</a-menu-item>
            <a-menu-item key="/data-relation">数据关系图谱</a-menu-item>
            <a-menu-item key="/data-quality">数据质量检测</a-menu-item>
            <a-menu-item key="/sensitive-data">敏感数据管理</a-menu-item>
            <a-menu-item key="/reports">报表管理</a-menu-item>
            <a-menu-item key="/lineage">数据血缘</a-menu-item>
            <a-menu-item key="/sql-optimization">SQL优化</a-menu-item>
            <a-menu-item key="/metrics">指标库</a-menu-item>
            <a-menu-item key="/data-story">数据故事</a-menu-item>
          </a-sub-menu>
          
          <a-menu-item key="/history">
            <template #icon><icon-history /></template>
            查询历史
          </a-menu-item>
          
          <a-menu-item key="/import-export">
            <template #icon><icon-import /></template>
            导入导出
          </a-menu-item>
          
          <a-sub-menu key="/ai">
            <template #icon><icon-robot /></template>
            <template #title>AI 助手</template>
            <a-menu-item key="/ai/iflow">iFlow AI</a-menu-item>
            <a-menu-item key="/ai/code">代码助手</a-menu-item>
          </a-sub-menu>
          
          <a-menu-item key="/settings">
            <template #icon><icon-settings /></template>
            设置
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <!-- 主内容区 -->
      <a-layout>
        <a-layout-header class="header">
          <div class="header-left">
            <Breadcrumb />
          </div>
          <div class="header-right">
            <a-button type="primary" @click="checkConnection">
              <template #icon><icon-link /></template>
              连接状态
            </a-button>
          </div>
        </a-layout-header>
        
        <a-layout-content class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  IconApps,
  IconDashboard,
  IconStorage,
  IconHistory,
  IconImport,
  IconRobot,
  IconSettings,
  IconLink,
} from '@arco-design/web-vue/es/icon'
import Breadcrumb from './components/Breadcrumb.vue'
import BackendStatusBar from './components/BackendStatusBar.vue'
import { useBackendStatus } from './composables/useBackendStatus'
import { Message } from '@arco-design/web-vue'

const route = useRoute()
const router = useRouter()
const { startMonitoring, stopMonitoring, checkStatus } = useBackendStatus()

// 侧边栏收起状态
const collapsed = ref(false)
const openKeys = ref<string[]>(['/data', '/ai'])

// 当前激活的菜单
const activeKey = computed(() => route.path)

// 处理侧边栏收起/展开
const handleCollapse = (val: boolean) => {
  collapsed.value = val
}

// 处理菜单选择
const handleMenuSelect = (key: string) => {
  router.push(key)
}

// 处理子菜单点击
const handleSubMenuClick = (key: string, openKeysList: string[]) => {
  openKeys.value = openKeysList
}

// 检查连接
const checkConnection = async () => {
  await checkStatus()
  Message.success('已检测后端连接状态')
}

onMounted(() => {
  startMonitoring(30000)
})

onUnmounted(() => {
  stopMonitoring()
})
</script>

<style scoped lang="scss">
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-layout {
  flex: 1;
}

.sidebar {
  :deep(.arco-layout-sider-children) {
    background: #232324;
  }

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 20px;
    background: #232324;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    white-space: nowrap;
    overflow: hidden;

    .logo-text {
      margin-left: 12px;
      font-size: 18px;
      font-weight: 600;
      color: #fff;
      white-space: nowrap;
    }
  }

  :deep(.arco-menu) {
    background: #232324;
  }

  :deep(.arco-menu-dark) {
    background: #232324;
    
    .arco-menu-item.arco-menu-selected {
      background: #165DFF;
      color: #fff;
    }
    
    .arco-menu-inline-header.arco-menu-selected {
      background: rgba(22, 93, 255, 0.2);
      color: #165DFF;
    }
  }
}

.header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;

  .header-left {
    display: flex;
    align-items: center;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.main-content {
  background: #f2f3f5;
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
