# IntelliJ IDEA MCP 服务器 - stdio 通信示例

## 文档信息

- **文档版本**: v1.0
- **创建日期**: 2025-11-13
- **文档状态**: 通信示例
- **项目名称**: IDEA MCP Server

---

## 1. stdio 通信概述

### 1.1 什么是 stdio 通信?

**stdio** (Standard Input/Output) 是 MCP 协议的标准传输方式:
- **stdin**: 标准输入,MCP Client (AI 工具) 发送请求
- **stdout**: 标准输出,MCP Server 返回响应
- **stderr**: 标准错误,输出日志和错误信息

### 1.2 通信流程

```
┌─────────────┐        stdin         ┌─────────────┐
│             │ ───────────────────> │             │
│  AI Tool    │                      │ MCP Server  │
│ (Claude Code)│ <─────────────────── │  (Node.js)  │
└─────────────┘       stdout         └─────────────┘
                        │
                        │ stderr (logs)
                        ▼
                   终端/日志文件
```

### 1.3 消息格式

MCP 使用 **JSON-RPC 2.0** 格式,每条消息占一行,以换行符 `\n` 分隔。

---

## 2. MCP 协议基础

### 2.1 请求消息格式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "refactor_rename",
    "arguments": {
      "filePath": "/src/User.java",
      "offset": 1234,
      "newName": "Customer"
    }
  }
}
```

**字段说明**:
- `jsonrpc`: 协议版本,固定为 "2.0"
- `id`: 请求 ID,用于匹配响应 (可以是数字或字符串)
- `method`: 方法名,MCP 定义的方法
- `params`: 参数对象

### 2.2 响应消息格式

**成功响应**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"success\": true, \"data\": {...}}"
      }
    ]
  }
}
```

**错误响应**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32603,
    "message": "Internal error",
    "data": {
      "details": "File not found: /src/User.java"
    }
  }
}
```

**字段说明**:
- `result`: 成功时返回
- `error`: 失败时返回
  - `code`: 错误码 (JSON-RPC 标准错误码)
  - `message`: 错误消息
  - `data`: 额外错误信息

### 2.3 MCP 标准错误码

| 错误码 | 说明 |
|-------|------|
| -32700 | Parse error (JSON 解析错误) |
| -32600 | Invalid Request (无效请求) |
| -32601 | Method not found (方法未找到) |
| -32602 | Invalid params (参数无效) |
| -32603 | Internal error (内部错误) |
| -32000 到 -32099 | 服务器自定义错误 |

---

## 3. MCP Server 实现

### 3.1 TypeScript 实现示例

#### 3.1.1 index.ts (入口文件)

```typescript
// mcp-server/src/index.ts
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';

import { IdeaClient } from './ideaClient.js';
import { RefactoringTools } from './tools/refactoring.js';
import { NavigationTools } from './tools/navigation.js';
import { AnalysisTools } from './tools/analysis.js';

// 创建 MCP Server
const server = new Server(
  {
    name: 'idea-mcp-server',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// 创建 IDEA HTTP 客户端
const ideaClient = new IdeaClient('http://localhost:58888');

// 注册工具
const refactoringTools = new RefactoringTools(ideaClient);
const navigationTools = new NavigationTools(ideaClient);
const analysisTools = new AnalysisTools(ideaClient);

// 处理 tools/list 请求
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      ...refactoringTools.getTools(),
      ...navigationTools.getTools(),
      ...analysisTools.getTools(),
    ],
  };
});

// 处理 tools/call 请求
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    let result;

    // 路由到对应的工具处理器
    if (name.startsWith('refactor_')) {
      result = await refactoringTools.execute(name, args);
    } else if (name.startsWith('navigate_')) {
      result = await navigationTools.execute(name, args);
    } else if (name.startsWith('analyze_')) {
      result = await analysisTools.execute(name, args);
    } else {
      throw new Error(`Unknown tool: ${name}`);
    }

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2),
        },
      ],
    };
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);

    // 记录错误到 stderr
    console.error(`[ERROR] Tool execution failed: ${name}`, error);

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(
            {
              success: false,
              error: {
                code: 'TOOL_EXECUTION_FAILED',
                message: errorMessage,
              },
            },
            null,
            2
          ),
        },
      ],
      isError: true,
    };
  }
});

// 启动服务器 (stdio 传输)
async function main() {
  console.error('[INFO] Starting IDEA MCP Server...');

  // 创建 stdio transport
  const transport = new StdioServerTransport();

  // 连接 server 和 transport
  await server.connect(transport);

  console.error('[INFO] IDEA MCP Server started successfully');
  console.error('[INFO] Listening on stdio...');

  // 验证 IDEA Plugin 连接
  try {
    const health = await ideaClient.healthCheck();
    console.error(`[INFO] Connected to IDEA Plugin: ${health.ideaVersion}`);
  } catch (error) {
    console.error('[WARN] Failed to connect to IDEA Plugin:', error);
    console.error('[WARN] Make sure IDEA Plugin is running on port 58888');
  }
}

// 启动主函数
main().catch((error) => {
  console.error('[FATAL] Failed to start server:', error);
  process.exit(1);
});
```

#### 3.1.2 ideaClient.ts (HTTP 客户端)

```typescript
// mcp-server/src/ideaClient.ts
import axios, { AxiosInstance, AxiosError } from 'axios';

export class IdeaClient {
  private client: AxiosInstance;

  constructor(private baseUrl: string) {
    this.client = axios.create({
      baseURL: baseUrl,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // 响应拦截器
    this.client.interceptors.response.use(
      response => response,
      error => {
        this.handleError(error);
        throw error;
      }
    );
  }

  /**
   * 发送 POST 请求到 IDEA Plugin
   */
  async post<T = any>(endpoint: string, data: any): Promise<T> {
    try {
      const response = await this.client.post<T>(endpoint, data);
      return response.data;
    } catch (error) {
      console.error(`[ERROR] POST ${endpoint} failed:`, error);
      throw error;
    }
  }

  /**
   * 发送 GET 请求到 IDEA Plugin
   */
  async get<T = any>(endpoint: string): Promise<T> {
    try {
      const response = await this.client.get<T>(endpoint);
      return response.data;
    } catch (error) {
      console.error(`[ERROR] GET ${endpoint} failed:`, error);
      throw error;
    }
  }

  /**
   * 健康检查
   */
  async healthCheck(): Promise<any> {
    return this.get('/api/v1/health');
  }

  /**
   * 等待索引就绪
   */
  async waitForIndex(timeout: number = 60000): Promise<void> {
    const start = Date.now();

    while (Date.now() - start < timeout) {
      try {
        const health = await this.healthCheck();
        if (health.data.indexReady) {
          return;
        }
      } catch {
        // 忽略错误,继续等待
      }

      await new Promise(resolve => setTimeout(resolve, 1000));
    }

    throw new Error('Timeout waiting for index to be ready');
  }

  /**
   * 错误处理
   */
  private handleError(error: AxiosError): void {
    if (error.response) {
      // HTTP 错误响应
      console.error(`[ERROR] HTTP ${error.response.status}:`, error.response.data);
    } else if (error.request) {
      // 请求发送但无响应
      console.error('[ERROR] No response from IDEA Plugin');
      console.error('[ERROR] Make sure IDEA Plugin is running');
    } else {
      // 其他错误
      console.error('[ERROR] Request failed:', error.message);
    }
  }
}
```

#### 3.1.3 refactoring.ts (重构工具)

```typescript
// mcp-server/src/tools/refactoring.ts
import { IdeaClient } from '../ideaClient.js';

export class RefactoringTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有重构工具定义
   */
  getTools() {
    return [
      {
        name: 'refactor_rename',
        description: '重命名代码中的符号(变量、方法、类等)',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: '文件路径(项目相对路径)',
            },
            offset: {
              type: 'number',
              description: '符号在文件中的偏移量(从0开始)',
            },
            newName: {
              type: 'string',
              description: '新名称',
            },
            searchInComments: {
              type: 'boolean',
              description: '是否在注释中搜索(默认false)',
              default: false,
            },
            searchInStrings: {
              type: 'boolean',
              description: '是否在字符串中搜索(默认false)',
              default: false,
            },
            preview: {
              type: 'boolean',
              description: '是否仅预览不执行(默认false)',
              default: false,
            },
          },
          required: ['filePath', 'offset', 'newName'],
        },
      },
      // 其他重构工具...
    ];
  }

  /**
   * 执行重构工具
   */
  async execute(toolName: string, args: any) {
    console.error(`[INFO] Executing tool: ${toolName}`);
    console.error(`[INFO] Arguments:`, JSON.stringify(args, null, 2));

    switch (toolName) {
      case 'refactor_rename':
        return this.rename(args);
      // 其他工具...
      default:
        throw new Error(`Unknown tool: ${toolName}`);
    }
  }

  /**
   * 重命名操作
   */
  private async rename(args: any) {
    const response = await this.ideaClient.post('/api/v1/refactor/rename', args);

    console.error(`[INFO] Rename completed successfully`);
    console.error(`[INFO] Affected files: ${response.data?.affectedFiles?.length || 0}`);

    return response;
  }
}
```

---

## 4. 消息流示例

### 4.1 初始化握手

**AI Tool → MCP Server (stdin)**:
```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"1.0","capabilities":{},"clientInfo":{"name":"claude-code","version":"1.0.0"}}}
```

**MCP Server → AI Tool (stdout)**:
```json
{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"1.0","capabilities":{"tools":{}},"serverInfo":{"name":"idea-mcp-server","version":"1.0.0"}}}
```

### 4.2 列出工具

**AI Tool → MCP Server (stdin)**:
```json
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

**MCP Server → AI Tool (stdout)**:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "refactor_rename",
        "description": "重命名代码中的符号(变量、方法、类等)",
        "inputSchema": {
          "type": "object",
          "properties": {
            "filePath": {"type": "string"},
            "offset": {"type": "number"},
            "newName": {"type": "string"}
          },
          "required": ["filePath", "offset", "newName"]
        }
      }
    ]
  }
}
```

### 4.3 调用工具 - 成功

**AI Tool → MCP Server (stdin)**:
```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"refactor_rename","arguments":{"filePath":"/src/User.java","offset":1234,"newName":"Customer"}}}
```

**MCP Server 日志 (stderr)**:
```
[INFO] Executing tool: refactor_rename
[INFO] Arguments: {"filePath":"/src/User.java","offset":1234,"newName":"Customer"}
[INFO] POST /api/v1/refactor/rename
[INFO] Rename completed successfully
[INFO] Affected files: 3
```

**MCP Server → AI Tool (stdout)**:
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"success\":true,\"data\":{\"affectedFiles\":[{\"filePath\":\"/src/User.java\",\"changes\":[{\"startOffset\":1234,\"endOffset\":1238,\"oldText\":\"User\",\"newText\":\"Customer\"}]}],\"usageCount\":15}}"
      }
    ]
  }
}
```

### 4.4 调用工具 - 失败

**AI Tool → MCP Server (stdin)**:
```json
{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"refactor_rename","arguments":{"filePath":"/nonexistent.java","offset":1234,"newName":"Customer"}}}
```

**MCP Server 日志 (stderr)**:
```
[ERROR] POST /api/v1/refactor/rename failed
[ERROR] HTTP 404: {"success":false,"error":{"code":"FILE_NOT_FOUND","message":"File not found: /nonexistent.java"}}
[ERROR] Tool execution failed: refactor_rename
```

**MCP Server → AI Tool (stdout)**:
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"success\":false,\"error\":{\"code\":\"FILE_NOT_FOUND\",\"message\":\"File not found: /nonexistent.java\"}}"
      }
    ],
    "isError": true
  }
}
```

---

## 5. 本地测试

### 5.1 手动测试 (使用 curl 模拟)

**启动 MCP Server**:
```bash
cd mcp-server
npm run dev
```

**在另一个终端,通过 stdin 发送请求**:
```bash
# 列出工具
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | node dist/index.js

# 调用工具
echo '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"refactor_rename","arguments":{"filePath":"/src/User.java","offset":1234,"newName":"Customer"}}}' | node dist/index.js
```

### 5.2 使用 MCP Inspector (推荐)

```bash
# 安装 MCP Inspector
npm install -g @modelcontextprotocol/inspector

# 启动 Inspector (提供 Web UI)
mcp-inspector node dist/index.js

# 在浏览器打开 http://localhost:5173
```

**Inspector 功能**:
- 可视化工具列表
- 交互式工具调用
- 实时查看请求/响应
- 日志输出

### 5.3 调试技巧

**1. 输出调试日志到 stderr**:
```typescript
// 使用 console.error (不会干扰 stdout)
console.error('[DEBUG] Request:', JSON.stringify(request, null, 2));
console.error('[DEBUG] Response:', JSON.stringify(response, null, 2));
```

**2. 保存日志到文件**:
```bash
# 运行时重定向 stderr
node dist/index.js 2> debug.log

# 或在代码中配置
import fs from 'fs';
const logStream = fs.createWriteStream('debug.log', { flags: 'a' });
console.error = (...args) => logStream.write(args.join(' ') + '\n');
```

**3. 使用 VSCode 调试**:
```json
// .vscode/launch.json
{
  "type": "node",
  "request": "launch",
  "name": "Debug MCP Server",
  "program": "${workspaceFolder}/mcp-server/dist/index.js",
  "console": "integratedTerminal",
  "internalConsoleOptions": "neverOpen"
}
```

---

## 6. 与 Claude Code 集成

### 6.1 配置 Claude Code

在 Claude Code 配置文件中添加 MCP Server:

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Linux**: `~/.config/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "idea-mcp": {
      "command": "node",
      "args": ["E:/Idea-mcp/mcp-server/dist/index.js"],
      "env": {
        "IDEA_PLUGIN_URL": "http://localhost:58888"
      }
    }
  }
}
```

### 6.2 验证连接

1. 启动 IntelliJ IDEA 并打开项目
2. 确保 IDEA MCP Plugin 已启动 (查看端口 58888)
3. 启动 Claude Code
4. 在 Claude Code 中输入:
   ```
   Please list available IDEA MCP tools
   ```
5. Claude Code 应显示所有可用工具

### 6.3 使用示例

**重命名类**:
```
Please rename the class "User" to "Customer" in file /src/User.java
```

**查找用途**:
```
Find all usages of the method "getName" in User.java
```

---

## 7. 错误处理

### 7.1 常见错误

#### 错误 1: IDEA Plugin 连接失败

**错误信息**:
```
[WARN] Failed to connect to IDEA Plugin: connect ECONNREFUSED 127.0.0.1:58888
```

**解决方案**:
1. 确保 IDEA 正在运行
2. 确保 IDEA MCP Plugin 已启动
3. 检查端口 58888 是否被占用:
   ```bash
   # Windows
   netstat -ano | findstr :58888

   # macOS/Linux
   lsof -i :58888
   ```

#### 错误 2: JSON 解析错误

**错误信息**:
```
[ERROR] Parse error: Unexpected token
```

**解决方案**:
- 检查消息格式是否正确
- 确保每条消息占一行
- 验证 JSON 语法

#### 错误 3: 索引未就绪

**错误信息**:
```
{"success":false,"error":{"code":"INDEX_NOT_READY","message":"索引未就绪"}}
```

**解决方案**:
- 等待 IDEA 完成索引
- 使用 `waitForIndex()` 方法:
  ```typescript
  await ideaClient.waitForIndex();
  ```

---

## 8. 性能优化

### 8.1 批量请求

如果需要执行多个操作,考虑批量处理:

```typescript
// 不推荐: 串行执行
for (const file of files) {
  await rename(file);
}

// 推荐: 并行执行
await Promise.all(files.map(file => rename(file)));
```

### 8.2 连接池

对于频繁请求,使用 HTTP 连接池:

```typescript
import axios from 'axios';
import { Agent } from 'http';

const client = axios.create({
  httpAgent: new Agent({
    keepAlive: true,
    maxSockets: 10,
  }),
});
```

---

## 9. 附录

### 9.1 完整的 package.json

```json
{
  "name": "idea-mcp-server",
  "version": "1.0.0",
  "description": "MCP Server for IntelliJ IDEA",
  "type": "module",
  "main": "dist/index.js",
  "scripts": {
    "build": "tsc",
    "dev": "tsc && node dist/index.js",
    "watch": "tsc --watch",
    "test": "jest"
  },
  "dependencies": {
    "@modelcontextprotocol/sdk": "^0.5.0",
    "axios": "^1.6.2"
  },
  "devDependencies": {
    "@types/node": "^20.10.0",
    "typescript": "^5.3.3"
  }
}
```

### 9.2 完整的 tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "moduleResolution": "node",
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

---

**文档维护**: stdio 通信实现变更时需更新此文档。
