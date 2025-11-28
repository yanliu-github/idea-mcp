package com.ly.ideamcp.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ly.ideamcp.config.PluginSettings
import com.ly.ideamcp.server.RequestHandler
import com.ly.ideamcp.server.RouteRegistrar
import com.ly.ideamcp.server.RouterConfig
import com.ly.ideamcp.server.UndertowServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 服务器启动活动
 * 在项目打开时自动启动 HTTP Server
 */
class ServerStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(ServerStartupActivity::class.java)

    companion object {
        @Volatile
        private var server: UndertowServer? = null

        // 使用协程友好的 Mutex 替代 synchronized
        private val mutex = Mutex()

        /**
         * 获取服务器实例
         */
        fun getServer(): UndertowServer? = server

        /**
         * 停止服务器（同步版本，用于非协程上下文）
         */
        fun stopServer() {
            val currentServer = server
            if (currentServer != null) {
                currentServer.stop()
                server = null
            }
        }
    }

    override suspend fun execute(project: Project) {
        val settings = PluginSettings.getInstance()

        // 检查是否启用自动启动
        if (!settings.autoStart) {
            logger.info("Auto-start is disabled, skipping server startup")
            return
        }

        // 使用协程友好的 Mutex 进行同步
        mutex.withLock {
            if (server != null && server!!.isRunning()) {
                logger.info("Server is already running, skipping startup")
                return
            }

            try {
                // 在后台线程中启动服务器，避免阻塞协程
                withContext(Dispatchers.IO) {
                    startServer()
                }
            } catch (e: Exception) {
                logger.error("Failed to start server on project startup", e)
            }
        }
    }

    /**
     * 启动服务器
     */
    private fun startServer() {
        logger.info("Starting MCP Server on project startup")

        // 创建路由配置
        val routerConfig = RouterConfig()

        // 使用 RouteRegistrar 注册所有路由
        RouteRegistrar.registerAll(routerConfig)

        // 创建请求处理器
        val requestHandler = RequestHandler(routerConfig)

        // 创建并启动服务器
        val newServer = UndertowServer()
        newServer.start(requestHandler)

        server = newServer

        logger.info("MCP Server started successfully")
    }
}
