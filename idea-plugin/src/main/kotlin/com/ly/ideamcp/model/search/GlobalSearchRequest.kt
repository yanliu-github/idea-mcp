package com.ly.ideamcp.model.search

data class GlobalSearchRequest(
    val query: String,
    val scope: String = "project", // project, module, directory
    val fileTypes: List<String>? = null
) {
    init {
        require(query.isNotBlank()) { "Query cannot be blank" }
    }
}
