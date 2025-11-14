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
