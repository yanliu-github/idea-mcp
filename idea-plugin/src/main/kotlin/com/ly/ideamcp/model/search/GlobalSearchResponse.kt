package com.ly.ideamcp.model.search

data class GlobalSearchResponse(
    val success: Boolean,
    val query: String,
    val results: List<SearchResult>,
    val totalResults: Int
)
