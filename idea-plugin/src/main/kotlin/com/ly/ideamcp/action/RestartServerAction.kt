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
 * 重启服务器 Action
 */
class RestartServerAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        try {
            // 停止当前服务器
            ServerStartupActivity.stopServer()

            // 等待一小段时间
            Thread.sleep(500)

            // 启动新服务器
            val routerConfig = RouterConfig()
            routerConfig.get("/api/v1/health") { _, _ ->
                mapOf("status" to "ok")
            }

            val requestHandler = RequestHandler(routerConfig)
            val newServer = UndertowServer()
            newServer.start(requestHandler)

            Messages.showInfoMessage(
                "Server restarted successfully at ${PluginSettings.getInstance().getServerUrl()}",
                "Server Restarted"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                "Failed to restart server: ${ex.message}",
                "Restart Server Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        // Always enabled
        e.presentation.isEnabled = true
    }
}
