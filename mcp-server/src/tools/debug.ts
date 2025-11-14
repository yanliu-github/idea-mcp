// mcp-server/src/tools/debug.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 调试工具集
 * 提供调试相关的 MCP 工具
 */
export class DebugTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有调试工具定义
   */
  getTools() {
    return [
      {
        name: 'debug_set_breakpoint',
        description: '在指定行设置断点。支持普通断点、条件断点和日志断点。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径,例如: src/main/java/Main.java)' },
            line: { type: 'number', description: '断点行号(从0开始)' },
            condition: { type: 'string', description: '条件表达式(可选,例如: count > 10)' },
            logMessage: { type: 'string', description: '日志消息(可选,用于日志断点)' },
            enabled: { type: 'boolean', description: '是否启用断点(默认true)', default: true },
          },
          required: ['filePath', 'line'],
        },
      },
      {
        name: 'debug_list_breakpoints',
        description: '列出当前所有断点。',
        inputSchema: {
          type: 'object',
          properties: {},
        },
      },
      {
        name: 'debug_remove_breakpoint',
        description: '删除指定断点。',
        inputSchema: {
          type: 'object',
          properties: {
            breakpointId: { type: 'string', description: '断点ID(由设置断点时返回)' },
          },
          required: ['breakpointId'],
        },
      },
    ];
  }

  /**
   * 执行调试工具
   */
  async execute(toolName: string, args: any) {
    console.error(`[INFO] Executing debug tool: ${toolName}`);
    console.error(`[INFO] Arguments:`, JSON.stringify(args, null, 2));

    switch (toolName) {
      case 'debug_set_breakpoint': return this.setBreakpoint(args);
      case 'debug_list_breakpoints': return this.listBreakpoints(args);
      case 'debug_remove_breakpoint': return this.removeBreakpoint(args);
      default:
        throw new Error(`Unknown debug tool: ${toolName}`);
    }
  }

  /**
   * 设置断点
   */
  private async setBreakpoint(args: any) {
    console.error(`[INFO] Setting breakpoint in ${args.filePath} at line ${args.line}`);

    if (args.condition) {
      console.error(`[INFO] Condition: ${args.condition}`);
    }
    if (args.logMessage) {
      console.error(`[INFO] Log message: ${args.logMessage}`);
    }

    const response = await this.ideaClient.post('/api/v1/debug/breakpoint', args);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Breakpoint set successfully`);
      console.error(`[INFO] Breakpoint ID: ${data.breakpointId}`);
      console.error(`[INFO] Type: ${data.type || 'line'}`);
      console.error(`[INFO] Location: ${data.location?.filePath}:${data.location?.line}`);
    } else {
      console.error(`[ERROR] Failed to set breakpoint:`, response.error);
    }

    return response;
  }

  /**
   * 列出所有断点
   */
  private async listBreakpoints(_args: any) {
    console.error(`[INFO] Listing all breakpoints`);

    const response = await this.ideaClient.get('/api/v1/debug/breakpoints');

    if (response.success) {
      const data = response.data;
      const breakpoints = data.breakpoints || [];
      console.error(`[INFO] Found ${breakpoints.length} breakpoint(s)`);

      if (breakpoints.length > 0) {
        console.error(`[INFO] Breakpoints:`);
        breakpoints.forEach((bp: any, index: number) => {
          console.error(`[INFO]   ${index + 1}. ${bp.breakpointId} - ${bp.location?.filePath}:${bp.location?.line} (${bp.type})`);
          if (bp.condition) {
            console.error(`[INFO]      Condition: ${bp.condition}`);
          }
          if (bp.logMessage) {
            console.error(`[INFO]      Log: ${bp.logMessage}`);
          }
        });
      }
    } else {
      console.error(`[ERROR] Failed to list breakpoints:`, response.error);
    }

    return response;
  }

  /**
   * 删除断点
   */
  private async removeBreakpoint(args: any) {
    console.error(`[INFO] Removing breakpoint: ${args.breakpointId}`);

    const response = await this.ideaClient.delete(`/api/v1/debug/breakpoint/${args.breakpointId}`);

    if (response.success) {
      console.error(`[INFO] Breakpoint removed successfully`);
      console.error(`[INFO] Breakpoint ID: ${args.breakpointId}`);
    } else {
      console.error(`[ERROR] Failed to remove breakpoint:`, response.error);
    }

    return response;
  }
}
