package com.ly.ideamcp.model.debug

/**
 * 列出断点响应
 * @property success 操作是否成功
 * @property breakpoints 断点列表
 */
data class ListBreakpointsResponse(
    val success: Boolean,
    val breakpoints: List<BreakpointInfo>
)
