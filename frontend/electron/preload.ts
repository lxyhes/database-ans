import { contextBridge, ipcRenderer } from 'electron'

// 暴露给渲染进程的 API
contextBridge.exposeInMainWorld('electronAPI', {
  // 通用 API 调用
  apiCall: (method: string, endpoint: string, data?: any) => 
    ipcRenderer.invoke('api-call', { method, endpoint, data }),

  // 数据查询
  queryNaturalLanguage: (query: string) => 
    ipcRenderer.invoke('query-natural-language', query),
  analyzeData: (query: string) => 
    ipcRenderer.invoke('analyze-data', query),

  // 数据管理
  getDataSummary: () => 
    ipcRenderer.invoke('get-data-summary'),
  getTableSchema: (tableName: string) => 
    ipcRenderer.invoke('get-table-schema', tableName),
  getAllTables: () => 
    ipcRenderer.invoke('get-all-tables'),

  // 可视化
  getDashboardData: () => 
    ipcRenderer.invoke('get-dashboard-data'),

  // 查询历史
  getQueryHistory: () => 
    ipcRenderer.invoke('get-query-history'),
  saveQuery: (queryData: any) => 
    ipcRenderer.invoke('save-query', queryData),
  toggleFavorite: (id: string, isFavorite: boolean) => 
    ipcRenderer.invoke('toggle-favorite', id, isFavorite),

  // iFlow AI
  iflowQuery: (prompt: string, functionType?: string) => 
    ipcRenderer.invoke('iflow-query', prompt, functionType),

  // Qwen Code
  codeAssistant: (action: string, data: any) => 
    ipcRenderer.invoke('code-assistant', action, data),

  // 文件操作
  importFile: (formData: any) => 
    ipcRenderer.invoke('import-file', formData),
  exportData: (sql: string, format: 'excel' | 'csv') => 
    ipcRenderer.invoke('export-data', sql, format)
})

// 类型声明
declare global {
  interface Window {
    electronAPI: {
      apiCall: (method: string, endpoint: string, data?: any) => Promise<any>
      queryNaturalLanguage: (query: string) => Promise<any>
      analyzeData: (query: string) => Promise<any>
      getDataSummary: () => Promise<any>
      getTableSchema: (tableName: string) => Promise<any>
      getAllTables: () => Promise<any>
      getDashboardData: () => Promise<any>
      getQueryHistory: () => Promise<any>
      saveQuery: (queryData: any) => Promise<any>
      toggleFavorite: (id: string, isFavorite: boolean) => Promise<any>
      iflowQuery: (prompt: string, functionType?: string) => Promise<any>
      codeAssistant: (action: string, data: any) => Promise<any>
      importFile: (formData: any) => Promise<any>
      exportData: (sql: string, format: 'excel' | 'csv') => Promise<any>
    }
  }
}
