package com.ly.ideamcp.model.tools

data class FormatCodeRequest(
    val filePath: String
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
    }
}
