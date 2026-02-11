"use strict";
const electron = require("electron");
electron.contextBridge.exposeInMainWorld("electronAPI", {
  // 通用 API 调用
  apiCall: (method, endpoint, data) => electron.ipcRenderer.invoke("api-call", { method, endpoint, data }),
  // 数据查询
  queryNaturalLanguage: (query) => electron.ipcRenderer.invoke("query-natural-language", query),
  analyzeData: (query) => electron.ipcRenderer.invoke("analyze-data", query),
  // 数据管理
  getDataSummary: () => electron.ipcRenderer.invoke("get-data-summary"),
  getTableSchema: (tableName) => electron.ipcRenderer.invoke("get-table-schema", tableName),
  getAllTables: () => electron.ipcRenderer.invoke("get-all-tables"),
  // 可视化
  getDashboardData: () => electron.ipcRenderer.invoke("get-dashboard-data"),
  // 查询历史
  getQueryHistory: () => electron.ipcRenderer.invoke("get-query-history"),
  saveQuery: (queryData) => electron.ipcRenderer.invoke("save-query", queryData),
  toggleFavorite: (id, isFavorite) => electron.ipcRenderer.invoke("toggle-favorite", id, isFavorite),
  // iFlow AI
  iflowQuery: (prompt, functionType) => electron.ipcRenderer.invoke("iflow-query", prompt, functionType),
  // Qwen Code
  codeAssistant: (action, data) => electron.ipcRenderer.invoke("code-assistant", action, data),
  // 文件操作
  importFile: (formData) => electron.ipcRenderer.invoke("import-file", formData),
  exportData: (sql, format) => electron.ipcRenderer.invoke("export-data", sql, format)
});
//# sourceMappingURL=preload.js.map
