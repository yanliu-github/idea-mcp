package com.ly.ideamcp.model.debug

/**
 * 表达式求值响应
 * @property success 是否成功
 * @property value 计算结果值
 * @property type 结果类型
 * @property error 错误信息(如果求值失败)
 */
data class EvaluateExpressionResponse(
    val success: Boolean,
    val value: String? = null,
    val type: String? = null,
    val error: String? = null
)
