package com.ly.ideamcp.model.search

data class StructuralSearchRequest(
    val pattern: String,
    val scope: String = "project",
    val caseSensitive: Boolean = false, // 区分大小写
    val maxResults: Int? = null // 最大结果数
) {
    init {
        require(pattern.isNotBlank()) { "Pattern cannot be blank" }
    }
}
