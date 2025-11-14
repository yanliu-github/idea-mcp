package com.ly.ideamcp.model.tools

data class QuickFixRequest(
    val filePath: String,
    val fixId: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(fixId.isNotBlank()) { "Fix ID cannot be blank" }
    }
}
