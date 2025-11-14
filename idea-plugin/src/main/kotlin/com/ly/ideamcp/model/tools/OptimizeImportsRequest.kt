package com.ly.ideamcp.model.tools

data class OptimizeImportsRequest(
    val filePath: String
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
    }
}
