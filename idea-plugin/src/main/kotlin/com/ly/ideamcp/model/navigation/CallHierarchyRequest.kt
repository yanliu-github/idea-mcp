package com.ly.ideamcp.model.navigation

data class CallHierarchyRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null
) {
    init {
        require(filePath.isNotBlank()) { "File path cannot be blank" }
        require(offset != null || (line != null && column != null)) {
            "Either offset or (line, column) must be provided"
        }
    }
}
