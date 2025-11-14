package com.ly.ideamcp.model.debug

/**
 * 表达式求值请求
 * @property sessionId 会话ID
 * @property expression 要计算的表达式
 * @property frameIndex 栈帧索引(可选,默认为0,即当前栈帧)
 */
data class EvaluateExpressionRequest(
    val sessionId: String,
    val expression: String,
    val frameIndex: Int? = 0
) {
    init {
        require(sessionId.isNotBlank()) {
            "Session ID cannot be blank"
        }
        require(expression.isNotBlank()) {
            "Expression cannot be blank"
        }
        frameIndex?.let {
            require(it >= 0) {
                "Frame index must be non-negative"
            }
        }
    }
}
