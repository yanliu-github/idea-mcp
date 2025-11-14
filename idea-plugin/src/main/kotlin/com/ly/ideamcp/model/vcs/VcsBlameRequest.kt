package com.ly.ideamcp.model.vcs

data class VcsBlameRequest(
    val filePath: String
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
    }
}
