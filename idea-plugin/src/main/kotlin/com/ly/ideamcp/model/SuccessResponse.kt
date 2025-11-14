package com.ly.ideamcp.model

/**
 * 成功响应
 * @param T 数据类型
 * @property data 响应数据
 */
data class SuccessResponse<T>(
    val data: T
) : BaseResponse() {
    override val success: Boolean = true
}
