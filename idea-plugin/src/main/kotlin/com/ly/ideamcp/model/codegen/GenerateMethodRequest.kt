package com.ly.ideamcp.model.codegen

/**
 * 生成方法请求（用于 toString, equals, hashCode 等）
 */
data class GenerateMethodRequest(
    /** 文件路径 */
    val filePath: String,

    /** 偏移量（与 line/column 二选一） */
    val offset: Int? = null,

    /** 行号（从0开始） */
    val line: Int? = null,

    /** 列号（从0开始） */
    val column: Int? = null,

    /** 要包含的字段名称列表 */
    val fields: List<String>? = null
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
    }
}
