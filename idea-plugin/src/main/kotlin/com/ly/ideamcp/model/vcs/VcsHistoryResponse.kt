package com.ly.ideamcp.model.vcs

data class VcsHistoryResponse(
    val success: Boolean,
    val filePath: String,
    val commits: List<CommitInfo>,
    val totalCommits: Int
)

data class CommitInfo(
    val hash: String,
    val author: String,
    val date: String,
    val message: String,
    val files: List<String>
)
