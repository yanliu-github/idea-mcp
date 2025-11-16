package com.ly.ideamcp.model.search

data class GlobalSearchRequest(
    val query: String,
    val scope: String = "project", // project, module, directory
    val fileTypes: List<String>? = null,
    val maxResults: Int? = null, // 最大结果数
    val wholeWords: Boolean = false // 全词匹配
) {
    init {
        require(query.isNotBlank()) { "Query cannot be blank" }
    }
}
