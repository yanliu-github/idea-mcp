# 在 IntelliJ IDEA 中打开项目 - 简化指南

由于 Gradle Wrapper 配置问题，最简单的方法是**直接在 IDEA 中打开项目**，IDEA 会自动处理一切。

## 📖 步骤

### 1. 配置 IDEA 使用 JDK 17

打开 IntelliJ IDEA：
1. **File** → **Project Structure** → **Project Settings** → **Project**
2. 设置 **SDK**: JDK 17 (`D:\software\devTools\sdk\versions\jdk_versions\jdk-17.0.17.0_11`)
3. 设置 **Language Level**: 17
4. 点击 **OK**

### 2. 打开项目

1. **File** → **Open...**
2. 选择 `E:\Idea-mcp` 目录
3. 点击 **OK**

IDEA 会自动：
- 检测到这是一个 Gradle 项目
- 提示"Gradle wrapper files not found"
- 询问是否创建 Gradle wrapper
- **点击 "Create Gradle wrapper"**

### 3. 配置 Gradle JVM

在 IDEA 中：
1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**
2. **Gradle JVM**: 选择 **JDK 17** (`jdk-17.0.17.0_11`)
3. 点击 **OK**

### 4. 同步项目

IDEA 会自动开始同步 Gradle 项目。你会在右下角看到进度条。

等待同步完成（可能需要几分钟下载依赖）。

### 5. 运行插件

同步完成后：

**方式 1：通过 Gradle 任务**
1. 打开右侧的 **Gradle** 工具窗口
2. 展开 `idea-mcp` → `Tasks` → `intellij`
3. 双击 **runIde**
4. 会启动一个新的 IDEA 实例，插件自动加载

**方式 2：通过运行配置**
1. 点击右上角的运行配置下拉菜单
2. 选择 **Edit Configurations...**
3. 点击 **+** → **Gradle**
4. Name: `Run IDEA Plugin`
5. Gradle project: 选择 `idea-mcp`
6. Tasks: 输入 `runIde`
7. 点击 **OK**
8. 点击绿色的运行按钮 ▶️

### 6. 验证插件运行

在新启动的 IDEA 实例中：
1. 打开任意项目
2. 检查菜单栏：**Tools** → **IDEA MCP**
3. 应该能看到：
   - Start MCP Server
   - Stop MCP Server
   - Restart MCP Server

### 7. 测试 HTTP Server

```bash
curl http://localhost:58888/api/v1/health
```

预期响应：
```json
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

## ✅ 完成！

现在你可以：
1. 继续按照 `快速开始指南.md` 的第五步配置 Claude Desktop
2. 开始使用 AI 辅助代码重构和分析

---

## 🐛 如果遇到问题

### 问题：Gradle 同步失败

**解决方案**：
1. 检查 **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**
2. 确认 **Gradle JVM** 设置为 JDK 17
3. 点击 **Gradle** 工具窗口中的刷新按钮 🔄

### 问题：找不到 JDK 17

**解决方案**：
1. **File** → **Project Structure** → **Platform Settings** → **SDKs**
2. 点击 **+** → **Add JDK...**
3. 浏览到 `D:\software\devTools\sdk\versions\jdk_versions\jdk-17.0.17.0_11`
4. 点击 **OK**

### 问题：IDEA 版本太旧

确保你使用的是 IntelliJ IDEA 2024.1 或更高版本。

如果版本过旧，请从 https://www.jetbrains.com/idea/download/ 下载最新版本。

---

**提示**: 使用 IDEA 打开项目是最可靠的方法，因为 IDEA 会自动处理所有 Gradle 配置和依赖问题。
