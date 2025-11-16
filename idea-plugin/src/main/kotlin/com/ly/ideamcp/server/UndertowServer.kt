package com.ly.ideamcp.server

import com.intellij.openapi.diagnostic.Logger
import com.ly.ideamcp.config.PluginSettings
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.StatusCodes
import java.net.InetSocketAddress

/**
 * Undertow HTTP Server 封装
 * 管理 HTTP 服务器的生命周期
 */
class UndertowServer(
    private val settings: PluginSettings = PluginSettings.getInstance()
) {
    private val logger = Logger.getInstance(UndertowServer::class.java)
    private var server: Undertow? = null
    private var isRunning = false

    /**
     * 启动服务器
     * @param handler HTTP 请求处理器
     * @throws IllegalStateException 如果服务器已经在运行
     */
    fun start(handler: HttpHandler) {
        if (isRunning) {
            throw IllegalStateException("Server is already running")
        }

        try {
            logger.info("Starting IDEA MCP Server on ${settings.host}:${settings.port}")

            server = Undertow.builder()
                .addHttpListener(settings.port, settings.host)
                .setHandler(createRootHandler(handler))
                .build()

            server?.start()
            isRunning = true

            logger.info("IDEA MCP Server started successfully at ${settings.getServerUrl()}")
        } catch (e: Exception) {
            logger.error("Failed to start IDEA MCP Server", e)
            isRunning = false
            throw e
        }
    }

    /**
     * 停止服务器
     */
    fun stop() {
        if (!isRunning) {
            logger.warn("Server is not running")
            return
        }

        try {
            logger.info("Stopping IDEA MCP Server")
            server?.stop()
            server = null
            isRunning = false
            logger.info("IDEA MCP Server stopped successfully")
        } catch (e: Exception) {
            logger.error("Failed to stop IDEA MCP Server", e)
            throw e
        }
    }

    /**
     * 重启服务器
     * @param handler HTTP 请求处理器
     */
    fun restart(handler: HttpHandler) {
        logger.info("Restarting IDEA MCP Server")
        if (isRunning) {
            stop()
        }
        // 等待一小段时间确保端口释放
        Thread.sleep(500)
        start(handler)
    }

    /**
     * 检查服务器是否正在运行
     */
    fun isRunning(): Boolean = isRunning

    /**
     * 获取服务器监听信息
     */
    fun getListeningInfo(): ServerInfo? {
        return if (isRunning) {
            ServerInfo(
                host = settings.host,
                port = settings.port,
                url = settings.getServerUrl()
            )
        } else {
            null
        }
    }

    /**
     * 创建根处理器（添加 CORS 和 IP 白名单检查）
     */
    private fun createRootHandler(handler: HttpHandler): HttpHandler {
        return HttpHandler { exchange ->
            try {
                // IP 白名单检查
                if (!checkIpWhitelist(exchange)) {
                    exchange.statusCode = StatusCodes.FORBIDDEN
                    exchange.responseSender.send("Access denied: IP not in whitelist")
                    return@HttpHandler
                }

                // CORS 处理
                if (settings.enableCors) {
                    addCorsHeaders(exchange)

                    // 处理 OPTIONS 预检请求
                    if (exchange.requestMethod.toString().equals("OPTIONS", ignoreCase = true)) {
                        exchange.statusCode = StatusCodes.OK
                        exchange.endExchange()
                        return@HttpHandler
                    }
                }

                // 传递给实际处理器
                handler.handleRequest(exchange)
            } catch (e: Exception) {
                logger.error("Error handling request", e)
                exchange.statusCode = StatusCodes.INTERNAL_SERVER_ERROR
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain")
                exchange.responseSender.send("Internal Server Error: ${e.message}")
            }
        }
    }

    /**
     * 检查 IP 白名单
     */
    private fun checkIpWhitelist(exchange: HttpServerExchange): Boolean {
        val sourceAddress = exchange.sourceAddress
        if (sourceAddress is InetSocketAddress) {
            val clientIp = sourceAddress.address.hostAddress
            return settings.isIpAllowed(clientIp)
        }
        return false
    }

    /**
     * 添加 CORS 头
     */
    private fun addCorsHeaders(exchange: HttpServerExchange) {
        val responseHeaders = exchange.responseHeaders

        val allowedOrigins = settings.corsAllowedOrigins
        responseHeaders.put(HttpString("Access-Control-Allow-Origin"), allowedOrigins)
        responseHeaders.put(HttpString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE, OPTIONS")
        responseHeaders.put(HttpString("Access-Control-Allow-Headers"), "Content-Type, Authorization")
        responseHeaders.put(HttpString("Access-Control-Max-Age"), "3600")
    }

    /**
     * 服务器信息
     */
    data class ServerInfo(
        val host: String,
        val port: Int,
        val url: String
    )
}
