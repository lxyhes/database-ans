const { ipcRenderer } = require('electron');
const axios = require('axios');

document.addEventListener('DOMContentLoaded', () => {
    // 数据分析界面元素
    const queryInput = document.getElementById('query-input');
    const submitBtn = document.getElementById('submit-btn');
    const analyzeBtn = document.getElementById('analyze-btn');
    const resultsContainer = document.getElementById('results-container');
    
    // 代码助手界面元素
    const codeFunctionSelect = document.getElementById('code-function-select');
    const codeInputArea = document.getElementById('code-input-area');
    const codeDescriptionArea = document.getElementById('code-description-area');
    const codeRequirementsArea = document.getElementById('code-requirements-area');
    const codeInput = document.getElementById('code-input');
    const codeDescription = document.getElementById('code-description');
    const codeRequirements = document.getElementById('code-requirements');
    const codeSubmitBtn = document.getElementById('code-submit-btn');
    const codeResultsContainer = document.getElementById('code-results-container');
    
    // 选项卡元素
    const dataTabBtn = document.getElementById('data-tab-btn');
    const codeTabBtn = document.getElementById('code-tab-btn');
    const dataInterface = document.getElementById('data-interface');
    const codeInterface = document.getElementById('code-interface');
    
    // 加载状态元素
    const loadingElement = document.getElementById('loading');
    
    // 初始设置
    setupTabs();
    setupDataInterface();
    setupCodeInterface();
    
    // 设置选项卡切换
    function setupTabs() {
        dataTabBtn.addEventListener('click', () => {
            dataTabBtn.classList.add('active');
            codeTabBtn.classList.remove('active');
            dataInterface.classList.remove('hidden');
            codeInterface.classList.add('hidden');
        });
        
        codeTabBtn.addEventListener('click', () => {
            codeTabBtn.classList.add('active');
            dataTabBtn.classList.remove('active');
            codeInterface.classList.remove('hidden');
            dataInterface.classList.add('hidden');
        });
    }
    
    // 设置数据分析界面
    function setupDataInterface() {
        // 提交查询事件
        submitBtn.addEventListener('click', handleQuery);
        // 数据分析事件
        analyzeBtn.addEventListener('click', handleAnalysis);
        
        queryInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleQuery();
            }
        });
    }
    
    // 设置代码助手界面
    function setupCodeInterface() {
        // 函数选择变化事件
        codeFunctionSelect.addEventListener('change', function() {
            const selectedFunction = this.value;
            
            // 隐藏所有输入区域
            codeInputArea.classList.add('hidden');
            codeDescriptionArea.classList.add('hidden');
            codeRequirementsArea.classList.add('hidden');
            
            // 根据选择显示相应的输入区域
            if (selectedFunction === 'analyze' || selectedFunction === 'optimize' || selectedFunction === 'explain' || selectedFunction === 'fix') {
                codeInputArea.classList.remove('hidden');
            } else {
                codeInputArea.classList.add('hidden');
            }

            if (selectedFunction === 'generate' || selectedFunction === 'acp-query') {
                codeDescriptionArea.classList.remove('hidden');
            } else {
                codeDescriptionArea.classList.add('hidden');
            }

            if (selectedFunction === 'optimize') {
                codeRequirementsArea.classList.remove('hidden');
                document.querySelector('#code-requirements-area label').textContent = '优化要求:';
                document.querySelector('#code-requirements').placeholder = '例如：提高性能，增加错误处理';
            } else if (selectedFunction === 'fix') {
                codeRequirementsArea.classList.remove('hidden');
                document.querySelector('#code-requirements-area label').textContent = '错误描述:';
                document.querySelector('#code-requirements').placeholder = '例如：语法错误，变量未定义等';
            } else {
                codeRequirementsArea.classList.add('hidden');
            }
        });
        
        // 触发初始变化事件以设置正确的界面
        codeFunctionSelect.dispatchEvent(new Event('change'));
        
        // 代码提交事件
        codeSubmitBtn.addEventListener('click', handleCodeAction);
        
        codeInput.addEventListener('keydown', (e) => {
            if (e.key === 'Tab') {
                e.preventDefault();
                const start = e.target.selectionStart;
                const end = e.target.selectionEnd;
                
                // 在光标位置插入4个空格
                e.target.value = e.target.value.substring(0, start) + '    ' + e.target.value.substring(end);
                
                // 移动光标到插入的空格后面
                e.target.selectionStart = e.target.selectionEnd = start + 4;
            }
        });
    }
    
    // 处理数据分析查询
    async function handleQuery() {
        const query = queryInput.value.trim();
        
        if (!query) {
            alert('请输入查询内容');
            return;
        }
        
        // 显示加载状态
        loadingElement.classList.remove('hidden');
        resultsContainer.innerHTML = '';
        
        try {
            // 发送查询到后端
            const response = await ipcRenderer.invoke('query-natural-language', query);
            
            // 隐藏加载状态
            loadingElement.classList.add('hidden');
            
            if (response.success) {
                displayResults(response, resultsContainer);
            } else {
                displayError(response.message, resultsContainer);
            }
        } catch (error) {
            loadingElement.classList.add('hidden');
            displayError('查询失败: ' + error.message, resultsContainer);
        }
    }
    
    // 处理数据分析
    async function handleAnalysis() {
        const query = queryInput.value.trim();
        
        if (!query) {
            alert('请输入分析内容');
            return;
        }
        
        // 显示加载状态
        loadingElement.classList.remove('hidden');
        resultsContainer.innerHTML = '';
        
        try {
            // 发送分析请求到后端
            const response = await ipcRenderer.invoke('analyze-natural-language', query);
            
            // 隐藏加载状态
            loadingElement.classList.add('hidden');
            
            if (response.success) {
                displayResults(response, resultsContainer);
            } else {
                displayError(response.message, resultsContainer);
            }
        } catch (error) {
            loadingElement.classList.add('hidden');
            displayError('分析失败: ' + error.message, resultsContainer);
        }
    }
    
    // 处理代码操作
    async function handleCodeAction() {
        const selectedFunction = codeFunctionSelect.value;
        
        let requestData = {};
        
        if (selectedFunction === 'analyze') {
            const code = codeInput.value.trim();
            if (!code) {
                alert('请输入要分析的代码');
                return;
            }
            requestData = { code };
        } else if (selectedFunction === 'generate') {
            const description = codeDescription.value.trim();
            if (!description) {
                alert('请输入代码描述');
                return;
            }
            requestData = { description };
        } else if (selectedFunction === 'optimize') {
            const code = codeInput.value.trim();
            const requirements = codeRequirements.value.trim() || '提高性能和可读性';
            
            if (!code) {
                alert('请输入要优化的代码');
                return;
            }
            
            requestData = { code, requirements };
        } else if (selectedFunction === 'explain') {
            const code = codeInput.value.trim();
            if (!code) {
                alert('请输入要解释的代码');
                return;
            }
            requestData = { code };
        } else if (selectedFunction === 'fix') {
            const code = codeInput.value.trim();
            const errorDescription = codeRequirements.value.trim();
            
            if (!code) {
                alert('请输入要修复的代码');
                return;
            }
            
            if (!errorDescription) {
                alert('请输入错误描述');
                return;
            }
            
            requestData = { code, errorDescription };
        } else if (selectedFunction === 'acp-query') {
            const description = codeDescription.value.trim();
            if (!description) {
                alert('请输入查询内容');
                return;
            }
            requestData = { description };
        }
        
        // 显示加载状态
        loadingElement.classList.remove('hidden');
        codeResultsContainer.innerHTML = '';
        
        try {
            let response;
            
            if (selectedFunction === 'analyze') {
                response = await ipcRenderer.invoke('analyze-code', requestData.code);
            } else if (selectedFunction === 'generate') {
                response = await ipcRenderer.invoke('generate-code', requestData.description);
            } else if (selectedFunction === 'optimize') {
                response = await ipcRenderer.invoke('optimize-code', requestData.code, requestData.requirements);
            } else if (selectedFunction === 'explain') {
                response = await ipcRenderer.invoke('explain-code', requestData.code);
            } else if (selectedFunction === 'fix') {
                response = await ipcRenderer.invoke('fix-code', requestData.code, requestData.errorDescription);
            } else if (selectedFunction === 'acp-query') {
                response = await ipcRenderer.invoke('acp-query', requestData.description);
            }
            
            // 隐藏加载状态
            loadingElement.classList.add('hidden');
            
            displayCodeResult(response, codeResultsContainer);
        } catch (error) {
            loadingElement.classList.add('hidden');
            displayError('代码操作失败: ' + error.message, codeResultsContainer);
        }
    }
    
    // 显示结果
    function displayResults(response, container) {
        const { data, message, sqlQuery } = response;
        
        if (!data || data.length === 0) {
            container.innerHTML = '<p class="placeholder">没有找到相关数据</p>';
            return;
        }
        
        // 显示成功消息
        const successDiv = document.createElement('div');
        successDiv.className = 'success-message';
        successDiv.textContent = message || '查询成功';
        container.appendChild(successDiv);
        
        // 如果有SQL查询信息，也显示出来
        if (sqlQuery) {
            const queryInfoDiv = document.createElement('div');
            queryInfoDiv.className = 'query-info';
            queryInfoDiv.innerHTML = `<strong>生成的SQL查询:</strong> <code>${sqlQuery}</code>`;
            container.appendChild(queryInfoDiv);
        }
        
        // 创建表格显示数据
        const table = document.createElement('table');
        table.className = 'result-table';
        
        if (data.length > 0) {
            // 创建表头
            const headerRow = document.createElement('tr');
            Object.keys(data[0]).forEach(key => {
                const th = document.createElement('th');
                th.textContent = key;
                headerRow.appendChild(th);
            });
            table.appendChild(headerRow);
            
            // 创建数据行
            data.forEach(row => {
                const tr = document.createElement('tr');
                Object.values(row).forEach(value => {
                    const td = document.createElement('td');
                    // 如果值是对象或数组，转换为字符串
                    td.textContent = typeof value === 'object' ? JSON.stringify(value) : value;
                    tr.appendChild(td);
                });
                table.appendChild(tr);
            });
        }
        
        container.appendChild(table);
    }
    
    // 显示代码结果
    function displayCodeResult(result, container) {
        if (typeof result === 'string') {
            const resultDiv = document.createElement('div');
            resultDiv.className = 'code-result';
            resultDiv.textContent = result;
            container.appendChild(resultDiv);
        } else {
            // 如果结果是对象，转换为JSON字符串显示
            const resultDiv = document.createElement('div');
            resultDiv.className = 'code-result';
            resultDiv.textContent = JSON.stringify(result, null, 2);
            container.appendChild(resultDiv);
        }
    }
    
    // 显示错误
    function displayError(message, container) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = message || '发生未知错误';
        container.appendChild(errorDiv);
    }
    
    // 添加一些示例查询提示
    const examples = [
        "显示总销售额",
        "查询客户数量", 
        "显示产品列表",
        "平均销售额是多少"
    ];
    
    const exampleContainer = document.createElement('div');
    exampleContainer.style.marginTop = '15px';
    exampleContainer.innerHTML = `
        <p><strong>示例查询:</strong></p>
        <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 5px;">
            ${examples.map(example => `
                <span class="example-query" style="
                    background: #eef2f7; 
                    padding: 5px 10px; 
                    border-radius: 15px; 
                    font-size: 0.9em; 
                    cursor: pointer;
                    border: 1px solid #cbd5e0;
                " onclick="fillExample('${example}')">${example}</span>
            `).join('')}
        </div>
    `;
    
    document.querySelector('.input-section').appendChild(exampleContainer);
    
    // 添加填充示例查询的全局函数
    window.fillExample = function(query) {
        queryInput.value = query;
    };
});