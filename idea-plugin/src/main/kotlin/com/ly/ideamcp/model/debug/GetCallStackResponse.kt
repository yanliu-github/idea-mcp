package com.ly.ideamcp.model.debug

/**
 * 获取调用栈响应
 * @property success 是否成功
 * @property frames 栈帧列表
 */
data class GetCallStackResponse(
    val success: Boolean,
    val frames: List<StackFrame>
)
