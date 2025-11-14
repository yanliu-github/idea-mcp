package com.ly.ideamcp.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ly.ideamcp.model.*

/**
 * 响应序列化工具
 * 将响应对象序列化为 JSON
 */
object ResponseSerializer {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    /**
     * 序列化成功响应
     * @param data 响应数据
     * @return JSON 字符串
     */
    fun <T> serializeSuccess(data: T): String {
        val response = SuccessResponse(data)
        return gson.toJson(response)
    }

    /**
     * 序列化错误响应
     * @param errorCode 错误码
     * @param message 错误消息（可选，如果为空使用默认消息）
     * @param details 错误详情（可选）
     * @return JSON 字符串
     */
    fun serializeError(
        errorCode: ErrorCode,
        message: String? = null,
        details: Map<String, Any>? = null
    ): String {
        val errorInfo = ErrorInfo(
            code = errorCode.code,
            message = message ?: errorCode.defaultMessage,
            details = details
        )
        val response = ErrorResponse(errorInfo)
        return gson.toJson(response)
    }

    /**
     * 序列化错误响应（使用错误码字符串）
     * @param errorCode 错误码字符串
     * @param message 错误消息
     * @param details 错误详情（可选）
     * @return JSON 字符串
     */
    fun serializeError(
        errorCode: String,
        message: String,
        details: Map<String, Any>? = null
    ): String {
        val errorInfo = ErrorInfo(
            code = errorCode,
            message = message,
            details = details
        )
        val response = ErrorResponse(errorInfo)
        return gson.toJson(response)
    }

    /**
     * 序列化异常为错误响应
     * @param exception 异常
     * @param errorCode 错误码（可选，默认为 INTERNAL_ERROR）
     * @return JSON 字符串
     */
    fun serializeException(
        exception: Throwable,
        errorCode: ErrorCode = ErrorCode.INTERNAL_ERROR
    ): String {
        val details = mapOf(
            "exception" to (exception.javaClass.simpleName ?: "Unknown"),
            "message" to (exception.message ?: "No message"),
            "stackTrace" to exception.stackTrace.take(5).map { it.toString() }
        )

        return serializeError(
            errorCode = errorCode,
            message = exception.message ?: errorCode.defaultMessage,
            details = details
        )
    }

    /**
     * 将对象序列化为 JSON
     * @param obj 对象
     * @return JSON 字符串
     */
    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    /**
     * 将 JSON 反序列化为对象
     * @param json JSON 字符串
     * @param clazz 目标类
     * @return 对象
     */
    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    /**
     * 创建格式化的 JSON 响应
     * @param success 是否成功
     * @param data 数据（成功时使用）
     * @param error 错误信息（失败时使用）
     * @return JSON 字符串
     */
    fun createResponse(
        success: Boolean,
        data: Any? = null,
        error: ErrorInfo? = null
    ): String {
        val response = if (success) {
            SuccessResponse(data)
        } else {
            ErrorResponse(error ?: ErrorInfo(
                code = ErrorCode.INTERNAL_ERROR.code,
                message = ErrorCode.INTERNAL_ERROR.defaultMessage
            ))
        }
        return gson.toJson(response)
    }

    /**
     * 获取 Gson 实例
     * @return Gson 实例
     */
    fun getGson(): Gson = gson
}
