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

        // ========== 重构 API（占位符，后续实现）==========
        // router.post("/api/v1/refactor/rename") { exchange, project -> /* TODO */ }

        // ========== 导航 API（占位符，后续实现）==========
        // router.post("/api/v1/navigation/find-usages") { exchange, project -> /* TODO */ }
        // router.post("/api/v1/navigation/goto-definition") { exchange, project -> /* TODO */ }

        // ========== 分析 API（占位符，后续实现）==========
        // router.post("/api/v1/analysis/inspections") { exchange, project -> /* TODO */ }

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
