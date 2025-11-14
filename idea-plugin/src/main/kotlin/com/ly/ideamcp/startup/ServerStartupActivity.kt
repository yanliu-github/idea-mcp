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
