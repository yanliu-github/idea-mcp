package com.ly.ideamcp.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.ly.ideamcp.config.PluginSettings
import com.ly.ideamcp.server.RequestHandler
import com.ly.ideamcp.server.RouterConfig
import com.ly.ideamcp.server.UndertowServer

/**
 * 服务器启动活动
 * 在项目打开时自动启动 HTTP Server
 */
class ServerStartupActivity : StartupActivity {
    private val logger = Logger.getInstance(ServerStartupActivity::class.java)

    companion object {
        @Volatile
        private var server: UndertowServer? = null

        private val lock = Any()

        /**
         * 获取服务器实例
         */
        fun getServer(): UndertowServer? = server

        /**
         * 停止服务器
         */
        fun stopServer() {
            synchronized(lock) {
                server?.stop()
                server = null
            }
        }
    }

    override fun runActivity(project: Project) {
        val settings = PluginSettings.getInstance()

        // 检查是否启用自动启动
        if (!settings.autoStart) {
            logger.info("Auto-start is disabled, skipping server startup")
            return
        }

        // 只启动一次服务器（跨所有项目共享）
        synchronized(lock) {
            if (server != null && server!!.isRunning()) {
                logger.info("Server is already running, skipping startup")
                return
            }

            try {
                startServer()
            } catch (e: Exception) {
                logger.error("Failed to start server on project startup", e)
            }
        }
    }

    /**
     * 启动服务器
     */
    private fun startServer() {
        logger.info("Starting IDEA MCP Server on project startup")

        // 创建路由配置
        val routerConfig = createRouterConfig()

        // 创建请求处理器
        val requestHandler = RequestHandler(routerConfig)

        // 创建并启动服务器
        val newServer = UndertowServer()
        newServer.start(requestHandler)

        server = newServer

        logger.info("IDEA MCP Server started successfully")
    }

    /**
     * 创建路由配置
     * 注册所有 API 端点
     */
    private fun createRouterConfig(): RouterConfig {
        val router = RouterConfig()

        // ========== 系统管理 API ==========

        // 健康检查
        router.get("/api/v1/health") { _, _ ->
            mapOf(
                "status" to "ok",
                "ideaVersion" to getIdeaVersion(),
                "indexReady" to isIndexReady()
            )
        }

        // 项目信息
        router.get("/api/v1/project/info") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            mapOf(
                "name" to project.name,
                "path" to project.basePath,
                "modules" to getProjectModules(project)
            )
        }

        // ========== 重构 API ==========

        // 重命名符号
        router.post("/api/v1/refactor/rename") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val renameRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.RenameRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.renameSymbol(renameRequest)
        }

        // 提取方法
        router.post("/api/v1/refactor/extract-method") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val extractMethodRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.ExtractMethodRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.extractMethod(extractMethodRequest)
        }

        // 提取变量
        router.post("/api/v1/refactor/extract-variable") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val extractVariableRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.ExtractVariableRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.extractVariable(extractVariableRequest)
        }

        // 内联变量
        router.post("/api/v1/refactor/inline-variable") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val inlineVariableRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.InlineVariableRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.inlineVariable(inlineVariableRequest)
        }

        // 改变签名
        router.post("/api/v1/refactor/change-signature") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val changeSignatureRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.ChangeSignatureRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.changeSignature(changeSignatureRequest)
        }

        // 移动类/方法
        router.post("/api/v1/refactor/move") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val moveRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.MoveRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.move(moveRequest)
        }

        // 提取接口
        router.post("/api/v1/refactor/extract-interface") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val extractInterfaceRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.ExtractInterfaceRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.extractInterface(extractInterfaceRequest)
        }

        // 提取超类
        router.post("/api/v1/refactor/extract-superclass") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val extractSuperclassRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.ExtractSuperclassRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.extractSuperclass(extractSuperclassRequest)
        }

        // 封装字段
        router.post("/api/v1/refactor/encapsulate-field") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val encapsulateFieldRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.EncapsulateFieldRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.encapsulateField(encapsulateFieldRequest)
        }

        // 引入参数对象
        router.post("/api/v1/refactor/introduce-parameter-object") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val introduceParameterObjectRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.refactor.IntroduceParameterObjectRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val refactoringService = com.ly.ideamcp.service.RefactoringService.getInstance(project)
            refactoringService.introduceParameterObject(introduceParameterObjectRequest)
        }

        // ========== 导航 API ==========

        // 查找用途
        router.post("/api/v1/navigation/find-usages") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val findUsagesRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.navigation.FindUsagesRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val navigationService = com.ly.ideamcp.service.NavigationService.getInstance(project)
            navigationService.findUsages(findUsagesRequest)
        }

        // 跳转到定义
        router.post("/api/v1/navigation/goto-definition") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val gotoDefinitionRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.navigation.GotoDefinitionRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val navigationService = com.ly.ideamcp.service.NavigationService.getInstance(project)
            navigationService.gotoDefinition(gotoDefinitionRequest)
        }

        // ========== 分析 API ==========

        // 运行代码检查
        router.post("/api/v1/analysis/inspections") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val inspectionRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.analysis.InspectionRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val analysisService = com.ly.ideamcp.service.AnalysisService.getInstance(project)
            analysisService.runInspections(inspectionRequest)
        }

        // ========== 调试 API ==========

        // 设置断点
        router.post("/api/v1/debug/breakpoint") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val breakpointRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.debug.BreakpointRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.setBreakpoint(breakpointRequest)
        }

        // 列出所有断点
        router.get("/api/v1/debug/breakpoints") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.listBreakpoints()
        }

        // 删除断点
        router.delete("/api/v1/debug/breakpoint/{id}") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            // 从路径参数提取断点ID
            val path = exchange.requestPath
            val breakpointId = path.substringAfterLast("/")

            if (breakpointId.isBlank()) {
                throw IllegalArgumentException("Breakpoint ID is required")
            }

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.removeBreakpoint(breakpointId)
        }

        // ========== Phase 3.2: 调试会话管理 API ==========

        // 启动调试会话
        router.post("/api/v1/debug/session/start") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val debugSessionRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.debug.DebugSessionRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.startDebugSession(debugSessionRequest)
        }

        // 停止调试会话
        router.post("/api/v1/debug/session/stop") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            // 从请求体提取 sessionId
            val requestHandler = RequestHandler(router)
            val body = requestHandler.parseRequestBody(exchange, Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Missing request body")

            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.stopDebugSession(sessionId)
        }

        // 暂停调试会话
        router.post("/api/v1/debug/session/pause") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val body = requestHandler.parseRequestBody(exchange, Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Missing request body")

            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.pauseDebugSession(sessionId)
        }

        // 继续调试会话
        router.post("/api/v1/debug/session/resume") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val body = requestHandler.parseRequestBody(exchange, Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Missing request body")

            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.resumeDebugSession(sessionId)
        }

        // 步过
        router.post("/api/v1/debug/session/step-over") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val body = requestHandler.parseRequestBody(exchange, Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Missing request body")

            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.stepOver(sessionId)
        }

        // 步入
        router.post("/api/v1/debug/session/step-into") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val body = requestHandler.parseRequestBody(exchange, Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Missing request body")

            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.stepInto(sessionId)
        }

        // 步出
        router.post("/api/v1/debug/session/step-out") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val body = requestHandler.parseRequestBody(exchange, Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Missing request body")

            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.stepOut(sessionId)
        }

        // ========== Phase 3.3: 表达式和变量 API ==========

        // 计算表达式
        router.post("/api/v1/debug/evaluate") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val evaluateRequest = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.debug.EvaluateExpressionRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.evaluateExpression(evaluateRequest)
        }

        // 获取变量列表
        router.get("/api/v1/debug/variables") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            // 从查询参数解析请求
            val queryParams = exchange.queryParameters
            val sessionId = queryParams["sessionId"]?.firstOrNull()
                ?: throw IllegalArgumentException("Session ID is required")

            val frameIndex = queryParams["frameIndex"]?.firstOrNull()?.toIntOrNull() ?: 0
            val scope = queryParams["scope"]?.firstOrNull() ?: "local"

            val getVariablesRequest = com.ly.ideamcp.model.debug.GetVariablesRequest(
                sessionId = sessionId,
                frameIndex = frameIndex,
                scope = scope
            )

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.getVariables(getVariablesRequest)
        }

        // 获取指定变量
        router.get("/api/v1/debug/variable/{name}") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            // 从路径参数提取变量名
            val path = exchange.requestPath
            val variableName = path.substringAfterLast("/")

            if (variableName.isBlank()) {
                throw IllegalArgumentException("Variable name is required")
            }

            // 从查询参数获取 sessionId
            val queryParams = exchange.queryParameters
            val sessionId = queryParams["sessionId"]?.firstOrNull()
                ?: throw IllegalArgumentException("Session ID is required")

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.getVariable(sessionId, variableName)
        }

        // ========== Phase 3.4: 调用栈 API ==========

        // 获取调用栈
        router.get("/api/v1/debug/call-stack") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            // 从查询参数获取 sessionId
            val queryParams = exchange.queryParameters
            val sessionId = queryParams["sessionId"]?.firstOrNull()
                ?: throw IllegalArgumentException("Session ID is required")

            val getCallStackRequest = com.ly.ideamcp.model.debug.GetCallStackRequest(sessionId = sessionId)

            val debugService = com.ly.ideamcp.service.DebugService.getInstance(project)
            debugService.getCallStack(getCallStackRequest)
        }

        // ========== Phase 4.1: 代码生成 API ==========

        // 生成 Getter/Setter
        router.post("/api/v1/codegen/getters-setters") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.codegen.GenerateGettersSettersRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val codegenService = com.ly.ideamcp.service.CodeGenerationService.getInstance(project)
            codegenService.generateGettersSetters(request)
        }

        // 生成构造函数
        router.post("/api/v1/codegen/constructor") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.codegen.GenerateConstructorRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val codegenService = com.ly.ideamcp.service.CodeGenerationService.getInstance(project)
            codegenService.generateConstructor(request)
        }

        // 生成 toString
        router.post("/api/v1/codegen/tostring") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.codegen.GenerateMethodRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val codegenService = com.ly.ideamcp.service.CodeGenerationService.getInstance(project)
            codegenService.generateToString(request)
        }

        // 生成 equals
        router.post("/api/v1/codegen/equals") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.codegen.GenerateMethodRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val codegenService = com.ly.ideamcp.service.CodeGenerationService.getInstance(project)
            codegenService.generateEquals(request)
        }

        // 生成 hashCode
        router.post("/api/v1/codegen/hashcode") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.codegen.GenerateMethodRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val codegenService = com.ly.ideamcp.service.CodeGenerationService.getInstance(project)
            codegenService.generateHashCode(request)
        }

        // 重写方法
        router.post("/api/v1/codegen/override-method") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.codegen.OverrideMethodRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val codegenService = com.ly.ideamcp.service.CodeGenerationService.getInstance(project)
            codegenService.overrideMethod(request)
        }

        // ========== Phase 4.2: 搜索 API ==========

        // 全局搜索
        router.post("/api/v1/search/global") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.search.GlobalSearchRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val searchService = com.ly.ideamcp.service.SearchService.getInstance(project)
            searchService.globalSearch(request)
        }

        // 文本搜索
        router.post("/api/v1/search/text") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.search.TextSearchRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val searchService = com.ly.ideamcp.service.SearchService.getInstance(project)
            searchService.textSearch(request)
        }

        // 结构化搜索
        router.post("/api/v1/search/structural") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.search.StructuralSearchRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val searchService = com.ly.ideamcp.service.SearchService.getInstance(project)
            searchService.structuralSearch(request)
        }

        // ========== Phase 4.3: 导航增强 API ==========

        // 类型层次
        router.post("/api/v1/navigation/type-hierarchy") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.navigation.TypeHierarchyRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val navigationService = com.ly.ideamcp.service.NavigationService.getInstance(project)
            navigationService.showTypeHierarchy(request)
        }

        // 调用层次
        router.post("/api/v1/navigation/call-hierarchy") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.navigation.CallHierarchyRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val navigationService = com.ly.ideamcp.service.NavigationService.getInstance(project)
            navigationService.showCallHierarchy(request)
        }

        // 查找实现
        router.post("/api/v1/navigation/find-implementations") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.navigation.FindImplementationsRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val navigationService = com.ly.ideamcp.service.NavigationService.getInstance(project)
            navigationService.findImplementations(request)
        }

        // ========== Phase 4.4: 依赖分析 API ==========

        // 分析依赖
        router.post("/api/v1/analysis/dependencies") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.dependency.AnalyzeDependenciesRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val dependencyService = com.ly.ideamcp.service.DependencyService.getInstance(project)
            dependencyService.analyzeDependencies(request)
        }

        // 检测循环依赖
        router.post("/api/v1/analysis/dependency-cycles") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.dependency.DetectCyclesRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val dependencyService = com.ly.ideamcp.service.DependencyService.getInstance(project)
            dependencyService.detectCycles(request)
        }

        // ========== Phase 4.5: 版本控制 API ==========

        // 获取 VCS 状态
        router.get("/api/v1/vcs/status") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val vcsService = com.ly.ideamcp.service.VcsService.getInstance(project)
            vcsService.getStatus()
        }

        // 获取文件历史
        router.post("/api/v1/vcs/history") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.vcs.VcsHistoryRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val vcsService = com.ly.ideamcp.service.VcsService.getInstance(project)
            vcsService.getHistory(request)
        }

        // 获取文件差异
        router.post("/api/v1/vcs/diff") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.vcs.VcsDiffRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val vcsService = com.ly.ideamcp.service.VcsService.getInstance(project)
            vcsService.getDiff(request)
        }

        // 获取文件 blame
        router.post("/api/v1/vcs/blame") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.vcs.VcsBlameRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val vcsService = com.ly.ideamcp.service.VcsService.getInstance(project)
            vcsService.blame(request)
        }

        // ========== Phase 4.6: 项目管理 API ==========

        // 获取项目结构
        router.get("/api/v1/project/structure") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val projectService = com.ly.ideamcp.service.ProjectService.getInstance(project)
            projectService.getStructure()
        }

        // 获取模块列表
        router.get("/api/v1/project/modules") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val projectService = com.ly.ideamcp.service.ProjectService.getInstance(project)
            projectService.getModules()
        }

        // 获取库列表
        router.get("/api/v1/project/libraries") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val projectService = com.ly.ideamcp.service.ProjectService.getInstance(project)
            projectService.getLibraries()
        }

        // 获取构建配置
        router.get("/api/v1/project/build-config") { _, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val projectService = com.ly.ideamcp.service.ProjectService.getInstance(project)
            projectService.getBuildConfig()
        }

        // ========== Phase 4.7: 工具 API ==========

        // 格式化代码
        router.post("/api/v1/tools/format") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.tools.FormatCodeRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val toolsService = com.ly.ideamcp.service.ToolsService.getInstance(project)
            toolsService.formatCode(request)
        }

        // 优化导入
        router.post("/api/v1/tools/optimize-imports") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.tools.OptimizeImportsRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val toolsService = com.ly.ideamcp.service.ToolsService.getInstance(project)
            toolsService.optimizeImports(request)
        }

        // 应用快速修复
        router.post("/api/v1/tools/quick-fix") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.tools.QuickFixRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val toolsService = com.ly.ideamcp.service.ToolsService.getInstance(project)
            toolsService.applyQuickFix(request)
        }

        // 应用 Intention
        router.post("/api/v1/tools/intention") { exchange, project ->
            if (project == null) {
                throw IllegalStateException("No active project")
            }

            val requestHandler = RequestHandler(router)
            val request = requestHandler.parseRequestBody(exchange, com.ly.ideamcp.model.tools.IntentionRequest::class.java)
                ?: throw IllegalArgumentException("Missing request body")

            val toolsService = com.ly.ideamcp.service.ToolsService.getInstance(project)
            toolsService.applyIntention(request)
        }

        return router
    }

    /**
     * 获取 IDEA 版本
     */
    private fun getIdeaVersion(): String {
        return try {
            com.intellij.openapi.application.ApplicationInfo.getInstance().fullVersion
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 检查索引是否就绪
     */
    private fun isIndexReady(): Boolean {
        return try {
            !com.intellij.openapi.project.DumbService.isDumb(
                com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
                    ?: return false
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取项目模块列表
     */
    private fun getProjectModules(project: Project): List<String> {
        return try {
            com.intellij.openapi.module.ModuleManager.getInstance(project)
                .modules.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
