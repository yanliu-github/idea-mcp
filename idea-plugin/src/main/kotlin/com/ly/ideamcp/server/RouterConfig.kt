package com.ly.ideamcp.server

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.undertow.server.HttpServerExchange

/**
 * 路由配置
 * 管理 URL 路径到处理器的映射
 */
class RouterConfig {
    private val logger = Logger.getInstance(RouterConfig::class.java)
    private val routes = mutableListOf<Route>()

    /**
     * 注册路由
     * @param path URL 路径（支持路径参数，如 /api/v1/user/{id}）
     * @param method HTTP 方法
     * @param handler 处理器函数
     */
    fun register(path: String, method: String, handler: RouteHandler) {
        val route = Route(path, method.uppercase(), handler)
        routes.add(route)
        logger.info("Registered route: $method $path")
    }

    /**
     * 注册 GET 路由
     */
    fun get(path: String, handler: RouteHandler) {
        register(path, "GET", handler)
    }

    /**
     * 注册 POST 路由
     */
    fun post(path: String, handler: RouteHandler) {
        register(path, "POST", handler)
    }

    /**
     * 注册 PUT 路由
     */
    fun put(path: String, handler: RouteHandler) {
        register(path, "PUT", handler)
    }

    /**
     * 注册 DELETE 路由
     */
    fun delete(path: String, handler: RouteHandler) {
        register(path, "DELETE", handler)
    }

    /**
     * 查找匹配的路由
     * @param path 请求路径
     * @param method HTTP 方法
     * @return 匹配的路由，如果没有找到返回 null
     */
    fun findRoute(path: String, method: String): Route? {
        return routes.find { route ->
            route.method == method.uppercase() && matchPath(route.path, path)
        }
    }

    /**
     * 检查路径是否匹配
     * 支持路径参数，如 /api/v1/user/{id} 匹配 /api/v1/user/123
     */
    private fun matchPath(pattern: String, path: String): Boolean {
        val patternParts = pattern.split("/").filter { it.isNotEmpty() }
        val pathParts = path.split("/").filter { it.isNotEmpty() }

        if (patternParts.size != pathParts.size) {
            return false
        }

        return patternParts.zip(pathParts).all { (patternPart, pathPart) ->
            patternPart.startsWith("{") && patternPart.endsWith("}") || patternPart == pathPart
        }
    }

    /**
     * 获取所有已注册的路由
     */
    fun getRoutes(): List<Route> = routes.toList()

    /**
     * 清空所有路由
     */
    fun clear() {
        routes.clear()
    }

    /**
     * 路由定义
     */
    data class Route(
        val path: String,
        val method: String,
        val handler: RouteHandler
    )
}

/**
 * 路由处理器函数类型
 * @param exchange HTTP 请求交换对象
 * @param project 当前项目（可能为 null）
 * @return 响应数据
 */
typealias RouteHandler = (exchange: HttpServerExchange, project: Project?) -> Any?
