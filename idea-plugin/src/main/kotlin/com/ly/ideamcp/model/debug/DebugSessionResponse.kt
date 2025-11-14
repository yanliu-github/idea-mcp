package com.ly.ideamcp.model.debug

/**
 * 调试会话响应
 * @property success 是否成功
 * @property sessionId 会话ID
 * @property status 会话状态(running, paused, stopped)
 * @property breakpoints 当前设置的断点列表
 */
data class DebugSessionResponse(
    val success: Boolean,
    val sessionId: String,
    val status: String,
    val breakpoints: List<BreakpointInfo>? = null
)
