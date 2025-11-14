package com.ly.ideamcp.model.navigation

/**
 * 查找用途请求
 * @property filePath 文件路径
 * @property offset 符号位置偏移量（优先）
 * @property line 符号位置行号（如果没有 offset）
 * @property column 符号位置列号（如果没有 offset）
 * @property includeComments 是否包含注释中的用途
 * @property includeStrings 是否包含字符串中的用途
 * @property maxResults 最大结果数量（0表示不限制）
 */
data class FindUsagesRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val includeComments: Boolean = false,
    val includeStrings: Boolean = false,
    val maxResults: Int = 0
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
        require(maxResults >= 0) {
            "maxResults must be non-negative"
        }
    }
}
