package com.ly.ideamcp.server

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.undertow.server.HttpServerExchange
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 路由注册 DSL
 * 简化路由注册，消除重复代码
 */
class RouteDsl(@PublishedApi internal val router: RouterConfig) {
    private val logger = Logger.getInstance(RouteDsl::class.java)
    @PublishedApi internal val gson = Gson()

    /**
     * 注册需要项目和请求体的 POST 路由
     */
    inline fun <reified T> postWithBody(
        path: String,
        crossinline handler: (Project, T) -> Any
    ) {
        router.post(path) { exchange, project ->
            requireProject(project)
            val request = parseBody<T>(exchange)
            handler(project!!, request)
        }
    }

    /**
     * 注册只需要项目的 GET 路由
     */
    fun getWithProject(
        path: String,
        handler: (Project) -> Any
    ) {
        router.get(path) { _, project ->
            requireProject(project)
            handler(project!!)
        }
    }

    /**
     * 注册不需要项目的 GET 路由
     */
    fun get(
        path: String,
        handler: () -> Any
    ) {
        router.get(path) { _, _ ->
            handler()
        }
    }

    /**
     * 注册带查询参数的 GET 路由
     */
    fun getWithParams(
        path: String,
        handler: (Project, Map<String, String?>) -> Any
    ) {
        router.get(path) { exchange, project ->
            requireProject(project)
            val params = extractQueryParams(exchange)
            handler(project!!, params)
        }
    }

    /**
     * 注册带路径参数的 DELETE 路由
     */
    fun deleteWithPathParam(
        path: String,
        paramName: String,
        handler: (Project, String) -> Any
    ) {
        router.delete(path) { exchange, project ->
            requireProject(project)
            val paramValue = extractPathParam(exchange, paramName)
            handler(project!!, paramValue)
        }
    }

    /**
     * 注册带 Map 请求体的 POST 路由（用于简单请求）
     */
    fun postWithMap(
        path: String,
        handler: (Project, Map<String, Any?>) -> Any
    ) {
        router.post(path) { exchange, project ->
            requireProject(project)
            val body = parseMapBody(exchange)
            handler(project!!, body)
        }
    }

    /**
     * 验证项目存在
     */
    fun requireProject(project: Project?) {
        if (project == null) {
            throw IllegalStateException("No active project")
        }
    }

    /**
     * 解析请求体为指定类型
     */
    inline fun <reified T> parseBody(exchange: HttpServerExchange): T {
        val body = readRequestBody(exchange)
        if (body.isBlank()) {
            throw IllegalArgumentException("Missing request body")
        }
        return gson.fromJson(body, T::class.java)
            ?: throw IllegalArgumentException("Failed to parse request body")
    }

    /**
     * 解析请求体为 Map
     */
    @Suppress("UNCHECKED_CAST")
    fun parseMapBody(exchange: HttpServerExchange): Map<String, Any?> {
        val body = readRequestBody(exchange)
        if (body.isBlank()) {
            throw IllegalArgumentException("Missing request body")
        }
        return gson.fromJson(body, Map::class.java) as? Map<String, Any?>
            ?: throw IllegalArgumentException("Failed to parse request body as map")
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
     * 提取查询参数
     */
    fun extractQueryParams(exchange: HttpServerExchange): Map<String, String?> {
        return exchange.queryParameters.mapValues { it.value.firstOrNull() }
    }

    /**
     * 从路径中提取参数
     */
    fun extractPathParam(exchange: HttpServerExchange, paramName: String): String {
        val path = exchange.requestPath
        val value = path.substringAfterLast("/")
        if (value.isBlank()) {
            throw IllegalArgumentException("$paramName is required")
        }
        return value
    }

    /**
     * 从查询参数中获取必需参数
     */
    fun requireQueryParam(exchange: HttpServerExchange, name: String): String {
        return exchange.queryParameters[name]?.firstOrNull()
            ?: throw IllegalArgumentException("$name is required")
    }

    /**
     * 从查询参数中获取可选参数
     */
    fun getQueryParam(exchange: HttpServerExchange, name: String, default: String? = null): String? {
        return exchange.queryParameters[name]?.firstOrNull() ?: default
    }

    /**
     * 从查询参数中获取整数
     */
    fun getQueryParamInt(exchange: HttpServerExchange, name: String, default: Int): Int {
        return exchange.queryParameters[name]?.firstOrNull()?.toIntOrNull() ?: default
    }
}

/**
 * 扩展函数：在 RouterConfig 上使用 DSL
 */
fun RouterConfig.routes(block: RouteDsl.() -> Unit) {
    val dsl = RouteDsl(this)
    dsl.block()
}
