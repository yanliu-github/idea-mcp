package com.ly.ideamcp.model.debug

/**
 * 获取变量请求
 * @property sessionId 会话ID
 * @property frameIndex 栈帧索引(可选,默认为0,即当前栈帧)
 * @property scope 作用域(可选: local, instance, static, all; 默认: local)
 */
data class GetVariablesRequest(
    val sessionId: String,
    val frameIndex: Int? = 0,
    val scope: String? = "local"
) {
    init {
        require(sessionId.isNotBlank()) {
            "Session ID cannot be blank"
        }
        frameIndex?.let {
            require(it >= 0) {
                "Frame index must be non-negative"
            }
        }
        scope?.let {
            require(it in listOf("local", "instance", "static", "all")) {
                "Invalid scope: $scope"
            }
        }
    }
}
