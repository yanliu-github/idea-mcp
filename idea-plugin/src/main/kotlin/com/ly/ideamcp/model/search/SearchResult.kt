package com.ly.ideamcp.model.search

data class SearchResult(
    val filePath: String,
    val line: Int,
    val column: Int,
    val preview: String,
    val matchType: String // class, method, field, text, structure
)
