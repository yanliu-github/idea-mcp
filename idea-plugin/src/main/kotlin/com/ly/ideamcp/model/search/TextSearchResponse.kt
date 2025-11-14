package com.ly.ideamcp.model.search

data class TextSearchResponse(
    val success: Boolean,
    val pattern: String,
    val results: List<SearchResult>,
    val totalResults: Int,
    val caseSensitive: Boolean,
    val regex: Boolean
)
