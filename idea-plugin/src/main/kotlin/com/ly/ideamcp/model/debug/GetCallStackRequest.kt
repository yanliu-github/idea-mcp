package com.ly.ideamcp.model.debug

/**
 * 获取调用栈请求
 * @property sessionId 会话ID
 */
data class GetCallStackRequest(
    val sessionId: String
) {
    init {
        require(sessionId.isNotBlank()) {
            "Session ID cannot be blank"
        }
    }
}
