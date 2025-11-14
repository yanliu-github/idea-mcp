package com.ly.ideamcp.model.search

data class StructuralSearchResponse(
    val success: Boolean,
    val pattern: String,
    val results: List<SearchResult>,
    val totalResults: Int
)
