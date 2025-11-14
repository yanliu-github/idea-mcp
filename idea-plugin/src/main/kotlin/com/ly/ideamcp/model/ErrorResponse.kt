package com.ly.ideamcp.model

/**
 * 错误响应
 * @property error 错误信息
 */
data class ErrorResponse(
    val error: ErrorInfo
) : BaseResponse() {
    override val success: Boolean = false
}

/**
 * 错误信息
 * @property code 错误码
 * @property message 错误消息
 * @property details 错误详情（可选）
 */
data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)
