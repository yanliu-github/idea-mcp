// mcp-server/src/tools/search.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 搜索工具集
 * 提供代码搜索相关的 MCP 工具
 */
export class SearchTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有搜索工具定义
   */
  getTools() {
    return [
      {
        name: 'search_global',
        description: '全局搜索类、方法、字段等代码元素',
        inputSchema: {
          type: 'object',
          properties: {
            query: { type: 'string', description: '搜索查询字符串' },
            scope: { type: 'string', description: '搜索范围(project/module/directory,默认project)', enum: ['project', 'module', 'directory'], default: 'project' },
            fileTypes: { type: 'array', items: { type: 'string' }, description: '文件类型过滤(例如:java,kt,ts)' },
          },
          required: ['query'],
        },
      },
      {
        name: 'search_text',
        description: '文本搜索,支持正则表达式',
        inputSchema: {
          type: 'object',
          properties: {
            pattern: { type: 'string', description: '搜索模式(支持正则表达式)' },
            caseSensitive: { type: 'boolean', description: '是否区分大小写(默认false)', default: false },
            regex: { type: 'boolean', description: '是否使用正则表达式(默认false)', default: false },
            scope: { type: 'string', description: '搜索范围(默认project)', enum: ['project', 'module', 'directory'], default: 'project' },
          },
          required: ['pattern'],
        },
      },
      {
        name: 'search_structural',
        description: '结构化搜索,基于代码结构模式匹配',
        inputSchema: {
          type: 'object',
          properties: {
            pattern: { type: 'string', description: '结构化搜索模式(例如:if ($x$ == null) { })' },
            scope: { type: 'string', description: '搜索范围(默认project)', enum: ['project', 'module', 'directory'], default: 'project' },
          },
          required: ['pattern'],
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
      search_global: '/api/v1/search/global',
      search_text: '/api/v1/search/text',
      search_structural: '/api/v1/search/structural',
    };

    return endpoints[toolName] || '';
  }
}
