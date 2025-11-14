// mcp-server/src/tools/refactoring.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 重构工具集
 * 提供代码重构相关的 MCP 工具
 */
export class RefactoringTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有重构工具定义
   */
  getTools() {
    return [
      {
        name: 'refactor_rename',
        description: '重命名代码中的符号(变量、方法、类等)。使用 IDEA 的重命名引擎,会自动更新所有引用。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: '文件路径(项目相对路径,例如: src/main/java/User.java)',
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
            newName: {
              type: 'string',
              description: '新名称(符合编程语言命名规范)',
            },
            searchInComments: {
              type: 'boolean',
              description: '是否在注释中搜索并替换(默认false)',
              default: false,
            },
            searchInStrings: {
              type: 'boolean',
              description: '是否在字符串中搜索并替换(默认false)',
              default: false,
            },
            preview: {
              type: 'boolean',
              description: '是否仅预览不执行(默认false)',
              default: false,
            },
          },
          required: ['filePath', 'newName'],
        },
      },
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
      default:
        throw new Error(`Unknown refactoring tool: ${toolName}`);
    }
  }

  /**
   * 重命名操作
   */
  private async rename(args: any) {
    console.error(`[INFO] Renaming symbol in ${args.filePath} to "${args.newName}"`);

    const response = await this.ideaClient.post('/api/v1/refactor/rename', args);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Rename completed successfully`);
      console.error(`[INFO] Old name: ${data.oldName || 'N/A'}`);
      console.error(`[INFO] New name: ${data.newName}`);
      console.error(`[INFO] Affected files: ${data.affectedFiles?.length || 0}`);

      // 如果有冲突,记录警告
      if (data.conflicts && data.conflicts.length > 0) {
        console.error(`[WARN] Found ${data.conflicts.length} conflicts`);
        data.conflicts.forEach((conflict: any) => {
          console.error(`[WARN] - ${conflict.message}`);
        });
      }
    } else {
      console.error(`[ERROR] Rename failed:`, response.error);
    }

    return response;
  }
}
