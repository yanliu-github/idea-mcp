package com.ly.ideamcp.model.search

data class TextSearchRequest(
    val pattern: String,
    val caseSensitive: Boolean = false,
    val regex: Boolean = false,
    val scope: String = "project"
) {
    init {
        require(pattern.isNotBlank()) { "Pattern cannot be blank" }
    }
}
