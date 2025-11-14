package com.ly.ideamcp.model.tools

data class OptimizeImportsResponse(
    val success: Boolean,
    val filePath: String,
    val removedImports: List<String>,
    val addedImports: List<String>
)
