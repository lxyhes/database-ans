# ACP 客户端 SDK (Agent Client Protocol Java SDK For Client)

ACP 客户端 SDK 是一个用于与支持代理客户端协议（ACP）的 AI 代理进行通信的 Java 软件开发工具包。它提供了一种标准化的方式来与支持代理客户端协议的 AI 代理进行交互，支持会话管理、文件系统操作、终端命令和工具调用等功能。

## 项目概述

ACP SDK 实现了代理客户端协议，允许客户端应用程序与 AI 代理进行通信。SDK 提供以下核心功能：

- 会话管理（创建、加载、管理对话）
- 文件系统操作（读写文本文件）
- 终端命令执行
- 工具调用处理和权限管理
- 支持丰富的内容类型（文本、图像、音频、资源）
- 与模型上下文协议（MCP）服务器集成

## 环境要求

- Java 8 或更高版本
- Maven 3.6.0 或更高版本

## 功能特性

- **标准化协议**：实现 ACP 协议，确保与各种 ACP 兼容代理的互操作性
- **灵活的传输层**：支持多种传输机制（stdio、HTTP 等）
- **会话管理**：完整的会话生命周期管理
- **权限控制**：细粒度的权限管理，保护敏感操作
- **内容块处理**：支持内容块中的多种数据类型
- **MCP 集成**：与外部 MCP 服务器集成，扩展工具能力

## 安装

### Maven

在您的 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>acp-sdk</artifactId>
    <version>0.0.1-alpha</version>
</dependency>
```

### Gradle

在您的 `build.gradle` 文件中添加以下内容：

```gradle
implementation 'com.alibaba:acp-sdk:0.0.1-alpha'
```

## 快速开始

以下是一个简单的示例，展示如何使用 ACP SDK 创建客户端并建立会话：

```java
@Test
public void testSession() throws AgentInitializeException, SessionNewException, IOException {
    // 使用进程传输创建 ACP 客户端
    AcpClient acpClient = new AcpClient(
            new ProcessTransport(new ProcessTransportOptions().setCommandArgs(new String[] {"qwen", "--acp", "-y"})));

    try {
        // 向代理发送提示
        acpClient.sendPrompt(Collections.singletonList(new TextContent("你是谁")),
                new AgentEventConsumer().setContentEventConsumer(new ContentEventSimpleConsumer() {
                    @Override
                    public void onAgentMessageChunkSessionUpdate(AgentMessageChunkSessionUpdate sessionUpdate) {
                        logger.info(sessionUpdate.toString());
                    }

                    @Override
                    public void onAvailableCommandsUpdateSessionUpdate(AvailableCommandsUpdateSessionUpdate sessionUpdate) {
                        logger.info(sessionUpdate.toString());
                    }

                    @Override
                    public void onCurrentModeUpdateSessionUpdate(CurrentModeUpdateSessionUpdate sessionUpdate) {
                        logger.info(sessionUpdate.toString());
                    }

                    @Override
                    public void onPlanSessionUpdate(PlanSessionUpdate sessionUpdate) {
                        logger.info(sessionUpdate.toString());
                    }

                    @Override
                    public void onToolCallUpdateSessionUpdate(ToolCallUpdateSessionUpdate sessionUpdate) {
                        logger.info(sessionUpdate.toString());
                    }

                    @Override
                    public void onToolCallSessionUpdate(ToolCallSessionUpdate sessionUpdate) {
                        logger.info(sessionUpdate.toString());
                    }
                }));
    } finally {
        // 使用完毕后关闭客户端
        acpClient.close();
    }
}
```

## 架构组件

### 核心组件

1. **AcpClient**：管理与支持 ACP 代理连接的主客户端类
2. **Session**：表示与代理的对话会话
3. **Transport**：处理底层通信协议（stdio 上的 JSON-RPC、HTTP 等）
4. **Protocol Definitions**：从 JSON 模式生成，定义所有 ACP 消息类型

### 协议结构

SDK 基于全面的 JSON 模式实现代理客户端协议，定义了：

- 代理-客户端通信的请求/响应类型
- 实时更新的通知机制
- 错误处理和功能协商
- 各种数据类型的内容块
- 工具调用定义和执行流程

## 使用场景

- 企业应用程序中的 AI 代理集成
- 自动化脚本和任务执行
- 文件系统操作自动化
- 终端命令执行和结果处理
- 与外部服务和工具的集成

## 开发

### 构建

```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 打包 JAR
mvn package

# 安装到本地仓库
mvn install
```

### 测试

项目包含全面的单元测试，涵盖：

- 从 JSON 模式生成协议消息
- 会话管理功能
- 权限处理工作流
- 内容类型处理

## 依赖项

主要依赖包括：

- SLF4J API 用于日志记录
- Apache Commons Lang3 和 IO 用于工具函数
- FastJSON2 用于 JSON 序列化/反序列化
- JUnit 5 用于测试
- Logback Classic 用于测试日志记录

## 许可证

本项目采用 Apache 2.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

## 贡献

欢迎贡献！请提交问题和拉取请求。

## 支持

如果您遇到任何问题，请通过 GitHub Issues 提交问题报告。
