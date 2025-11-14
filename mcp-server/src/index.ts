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
import { DebugTools } from './tools/debug.js';
import { CodegenTools } from './tools/codegen.js';
import { SearchTools } from './tools/search.js';
import { DependencyTools } from './tools/dependency.js';
import { VcsTools } from './tools/vcs.js';
import { ProjectTools } from './tools/project.js';
import { ToolsTools } from './tools/tools.js';

/**
 * IDEA MCP Server
 * 通过 MCP 协议将 IntelliJ IDEA 的功能暴露给 AI 工具
 */

// 从环境变量读取 IDEA Plugin URL (默认 localhost:58888)
const ideaPluginUrl = process.env.IDEA_PLUGIN_URL || 'http://localhost:58888';

console.error(`[INFO] IDEA Plugin URL: ${ideaPluginUrl}`);

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
const ideaClient = new IdeaClient(ideaPluginUrl);

// 创建工具集
const refactoringTools = new RefactoringTools(ideaClient);
const navigationTools = new NavigationTools(ideaClient);
const analysisTools = new AnalysisTools(ideaClient);
const debugTools = new DebugTools(ideaClient);
const codegenTools = new CodegenTools(ideaClient);
const searchTools = new SearchTools(ideaClient);
const dependencyTools = new DependencyTools(ideaClient);
const vcsTools = new VcsTools(ideaClient);
const projectTools = new ProjectTools(ideaClient);
const toolsTools = new ToolsTools(ideaClient);

// 处理 tools/list 请求
server.setRequestHandler(ListToolsRequestSchema, async () => {
  console.error('[INFO] Listing available tools');

  const tools = [
    ...refactoringTools.getTools(),
    ...navigationTools.getTools(),
    ...analysisTools.getTools(),
    ...debugTools.getTools(),
    ...codegenTools.getTools(),
    ...searchTools.getTools(),
    ...dependencyTools.getTools(),
    ...vcsTools.getTools(),
    ...projectTools.getTools(),
    ...toolsTools.getTools(),
  ];

  console.error(`[INFO] Returning ${tools.length} tools`);
  return { tools };
});

// 处理 tools/call 请求
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  console.error(`[INFO] Tool call requested: ${name}`);

  try {
    let result;

    // 路由到对应的工具处理器
    if (name.startsWith('refactor_')) {
      result = await refactoringTools.execute(name, args);
    } else if (name.startsWith('navigate_')) {
      result = await navigationTools.execute(name, args);
    } else if (name.startsWith('analyze_')) {
      result = await analysisTools.execute(name, args);
    } else if (name.startsWith('debug_')) {
      result = await debugTools.execute(name, args);
    } else if (name.startsWith('codegen_')) {
      result = await codegenTools.execute(name, args);
    } else if (name.startsWith('search_')) {
      result = await searchTools.execute(name, args);
    } else if (name.startsWith('vcs_')) {
      result = await vcsTools.execute(name, args);
    } else if (name.startsWith('project_')) {
      result = await projectTools.execute(name, args);
    } else if (name.startsWith('tools_')) {
      result = await toolsTools.execute(name, args);
    } else {
      console.error(`[ERROR] Unknown tool: ${name}`);
      throw new Error(`Unknown tool: ${name}`);
    }

    console.error(`[INFO] Tool execution completed: ${name}`);

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

    // 记录错误到 stderr (不影响 stdout)
    console.error(`[ERROR] Tool execution failed: ${name}`);
    console.error(`[ERROR] Error message: ${errorMessage}`);
    if (error instanceof Error && error.stack) {
      console.error(`[ERROR] Stack trace:`, error.stack);
    }

    // 返回错误响应 (仍然是成功的 MCP 响应,但内容是错误信息)
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
                details: error instanceof Error ? { stack: error.stack } : {},
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

/**
 * 主函数 - 启动 MCP Server
 */
async function main() {
  console.error('[INFO] ==========================================');
  console.error('[INFO] Starting IDEA MCP Server...');
  console.error('[INFO] ==========================================');
  console.error(`[INFO] Server name: idea-mcp-server`);
  console.error(`[INFO] Server version: 1.0.0`);
  console.error(`[INFO] IDEA Plugin URL: ${ideaPluginUrl}`);
  console.error('[INFO] Transport: stdio (standard input/output)');

  try {
    // 创建 stdio transport
    const transport = new StdioServerTransport();
    console.error('[INFO] Created stdio transport');

    // 连接 server 和 transport
    await server.connect(transport);
    console.error('[INFO] Server connected to transport');

    console.error('[INFO] ==========================================');
    console.error('[INFO] IDEA MCP Server started successfully');
    console.error('[INFO] ==========================================');
    console.error('[INFO] Listening on stdio...');
    console.error('[INFO] Waiting for requests from AI tools (e.g., Claude Code)');

    // 验证 IDEA Plugin 连接 (非阻塞)
    console.error('[INFO] Verifying connection to IDEA Plugin...');
    try {
      const health = await ideaClient.healthCheck();
      if (health.success && health.data) {
        console.error(`[INFO] ✓ Connected to IDEA Plugin`);
        console.error(`[INFO]   IDEA Version: ${health.data.ideaVersion || 'Unknown'}`);
        console.error(`[INFO]   Index Ready: ${health.data.indexReady ? 'Yes' : 'No'}`);
      } else {
        console.error('[WARN] IDEA Plugin health check returned unexpected response');
      }
    } catch (error) {
      console.error('[WARN] ✗ Failed to connect to IDEA Plugin');
      console.error('[WARN] This is OK if IDEA is not running yet');
      console.error('[WARN] The server will still accept requests');
      console.error('[WARN] Make sure IDEA Plugin is running when you use tools');
      console.error(`[WARN] Error: ${error instanceof Error ? error.message : String(error)}`);
    }

    console.error('[INFO] ==========================================');
    console.error('[INFO] Ready to handle tool requests');
    console.error('[INFO] ==========================================');
  } catch (error) {
    console.error('[FATAL] Failed to start server');
    console.error('[FATAL] Error:', error);
    throw error;
  }
}

// 启动主函数
main().catch((error) => {
  console.error('[FATAL] ==========================================');
  console.error('[FATAL] Server startup failed');
  console.error('[FATAL] ==========================================');
  console.error('[FATAL] Error:', error);
  console.error('[FATAL] Stack:', error instanceof Error ? error.stack : 'N/A');
  process.exit(1);
});
