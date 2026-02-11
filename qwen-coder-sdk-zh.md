# Qwen Code Java SDK

Qwen Code Java SDK 是一个用于以编程方式访问 Qwen Code 功能的最小实验性 SDK。它提供了一个 Java 接口来与 Qwen Code CLI 进行交互，允许开发者将 Qwen Code 功能集成到他们的 Java 应用程序中。

## 环境要求

- Java >= 1.8
- Maven >= 3.6.0（用于从源代码构建）
- qwen-code >= 0.5.0

### 依赖项

- **日志记录**: ch.qos.logback:logback-classic
- **工具类**: org.apache.commons:commons-lang3
- **JSON 处理**: com.alibaba.fastjson2:fastjson2
- **测试**: JUnit 5 (org.junit.jupiter:junit-jupiter)

## 安装

将以下依赖添加到您的 Maven `pom.xml`：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>qwencode-sdk</artifactId>
    <version>{$version}</version>
</dependency>
```

如果使用 Gradle，请添加到您的 `build.gradle`：

```gradle
implementation 'com.alibaba:qwencode-sdk:{$version}'
```

## 构建和运行

### 构建命令

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

## 快速开始

使用 SDK 最简单的方式是通过 `QwenCodeCli.simpleQuery()` 方法：

```java
public static void runSimpleExample() {
    List<String> result = QwenCodeCli.simpleQuery("hello world");
    result.forEach(logger::info);
}
```

更高级的用法，使用自定义传输选项：

```java
public static void runTransportOptionsExample() {
    TransportOptions options = new TransportOptions()
            .setModel("qwen3-coder-flash")
            .setPermissionMode(PermissionMode.AUTO_EDIT)
            .setCwd("./")
            .setEnv(new HashMap<String, String>() {{put("CUSTOM_VAR", "value");}})
            .setIncludePartialMessages(true)
            .setTurnTimeout(new Timeout(120L, TimeUnit.SECONDS))
            .setMessageTimeout(new Timeout(90L, TimeUnit.SECONDS))
            .setAllowedTools(Arrays.asList("read_file", "write_file", "list_directory"));

    List<String> result = QwenCodeCli.simpleQuery("你是谁，你有什么能力？", options);
    result.forEach(logger::info);
}
```

使用自定义内容消费者处理流式内容：

```java
public static void runStreamingExample() {
    QwenCodeCli.simpleQuery("你是谁，你有什么能力？",
            new TransportOptions().setMessageTimeout(new Timeout(10L, TimeUnit.SECONDS)), new AssistantContentSimpleConsumers() {

                @Override
                public void onText(Session session, TextAssistantContent textAssistantContent) {
                    logger.info("收到文本内容: {}", textAssistantContent.getText());
                }

                @Override
                public void onThinking(Session session, ThingkingAssistantContent thingkingAssistantContent) {
                    logger.info("收到思考内容: {}", thingkingAssistantContent.getThinking());
                }

                @Override
                public void onToolUse(Session session, ToolUseAssistantContent toolUseContent) {
                    logger.info("收到工具使用内容: {}，参数: {}",
                            toolUseContent, toolUseContent.getInput());
                }

                @Override
                public void onToolResult(Session session, ToolResultAssistantContent toolResultContent) {
                    logger.info("收到工具结果内容: {}", toolResultContent.getContent());
                }

                @Override
                public void onOtherContent(Session session, AssistantContent<?> other) {
                    logger.info("收到其他内容: {}", other);
                }

                @Override
                public void onUsage(Session session, AssistantUsage assistantUsage) {
                    logger.info("收到使用信息: 输入 token: {}, 输出 token: {}",
                            assistantUsage.getUsage().getInputTokens(), assistantUsage.getUsage().getOutputTokens());
                }
            }.setDefaultPermissionOperation(Operation.allow));
    logger.info("流式示例完成。");
}
```

其他示例请参见 src/test/java/com/alibaba/qwen/code/cli/example

## 架构

SDK 采用分层架构：

- **API 层**: 通过 `QwenCodeCli` 类提供主要入口点，包含用于基本用途的简单静态方法
- **会话层**: 通过 `Session` 类管理与 Qwen Code CLI 的通信会话
- **传输层**: 处理 SDK 与 CLI 进程之间的通信机制（当前通过 `ProcessTransport` 使用进程传输）
- **协议层**: 基于 CLI 协议定义通信的数据结构
- **工具类**: 用于并发执行、超时处理和错误管理的通用工具

## 主要特性

### 权限模式

SDK 支持不同的权限模式来控制工具执行：

- **`default`**: 写入工具默认被拒绝，除非通过 `canUseTool` 回调或在 `allowedTools` 中获得批准。只读工具无需确认即可执行。
- **`plan`**: 阻止所有写入工具，指示 AI 先展示计划。
- **`auto-edit`**: 自动批准编辑工具（edit、write_file），其他工具需要确认。
- **`yolo`**: 所有工具自动执行，无需确认。

### 会话事件消费者和助手内容消费者

SDK 提供两个关键接口来处理来自 CLI 的事件和内容：

#### SessionEventConsumers 接口

`SessionEventConsumers` 接口为会话期间不同类型的消息提供回调：

- `onSystemMessage`: 处理来自 CLI 的系统消息（接收 Session 和 SDKSystemMessage）
- `onResultMessage`: 处理来自 CLI 的结果消息（接收 Session 和 SDKResultMessage）
- `onAssistantMessage`: 处理助手消息（AI 响应）（接收 Session 和 SDKAssistantMessage）
- `onPartialAssistantMessage`: 处理流式传输期间的部分助手消息（接收 Session 和 SDKPartialAssistantMessage）
- `onUserMessage`: 处理用户消息（接收 Session 和 SDKUserMessage）
- `onOtherMessage`: 处理其他类型的消息（接收 Session 和 String message）
- `onControlResponse`: 处理控制响应（接收 Session 和 CLIControlResponse）
- `onControlRequest`: 处理控制请求（接收 Session 和 CLIControlRequest，返回 CLIControlResponse）
- `onPermissionRequest`: 处理权限请求（接收 Session 和 CLIControlRequest<CLIControlPermissionRequest>，返回 Behavior）

#### AssistantContentConsumers 接口

`AssistantContentConsumers` 接口处理助手消息中的不同类型的内容：

- `onText`: 处理文本内容（接收 Session 和 TextAssistantContent）
- `onThinking`: 处理思考内容（接收 Session 和 ThingkingAssistantContent）
- `onToolUse`: 处理工具使用内容（接收 Session 和 ToolUseAssistantContent）
- `onToolResult`: 处理工具结果内容（接收 Session 和 ToolResultAssistantContent）
- `onOtherContent`: 处理其他内容类型（接收 Session 和 AssistantContent）
- `onUsage`: 处理使用信息（接收 Session 和 AssistantUsage）
- `onPermissionRequest`: 处理权限请求（接收 Session 和 CLIControlPermissionRequest，返回 Behavior）
- `onOtherControlRequest`: 处理其他控制请求（接收 Session 和 ControlRequestPayload，返回 ControlResponsePayload）

#### 接口之间的关系

**事件层次结构的重要说明：**

- `SessionEventConsumers` 是**高级**事件处理器，处理不同类型的消息（系统、助手、用户等）
- `AssistantContentConsumers` 是**低级**内容处理器，处理助手消息中的不同类型的内容（文本、工具、思考等）

**处理器关系：**

- `SessionEventConsumers` → `AssistantContentConsumers`（SessionEventConsumers 使用 AssistantContentConsumers 处理助手消息中的内容）

**事件派生关系：**

- `onAssistantMessage` → `onText`、`onThinking`、`onToolUse`、`onToolResult`、`onOtherContent`、`onUsage`
- `onPartialAssistantMessage` → `onText`、`onThinking`、`onToolUse`、`onToolResult`、`onOtherContent`
- `onControlRequest` → `onPermissionRequest`、`onOtherControlRequest`

**事件超时关系：**

每个事件处理器方法都有一个对应的超时方法，允许为特定事件自定义超时行为：

- `onSystemMessage` ↔ `onSystemMessageTimeout`
- `onResultMessage` ↔ `onResultMessageTimeout`
- `onAssistantMessage` ↔ `onAssistantMessageTimeout`
- `onPartialAssistantMessage` ↔ `onPartialAssistantMessageTimeout`
- `onUserMessage` ↔ `onUserMessageTimeout`
- `onOtherMessage` ↔ `onOtherMessageTimeout`
- `onControlResponse` ↔ `onControlResponseTimeout`
- `onControlRequest` ↔ `onControlRequestTimeout`

AssistantContentConsumers 的超时方法：

- `onText` ↔ `onTextTimeout`
- `onThinking` ↔ `onThinkingTimeout`
- `onToolUse` ↔ `onToolUseTimeout`
- `onToolResult` ↔ `onToolResultTimeout`
- `onOtherContent` ↔ `onOtherContentTimeout`
- `onPermissionRequest` ↔ `onPermissionRequestTimeout`
- `onOtherControlRequest` ↔ `onOtherControlRequestTimeout`

**默认超时值：**

- `SessionEventSimpleConsumers` 默认超时：180 秒（Timeout.TIMEOUT_180_SECONDS）
- `AssistantContentSimpleConsumers` 默认超时：60 秒（Timeout.TIMEOUT_60_SECONDS）

**超时层次结构要求：**

为了正确运行，应遵循以下超时关系：

- `onAssistantMessageTimeout` 返回值应大于 `onTextTimeout`、`onThinkingTimeout`、`onToolUseTimeout`、`onToolResultTimeout` 和 `onOtherContentTimeout` 返回值
- `onControlRequestTimeout` 返回值应大于 `onPermissionRequestTimeout` 和 `onOtherControlRequestTimeout` 返回值

### 传输选项

`TransportOptions` 类允许配置 SDK 与 Qwen Code CLI 的通信方式：

- `pathToQwenExecutable`: Qwen Code CLI 可执行文件的路径
- `cwd`: CLI 进程的工作目录
- `model`: 会话使用的 AI 模型
- `permissionMode`: 控制工具执行的权限模式
- `env`: 传递给 CLI 进程的环境变量
- `maxSessionTurns`: 限制会话中的对话轮数
- `coreTools`: AI 可用的核心工具列表
- `excludeTools`: 从 AI 可用工具中排除的工具列表
- `allowedTools`: 预先批准无需额外确认即可使用的工具列表
- `authType`: 会话使用的认证类型
- `includePartialMessages`: 启用接收流式响应期间的部分消息
- `turnTimeout`: 完整对话轮的超时时间
- `messageTimeout`: 一轮中单个消息的超时时间
- `resumeSessionId`: 要恢复的先前会话的 ID
- `otherOptions`: 传递给 CLI 的额外命令行选项

### 会话控制特性

- **会话创建**: 使用 `QwenCodeCli.newSession()` 创建带有自定义选项的新会话
- **会话管理**: `Session` 类提供发送提示、处理响应和管理会话状态的方法
- **会话清理**: 始终使用 `session.close()` 关闭会话，以正确终止 CLI 进程
- **会话恢复**: 在 `TransportOptions` 中使用 `setResumeSessionId()` 恢复先前的会话
- **会话中断**: 使用 `session.interrupt()` 中断当前正在运行的提示
- **动态模型切换**: 使用 `session.setModel()` 在会话期间更改模型
- **动态权限模式切换**: 使用 `session.setPermissionMode()` 在会话期间更改权限模式

### 线程池配置

SDK 使用线程池管理并发操作，具有以下默认配置：

- **核心池大小**: 30 个线程
- **最大池大小**: 100 个线程
- **保活时间**: 60 秒
- **队列容量**: 300 个任务（使用 LinkedBlockingQueue）
- **线程命名**: "qwen_code_cli-pool-{number}"
- **守护线程**: false
- **拒绝执行处理器**: CallerRunsPolicy

## 错误处理

SDK 为不同的错误场景提供特定的异常类型：

- `SessionControlException`: 会话控制出现问题时抛出（创建、初始化等）
- `SessionSendPromptException`: 发送提示或接收响应出现问题时抛出
- `SessionClosedException`: 尝试使用已关闭的会话时抛出

## 常见问题 / 故障排除

### Q: 支持哪些 Java 版本？

A: SDK 需要 Java 1.8 或更高版本。

### Q: 如何处理长时间运行的请求？

A: SDK 包含超时工具。您可以在 `TransportOptions` 中使用 `Timeout` 类配置超时。

### Q: 为什么某些工具没有执行？

A: 这可能是由于权限模式导致的。检查您的权限模式设置，并考虑使用 `allowedTools` 预先批准某些工具。

### Q: 如何恢复先前的会话？

A: 在 `TransportOptions` 中使用 `setResumeSessionId()` 方法恢复先前的会话。

### Q: 我可以自定义 CLI 进程的环境吗？

A: 是的，在 `TransportOptions` 中使用 `setEnv()` 方法将环境变量传递给 CLI 进程。

## 许可证

Apache-2.0 - 详情请参阅 [LICENSE](./LICENSE)。
