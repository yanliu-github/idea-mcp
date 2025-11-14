// mcp-server/src/tools/analysis.ts
import { IdeaClient } from '../ideaClient.js';

/**
 * 分析工具集
 * 提供代码分析相关的 MCP 工具
 */
export class AnalysisTools {
  constructor(private ideaClient: IdeaClient) {}

  /**
   * 获取所有分析工具定义
   */
  getTools() {
    return [
      {
        name: 'analyze_inspections',
        description: '运行代码检查,查找潜在问题(语法错误、代码质量问题、最佳实践违规等)。使用 IDEA 的检查引擎。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: {
              type: 'string',
              description: '文件路径(项目相对路径)。如果不指定,则检查整个项目(可能很慢)。',
            },
            severity: {
              type: 'string',
              description: '最小严重程度过滤(ERROR, WARNING, WEAK_WARNING, INFO)。默认为 WARNING。',
              enum: ['ERROR', 'WARNING', 'WEAK_WARNING', 'INFO'],
              default: 'WARNING',
            },
            includeQuickFixes: {
              type: 'boolean',
              description: '是否包含快速修复建议(默认false,启用会增加响应大小)',
              default: false,
            },
            maxResults: {
              type: 'number',
              description: '最大结果数量(默认100,0表示无限制)',
              default: 100,
            },
          },
        },
      },
    ];
  }

  /**
   * 执行分析工具
   */
  async execute(toolName: string, args: any) {
    console.error(`[INFO] Executing tool: ${toolName}`);
    console.error(`[INFO] Arguments:`, JSON.stringify(args, null, 2));

    switch (toolName) {
      case 'analyze_inspections':
        return this.runInspections(args);
      default:
        throw new Error(`Unknown analysis tool: ${toolName}`);
    }
  }

  /**
   * 运行代码检查
   */
  private async runInspections(args: any) {
    const target = args.filePath || 'entire project';
    console.error(`[INFO] Running inspections on ${target}`);
    console.error(`[INFO] Severity filter: ${args.severity || 'WARNING'}`);

    const response = await this.ideaClient.post('/api/v1/analysis/inspections', args);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Inspection completed`);
      console.error(`[INFO] Found ${data.totalCount} problems`);
      if (data.truncated) {
        console.error(`[INFO] Results truncated to ${data.problems.length} items`);
      }

      // 按严重程度统计
      if (data.problems && data.problems.length > 0) {
        const severityCounts: Record<string, number> = {};
        data.problems.forEach((problem: any) => {
          const severity = problem.severity || 'UNKNOWN';
          severityCounts[severity] = (severityCounts[severity] || 0) + 1;
        });

        console.error(`[INFO] Problems by severity:`);
        Object.entries(severityCounts).forEach(([severity, count]) => {
          console.error(`[INFO]   ${severity}: ${count}`);
        });

        // 输出前几个问题作为预览
        console.error(`[INFO] Sample problems:`);
        data.problems.slice(0, 3).forEach((problem: any) => {
          console.error(`[INFO]   [${problem.severity}] ${problem.message}`);
          console.error(`[INFO]   Location: ${problem.location.filePath}:${problem.location.line}:${problem.location.column}`);
        });
      }
    } else {
      console.error(`[ERROR] Inspection failed:`, response.error);
    }

    return response;
  }
}
