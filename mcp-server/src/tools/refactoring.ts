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
            filePath: { type: 'string', description: '文件路径(项目相对路径,例如: src/main/java/User.java)' },
            offset: { type: 'number', description: '符号在文件中的偏移量(从0开始的字符位置)' },
            line: { type: 'number', description: '符号所在行号(从0开始,与offset二选一)' },
            column: { type: 'number', description: '符号所在列号(从0开始,必须和line一起使用)' },
            newName: { type: 'string', description: '新名称(符合编程语言命名规范)' },
            searchInComments: { type: 'boolean', description: '是否在注释中搜索并替换(默认false)', default: false },
            searchInStrings: { type: 'boolean', description: '是否在字符串中搜索并替换(默认false)', default: false },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'newName'],
        },
      },
      {
        name: 'refactor_extract_method',
        description: '提取选中代码为独立方法。使用 IDEA 的提取方法功能,自动处理参数和返回值。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            startOffset: { type: 'number', description: '起始偏移量(从0开始的字符位置)' },
            endOffset: { type: 'number', description: '结束偏移量' },
            startLine: { type: 'number', description: '起始行号(从0开始,与startOffset二选一)' },
            startColumn: { type: 'number', description: '起始列号(从0开始)' },
            endLine: { type: 'number', description: '结束行号(从0开始)' },
            endColumn: { type: 'number', description: '结束列号(从0开始)' },
            methodName: { type: 'string', description: '新方法名称' },
            visibility: { type: 'string', description: '可见性(public/protected/private/package,默认private)', enum: ['public', 'protected', 'private', 'package'], default: 'private' },
            isStatic: { type: 'boolean', description: '是否静态方法(默认false)', default: false },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'methodName'],
        },
      },
      {
        name: 'refactor_extract_variable',
        description: '提取选中表达式为局部变量。使用 IDEA 的提取变量功能,自动推断类型。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            startOffset: { type: 'number', description: '起始偏移量' },
            endOffset: { type: 'number', description: '结束偏移量' },
            startLine: { type: 'number', description: '起始行号(从0开始,与startOffset二选一)' },
            startColumn: { type: 'number', description: '起始列号' },
            endLine: { type: 'number', description: '结束行号' },
            endColumn: { type: 'number', description: '结束列号' },
            variableName: { type: 'string', description: '新变量名称' },
            replaceAll: { type: 'boolean', description: '是否替换所有相同表达式(默认true)', default: true },
            declareFinal: { type: 'boolean', description: '是否声明为final(默认true)', default: true },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'variableName'],
        },
      },
      {
        name: 'refactor_inline_variable',
        description: '内联变量,将变量的所有使用替换为其初始化表达式。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径(项目相对路径)' },
            offset: { type: 'number', description: '变量声明的偏移量' },
            line: { type: 'number', description: '变量所在行号(从0开始,与offset二选一)' },
            column: { type: 'number', description: '变量所在列号' },
            inlineAll: { type: 'boolean', description: '是否内联所有引用(默认true)', default: true },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'refactor_change_signature',
        description: '改变方法签名,包括方法名、参数列表、返回类型和可见性。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径' },
            offset: { type: 'number', description: '方法的偏移量' },
            line: { type: 'number', description: '方法所在行号(与offset二选一)' },
            column: { type: 'number', description: '方法所在列号' },
            newName: { type: 'string', description: '新方法名(可选)' },
            newReturnType: { type: 'string', description: '新返回类型(可选)' },
            newVisibility: { type: 'string', description: '新可见性(可选)' },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'refactor_move',
        description: '移动类、方法或字段到新位置。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '源文件路径' },
            offset: { type: 'number', description: '要移动的元素偏移量' },
            line: { type: 'number', description: '元素所在行号(与offset二选一)' },
            column: { type: 'number', description: '元素所在列号' },
            targetPath: { type: 'string', description: '目标路径(包名或文件路径)' },
            searchInComments: { type: 'boolean', description: '是否搜索注释(默认false)', default: false },
            searchInStrings: { type: 'boolean', description: '是否搜索字符串(默认false)', default: false },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'targetPath'],
        },
      },
      {
        name: 'refactor_extract_interface',
        description: '从类中提取接口。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径' },
            offset: { type: 'number', description: '类的偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            interfaceName: { type: 'string', description: '新接口名称' },
            targetPackage: { type: 'string', description: '目标包名(可选)' },
            members: { type: 'array', items: { type: 'string' }, description: '要提取的成员列表(方法名)' },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'interfaceName'],
        },
      },
      {
        name: 'refactor_extract_superclass',
        description: '从类中提取超类。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径' },
            offset: { type: 'number', description: '类的偏移量' },
            line: { type: 'number', description: '类所在行号(与offset二选一)' },
            column: { type: 'number', description: '类所在列号' },
            superclassName: { type: 'string', description: '新超类名称' },
            targetPackage: { type: 'string', description: '目标包名(可选)' },
            members: { type: 'array', items: { type: 'string' }, description: '要提取的成员列表(字段和方法名)' },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'superclassName'],
        },
      },
      {
        name: 'refactor_encapsulate_field',
        description: '封装字段,生成getter/setter方法。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径' },
            offset: { type: 'number', description: '字段的偏移量' },
            line: { type: 'number', description: '字段所在行号(与offset二选一)' },
            column: { type: 'number', description: '字段所在列号' },
            generateGetter: { type: 'boolean', description: '是否生成getter(默认true)', default: true },
            generateSetter: { type: 'boolean', description: '是否生成setter(默认true)', default: true },
            getterVisibility: { type: 'string', description: 'getter可见性(默认public)', default: 'public' },
            setterVisibility: { type: 'string', description: 'setter可见性(默认public)', default: 'public' },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath'],
        },
      },
      {
        name: 'refactor_introduce_parameter_object',
        description: '引入参数对象,将多个参数合并为一个对象。',
        inputSchema: {
          type: 'object',
          properties: {
            filePath: { type: 'string', description: '文件路径' },
            offset: { type: 'number', description: '方法的偏移量' },
            line: { type: 'number', description: '方法所在行号(与offset二选一)' },
            column: { type: 'number', description: '方法所在列号' },
            className: { type: 'string', description: '参数对象类名' },
            packageName: { type: 'string', description: '包名(可选)' },
            parameters: { type: 'array', items: { type: 'number' }, description: '要合并的参数索引列表' },
            keepParameters: { type: 'boolean', description: '是否保留原参数(默认false)', default: false },
            preview: { type: 'boolean', description: '是否仅预览不执行(默认false)', default: false },
          },
          required: ['filePath', 'className'],
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
      case 'refactor_rename': return this.rename(args);
      case 'refactor_extract_method': return this.extractMethod(args);
      case 'refactor_extract_variable': return this.extractVariable(args);
      case 'refactor_inline_variable': return this.inlineVariable(args);
      case 'refactor_change_signature': return this.changeSignature(args);
      case 'refactor_move': return this.move(args);
      case 'refactor_extract_interface': return this.extractInterface(args);
      case 'refactor_extract_superclass': return this.extractSuperclass(args);
      case 'refactor_encapsulate_field': return this.encapsulateField(args);
      case 'refactor_introduce_parameter_object': return this.introduceParameterObject(args);
      default:
        throw new Error(`Unknown refactoring tool: ${toolName}`);
    }
  }

  private async rename(args: any) {
    console.error(`[INFO] Renaming symbol in ${args.filePath} to "${args.newName}"`);
    const response = await this.ideaClient.post('/api/v1/refactor/rename', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Rename completed successfully`);
      console.error(`[INFO] Old name: ${data.oldName || 'N/A'}`);
      console.error(`[INFO] New name: ${data.newName}`);
      console.error(`[INFO] Affected files: ${data.affectedFiles?.length || 0}`);
    } else {
      console.error(`[ERROR] Rename failed:`, response.error);
    }
    return response;
  }

  private async extractMethod(args: any) {
    console.error(`[INFO] Extracting method "${args.methodName}" in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/extract-method', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Extract method completed successfully`);
      console.error(`[INFO] Method name: ${data.methodName}`);
      console.error(`[INFO] Method signature: ${data.methodSignature || 'N/A'}`);
    } else {
      console.error(`[ERROR] Extract method failed:`, response.error);
    }
    return response;
  }

  private async extractVariable(args: any) {
    console.error(`[INFO] Extracting variable "${args.variableName}" in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/extract-variable', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Extract variable completed successfully`);
      console.error(`[INFO] Variable name: ${data.variableName}`);
      console.error(`[INFO] Replacement count: ${data.replacementCount || 0}`);
    } else {
      console.error(`[ERROR] Extract variable failed:`, response.error);
    }
    return response;
  }

  private async inlineVariable(args: any) {
    console.error(`[INFO] Inlining variable in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/inline-variable', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Inline variable completed successfully`);
      console.error(`[INFO] Variable name: ${data.variableName}`);
      console.error(`[INFO] Replacement count: ${data.replacementCount || 0}`);
    } else {
      console.error(`[ERROR] Inline variable failed:`, response.error);
    }
    return response;
  }

  private async changeSignature(args: any) {
    console.error(`[INFO] Changing signature in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/change-signature', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Change signature completed successfully`);
      console.error(`[INFO] Method name: ${data.methodName}`);
      console.error(`[INFO] Old signature: ${data.oldSignature || 'N/A'}`);
      console.error(`[INFO] New signature: ${data.newSignature || 'N/A'}`);
    } else {
      console.error(`[ERROR] Change signature failed:`, response.error);
    }
    return response;
  }

  private async move(args: any) {
    console.error(`[INFO] Moving element in ${args.filePath} to ${args.targetPath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/move', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Move completed successfully`);
      console.error(`[INFO] Element name: ${data.elementName}`);
      console.error(`[INFO] Affected files: ${data.affectedFiles?.length || 0}`);
    } else {
      console.error(`[ERROR] Move failed:`, response.error);
    }
    return response;
  }

  private async extractInterface(args: any) {
    console.error(`[INFO] Extracting interface "${args.interfaceName}" in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/extract-interface', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Extract interface completed successfully`);
      console.error(`[INFO] Interface name: ${data.interfaceName}`);
      console.error(`[INFO] Extracted members: ${data.extractedMembers?.length || 0}`);
    } else {
      console.error(`[ERROR] Extract interface failed:`, response.error);
    }
    return response;
  }

  private async extractSuperclass(args: any) {
    console.error(`[INFO] Extracting superclass "${args.superclassName}" in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/extract-superclass', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Extract superclass completed successfully`);
      console.error(`[INFO] Superclass name: ${data.superclassName}`);
      console.error(`[INFO] Extracted members: ${data.extractedMembers?.length || 0}`);
    } else {
      console.error(`[ERROR] Extract superclass failed:`, response.error);
    }
    return response;
  }

  private async encapsulateField(args: any) {
    console.error(`[INFO] Encapsulating field in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/encapsulate-field', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Encapsulate field completed successfully`);
      console.error(`[INFO] Field name: ${data.fieldName}`);
      console.error(`[INFO] Getter: ${data.getterName || 'N/A'}`);
      console.error(`[INFO] Setter: ${data.setterName || 'N/A'}`);
    } else {
      console.error(`[ERROR] Encapsulate field failed:`, response.error);
    }
    return response;
  }

  private async introduceParameterObject(args: any) {
    console.error(`[INFO] Introducing parameter object "${args.className}" in ${args.filePath}`);
    const response = await this.ideaClient.post('/api/v1/refactor/introduce-parameter-object', args);
    if (response.success) {
      const data = response.data;
      console.error(`[INFO] Introduce parameter object completed successfully`);
      console.error(`[INFO] Class name: ${data.className}`);
      console.error(`[INFO] Method: ${data.methodName}`);
    } else {
      console.error(`[ERROR] Introduce parameter object failed:`, response.error);
    }
    return response;
  }
}
