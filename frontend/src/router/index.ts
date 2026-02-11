import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '数据仪表盘' }
  },
  {
    path: '/nl2sql',
    name: 'NL2SQL',
    component: () => import('../views/NL2SQL.vue'),
    meta: { title: '智能查询 (NL2SQL)' }
  },
  {
    path: '/datasource',
    name: 'DataSource',
    component: () => import('../views/DataSourceManage.vue'),
    meta: { title: '数据源管理' }
  },
  {
    path: '/query',
    name: 'Query',
    component: () => import('../views/Query.vue'),
    meta: { title: 'SQL 查询' }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/History.vue'),
    meta: { title: '查询历史' }
  },
  {
    path: '/import-export',
    name: 'ImportExport',
    component: () => import('../views/ImportExport.vue'),
    meta: { title: '导入导出' }
  },
  {
    path: '/ai/iflow',
    name: 'iFlowAI',
    component: () => import('../views/iFlowAI.vue'),
    meta: { title: 'iFlow AI' }
  },
  {
    path: '/ai/code',
    name: 'CodeAssistant',
    component: () => import('../views/CodeAssistant.vue'),
    meta: { title: '代码助手' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: { title: '设置' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '页面'} - 数据分析助手`
  next()
})

export default router
