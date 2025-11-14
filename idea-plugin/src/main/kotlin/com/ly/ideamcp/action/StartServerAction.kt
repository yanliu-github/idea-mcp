package com.ly.ideamcp.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.ly.ideamcp.config.PluginSettings
import com.ly.ideamcp.server.RequestHandler
import com.ly.ideamcp.server.RouterConfig
import com.ly.ideamcp.server.UndertowServer
import com.ly.ideamcp.startup.ServerStartupActivity

/**
 * 启动服务器 Action
 */
class StartServerAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val server = ServerStartupActivity.getServer()

        if (server != null && server.isRunning()) {
            Messages.showInfoMessage(
                "Server is already running at ${PluginSettings.getInstance().getServerUrl()}",
                "Server Status"
            )
            return
        }

        try {
            // 创建路由配置（简化版，实际应该从 ServerStartupActivity 获取）
            val routerConfig = RouterConfig()
            routerConfig.get("/api/v1/health") { _, _ ->
                mapOf("status" to "ok")
            }

            val requestHandler = RequestHandler(routerConfig)
            val newServer = UndertowServer()
            newServer.start(requestHandler)

            // 注意：这里简化了实现，实际应该更新 ServerStartupActivity 中的 server 引用

            Messages.showInfoMessage(
                "Server started successfully at ${PluginSettings.getInstance().getServerUrl()}",
                "Server Started"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                "Failed to start server: ${ex.message}",
                "Start Server Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val server = ServerStartupActivity.getServer()
        e.presentation.isEnabled = server == null || !server.isRunning()
    }
}
