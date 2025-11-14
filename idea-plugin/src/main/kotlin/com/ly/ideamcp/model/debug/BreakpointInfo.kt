package com.ly.ideamcp.model.debug

import com.ly.ideamcp.model.CodeLocation

/**
 * 断点信息
 * @property breakpointId 断点ID
 * @property location 断点位置
 * @property type 断点类型
 * @property condition 条件表达式(如有)
 * @property logMessage 日志消息(如有)
 * @property enabled 是否启用
 * @property verified 是否已验证(在调试会话中有效)
 */
data class BreakpointInfo(
    val breakpointId: String,
    val location: CodeLocation,
    val type: String = "line",
    val condition: String? = null,
    val logMessage: String? = null,
    val enabled: Boolean = true,
    val verified: Boolean = false
)
