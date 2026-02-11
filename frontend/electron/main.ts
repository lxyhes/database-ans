import { app, BrowserWindow, ipcMain } from 'electron'
import axios from 'axios'
import path from 'path'

const BACKEND_URL = 'http://localhost:9090'

let mainWindow: BrowserWindow | null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 700,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    },
    titleBarStyle: 'hiddenInset',
    show: false
  })

  // 加载页面
  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow?.show()
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})

// ==================== IPC Handlers ====================

// 通用API调用处理器
ipcMain.handle('api-call', async (event, { method, endpoint, data }: { method: string, endpoint: string, data?: any }) => {
  try {
    const url = `${BACKEND_URL}${endpoint}`
    let response
    
    switch (method.toUpperCase()) {
      case 'GET':
        response = await axios.get(url)
        break
      case 'POST':
        response = await axios.post(url, data)
        break
      case 'PUT':
        response = await axios.put(url, data)
        break
      case 'DELETE':
        response = await axios.delete(url)
        break
      default:
        throw new Error(`Unsupported method: ${method}`)
    }
    
    return { success: true, data: response.data }
  } catch (error: any) {
    console.error('API call error:', error)
    return { 
      success: false, 
      message: error.response?.data?.message || error.message 
    }
  }
})

// 数据查询相关
ipcMain.handle('query-natural-language', async (event, query: string) => {
  try {
    const response = await axios.post(`${BACKEND_URL}/api/query/natural`, {
      naturalLanguageQuery: query
    })
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

ipcMain.handle('analyze-data', async (event, query: string) => {
  try {
    const response = await axios.post(`${BACKEND_URL}/api/query/analyze`, {
      naturalLanguageQuery: query
    })
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

// 数据管理
ipcMain.handle('get-data-summary', async () => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/data/summary`)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

ipcMain.handle('get-table-schema', async (event, tableName: string) => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/data/schema/${tableName}`)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

ipcMain.handle('get-all-tables', async () => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/data/tables`)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

// 可视化数据
ipcMain.handle('get-dashboard-data', async () => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/visualization/dashboard`)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

// 查询历史
ipcMain.handle('get-query-history', async () => {
  try {
    const response = await axios.get(`${BACKEND_URL}/api/history`)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

ipcMain.handle('save-query', async (event, queryData: any) => {
  try {
    const response = await axios.post(`${BACKEND_URL}/api/history`, queryData)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

ipcMain.handle('toggle-favorite', async (event, id: string, isFavorite: boolean) => {
  try {
    const response = await axios.put(`${BACKEND_URL}/api/history/${id}/favorite`, { isFavorite })
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

// iFlow AI
ipcMain.handle('iflow-query', async (event, prompt: string, functionType: string = 'query') => {
  try {
    const endpointMap: Record<string, string> = {
      'query': '/api/iflow/query',
      'analyze': '/api/iflow/analyze',
      'generate-sql': '/api/iflow/generate-sql',
      'generate-code': '/api/iflow/generate-code',
      'explain-code': '/api/iflow/explain-code',
      'insights': '/api/iflow/insights'
    }
    
    const response = await axios.post(`${BACKEND_URL}${endpointMap[functionType] || endpointMap['query']}`, {
      naturalLanguageQuery: prompt
    })
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

// Qwen Code SDK
ipcMain.handle('code-assistant', async (event, action: string, data: any) => {
  try {
    const endpointMap: Record<string, string> = {
      'generate': '/api/qwen-code-sdk/generate',
      'analyze': '/api/qwen-code-sdk/analyze',
      'optimize': '/api/qwen-code-sdk/optimize',
      'explain': '/api/qwen-code-sdk/explain',
      'fix': '/api/qwen-code-sdk/fix'
    }
    
    const response = await axios.post(`${BACKEND_URL}${endpointMap[action]}`, data)
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

// 文件导入导出
ipcMain.handle('import-file', async (event, formData: any) => {
  try {
    const response = await axios.post(`${BACKEND_URL}/api/data/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})

ipcMain.handle('export-data', async (event, sql: string, format: 'excel' | 'csv') => {
  try {
    const response = await axios.post(`${BACKEND_URL}/api/data/export/${format}`, { sql })
    return { success: true, data: response.data }
  } catch (error: any) {
    return { success: false, message: error.message }
  }
})
