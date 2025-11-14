# IntelliJ IDEA MCP Server

通过 MCP (Model Context Protocol) 协议将 IntelliJ IDEA 的强大功能暴露给 AI 工具（如 Claude Code），实现精确的代码重构、导航和分析。

## 核心价值

让 AI 能够调用 IDEA 的原生重构引擎和 PSI 语义分析，比纯文本解析更准确可靠。

## 项目架构

```
AI 工具 (Claude Code)
    ↓ MCP Protocol (stdio)
MCP Server (Node.js/TypeScript)
    ↓ HTTP REST API (JSON)
IDEA Plugin (Kotlin 1.9+/JVM 17)
    ↓ IntelliJ Platform API
IntelliJ IDEA 核心 (PSI/重构引擎/调试器)
```

## 功能特性

### Phase 1 (MVP) - 核心功能
- **重构**: 重命名符号
- **导航**: 查找用途、跳转到定义
- **分析**: 代码检查、错误检测

### Phase 2 - 完整重构
- 提取方法、提取变量、内联
- 改变方法签名、安全删除、移动

### Phase 3 - 调试功能
- 断点管理、调试会话控制
- 表达式计算、变量查看

### Phase 4 - 高级功能
- 代码生成（Getter/Setter、构造函数、测试）
- 全局搜索、结构化搜索
- 类型层次、调用层次
- 版本控制集成、项目管理

## 项目结构

```
idea-mcp/
├── idea-plugin/              # IDEA Plugin (Kotlin)
│   ├── src/main/kotlin/com/ly/ideamcp/
│   │   ├── server/          # HTTP Server (Undertow)
│   │   ├── service/         # 业务服务层
│   │   ├── util/            # 工具类
│   │   └── model/           # 数据模型
│   ├── build.gradle.kts
│   └── plugin.xml
├── mcp-server/               # MCP Server (TypeScript)
│   ├── src/
│   │   ├── index.ts         # 入口 (stdio transport)
│   │   ├── ideaClient.ts    # HTTP 客户端
│   │   └── tools/           # MCP 工具定义
│   ├── package.json
│   └── tsconfig.json
├── docs/                     # 文档目录
│   ├── 需求文档.md
│   ├── 架构文档.md
│   ├── 实现任务文档.md
│   └── ...
├── settings.gradle.kts       # Gradle 多模块配置
├── build.gradle.kts          # 根构建脚本
└── README.md                 # 本文件
```

## 快速开始

### 前置要求

- **JDK**: 17 或更高版本
- **IntelliJ IDEA**: 2024.1 或更高版本
- **Node.js**: 18 或更高版本
- **Gradle**: 8.5 或更高版本（使用 Gradle Wrapper）

### 1. 克隆项目

```bash
git clone <repository-url>
cd idea-mcp
```

### 2. 构建 IDEA Plugin

```bash
# 编译项目
./gradlew build

# 运行 IDEA 沙箱 (启动新 IDEA 实例测试插件)
./gradlew runIde

# 运行带远程调试的 IDEA (端口 5005)
./gradlew runWithDebug

# 构建插件 ZIP (用于分发)
./gradlew buildPlugin
```

### 3. 构建 MCP Server

```bash
cd mcp-server

# 安装依赖
npm install

# 编译 TypeScript
npm run build

# 开发模式
npm run dev
```

### 4. 配置 Claude Code

在 Claude Code 配置文件中添加：

```json
{
  "mcpServers": {
    "idea-mcp": {
      "command": "node",
      "args": ["<project-path>/mcp-server/dist/index.js"],
      "env": {
        "IDEA_API_URL": "http://localhost:58888"
      }
    }
  }
}
```

## 开发指南

### IDEA Plugin 开发

```bash
# 运行测试
./gradlew test

# 查看配置
./gradlew showConfig

# 验证插件
./gradlew verifyPlugin
```

### MCP Server 开发

```bash
# 监听模式（自动重新编译）
npm run watch

# 运行测试
npm test

# 代码格式化
npm run format
```

## 文档

- [需求文档](./docs/需求文档.md) - 功能需求和错误码定义
- [架构文档](./docs/架构文档.md) - 系统架构和技术设计
- [实现任务文档](./docs/实现任务文档.md) - 任务清单和进度跟踪
- [API端点清单](./docs/API端点清单.md) - 34个API端点详细定义
- [开发环境配置指南](./docs/开发环境配置指南.md) - 环境搭建步骤
- [文档索引](./docs/文档索引.md) - 所有文档导航

## 技术栈

### IDEA Plugin
- **语言**: Kotlin 1.9.21
- **JVM**: 17
- **框架**: IntelliJ Platform SDK 2024.1
- **HTTP Server**: Undertow 2.3.10
- **JSON**: Gson 2.10.1

### MCP Server
- **语言**: TypeScript 5.3
- **运行时**: Node.js 18+
- **框架**: @modelcontextprotocol/sdk 0.5
- **HTTP 客户端**: axios 1.6

## 许可证

MIT

## 贡献

欢迎贡献！请阅读 [开发环境配置指南](./docs/开发环境配置指南.md) 了解如何开始开发。

## 联系方式

- **作者**: ly
- **邮箱**: support@example.com
- **问题反馈**: [GitHub Issues](<repository-url>/issues)

---

**项目状态**: 开发中

**当前版本**: 1.0.0-SNAPSHOT

**最后更新**: 2025-11-14
