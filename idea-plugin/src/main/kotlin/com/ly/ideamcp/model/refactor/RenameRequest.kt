package com.ly.ideamcp.model.refactor

/**
 * 重命名请求
 * @property filePath 文件路径
 * @property offset 符号位置偏移量（优先）
 * @property line 符号位置行号（如果没有 offset）
 * @property column 符号位置列号（如果没有 offset）
 * @property newName 新名称
 * @property searchInComments 是否在注释中搜索
 * @property searchInStrings 是否在字符串中搜索
 * @property preview 是否预览（不实际执行）
 */
data class RenameRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val newName: String,
    val searchInComments: Boolean = true,
    val searchInStrings: Boolean = false,
    val preview: Boolean = false
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
        require(newName.isNotBlank()) {
            "New name cannot be blank"
        }
    }
}
