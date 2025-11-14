// mcp-server/src/tools/dependency.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 依赖分析工具集
 * 提供依赖分析相关的 MCP 工具
 */
export class DependencyTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有依赖分析工具定义
   */
  getTools() {
    return [
      {
        name: 'analyze_dependencies',
        description: '分析模块或包之间的依赖关系',
        inputSchema: {
          type: 'object',
          properties: {
            scope: { type: 'string', description: '分析范围(project/module/package,默认project)', enum: ['project', 'module', 'package'], default: 'project' },
          },
          required: [],
        },
      },
      {
        name: 'analyze_dependency_cycles',
        description: '检测循环依赖',
        inputSchema: {
          type: 'object',
          properties: {
            scope: { type: 'string', description: '分析范围(project/module/package,默认project)', enum: ['project', 'module', 'package'], default: 'project' },
          },
          required: [],
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
      analyze_dependencies: '/api/v1/analysis/dependencies',
      analyze_dependency_cycles: '/api/v1/analysis/dependency-cycles',
    };

    return endpoints[toolName] || '';
  }
}
