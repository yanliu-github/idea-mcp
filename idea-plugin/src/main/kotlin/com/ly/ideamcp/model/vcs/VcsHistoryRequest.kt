package com.ly.ideamcp.model.vcs

data class VcsHistoryRequest(
    val filePath: String,
    val limit: Int = 100,
    val maxResults: Int = 100 // 别名,为了兼容代码中的使用
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(limit > 0) { "Limit must be positive" }
    }
}
