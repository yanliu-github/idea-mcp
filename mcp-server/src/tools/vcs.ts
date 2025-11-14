// mcp-server/src/tools/vcs.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 版本控制工具集
 * 提供 Git 相关的 MCP 工具
 */
export class VcsTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有版本控制工具定义
   */
  getTools() {
    return [
      {
        name: 'vcs_get_status',
        description: '获取版本控制系统状态',
        inputSchema: {
          type: 'object',
          properties: {},
          required: [],
        },
      },
      {
        name: 'vcs_get_history',
        description: '获取文件的版本历史',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            limit: { type: 'number', description: '返回的提交数量限制(默认100)', default: 100 },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'vcs_get_diff',
        description: '获取文件差异对比',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            oldRevision: { type: 'string', description: '旧版本(留空表示工作目录)' },
            newRevision: { type: 'string', description: '新版本(留空表示HEAD)' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'vcs_blame',
        description: '获取文件的逐行注解(blame/annotate)',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
          },
          required: ['filePath'],
        },
      },
    ];
  }

  /**
   * 执行工具调用
   */
  async execute(toolName: string, args: any): Promise<any> {
    const endpoint = this.getEndpoint(toolName);

    if (toolName === 'vcs_get_status') {
      return await this.ideaClient.get(endpoint);
    }

    return await this.ideaClient.post(endpoint, args);
  }

  /**
   * 获取 API 端点
   */
  private getEndpoint(toolName: string): string {
    const endpoints: Record<string, string> = {
      vcs_get_status: '/api/v1/vcs/status',
      vcs_get_history: '/api/v1/vcs/history',
      vcs_get_diff: '/api/v1/vcs/diff',
      vcs_blame: '/api/v1/vcs/blame',
    };

    return endpoints[toolName] || '';
  }
}
