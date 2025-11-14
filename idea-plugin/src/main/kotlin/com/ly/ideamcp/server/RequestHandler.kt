package com.ly.ideamcp.server

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.ly.ideamcp.config.PluginSettings
import com.ly.ideamcp.model.ErrorCode
import com.ly.ideamcp.util.ResponseSerializer
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * HTTP 请求处理器
 * 处理所有 HTTP 请求，解析路径和参数，路由到对应的处理器
 */
class RequestHandler(
    private val routerConfig: RouterConfig,
    private val settings: PluginSettings = PluginSettings.getInstance()
) : HttpHandler {

    private val logger = Logger.getInstance(RequestHandler::class.java)
    private val gson = Gson()

    override fun handleRequest(exchange: HttpServerExchange) {
        if (exchange.isInIoThread) {
            exchange.dispatch(this)
            return
        }

        try {
            // 认证检查
            if (settings.enableAuth && !checkAuthentication(exchange)) {
                sendErrorResponse(
                    exchange,
                    StatusCodes.UNAUTHORIZED,
                    ErrorCode.VALIDATION_ERROR,
                    "Authentication required"
                )
                return
            }

            // 记录请求
            if (settings.verboseLogging) {
                logRequest(exchange)
            }

            // 路由请求
            routeRequest(exchange)
        } catch (e: Exception) {
            logger.error("Error handling request", e)
            sendErrorResponse(
                exchange,
                StatusCodes.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                e.message ?: "Internal server error"
            )
        }
    }

    /**
     * 路由请求到对应的处理器
     */
    private fun routeRequest(exchange: HttpServerExchange) {
        val path = exchange.relativePath
        val method = exchange.requestMethod.toString()

        // 查找路由
        val route = routerConfig.findRoute(path, method)

        if (route == null) {
            sendErrorResponse(
                exchange,
                StatusCodes.NOT_FOUND,
                ErrorCode.UNSUPPORTED_OPERATION,
                "Endpoint not found: $method $path"
            )
            return
        }

        // 获取当前项目
        val project = getCurrentProject()

        // 执行路由处理器
        try {
            val result = route.handler(exchange, project)
            sendSuccessResponse(exchange, result)
        } catch (e: Exception) {
            logger.error("Error executing route handler for $path", e)

            // 根据异常类型返回适当的错误码
            val errorCode = when (e) {
                is IllegalArgumentException -> ErrorCode.INVALID_PARAMETER
                is IllegalStateException -> ErrorCode.UNSUPPORTED_OPERATION
                is java.util.concurrent.TimeoutException -> ErrorCode.TIMEOUT
                else -> ErrorCode.INTERNAL_ERROR
            }

            sendErrorResponse(
                exchange,
                StatusCodes.INTERNAL_SERVER_ERROR,
                errorCode,
                e.message ?: "Error processing request"
            )
        }
    }

    /**
     * 检查认证
     */
    private fun checkAuthentication(exchange: HttpServerExchange): Boolean {
        val authHeader = exchange.requestHeaders.getFirst(Headers.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false
        }

        val token = authHeader.substring(7) // Remove "Bearer " prefix
        return settings.isValidToken(token)
    }

    /**
     * 发送成功响应
     */
    private fun sendSuccessResponse(exchange: HttpServerExchange, data: Any?) {
        val json = ResponseSerializer.serializeSuccess(data)
        sendJsonResponse(exchange, StatusCodes.OK, json)
    }

    /**
     * 发送错误响应
     */
    private fun sendErrorResponse(
        exchange: HttpServerExchange,
        statusCode: Int,
        errorCode: ErrorCode,
        message: String,
        details: Map<String, Any>? = null
    ) {
        val json = ResponseSerializer.serializeError(errorCode, message, details)
        sendJsonResponse(exchange, statusCode, json)
    }

    /**
     * 发送 JSON 响应
     */
    private fun sendJsonResponse(exchange: HttpServerExchange, statusCode: Int, json: String) {
        exchange.statusCode = statusCode
        exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json; charset=utf-8")
        exchange.responseSender.send(json, StandardCharsets.UTF_8)
    }

    /**
     * 读取请求体
     */
    fun readRequestBody(exchange: HttpServerExchange): String {
        exchange.startBlocking()
        val reader = BufferedReader(InputStreamReader(exchange.inputStream, StandardCharsets.UTF_8))
        return reader.readText()
    }

    /**
     * 解析 JSON 请求体
     */
    fun <T> parseRequestBody(exchange: HttpServerExchange, clazz: Class<T>): T? {
        try {
            val body = readRequestBody(exchange)
            if (body.isBlank()) {
                return null
            }
            return gson.fromJson(body, clazz)
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse request body", e)
            throw IllegalArgumentException("Invalid JSON in request body: ${e.message}")
        }
    }

    /**
     * 获取查询参数
     */
    fun getQueryParameter(exchange: HttpServerExchange, name: String): String? {
        return exchange.queryParameters[name]?.firstOrNull()
    }

    /**
     * 获取路径参数
     */
    fun getPathParameter(exchange: HttpServerExchange, name: String): String? {
        // 这里需要从路由匹配结果中获取，简化实现
        return exchange.queryParameters[name]?.firstOrNull()
    }

    /**
     * 获取当前活动项目
     */
    private fun getCurrentProject(): Project? {
        val projects = ProjectManager.getInstance().openProjects
        return projects.firstOrNull() // 返回第一个打开的项目
    }

    /**
     * 记录请求日志
     */
    private fun logRequest(exchange: HttpServerExchange) {
        val method = exchange.requestMethod
        val path = exchange.relativePath
        val queryString = exchange.queryString
        val fullPath = if (queryString.isNullOrBlank()) path else "$path?$queryString"

        logger.info("Request: $method $fullPath")
    }
}
