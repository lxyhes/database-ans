const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const axios = require('axios');

// 创建窗口函数
function createWindow () {
  const mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      enableRemoteModule: true
    }
  });

  mainWindow.loadFile('index.html');
  
  // 打开开发者工具（仅在开发模式下）
  if (process.argv.includes('--dev')) {
    mainWindow.webContents.openDevTools();
  }
}

// 当Electron完成初始化时调用
app.whenReady().then(() => {
  createWindow();

  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

// 当所有窗口都关闭时退出应用
app.on('window-all-closed', function () {
  if (process.platform !== 'darwin') app.quit();
});

// 处理来自渲染进程的自然语言查询请求
ipcMain.handle('query-natural-language', async (event, query) => {
  try {
    const response = await axios.post('http://localhost:9090/api/query/natural', {
      naturalLanguageQuery: query
    });
    return response.data;
  } catch (error) {
    console.error('Error querying backend:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的数据分析请求
ipcMain.handle('analyze-natural-language', async (event, query) => {
  try {
    const response = await axios.post('http://localhost:9090/api/query/analyze', {
      naturalLanguageQuery: query
    });
    return response.data;
  } catch (error) {
    console.error('Error analyzing data:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的代码分析请求
ipcMain.handle('analyze-code', async (event, code) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/analyze', {
      code: code
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error analyzing code:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的代码生成请求
ipcMain.handle('generate-code', async (event, description) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/generate', {
      description: description
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error generating code:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的代码优化请求
ipcMain.handle('optimize-code', async (event, code, requirements) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/optimize', {
      code: code,
      requirements: requirements
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error optimizing code:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的代码解释请求
ipcMain.handle('explain-code', async (event, code) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/explain', {
      code: code
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error explaining code:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的代码修复请求
ipcMain.handle('fix-code', async (event, code, errorDescription) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/fix', {
      code: code,
      errorDescription: errorDescription
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error fixing code:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的 ACP 代理查询请求
ipcMain.handle('acp-query', async (event, query) => {
  try {
    const response = await axios.post('http://localhost:9090/api/acp/query', {
      naturalLanguageQuery: query
    });
    return response.data;
  } catch (error) {
    console.error('Error querying ACP agent:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的异步代码分析请求
ipcMain.handle('analyze-code-async', async (event, code) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/analyze-async', {
      code: code
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error analyzing code asynchronously:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的异步代码生成请求
ipcMain.handle('generate-code-async', async (event, description) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/generate-async', {
      description: description
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error generating code asynchronously:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的异步代码优化请求
ipcMain.handle('optimize-code-async', async (event, code, requirements) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/optimize-async', {
      code: code,
      requirements: requirements
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error optimizing code asynchronously:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的异步代码解释请求
ipcMain.handle('explain-code-async', async (event, code) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/explain-async', {
      code: code
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error explaining code asynchronously:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});

// 处理来自渲染进程的异步代码修复请求
ipcMain.handle('fix-code-async', async (event, code, errorDescription) => {
  try {
    const response = await axios.post('http://localhost:9090/api/qwen-code/fix-async', {
      code: code,
      errorDescription: errorDescription
    });
    return response.data || response; // Qwen Code接口可能返回纯文本
  } catch (error) {
    console.error('Error fixing code asynchronously:', error);
    return {
      success: false,
      message: error.response?.data?.message || 'Failed to connect to backend'
    };
  }
});