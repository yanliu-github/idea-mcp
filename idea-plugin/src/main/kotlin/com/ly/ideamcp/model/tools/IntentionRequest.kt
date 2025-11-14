package com.ly.ideamcp.model.tools

data class IntentionRequest(
    val filePath: String,
    val intentionId: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(intentionId.isNotBlank()) { "Intention ID cannot be blank" }
    }
}
