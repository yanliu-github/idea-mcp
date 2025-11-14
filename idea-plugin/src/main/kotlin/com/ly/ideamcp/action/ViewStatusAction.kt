package com.ly.ideamcp.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.ly.ideamcp.config.PluginSettings
import com.ly.ideamcp.startup.ServerStartupActivity

/**
 * 查看服务器状态 Action
 */
class ViewStatusAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val server = ServerStartupActivity.getServer()
        val settings = PluginSettings.getInstance()

        val status = if (server != null && server.isRunning()) {
            val info = server.getListeningInfo()
            """
            Server Status: Running
            Host: ${info?.host ?: "N/A"}
            Port: ${info?.port ?: "N/A"}
            URL: ${info?.url ?: "N/A"}

            Settings:
            Auto Start: ${settings.autoStart}
            Auth Enabled: ${settings.enableAuth}
            CORS Enabled: ${settings.enableCors}
            Verbose Logging: ${settings.verboseLogging}
            """.trimIndent()
        } else {
            """
            Server Status: Stopped

            Settings:
            Port: ${settings.port}
            Host: ${settings.host}
            Auto Start: ${settings.autoStart}
            Auth Enabled: ${settings.enableAuth}
            """.trimIndent()
        }

        Messages.showInfoMessage(
            status,
            "IDEA MCP Server Status"
        )
    }

    override fun update(e: AnActionEvent) {
        // Always enabled
        e.presentation.isEnabled = true
    }
}
