package com.ly.ideamcp.model.vcs

data class VcsDiffRequest(
    val filePath: String,
    val oldRevision: String? = null,
    val newRevision: String? = null
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
    }
}
