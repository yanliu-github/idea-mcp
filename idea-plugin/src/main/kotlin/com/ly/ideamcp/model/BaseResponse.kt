package com.ly.ideamcp.model

import java.util.UUID

/**
 * 基础响应类
 * 所有 API 响应都继承此类
 */
abstract class BaseResponse {
    /**
     * 请求ID，用于追踪请求
     */
    val requestId: String = UUID.randomUUID().toString()

    /**
     * 响应时间戳（毫秒）
     */
    val timestamp: Long = System.currentTimeMillis()

    /**
     * 操作是否成功
     */
    abstract val success: Boolean
}
