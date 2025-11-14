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
      // ========== Phase 3.1: 断点管理 ==========
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

      // ========== Phase 3.2: 调试会话管理 ==========
      {
        name: 'debug_start_session',
        description: '启动调试会话。在指定的主类上启动调试器。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '主程序文件路径(项目相对路径)' },
            mainClass: { type: 'string', description: '主类全限定名(例如: com.example.Main)' },
            programArgs: { type: 'array', items: { type: 'string' }, description: '程序参数(可选)' },
            vmArgs: { type: 'array', items: { type: 'string' }, description: 'JVM参数(可选)' },
          },
          required: ['filePath', 'mainClass'],
        },
      },
      {
        name: 'debug_stop_session',
        description: '停止调试会话。终止正在运行的调试会话。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID(由启动会话时返回)' },
          },
          required: ['sessionId'],
        },
      },
      {
        name: 'debug_pause',
        description: '暂停调试会话。暂停正在运行的程序执行。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
          },
          required: ['sessionId'],
        },
      },
      {
        name: 'debug_resume',
        description: '继续调试会话。继续执行已暂停的程序。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
          },
          required: ['sessionId'],
        },
      },
      {
        name: 'debug_step_over',
        description: '步过。执行当前行并停在下一行(不进入方法内部)。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
          },
          required: ['sessionId'],
        },
      },
      {
        name: 'debug_step_into',
        description: '步入。执行当前行并进入方法内部(如果当前行包含方法调用)。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
          },
          required: ['sessionId'],
        },
      },
      {
        name: 'debug_step_out',
        description: '步出。继续执行直到从当前方法返回。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
          },
          required: ['sessionId'],
        },
      },

      // ========== Phase 3.3: 表达式和变量 ==========
      {
        name: 'debug_evaluate_expression',
        description: '计算表达式。在当前调试上下文中计算表达式的值。会话必须处于暂停状态。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
            expression: { type: 'string', description: '要计算的表达式(例如: count + 1, user.getName())' },
            frameIndex: { type: 'number', description: '栈帧索引(可选,默认为0,即当前栈帧)', default: 0 },
          },
          required: ['sessionId', 'expression'],
        },
      },
      {
        name: 'debug_get_variables',
        description: '获取变量列表。获取当前作用域的所有变量。会话必须处于暂停状态。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
            frameIndex: { type: 'number', description: '栈帧索引(可选,默认为0)', default: 0 },
            scope: {
              type: 'string',
              description: '作用域(可选: local-局部变量, instance-实例变量, static-静态变量, all-所有; 默认: local)',
              enum: ['local', 'instance', 'static', 'all'],
              default: 'local'
            },
          },
          required: ['sessionId'],
        },
      },
      {
        name: 'debug_get_variable',
        description: '获取指定变量详情。获取单个变量的详细信息,包括其子字段(如果是对象)。会话必须处于暂停状态。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
            variableName: { type: 'string', description: '变量名' },
          },
          required: ['sessionId', 'variableName'],
        },
      },

      // ========== Phase 3.4: 调用栈 ==========
      {
        name: 'debug_get_call_stack',
        description: '获取调用栈。获取当前线程的完整调用栈信息。会话必须处于暂停状态。',
        inputSchema: {
          type: 'object',
          properties: {
            sessionId: { type: 'string', description: '会话ID' },
          },
          required: ['sessionId'],
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
      // Phase 3.1: 断点管理
      case 'debug_set_breakpoint': return this.setBreakpoint(args);
      case 'debug_list_breakpoints': return this.listBreakpoints(args);
      case 'debug_remove_breakpoint': return this.removeBreakpoint(args);

      // Phase 3.2: 调试会话管理
      case 'debug_start_session': return this.startSession(args);
      case 'debug_stop_session': return this.stopSession(args);
      case 'debug_pause': return this.pauseSession(args);
      case 'debug_resume': return this.resumeSession(args);
      case 'debug_step_over': return this.stepOver(args);
      case 'debug_step_into': return this.stepInto(args);
      case 'debug_step_out': return this.stepOut(args);

      // Phase 3.3: 表达式和变量
      case 'debug_evaluate_expression': return this.evaluateExpression(args);
      case 'debug_get_variables': return this.getVariables(args);
      case 'debug_get_variable': return this.getVariable(args);

      // Phase 3.4: 调用栈
      case 'debug_get_call_stack': return this.getCallStack(args);

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

  // ========== Phase 3.2: 调试会话管理 ==========

  /**
   * 启动调试会话
   */
  private async startSession(args: any) {
    console.error(`[INFO] Starting debug session`);
    console.error(`[INFO] Main class: ${args.mainClass}`);
    console.error(`[INFO] File path: ${args.filePath}`);

    if (args.programArgs) {
      console.error(`[INFO] Program arguments: ${args.programArgs.join(' ')}`);
    }
    if (args.vmArgs) {
      console.error(`[INFO] VM arguments: ${args.vmArgs.join(' ')}`);
    }

    const response = await this.ideaClient.post('/api/v1/debug/session/start', args);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Debug session started successfully`);
      console.error(`[INFO] Session ID: ${data.sessionId}`);
      console.error(`[INFO] Status: ${data.status}`);
      if (data.breakpoints && data.breakpoints.length > 0) {
        console.error(`[INFO] Active breakpoints: ${data.breakpoints.length}`);
      }
    } else {
      console.error(`[ERROR] Failed to start debug session:`, response.error);
    }

    return response;
  }

  /**
   * 停止调试会话
   */
  private async stopSession(args: any) {
    console.error(`[INFO] Stopping debug session: ${args.sessionId}`);

    const response = await this.ideaClient.post('/api/v1/debug/session/stop', {
      sessionId: args.sessionId
    });

    if (response.success) {
      console.error(`[INFO] Debug session stopped successfully`);
      console.error(`[INFO] Session ID: ${args.sessionId}`);
    } else {
      console.error(`[ERROR] Failed to stop debug session:`, response.error);
    }

    return response;
  }

  /**
   * 暂停调试会话
   */
  private async pauseSession(args: any) {
    console.error(`[INFO] Pausing debug session: ${args.sessionId}`);

    const response = await this.ideaClient.post('/api/v1/debug/session/pause', {
      sessionId: args.sessionId
    });

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Debug session paused successfully`);
      console.error(`[INFO] Status: ${data.status}`);
      if (data.currentLocation) {
        console.error(`[INFO] Current location: ${data.currentLocation.filePath}:${data.currentLocation.line}`);
      }
    } else {
      console.error(`[ERROR] Failed to pause debug session:`, response.error);
    }

    return response;
  }

  /**
   * 继续调试会话
   */
  private async resumeSession(args: any) {
    console.error(`[INFO] Resuming debug session: ${args.sessionId}`);

    const response = await this.ideaClient.post('/api/v1/debug/session/resume', {
      sessionId: args.sessionId
    });

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Debug session resumed successfully`);
      console.error(`[INFO] Status: ${data.status}`);
    } else {
      console.error(`[ERROR] Failed to resume debug session:`, response.error);
    }

    return response;
  }

  /**
   * 步过
   */
  private async stepOver(args: any) {
    console.error(`[INFO] Step over in session: ${args.sessionId}`);

    const response = await this.ideaClient.post('/api/v1/debug/session/step-over', {
      sessionId: args.sessionId
    });

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Step over executed successfully`);
      if (data.currentLocation) {
        console.error(`[INFO] Current location: ${data.currentLocation.filePath}:${data.currentLocation.line}`);
      }
    } else {
      console.error(`[ERROR] Failed to execute step over:`, response.error);
    }

    return response;
  }

  /**
   * 步入
   */
  private async stepInto(args: any) {
    console.error(`[INFO] Step into in session: ${args.sessionId}`);

    const response = await this.ideaClient.post('/api/v1/debug/session/step-into', {
      sessionId: args.sessionId
    });

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Step into executed successfully`);
      if (data.currentLocation) {
        console.error(`[INFO] Current location: ${data.currentLocation.filePath}:${data.currentLocation.line}`);
      }
    } else {
      console.error(`[ERROR] Failed to execute step into:`, response.error);
    }

    return response;
  }

  /**
   * 步出
   */
  private async stepOut(args: any) {
    console.error(`[INFO] Step out in session: ${args.sessionId}`);

    const response = await this.ideaClient.post('/api/v1/debug/session/step-out', {
      sessionId: args.sessionId
    });

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Step out executed successfully`);
      if (data.currentLocation) {
        console.error(`[INFO] Current location: ${data.currentLocation.filePath}:${data.currentLocation.line}`);
      }
    } else {
      console.error(`[ERROR] Failed to execute step out:`, response.error);
    }

    return response;
  }

  // ========== Phase 3.3: 表达式和变量 ==========

  /**
   * 计算表达式
   */
  private async evaluateExpression(args: any) {
    console.error(`[INFO] Evaluating expression in session: ${args.sessionId}`);
    console.error(`[INFO] Expression: ${args.expression}`);
    if (args.frameIndex !== undefined) {
      console.error(`[INFO] Frame index: ${args.frameIndex}`);
    }

    const response = await this.ideaClient.post('/api/v1/debug/evaluate', args);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Expression evaluated successfully`);
      console.error(`[INFO] Value: ${data.value}`);
      console.error(`[INFO] Type: ${data.type}`);
    } else {
      console.error(`[ERROR] Failed to evaluate expression:`, response.error);
    }

    return response;
  }

  /**
   * 获取变量列表
   */
  private async getVariables(args: any) {
    console.error(`[INFO] Getting variables in session: ${args.sessionId}`);
    if (args.frameIndex !== undefined) {
      console.error(`[INFO] Frame index: ${args.frameIndex}`);
    }
    if (args.scope) {
      console.error(`[INFO] Scope: ${args.scope}`);
    }

    // 构建查询参数
    const params = new URLSearchParams();
    params.append('sessionId', args.sessionId);
    if (args.frameIndex !== undefined) {
      params.append('frameIndex', args.frameIndex.toString());
    }
    if (args.scope) {
      params.append('scope', args.scope);
    }

    const response = await this.ideaClient.get(`/api/v1/debug/variables?${params.toString()}`);

    if (response.success) {
      const data = response.data;
      const variables = data.variables || [];
      console.error(`[INFO] Found ${variables.length} variable(s)`);

      if (variables.length > 0) {
        console.error(`[INFO] Variables:`);
        variables.forEach((v: any, index: number) => {
          console.error(`[INFO]   ${index + 1}. ${v.name} = ${v.value} (${v.type}) [${v.scope}]`);
          if (v.children && v.children.length > 0) {
            console.error(`[INFO]      Has ${v.children.length} child field(s)`);
          }
        });
      }
    } else {
      console.error(`[ERROR] Failed to get variables:`, response.error);
    }

    return response;
  }

  /**
   * 获取指定变量
   */
  private async getVariable(args: any) {
    console.error(`[INFO] Getting variable '${args.variableName}' in session: ${args.sessionId}`);

    const params = new URLSearchParams();
    params.append('sessionId', args.sessionId);

    const response = await this.ideaClient.get(`/api/v1/debug/variable/${args.variableName}?${params.toString()}`);

    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Variable retrieved successfully`);
      console.error(`[INFO] Name: ${data.name}`);
      console.error(`[INFO] Value: ${data.value}`);
      console.error(`[INFO] Type: ${data.type}`);
      console.error(`[INFO] Scope: ${data.scope}`);
      if (data.children && data.children.length > 0) {
        console.error(`[INFO] Has ${data.children.length} child field(s)`);
      }
    } else {
      console.error(`[ERROR] Failed to get variable:`, response.error);
    }

    return response;
  }

  // ========== Phase 3.4: 调用栈 ==========

  /**
   * 获取调用栈
   */
  private async getCallStack(args: any) {
    console.error(`[INFO] Getting call stack for session: ${args.sessionId}`);

    const params = new URLSearchParams();
    params.append('sessionId', args.sessionId);

    const response = await this.ideaClient.get(`/api/v1/debug/call-stack?${params.toString()}`);

    if (response.success) {
      const data = response.data;
      const frames = data.frames || [];
      console.error(`[INFO] Found ${frames.length} stack frame(s)`);

      if (frames.length > 0) {
        console.error(`[INFO] Call stack:`);
        frames.forEach((frame: any, index: number) => {
          console.error(`[INFO]   ${index}. ${frame.className}.${frame.methodName}()`);
          console.error(`[INFO]      at ${frame.location.filePath}:${frame.location.line}`);
          if (frame.variables && frame.variables.length > 0) {
            console.error(`[INFO]      Variables: ${frame.variables.length}`);
          }
        });
      }
    } else {
      console.error(`[ERROR] Failed to get call stack:`, response.error);
    }

    return response;
  }
}
