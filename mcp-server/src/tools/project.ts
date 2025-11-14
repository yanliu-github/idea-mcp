// mcp-server/src/tools/project.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 项目管理工具集
 * 提供项目结构和配置相关的 MCP 工具
 */
export class ProjectTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有项目管理工具定义
   */
  getTools() {
    return [
      {
        name: 'project_get_structure',
        description: '获取项目结构信息',
        inputSchema: {
          type: 'object',
          properties: {},
          required: [],
        },
      },
      {
        name: 'project_get_modules',
        description: '获取项目模块列表',
        inputSchema: {
          type: 'object',
          properties: {},
          required: [],
        },
      },
      {
        name: 'project_get_libraries',
        description: '获取项目依赖库列表',
        inputSchema: {
          type: 'object',
          properties: {},
          required: [],
        },
      },
      {
        name: 'project_get_build_config',
        description: '获取项目构建配置信息',
        inputSchema: {
          type: 'object',
          properties: {},
          required: [],
        },
      },
    ];
  }

  /**
   * 执行工具调用
   */
  async execute(toolName: string, _args: any): Promise<any> {
    const endpoint = this.getEndpoint(toolName);
    return await this.ideaClient.get(endpoint);
  }

  /**
   * 获取 API 端点
   */
  private getEndpoint(toolName: string): string {
    const endpoints: Record<string, string> = {
      project_get_structure: '/api/v1/project/structure',
      project_get_modules: '/api/v1/project/modules',
      project_get_libraries: '/api/v1/project/libraries',
      project_get_build_config: '/api/v1/project/build-config',
    };

    return endpoints[toolName] || '';
  }
}
