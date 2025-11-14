// mcp-server/src/tools/codegen.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 代码生成工具集
 * 提供代码生成相关的 MCP 工具
 */
export class CodegenTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有代码生成工具定义
   */
  getTools() {
    return [
      {
        name: 'codegen_generate_getters_setters',
        description: '为类字段生成 Getter 和 Setter 方法',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            fieldNames: { type: 'array', items: { type: 'string' }, description: '字段名称列表(空则生成所有字段)' },
            generateGetter: { type: 'boolean', description: '是否生成Getter(默认true)', default: true },
            generateSetter: { type: 'boolean', description: '是否生成Setter(默认true)', default: true },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'codegen_generate_constructor',
        description: '生成构造函数',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            fields: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  name: { type: 'string' },
                  type: { type: 'string' },
                },
                required: ['name', 'type'],
              },
              description: '包含的字段列表',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'codegen_generate_tostring',
        description: '生成 toString 方法',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            fields: { type: 'array', items: { type: 'string' }, description: '包含的字段名称' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'codegen_generate_equals',
        description: '生成 equals 方法',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            fields: { type: 'array', items: { type: 'string' }, description: '包含的字段名称' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'codegen_generate_hashcode',
        description: '生成 hashCode 方法',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            fields: { type: 'array', items: { type: 'string' }, description: '包含的字段名称' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'codegen_override_method',
        description: '重写父类或接口方法',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            methodName: { type: 'string', description: '要重写的方法名称' },
          },
          required: ['filePath', 'methodName'],
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
      codegen_generate_getters_setters: '/api/v1/codegen/getters-setters',
      codegen_generate_constructor: '/api/v1/codegen/constructor',
      codegen_generate_tostring: '/api/v1/codegen/tostring',
      codegen_generate_equals: '/api/v1/codegen/equals',
      codegen_generate_hashcode: '/api/v1/codegen/hashcode',
      codegen_override_method: '/api/v1/codegen/override-method',
    };

    return endpoints[toolName] || '';
  }
}
