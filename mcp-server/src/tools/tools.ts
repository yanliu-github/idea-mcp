// mcp-server/src/tools/tools.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 工具功能集
 * 提供代码格式化、优化导入等 MCP 工具
 */
export class ToolsTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有工具定义
   */
  getTools() {
    return [
      {
        name: 'tools_format_code',
        description: '格式化代码文件',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'tools_optimize_imports',
        description: '优化导入语句,移除未使用的导入并排序',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'tools_apply_quick_fix',
        description: '应用快速修复',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            fixId: { type: 'string', description: '快速修复ID' },
            offset: { type: 'number', description: '问题位置偏移量' },
            line: { type: 'number', description: '问题所在行号(与offset二选一)' },
            column: { type: 'number', description: '问题所在列号' },
          },
          required: ['filePath', 'fixId'],
        },
      },
      {
        name: 'tools_apply_intention',
        description: '应用 Intention Action',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            intentionId: { type: 'string', description: 'Intention ID' },
            offset: { type: 'number', description: '位置偏移量' },
            line: { type: 'number', description: '所在行号(与offset二选一)' },
            column: { type: 'number', description: '所在列号' },
          },
          required: ['filePath', 'intentionId'],
        },
      },
    ];
  }

  /**
   * 执行工具调用
   */
  async execute(toolName: string, args: any): Promise<any> {
    const endpoint = this.getEndpoint(toolName);
    const response = await this.ideaClient.post(endpoint, args);
    return response;
  }

  /**
   * 获取 API 端点
   */
  private getEndpoint(toolName: string): string {
    const endpoints: Record<string, string> = {
      tools_format_code: '/api/v1/tools/format',
      tools_optimize_imports: '/api/v1/tools/optimize-imports',
      tools_apply_quick_fix: '/api/v1/tools/quick-fix',
      tools_apply_intention: '/api/v1/tools/intention',
    };

    return endpoints[toolName] || '';
  }
}
