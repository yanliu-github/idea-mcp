package com.ly.ideamcp.model.debug

import com.ly.ideamcp.model.CodeLocation

/**
 * 断点设置响应
 * @property success 操作是否成功
 * @property breakpointId 断点ID
 * @property location 断点位置信息
 * @property type 断点类型(line/exception/method等)
 * @property condition 条件表达式(如有)
 * @property enabled 是否启用
 */
data class BreakpointResponse(
    val success: Boolean,
    val breakpointId: String,
    val location: CodeLocation,
    val type: String = "line",
    val condition: String? = null,
    val enabled: Boolean = true
)
