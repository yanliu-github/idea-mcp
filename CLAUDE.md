# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 项目概述

**IntelliJ IDEA MCP Server** - 通过 MCP (Model Context Protocol) 协议将 IntelliJ IDEA 的强大功能暴露给 AI 工具(如 Claude Code),实现精确的代码重构、导航和分析。

**核心价值**: 让 AI 能够调用 IDEA 的原生重构引擎和 PSI 语义分析,比纯文本解析更准确可靠。

---

## 项目架构

### 三层架构

```
AI 工具 (Claude Code)
    ↓ MCP Protocol (stdio)
MCP Server (Node.js/TypeScript)
    ↓ HTTP REST API (JSON)
IDEA Plugin (Kotlin 1.9+/JVM 17)
    ↓ IntelliJ Platform API
IntelliJ IDEA 核心 (PSI/重构引擎/调试器)
```

### 关键技术栈

- **IDEA Plugin**: Kotlin 1.9+ (JVM 17) + IntelliJ Platform SDK 2024.1 + Undertow 2.3 + Gson
- **MCP Server**: TypeScript 5.3 + Node.js 18 + @modelcontextprotocol/sdk + axios
- **项目结构**: Monorepo (单仓库多模块)
- **包名**: `com.ly.ideamcp`
- **HTTP 端口**: 58888 (默认)

### 项目结构

```
idea-mcp/
├── idea-plugin/              # IDEA Plugin (Kotlin)
│   ├── src/main/kotlin/com/ly/ideamcp/
│   │   ├── server/          # HTTP Server (Undertow)
│   │   ├── service/         # 业务服务层
│   │   │   ├── RefactoringService    # 重构
│   │   │   ├── NavigationService     # 导航
│   │   │   ├── AnalysisService       # 分析
│   │   │   └── DebugService          # 调试
│   │   ├── util/            # 工具类 (PsiHelper, ThreadHelper)
│   │   └── model/           # 数据模型
│   ├── build.gradle.kts
│   └── plugin.xml
├── mcp-server/               # MCP Server (TypeScript)
│   ├── src/
│   │   ├── index.ts         # 入口 (stdio transport)
│   │   ├── ideaClient.ts    # HTTP 客户端
│   │   └── tools/           # MCP 工具定义
│   │       ├── refactoring.ts
│   │       ├── navigation.ts
│   │       └── analysis.ts
│   ├── package.json
│   └── tsconfig.json
├── settings.gradle.kts       # Gradle 多模块配置
├── build.gradle.kts          # 根构建脚本
└── 文档/                     # 中文文档 (9个 .md 文件)
```

---

## 开发命令

### IDEA Plugin (Kotlin)

```bash
# 编译项目
./gradlew build

# 运行 IDEA 沙箱 (启动新 IDEA 实例测试插件)
./gradlew runIde

# 运行带远程调试的 IDEA (端口 5005)
./gradlew runWithDebug

# 运行测试
./gradlew test

# 构建插件 ZIP (用于分发)
./gradlew buildPlugin

# 验证插件配置
./gradlew verifyPlugin

# 查看配置信息
./gradlew showConfig
```

### MCP Server (TypeScript)

```bash
cd mcp-server

# 安装依赖
npm install

# 编译 TypeScript
npm run build

# 开发模式 (编译 + 运行)
npm run dev

# 监听模式 (自动重新编译)
npm run watch

# 运行测试
npm test
```

### 测试

```bash
# IDEA Plugin 单元测试
./gradlew test

# 特定测试类
./gradlew test --tests "RefactoringServiceTest"

# 集成测试
./gradlew integrationTest

# 生成覆盖率报告
./gradlew test jacocoTestReport
# 报告位置: build/reports/jacoco/test/html/index.html
```

---

## 核心设计模式

### 1. HTTP Server 启动策略

**自动启动**: IDEA 启动后自动启动 HTTP Server (端口 58888)

**实现**: 使用 `StartupActivity` 在项目打开时启动 Undertow Server

**用户控制**:
- Tools > IDEA MCP > Stop/Restart MCP Server
- Settings > Tools > IDEA MCP Server > Enable auto-start

### 2. stdio 通信机制

**MCP Server 使用 stdio (标准输入输出) 与 AI 工具通信**:
- **stdin**: 接收 JSON-RPC 2.0 请求
- **stdout**: 返回 JSON-RPC 2.0 响应
- **stderr**: 输出日志 (不干扰通信)

**关键点**:
- 每条消息占一行,以 `\n` 结尾
- 使用 `@modelcontextprotocol/sdk` 的 `StdioServerTransport`
- 日志必须输出到 stderr (`console.error`)

### 3. 线程模型 (IDEA Plugin)

**IDEA 严格的线程要求**:
```kotlin
// 读操作
ApplicationManager.getApplication().runReadAction<Unit> {
    // PSI 读取代码
}

// 写操作
ApplicationManager.getApplication().runWriteAction<Unit> {
    // PSI 修改代码
}

// 后台任务
ProgressManager.getInstance().run(
    object : Task.Backgroundable(project, "Task Name", true) {
        override fun run(indicator: ProgressIndicator) {
            // 长时间操作
        }
    }
)
```

### 4. API 端点设计

**34 个 RESTful API 端点**:
- **Base URL**: `http://localhost:58888/api/v1`
- **格式**: JSON
- **认证**: Bearer Token (可选)

**主要端点**:
- `POST /refactor/rename` - 重命名符号
- `POST /navigation/find-usages` - 查找用途
- `POST /analysis/inspections` - 代码检查
- `POST /debug/breakpoint` - 设置断点
- `GET /health` - 健康检查

**响应格式**:
```json
{
  "success": true,
  "data": { ... },
  "requestId": "uuid",
  "timestamp": 1234567890
}
```

**错误格式**:
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": { ... }
  }
}
```

### 5. 错误码体系

**26 个标准错误码**:
- **4xx 客户端错误** (18个): FILE_NOT_FOUND, INVALID_OFFSET, NAMING_CONFLICT 等
- **5xx 服务端错误** (10个): INDEX_NOT_READY, REFACTORING_FAILED, TIMEOUT 等

---

## 任务管理

### 任务状态标识

项目使用 `实现任务文档.md` 跟踪所有开发任务:
- `[ ]` 未开始
- `[x]` 已完成
- `[~]` 进行中
- `[-]` 已取消/暂停

**重要规则**: 每完成一项任务,立即更新 `实现任务文档.md` 中的状态标识

### Phase 执行优先级

- **Phase 1 (MVP)**: 必须完成 - 基础功能 (重命名、查找用途、代码检查)
- **Phase 2-4**: 必须完成 - 完整功能 (所有重构、调试、代码生成)
- **Phase 5**: 可选 - 性能优化和安全加固

### 关键依赖关系

**串行依赖** (必须按顺序):
1. 项目初始化 → 基础框架 → 核心功能
2. HTTP Server → 所有功能实现
3. 工具类 (PsiHelper/ThreadHelper) → 所有服务实现
4. IDEA Plugin 实现 → MCP Server 实现

**可并行**:
- RefactoringService, NavigationService, AnalysisService 可并行开发
- Phase 2-4 各模块可并行开发

---

## 测试策略

### 覆盖率目标

- **单元测试**: ≥ 70% (核心业务逻辑 ≥ 80%)
- **集成测试**: ≥ 50% (主要 API 端点 100%)

### 测试框架

- **Kotlin**: JUnit 5 + Mockito + AssertJ
- **TypeScript**: Jest

### 测试执行时机

- **Phase 1**: 单元测试立即编写,集成测试 Phase 1 完成后统一编写
- **Phase 2-4**: 单元测试 + 集成测试立即编写
- **Phase 5**: 完整测试套件 + 性能测试 + 安全测试

### 测试命名规范

**Kotlin 格式**: 使用反引号包裹的描述性名称

示例:
- ✅ `fun \`renameSymbol should succeed when valid input\`()`
- ✅ `fun \`findUsages should return empty when symbol not used\`()`

---

## 重要约定

### 1. 文档使用中文

所有项目文档使用中文命名和内容 (需求文档.md, 架构文档.md 等)。

### 2. 包名规范

统一使用包名: `com.ly.ideamcp`

### 3. 代码风格

- **Kotlin**: Kotlin Coding Conventions (官方风格)
- **TypeScript**: Prettier + ESLint

### 4. 文档更新

**任何架构/API 变更都必须同步更新相应文档**:
- 功能变更 → 需求文档.md + 实现任务文档.md
- 架构变更 → 架构文档.md
- API 变更 → API端点清单.md
- 完成任务 → 实现任务文档.md (标记为 `[x]`)

### 5. Git 提交规范

使用 `实现任务文档.md` 中的任务编号:
```
[1.3.1] 实现 RefactoringService.renameSymbol() 方法

- 添加重命名功能实现
- 添加单元测试
- 更新任务文档状态
```

---

## 快速参考

### 关键文档位置

所有文档位于项目根目录,使用中文命名:
- **文档索引.md** - 所有文档导航 (从这里开始)
- **需求文档.md** - 功能需求和错误码定义
- **架构文档.md** - 系统架构和技术设计
- **实现任务文档.md** - 任务清单和进度跟踪
- **API端点清单.md** - 34个API端点详细定义
- **stdio通信示例.md** - MCP Server 完整实现示例
- **Gradle配置示例.md** - 可直接使用的配置文件
- **测试计划.md** - 完整测试策略
- **开发环境配置指南.md** - 环境搭建步骤

### 常用调试端口

- **IDEA Plugin HTTP Server**: 58888
- **IDEA 远程调试**: 5005

### 健康检查

```bash
# 检查 IDEA Plugin 是否运行
curl http://localhost:58888/api/v1/health

# 预期响应
{
  "success": true,
  "data": {
    "status": "ok",
    "ideaVersion": "2024.1.1",
    "indexReady": true
  }
}
```

---

## 新开发者入门流程

1. 阅读 **文档索引.md** (了解文档结构)
2. 阅读 **需求文档.md** (了解项目目标)
3. 阅读 **架构文档.md** (理解系统架构)
4. 按照 **开发环境配置指南.md** 搭建环境
5. 参考 **Gradle配置示例.md** 初始化项目
6. 查看 **实现任务文档.md** 认领任务
7. 开发时参考 **API端点清单.md** 和 **stdio通信示例.md**

预计时间: 1-2 天

---

## 注意事项

### IDEA Plugin 开发

1. **等待索引就绪**: 很多操作需要等待 IDEA 完成项目索引
2. **PSI 操作**: 必须在正确的线程中执行 (ReadAction/WriteAction)
3. **空值检查**: PSI 元素可能为 null,必须检查
4. **性能考虑**: 避免在 UI 线程执行耗时操作

### MCP Server 开发

1. **stdio 规范**: stdout 仅用于 JSON-RPC 消息,日志输出到 stderr
2. **错误处理**: 所有工具执行失败必须返回 MCP 错误格式
3. **连接重试**: HTTP 客户端应实现重试机制
4. **超时设置**: HTTP 请求设置合理超时 (默认 30s)

### 测试

1. **隔离性**: 单元测试必须独立,不依赖外部状态
2. **Mock 使用**: 使用 Mockito mock IDEA API 和 PSI 对象
3. **集成测试**: 需要启动 HTTP Server 和 IDEA 测试环境

---

## 参考资源

### 官方文档

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [MCP 协议规范](https://modelcontextprotocol.io/)
- [Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html)

### 工具

- [MCP Inspector](https://github.com/modelcontextprotocol/inspector) - MCP 调试工具
- [Postman](https://www.postman.com/) - API 测试

---

**最后更新**: 2025-11-13
**项目状态**: 文档定义阶段,准备开始开发
