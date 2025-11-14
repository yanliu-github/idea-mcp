package com.ly.ideamcp.model.codegen

/**
 * 生成构造函数请求
 */
data class GenerateConstructorRequest(
    /** 文件路径 */
    val filePath: String,

    /** 偏移量（与 line/column 二选一） */
    val offset: Int? = null,

    /** 行号（从0开始） */
    val line: Int? = null,

    /** 列号（从0开始） */
    val column: Int? = null,

    /** 要包含的字段列表 */
    val fields: List<FieldInfo> = emptyList()
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
    }
}

/**
 * 字段信息
 */
data class FieldInfo(
    /** 字段名称 */
    val name: String,

    /** 字段类型 */
    val type: String
)
