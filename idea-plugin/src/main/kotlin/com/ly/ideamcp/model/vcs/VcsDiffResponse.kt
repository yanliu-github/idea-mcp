package com.ly.ideamcp.model.vcs

data class VcsDiffResponse(
    val success: Boolean,
    val filePath: String,
    val diff: String,
    val oldRevision: String?,
    val newRevision: String?
)
