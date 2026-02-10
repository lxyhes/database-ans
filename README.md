# 数据分析助手 - 自然语言查询数据

这是一个桌面应用程序，允许用户使用自然语言查询数据。

## 技术栈

- **后端**: Java Spring Boot
- **前端**: Electron (桌面应用)
- **自然语言处理**: 阿里云通义千问 (Qwen) via DashScope SDK
- **数据库**: H2 (内存数据库)

## 功能特性

- 使用自然语言查询数据 (例如："显示总销售额", "查询客户数量")
- 桌面应用程序界面
- 实时结果显示
- 集成阿里云通义千问大模型进行智能查询解析

## 项目结构

```
data-analysis-assistant/
├── backend/                 # Java后端
│   ├── src/main/java/
│   │   └── com/data/assistant/
│   │       ├── DataAnalysisApplication.java
│   │       ├── controller/
│   │       │   └── QueryController.java
│   │       ├── service/
│   │       │   ├── NaturalLanguageProcessor.java
│   │       │   ├── QwenNaturalLanguageProcessor.java
│   │       │   └── DataQueryService.java
│   │       ├── model/
│   │       │   ├── QueryRequest.java
│   │       │   └── QueryResponse.java
│   │       └── config/
│   └── pom.xml
├── frontend/                # Electron桌面客户端
│   ├── main.js              # 主进程
│   ├── index.html           # 主界面
│   ├── styles.css           # 样式
│   ├── renderer.js          # 渲染进程
│   └── package.json
└── README.md
```

## 如何运行

### 后端 (Java Spring Boot)

1. 确保已安装Java 17+ 和 Maven
2. 进入后端目录：
```bash
cd data-analysis-assistant/backend
```
3. 编译项目：
```bash
mvn clean compile
```
4. 设置阿里云DashScope API密钥（可选，如果没有则使用本地解析逻辑）：
```bash
export DASHSCOPE_API_KEY=your_api_key_here
```
5. 运行后端服务：
```bash
mvn spring-boot:run
```
后端将在 `http://localhost:8080` 上运行。

### 前端 (Electron 桌面应用)

1. 确保已安装Node.js
2. 进入前端目录：
```bash
cd data-analysis-assistant/frontend
```
3. 安装依赖：
```bash
npm install
```
4. 运行桌面应用：
```bash
npm start
```

## 使用说明

1. 首先启动后端服务
2. 然后启动前端桌面应用
3. 在输入框中输入自然语言查询，如"显示总销售额"
4. 点击"查询"按钮查看结果

## 示例查询

### 基础查询
- "显示总销售额"
- "查询客户数量" 
- "显示产品列表"
- "平均销售额是多少"

### 数据分析查询
- "分析销售数据趋势" 
- "比较各地区销售额"
- "预测下季度销售额"
- "汇总统计客户数据"
- "分析产品销售对比"

## 阿里云通义千问集成

本项目集成了阿里云通义千问大模型，通过DashScope SDK实现更智能的自然语言到SQL查询转换：

- `QwenNaturalLanguageProcessor.java`: 使用Qwen API进行自然语言理解
- `NaturalLanguageProcessor.java`: 主要处理器，优先使用Qwen，失败时回退到本地逻辑
- 配置文件 `application.properties` 中设置了API密钥读取

## 架构说明

- `NaturalLanguageProcessor.java`: 将自然语言转换为SQL查询（主入口）
- `QwenNaturalLanguageProcessor.java`: 阿里云Qwen SDK集成实现
- `DataQueryService.java`: 执行数据查询
- `QueryController.java`: 提供REST API接口
- `renderer.js`: 处理前端与后端的通信
- `main.js`: Electron主进程，管理窗口和API调用

## API 接口

### 查询接口
- **POST** `/api/query/natural`
- **请求体**:
```json
{
  "naturalLanguageQuery": "显示总销售额"
}
```
- **响应体**:
```json
{
  "success": true,
  "message": "Query processed successfully",
  "data": [...],
  "sqlQuery": "SELECT SUM(amount) as total_sales FROM sales"
}
```

### 数据分析接口
- **POST** `/api/query/analyze`
- **请求体**:
```json
{
  "naturalLanguageQuery": "分析销售数据趋势"
}
```
- **响应体**:
```json
{
  "success": true,
  "message": "Analysis completed successfully",
  "data": [...],
  "sqlQuery": "SELECT * FROM sales"
}
```

### 健康检查接口
- **GET** `/api/query/health`
- **响应**: "Data Analysis Assistant Backend is running!"

## 部署

### 后端部署
1. 打包应用：
```bash
mvn clean package
```
2. 运行打包后的JAR文件：
```bash
java -jar target/analysis-backend-0.0.1-SNAPSHOT.jar
```

### 环境变量
- `DASHSCOPE_API_KEY`: 阿里云DashScope API密钥（可选）
- `SERVER_PORT`: 服务器端口（默认8080）

## Qwen3-Coder CLI 集成

本项目还集成了Qwen3-Coder CLI功能，提供了代码生成和优化能力：

- `Qwen3CoderService.java`: 处理与Qwen3-Coder CLI的交互
- `Qwen3CoderController.java`: 提供Qwen3-Coder相关的API接口

### 安装Qwen3-Coder CLI

要使用Qwen3-Coder功能，需要先安装CLI工具：

```bash
npm install -g @qwen-code/qwen-code
```

### Qwen3-Coder API 接口

- **GET** `/api/qwen3coder/installed` - 检查CLI是否已安装
- **POST** `/api/qwen3coder/generate?description={description}` - 生成代码
- **POST** `/api/qwen3coder/optimize?code={code}&requirements={requirements}` - 优化代码
- **POST** `/api/qwen3coder/analyze?code={code}` - 分析代码
- **POST** `/api/qwen3coder/fix?code={code}&errorDescription={errorDescription}` - 修复代码
- **POST** `/api/qwen3coder/explain?code={code}` - 解释代码
- **GET** `/api/qwen3coder/help` - 获取帮助信息
- **POST** `/api/qwen3coder/generate-async?description={description}` - 异步生成代码
- **POST** `/api/qwen3coder/optimize-async?code={code}&requirements={requirements}` - 异步优化代码
- **POST** `/api/qwen3coder/analyze-async?code={code}` - 异步分析代码
- **POST** `/api/qwen3coder/fix-async?code={code}&errorDescription={errorDescription}` - 异步修复代码
- **POST** `/api/qwen3coder/explain-async?code={code}` - 异步解释代码

### Qwen Code SDK API 接口

- **POST** `/api/qwen-code-sdk/simple-query` - 简单查询
- **POST** `/api/qwen-code-sdk/query-with-options` - 带选项的查询
- **POST** `/api/qwen-code-sdk/analyze` - 分析代码
- **POST** `/api/qwen-code-sdk/generate` - 生成代码
- **POST** `/api/qwen-code-sdk/optimize` - 优化代码
- **POST** `/api/qwen-code-sdk/fix` - 修复代码
- **POST** `/api/qwen-code-sdk/explain` - 解释代码

### Qwen Code API 接口

- **POST** `/api/qwen-code/analyze?code={code}` - 分析代码
- **POST** `/api/qwen-code/generate?description={description}` - 生成代码
- **POST** `/api/qwen-code/optimize?code={code}&requirements={requirements}` - 优化代码
- **POST** `/api/qwen-code/explain?code={code}` - 解释代码
- **POST** `/api/qwen-code/fix?code={code}&errorDescription={errorDescription}` - 修复代码
- **POST** `/api/qwen-code/analyze-async?code={code}` - 异步分析代码
- **POST** `/api/qwen-code/generate-async?description={description}` - 异步生成代码
- **POST** `/api/qwen-code/optimize-async?code={code}&requirements={requirements}` - 异步优化代码
- **POST** `/api/qwen-code/explain-async?code={code}` - 异步解释代码
- **POST** `/api/qwen-code/fix-async?code={code}&errorDescription={errorDescription}` - 异步修复代码

## 配置说明

- 应用默认运行在 `http://localhost:9090`
- H2数据库控制台可在 `http://localhost:9090/h2-console` 访问
- 当未配置API密钥时，系统会自动使用本地解析逻辑作为备选方案

## 项目架构图

```
                    +------------------+
                    |   用户界面层      |
                    |   (Electron)    |
                    +--------+---------+
                             |
                    +--------v---------+
                    |   API网关层      |
                    |  (Spring Boot)  |
                    +--------+---------+
                             |
        +---------------------+---------------------+
        |                                         |
+-------v--------+                        +--------v---------+
|  自然语言处理层  |                        |  代码辅助层     |
|  (Qwen SDK)   |                        | (Qwen3-Coder)  |
+-------+--------+                        +--------+--------+
        |                                          |
+-------v--------+                        +--------v---------+
|  数据查询层    |                        |  系统交互层     |
|   (SQL)      |                        |  (CLI调用)    |
+-------+--------+                        +--------+--------+
        |                                          |
+-------v--------+                        +--------v---------+
|  数据存储层    |                        |  外部工具层     |
|   (H2 DB)    |                        | (Qwen3-Coder CLI)|
+--------------+                        +------------------+
```

## 开发扩展

本项目具有良好的扩展性，你可以：

1. **扩展自然语言理解能力**：
   - 在`QwenNaturalLanguageProcessor.java`中增强提示工程
   - 集成更多领域特定的查询模板

2. **增强数据查询功能**：
   - 在`DataQueryService.java`中添加真实数据库连接
   - 实现更复杂的数据分析算法

3. **扩展Qwen3-Coder功能**：
   - 添加更多代码生成模板
   - 集成代码审查和测试生成功能

4. **改进前端界面**：
   - 添加数据可视化图表
   - 实现查询历史记录功能
   - 增加主题切换功能

## 贡献

欢迎提交Issue和Pull Request来改进此项目。

## Startup Scripts

The project includes two startup scripts:

- `start_app.bat` - Starts the entire application (backend on port 9090, frontend starts automatically)
- `start_app_dev.bat` - Development mode startup (explicitly specifies backend runs on port 9090)

### Usage

1. Double-click to run `start_app.bat` or `start_app_dev.bat`
2. Wait for the backend service to start completely
3. The frontend desktop application will start automatically

## 许可证

MIT License