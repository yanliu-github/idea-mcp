package com.ly.ideamcp.model.debug

/**
 * 调试控制请求
 * @property sessionId 会话ID
 * @property action 控制操作(pause, resume, step-over, step-into, step-out)
 */
data class DebugControlRequest(
    val sessionId: String,
    val action: String
) {
    init {
        require(sessionId.isNotBlank()) {
            "Session ID cannot be blank"
        }
        require(action in listOf("pause", "resume", "step-over", "step-into", "step-out")) {
            "Invalid action: $action"
        }
    }
}
