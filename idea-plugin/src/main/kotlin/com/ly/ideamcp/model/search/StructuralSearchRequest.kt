package com.ly.ideamcp.model.search

data class StructuralSearchRequest(
    val pattern: String,
    val scope: String = "project"
) {
    init {
        require(pattern.isNotBlank()) { "Pattern cannot be blank" }
    }
}
