package com.ly.ideamcp.model.debug

/**
 * 调试会话启动请求
 * @property filePath 主程序文件路径
 * @property mainClass 主类全限定名(例如: com.example.Main)
 * @property programArgs 程序参数(可选)
 * @property vmArgs JVM参数(可选)
 */
data class DebugSessionRequest(
    val filePath: String,
    val mainClass: String,
    val programArgs: List<String>? = null,
    val vmArgs: List<String>? = null
) {
    init {
        require(filePath.isNotBlank()) {
            "File path cannot be blank"
        }
        require(mainClass.isNotBlank()) {
            "Main class cannot be blank"
        }
    }
}
