package com.ly.ideamcp.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 插件设置
 * 应用级别的单例服务，持久化配置
 */
@Service
@State(
    name = "IdeaMcpSettings",
    storages = [Storage("IdeaMcpSettings.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    private var state = State()

    /**
     * 获取配置状态
     */
    override fun getState(): State = state

    /**
     * 加载配置状态
     */
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    /**
     * 配置状态类
     */
    data class State(
        /**
         * HTTP Server 端口
         */
        var port: Int = 58888,

        /**
         * 监听地址
         */
        var host: String = "localhost",

        /**
         * 是否启用自动启动
         */
        var autoStart: Boolean = true,

        /**
         * 是否启用认证
         */
        var enableAuth: Boolean = false,

        /**
         * Bearer Token（如果启用认证）
         */
        var authToken: String = "",

        /**
         * 请求超时时间（秒）
         */
        var requestTimeout: Int = 30,

        /**
         * 是否启用详细日志
         */
        var verboseLogging: Boolean = false,

        /**
         * 允许的 IP 白名单（逗号分隔，空表示不限制）
         */
        var allowedIps: String = "127.0.0.1,::1",

        /**
         * 是否启用 CORS
         */
        var enableCors: Boolean = true,

        /**
         * CORS 允许的源（逗号分隔，空表示允许所有）
         */
        var corsAllowedOrigins: String = "*"
    )

    companion object {
        /**
         * 获取插件设置实例
         */
        fun getInstance(): PluginSettings {
            return com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(PluginSettings::class.java)
        }
    }

    // 便捷访问方法

    var port: Int
        get() = state.port
        set(value) {
            state.port = value
        }

    var host: String
        get() = state.host
        set(value) {
            state.host = value
        }

    var autoStart: Boolean
        get() = state.autoStart
        set(value) {
            state.autoStart = value
        }

    var enableAuth: Boolean
        get() = state.enableAuth
        set(value) {
            state.enableAuth = value
        }

    var authToken: String
        get() = state.authToken
        set(value) {
            state.authToken = value
        }

    var requestTimeout: Int
        get() = state.requestTimeout
        set(value) {
            state.requestTimeout = value
        }

    var verboseLogging: Boolean
        get() = state.verboseLogging
        set(value) {
            state.verboseLogging = value
        }

    var allowedIps: String
        get() = state.allowedIps
        set(value) {
            state.allowedIps = value
        }

    var enableCors: Boolean
        get() = state.enableCors
        set(value) {
            state.enableCors = value
        }

    var corsAllowedOrigins: String
        get() = state.corsAllowedOrigins
        set(value) {
            state.corsAllowedOrigins = value
        }

    /**
     * 检查 IP 是否在白名单中
     */
    fun isIpAllowed(ip: String): Boolean {
        if (allowedIps.isBlank()) {
            return true // 空白名单表示不限制
        }

        val allowedList = allowedIps.split(",").map { it.trim() }
        return allowedList.contains(ip) || allowedList.contains("*")
    }

    /**
     * 检查 Token 是否有效
     */
    fun isValidToken(token: String?): Boolean {
        if (!enableAuth) {
            return true // 未启用认证，直接通过
        }

        return token != null && token == authToken
    }

    /**
     * 获取服务器地址
     */
    fun getServerUrl(): String {
        return "http://$host:$port"
    }

    /**
     * 重置为默认配置
     */
    fun resetToDefaults() {
        loadState(State())
    }
}
