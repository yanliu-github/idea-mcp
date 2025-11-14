# IDEA MCP Server

MCP (Model Context Protocol) Server for IntelliJ IDEA integration.

## 功能特性

- 通过 MCP 协议暴露 IDEA 功能
- 支持代码重构操作
- 支持代码导航
- 支持代码分析

## 安装

```bash
npm install
```

## 构建

```bash
npm run build
```

## 开发

```bash
npm run dev
```

## 使用

```bash
npm start
```

## 配置

在 Claude Code 的配置文件中添加:

```json
{
  "mcpServers": {
    "idea-mcp": {
      "command": "node",
      "args": ["/path/to/idea-mcp-server/dist/index.js"],
      "env": {
        "IDEA_API_URL": "http://localhost:58888"
      }
    }
  }
}
```

## 许可证

MIT
