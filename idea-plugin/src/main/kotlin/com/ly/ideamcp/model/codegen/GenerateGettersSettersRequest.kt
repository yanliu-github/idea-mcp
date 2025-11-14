package com.ly.ideamcp.model.codegen

/**
 * 生成 Getter/Setter 请求
 */
data class GenerateGettersSettersRequest(
    /** 文件路径 */
    val filePath: String,

    /** 偏移量（与 line/column 二选一） */
    val offset: Int? = null,

    /** 行号（从0开始） */
    val line: Int? = null,

    /** 列号（从0开始） */
    val column: Int? = null,

    /** 要生成的字段名称列表（空则生成所有字段） */
    val fieldNames: List<String>? = null,

    /** 是否生成 Getter */
    val generateGetter: Boolean = true,

    /** 是否生成 Setter */
    val generateSetter: Boolean = true
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
    }
}
