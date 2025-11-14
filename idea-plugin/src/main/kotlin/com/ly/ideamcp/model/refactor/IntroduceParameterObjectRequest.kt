package com.ly.ideamcp.model.refactor

/**
 * 引入参数对象请求
 * @property filePath 文件路径
 * @property offset 方法的偏移量
 * @property line 方法所在行号(与offset二选一)
 * @property column 方法所在列号
 * @property className 参数对象类名
 * @property packageName 包名(可选)
 * @property parameters 要合并的参数索引列表
 * @property keepParameters 是否保留原参数(默认false)
 * @property preview 是否仅预览(默认false)
 */
data class IntroduceParameterObjectRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val className: String,
    val packageName: String? = null,
    val parameters: List<Int>? = null,
    val keepParameters: Boolean = false,
    val preview: Boolean = false
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Must provide either offset or line/column"
        }

        require(filePath.isNotBlank()) {
            "File path cannot be blank"
        }

        require(className.isNotBlank()) {
            "Class name cannot be blank"
        }
    }
}
