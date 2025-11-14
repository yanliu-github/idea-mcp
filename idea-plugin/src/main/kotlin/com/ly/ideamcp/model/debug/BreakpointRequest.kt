package com.ly.ideamcp.model.debug

/**
 * 断点设置请求
 * @property filePath 文件路径
 * @property line 断点行号(从0开始)
 * @property condition 条件表达式(可选)
 * @property logMessage 日志消息(可选,用于日志断点)
 * @property enabled 是否启用断点(默认true)
 */
data class BreakpointRequest(
    val filePath: String,
    val line: Int,
    val condition: String? = null,
    val logMessage: String? = null,
    val enabled: Boolean = true
) {
    init {
        require(filePath.isNotBlank()) {
            "File path cannot be blank"
        }
        require(line >= 0) {
            "Line number must be non-negative"
        }
    }
}
