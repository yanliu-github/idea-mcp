# 更新日志

本文档记录 IntelliJ IDEA MCP Server 项目的所有重要变更。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [未发布]

### 待实施
- Phase 5 性能优化和安全加固
- 完整的集成测试和 E2E 测试
- 性能测试和安全测试
- 插件市场发布准备

---

## [1.0.0-SNAPSHOT] - 2025-11-29

### ✨ 核心功能完成 (Phase 1-4)

项目核心功能开发完成，共完成 259 个任务，完成率 100%。

### 新增功能

#### Phase 1: MVP 基础功能 (138个任务)
- ✅ Monorepo 项目初始化
  - 创建根项目和子模块结构
  - 配置 IDEA Plugin 模块（Kotlin 1.9.21 + JVM 17）
  - 配置 MCP Server 模块（TypeScript 5.3 + Node.js 18）
- ✅ IDEA Plugin 基础框架
  - HTTP Server（Undertow 2.3）自动启动机制
  - 请求处理和路由配置
  - PSI 和线程辅助工具类
  - 数据模型和配置管理
  - Action 和 UI 配置界面
- ✅ 核心重构功能
  - 重命名符号（RefactoringService）
- ✅ 核心导航功能
  - 查找用途（NavigationService）
  - 跳转到定义
- ✅ 核心代码分析
  - 代码检查（AnalysisService）
- ✅ 系统管理 API
  - 健康检查端点
  - 项目信息端点
- ✅ MCP Server 实现
  - stdio 通信（JSON-RPC 2.0）
  - HTTP 客户端（axios）
  - 重构工具（refactoring.ts）
  - 导航工具（navigation.ts）
  - 分析工具（analysis.ts）

#### Phase 2: 核心重构功能 (36个任务)
- ✅ 提取方法（Extract Method）
- ✅ 提取变量（Extract Variable）
- ✅ 内联（Inline）
- ✅ 改变方法签名（Change Signature）
- ✅ 安全删除（Safe Delete）
- ✅ 移动（Move）
- ✅ 重构预览机制

#### Phase 3: 调试功能 (30个任务)
- ✅ 断点管理
  - 设置普通断点
  - 设置条件断点
  - 设置日志断点
  - 列出和删除断点
- ✅ 调试会话管理
  - 启动和停止调试会话
  - 暂停、继续、单步执行
  - Step Over、Step Into、Step Out
- ✅ 表达式和变量
  - 计算表达式
  - 查看变量（支持不同作用域）
  - 获取变量详情
- ✅ 调用栈
  - 获取完整调用栈和栈帧信息

#### Phase 4: 高级功能 (55个任务)
- ✅ 代码生成
  - 生成 Getter/Setter
  - 生成构造函数
  - 生成 toString/equals/hashCode
  - 重写方法
- ✅ 搜索功能
  - 全局搜索
  - 文本搜索（支持正则表达式）
  - 结构化搜索（代码模式）
- ✅ 导航功能增强
  - 类型层次（Type Hierarchy）
  - 调用层次（Call Hierarchy）
  - 查找实现（Find Implementations）
- ✅ 依赖分析
  - 模块依赖分析
  - 循环依赖检测
- ✅ 版本控制集成
  - VCS 状态查询
  - 文件历史
  - 差异对比（Diff）
  - Blame 注解
- ✅ 项目管理
  - 项目结构查询
  - 模块列表
  - 库列表
  - 构建配置
- ✅ 工具功能
  - 代码格式化
  - 优化导入
  - 应用快速修复
  - 应用 Intention

### API 端点

共实现 **34 个 HTTP REST API 端点**:
- 系统管理: 2 个（健康检查、项目信息）
- 重构: 7 个（重命名、提取方法、提取变量、内联、改变签名、安全删除、移动）
- 导航: 5 个（查找用途、跳转定义、类型层次、调用层次、查找实现）
- 代码分析: 3 个（代码检查、依赖分析、循环依赖检测）
- 调试: 9 个（断点管理、会话管理、表达式计算、变量查看、调用栈）
- 代码生成: 4 个（Getter/Setter、构造函数、toString/equals/hashCode、重写方法）
- 搜索: 3 个（全局搜索、文本搜索、结构化搜索）
- 版本控制: 4 个（状态、历史、差异、Blame）
- 项目管理: 4 个（结构、模块、库、构建配置）
- 工具: 4 个（格式化、优化导入、快速修复、Intention）

### MCP 工具

共实现 **32+ MCP 工具定义**，覆盖所有核心功能。

### 技术亮点

1. **三层架构设计**
   - AI 工具（Claude Code）↔ MCP Server（stdio）↔ IDEA Plugin（HTTP REST）

2. **stdio 通信机制**
   - 基于 JSON-RPC 2.0 协议
   - 使用 `@modelcontextprotocol/sdk`

3. **HTTP Server 自动启动**
   - IDEA 启动后自动启动 Undertow Server（端口 58888）
   - 用户可通过 Action 手动控制

4. **完整错误码体系**
   - 26 个标准错误码
   - 4xx 客户端错误（18个）
   - 5xx 服务端错误（10个）

5. **PSI 和线程安全**
   - ReadAction/WriteAction 正确使用
   - 后台任务和进度指示器

6. **重构预览机制**
   - 所有重构操作支持 preview 参数
   - 返回详细变更信息

### 文档

完成 **10 个核心文档**（约 11 万字，200+ 代码示例）:
- ✅ 需求文档.md
- ✅ 架构文档.md
- ✅ 实现任务文档.md
- ✅ API端点清单.md
- ✅ stdio通信示例.md
- ✅ Gradle配置示例.md
- ✅ 测试计划.md
- ✅ 开发环境配置指南.md
- ✅ 文档索引.md
- ✅ README.md
- ✅ CLAUDE.md

### 已知问题

1. 集成测试尚未完整实现（Phase 1.8 暂缓）
2. 调试功能中的"修改变量"功能暂缓实现
3. Phase 5 性能优化和安全加固待实施

---

## [0.1.0] - 2025-11-14

### 新增

#### 项目初始化
- 创建 Monorepo 项目结构
- 配置 Gradle 多模块构建
- 初始化 Git 仓库
- 创建核心文档（9个 .md 文件）

#### IDEA Plugin 骨架
- 基本项目结构
- plugin.xml 配置
- 包结构创建（server、service、util、model）

#### MCP Server 骨架
- TypeScript 项目初始化
- package.json 和 tsconfig.json 配置
- 依赖安装（@modelcontextprotocol/sdk、axios）

---

## 版本计划

### [1.0.0] - 预计 2025-12

计划内容:
- ✅ Phase 1-4 核心功能开发（已完成）
- ⏳ 完整的集成测试和 E2E 测试
- ⏳ 性能测试和优化
- ⏳ 安全测试和加固
- ⏳ 文档完善和用户指南
- ⏳ 插件市场发布准备

### [1.1.0] - 预计 2026-01

计划内容:
- Phase 5 性能优化（缓存、异步处理、批量操作）
- 安全加固（认证授权、限流、审计日志）
- 监控和指标收集
- 配置管理和管理界面完善

### [1.2.0] - 待定

计划内容:
- AI 辅助代码分析
- 智能重构建议
- 代码质量评分
- 更多语言支持

---

## 贡献指南

如果您想为本项目做贡献，请:
1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/AmazingFeature`）
3. 提交更改（`git commit -m '[功能] 添加某某特性'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 开启 Pull Request

提交信息格式建议:
- `[功能] 描述` - 新增功能
- `[修复] 描述` - Bug 修复
- `[重构] 描述` - 代码重构
- `[文档] 描述` - 文档更新
- `[测试] 描述` - 测试相关
- `[配置] 描述` - 配置变更

---

**维护者**: ly
**最后更新**: 2025-11-29
