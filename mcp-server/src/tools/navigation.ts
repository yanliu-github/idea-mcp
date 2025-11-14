// mcp-server/src/tools/navigation.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 导航工具集
 * 提供代码导航相关的 MCP 工具
 */
export class NavigationTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有导航工具定义
   */
  getTools() {
    return [
      {
        name: 'navigate_find_usages',
        description: '查找符号的所有使用位置(变量、方法、类等的引用)。使用 IDEA 的索引快速搜索整个项目。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: '文件路径(项目相对路径)',
            },
            offset: {
              type: 'number',
              description: '符号在文件中的偏移量(从0开始的字符位置)',
            },
            line: {
              type: 'number',
              description: '符号所在行号(从0开始,与offset二选一)',
            },
            column: {
              type: 'number',
              description: '符号所在列号(从0开始,必须和line一起使用)',
            },
            includeComments: {
              type: 'boolean',
              description: '是否包含注释中的使用(默认false)',
              default: false,
            },
            includeStrings: {
              type: 'boolean',
              description: '是否包含字符串中的使用(默认false)',
              default: false,
            },
            maxResults: {
              type: 'number',
              description: '最大结果数量(默认100,0表示无限制)',
              default: 100,
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'navigate_goto_definition',
        description: '跳转到符号定义位置。查找变量、方法、类等的声明/定义位置。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: '文件路径(项目相对路径)',
            },
            offset: {
              type: 'number',
              description: '符号在文件中的偏移量(从0开始的字符位置)',
            },
            line: {
              type: 'number',
              description: '符号所在行号(从0开始,与offset二选一)',
            },
            column: {
              type: 'number',
              description: '符号所在列号(从0开始,必须和line一起使用)',
            },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'navigate_type_hierarchy',
        description: '显示类型层次结构,包括父类和子类',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '类位置偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'navigate_call_hierarchy',
        description: '显示方法调用层次,包括调用者和被调用者',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '方法位置偏移量' },
            line: { type: 'number', description: '方法所在行号(与offset二选一)' },
            column: { type: 'number', description: '方法所在列号' },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'navigate_find_implementations',
        description: '查找接口或抽象方法的所有实现',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '元素位置偏移量' },
            line: { type: 'number', description: '元素所在行号(与offset二选一)' },
            column: { type: 'number', description: '元素所在列号' },
          },
          required: ['filePath'],
        },
      },
    ];
  }

  /**
   * 执行导航工具
   */
  async execute(toolName: string, args: any) {
    console.error(`[INFO] Executing tool: ${toolName}`);
    console.error(`[INFO] Arguments:`, JSON.stringify(args, null, 2));

    switch (toolName) {
      case 'navigate_find_usages':
        return this.findUsages(args);
      case 'navigate_goto_definition':
        return this.gotoDefinition(args);
      case 'navigate_type_hierarchy':
        return this.typeHierarchy(args);
      case 'navigate_call_hierarchy':
        return this.callHierarchy(args);
      case 'navigate_find_implementations':
        return this.findImplementations(args);
      default:
        throw new Error(`Unknown navigation tool: ${toolName}`);
    }
  }

  /**
   * 类型层次
   */
  private async typeHierarchy(args: any) {
    const response = await this.ideaClient.post('/api/v1/navigation/type-hierarchy', args);
    return response;
  }

  /**
   * 调用层次
   */
  private async callHierarchy(args: any) {
    const response = await this.ideaClient.post('/api/v1/navigation/call-hierarchy', args);
    return response;
  }

  /**
   * 查找实现
   */
  private async findImplementations(args: any) {
    const response = await this.ideaClient.post('/api/v1/navigation/find-implementations', args);
    return response;
  }

  /**
   * 查找用途
   */
  private async findUsages(args: any) {
    console.error(`[INFO] Finding usages in ${args.filePath}`);

    const response = await this.ideaClient.post('/api/v1/navigation/find-usages', args);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Found ${data.totalCount} usages of "${data.symbolName}"`);
      if (data.truncated) {
        console.error(`[INFO] Results truncated to ${data.usages.length} items`);
      }

      // 输出前几个用途作为预览
      if (data.usages && data.usages.length > 0) {
        console.error(`[INFO] Sample usages:`);
        data.usages.slice(0, 3).forEach((usage: any) => {
          console.error(`[INFO]   - ${usage.location.filePath}:${usage.location.line}:${usage.location.column}`);
          console.error(`[INFO]     ${usage.context}`);
        });
      }
    } else {
      console.error(`[ERROR] Find usages failed:`, response.error);
    }

    return response;
  }

  /**
   * 跳转到定义
   */
  private async gotoDefinition(args: any) {
    console.error(`[INFO] Finding definition in ${args.filePath}`);

    const response = await this.ideaClient.post('/api/v1/navigation/goto-definition', args);

    if (response.success) {
      const data = response.data;
      if (data.found && data.definition) {
        console.error(`[INFO] Found definition of "${data.symbolName}"`);
        console.error(`[INFO] Location: ${data.definition.filePath}:${data.definition.line}:${data.definition.column}`);
        console.error(`[INFO] Kind: ${data.kind || 'Unknown'}`);
      } else {
        console.error(`[INFO] No definition found for "${data.symbolName}"`);
      }
    } else {
      console.error(`[ERROR] Goto definition failed:`, response.error);
    }

    return response;
  }
}
