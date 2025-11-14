package com.ly.ideamcp.model.debug

import com.ly.ideamcp.model.CodeLocation

/**
 * 调试控制响应
 * @property success 是否成功
 * @property sessionId 会话ID
 * @property status 当前状态(running, paused, stopped)
 * @property currentLocation 当前执行位置(仅在paused时有效)
 */
data class DebugControlResponse(
    val success: Boolean,
    val sessionId: String,
    val status: String,
    val currentLocation: CodeLocation? = null
)
