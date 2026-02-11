const { app, BrowserWindow, ipcMain } = require('electron');
const axios = require('axios');
const path = require('path');

// 后端 API 地址
const BACKEND_URL = 'http://localhost:9090';

let mainWindow;

// 热重载支持
try {
    const reloader = require('electron-reloader');
    reloader(module, {
        debug: true,
        watchRenderer: true
    });
} catch (err) {
    console.log('Hot reload not available in production');
}

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            enableRemoteModule: true
        }
    });

    mainWindow.loadFile('index.html');

    // 开发模式下打开开发者工具
    if (process.argv.includes('--dev') || process.env.NODE_ENV === 'development') {
        mainWindow.webContents.openDevTools();
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
        createWindow();
    }
});

// ==================== 数据分析相关 IPC 处理器 ====================

// 自然语言查询
ipcMain.handle('query-natural-language', async (event, query) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/query/natural`, {
            naturalLanguageQuery: query
        });
        return response.data;
    } catch (error) {
        console.error('Query error:', error);
        return { success: false, message: '查询失败: ' + error.message };
    }
});

// 数据分析
ipcMain.handle('analyze-natural-language', async (event, query) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/query/analyze`, {
            naturalLanguageQuery: query
        });
        return response.data;
    } catch (error) {
        console.error('Analysis error:', error);
        return { success: false, message: '分析失败: ' + error.message };
    }
});

// ==================== iFlow 相关 IPC 处理器 ====================

// iFlow 简单查询
ipcMain.handle('iflow-query', async (event, prompt) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/query`, {
            naturalLanguageQuery: prompt
        });
        return response.data;
    } catch (error) {
        console.error('iFlow query error:', error);
        return { success: false, message: 'iFlow 查询失败: ' + error.message };
    }
});

// iFlow 异步查询
ipcMain.handle('iflow-query-async', async (event, prompt) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/query-async`, {
            naturalLanguageQuery: prompt
        });
        return response.data;
    } catch (error) {
        console.error('iFlow async query error:', error);
        return { success: false, message: 'iFlow 异步查询失败: ' + error.message };
    }
});

// iFlow 数据分析
ipcMain.handle('iflow-analyze', async (event, dataContext, question) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/analyze`, {
            dataContext: dataContext,
            question: question
        });
        return response.data;
    } catch (error) {
        console.error('iFlow analysis error:', error);
        return { success: false, message: 'iFlow 分析失败: ' + error.message };
    }
});

// iFlow SQL 生成
ipcMain.handle('iflow-generate-sql', async (event, schema, naturalLanguageQuery) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/generate-sql`, {
            schema: schema,
            naturalLanguageQuery: naturalLanguageQuery
        });
        return response.data;
    } catch (error) {
        console.error('iFlow SQL generation error:', error);
        return { success: false, message: 'SQL 生成失败: ' + error.message };
    }
});

// iFlow 代码生成
ipcMain.handle('iflow-generate-code', async (event, description, language) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/generate-code`, {
            description: description,
            language: language
        });
        return response.data;
    } catch (error) {
        console.error('iFlow code generation error:', error);
        return { success: false, message: '代码生成失败: ' + error.message };
    }
});

// iFlow 代码解释
ipcMain.handle('iflow-explain-code', async (event, code) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/explain-code`, {
            code: code
        });
        return response.data;
    } catch (error) {
        console.error('iFlow code explanation error:', error);
        return { success: false, message: '代码解释失败: ' + error.message };
    }
});

// iFlow 数据洞察
ipcMain.handle('iflow-insights', async (event, dataSummary) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/iflow/insights`, {
            dataSummary: dataSummary
        });
        return response.data;
    } catch (error) {
        console.error('iFlow insights error:', error);
        return { success: false, message: '数据洞察失败: ' + error.message };
    }
});

// ==================== 代码助手相关 IPC 处理器 ====================

// 分析代码
ipcMain.handle('analyze-code', async (event, code) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/qwen-code-sdk/analyze`, {
            code: code
        });
        return response.data;
    } catch (error) {
        console.error('Code analysis error:', error);
        return { success: false, message: '代码分析失败: ' + error.message };
    }
});

// 生成代码
ipcMain.handle('generate-code', async (event, description) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/qwen-code-sdk/generate`, {
            description: description
        });
        return response.data;
    } catch (error) {
        console.error('Code generation error:', error);
        return { success: false, message: '代码生成失败: ' + error.message };
    }
});

// 优化代码
ipcMain.handle('optimize-code', async (event, code, requirements) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/qwen-code-sdk/optimize`, {
            code: code,
            requirements: requirements
        });
        return response.data;
    } catch (error) {
        console.error('Code optimization error:', error);
        return { success: false, message: '代码优化失败: ' + error.message };
    }
});

// 解释代码
ipcMain.handle('explain-code', async (event, code) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/qwen-code-sdk/explain`, {
            code: code
        });
        return response.data;
    } catch (error) {
        console.error('Code explanation error:', error);
        return { success: false, message: '代码解释失败: ' + error.message };
    }
});

// 修复代码
ipcMain.handle('fix-code', async (event, code, errorDescription) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/qwen-code-sdk/fix`, {
            code: code,
            errorDescription: errorDescription
        });
        return response.data;
    } catch (error) {
        console.error('Code fix error:', error);
        return { success: false, message: '代码修复失败: ' + error.message };
    }
});

// ACP 代理查询
ipcMain.handle('acp-query', async (event, description) => {
    try {
        const response = await axios.post(`${BACKEND_URL}/api/acp/query`, {
            naturalLanguageQuery: description
        });
        return response.data;
    } catch (error) {
        console.error('ACP query error:', error);
        return { success: false, message: 'ACP 查询失败: ' + error.message };
    }
});

// ==================== 数据管理相关 IPC 处理器 ====================

// 获取数据摘要
ipcMain.handle('get-data-summary', async () => {
    try {
        const response = await axios.get(`${BACKEND_URL}/api/data/summary`);
        return response.data;
    } catch (error) {
        console.error('Get data summary error:', error);
        return { success: false, message: '获取数据摘要失败: ' + error.message };
    }
});

// 获取表结构
ipcMain.handle('get-table-schema', async (event, tableName) => {
    try {
        const response = await axios.get(`${BACKEND_URL}/api/data/schema/${tableName}`);
        return response.data;
    } catch (error) {
        console.error('Get table schema error:', error);
        return { success: false, message: '获取表结构失败: ' + error.message };
    }
});