package com.ly.ideamcp.model.vcs

data class VcsHistoryRequest(
    val filePath: String,
    val limit: Int = 100
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(limit > 0) { "Limit must be positive" }
    }
}
