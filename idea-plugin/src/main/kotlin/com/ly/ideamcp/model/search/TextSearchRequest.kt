package com.ly.ideamcp.model.search

data class TextSearchRequest(
    val pattern: String,
    val caseSensitive: Boolean = false,
    val regex: Boolean = false,
    val scope: String = "project",
    val maxResults: Int? = null, // 最大结果数
    val wholeWords: Boolean = false // 全词匹配
) {
    init {
        require(pattern.isNotBlank()) { "Pattern cannot be blank" }
    }
}
