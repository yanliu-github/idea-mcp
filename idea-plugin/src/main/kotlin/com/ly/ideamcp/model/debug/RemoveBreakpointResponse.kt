package com.ly.ideamcp.model.debug

/**
 * 删除断点响应
 * @property success 操作是否成功
 * @property breakpointId 被删除的断点ID
 * @property message 提示消息
 */
data class RemoveBreakpointResponse(
    val success: Boolean,
    val breakpointId: String,
    val message: String = "Breakpoint removed successfully"
)
