package com.ly.ideamcp.model.codegen

/**
 * 重写方法请求
 */
data class OverrideMethodRequest(
    /** 文件路径 */
    val filePath: String,

    /** 偏移量（与 line/column 二选一） */
    val offset: Int? = null,

    /** 行号（从0开始） */
    val line: Int? = null,

    /** 列号（从0开始） */
    val column: Int? = null,

    /** 要重写的方法名称 */
    val methodName: String
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(methodName.isNotBlank()) { "Method name cannot be blank" }
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
    }
}
