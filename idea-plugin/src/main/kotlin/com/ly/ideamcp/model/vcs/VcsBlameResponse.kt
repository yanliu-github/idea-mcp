package com.ly.ideamcp.model.vcs

data class VcsBlameResponse(
    val success: Boolean,
    val filePath: String,
    val annotations: List<LineAnnotation>,
    val totalLines: Int
)

data class LineAnnotation(
    val lineNumber: Int,
    val commitHash: String,
    val author: String,
    val date: String,
    val commitMessage: String
)
