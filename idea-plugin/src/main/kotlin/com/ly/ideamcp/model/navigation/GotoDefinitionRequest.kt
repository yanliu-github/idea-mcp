package com.ly.ideamcp.model.navigation

/**
 * 跳转到定义请求
 * @property filePath 文件路径
 * @property offset 符号位置偏移量（优先）
 * @property line 符号位置行号（如果没有 offset）
 * @property column 符号位置列号（如果没有 offset）
 */
data class GotoDefinitionRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
    }
}
