package com.ly.ideamcp.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.ly.ideamcp.startup.ServerStartupActivity

/**
 * 停止服务器 Action
 */
class StopServerAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val server = ServerStartupActivity.getServer()

        if (server == null || !server.isRunning()) {
            Messages.showInfoMessage(
                "Server is not running",
                "Server Status"
            )
            return
        }

        try {
            ServerStartupActivity.stopServer()

            Messages.showInfoMessage(
                "Server stopped successfully",
                "Server Stopped"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                "Failed to stop server: ${ex.message}",
                "Stop Server Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val server = ServerStartupActivity.getServer()
        e.presentation.isEnabled = server != null && server.isRunning()
    }
}
