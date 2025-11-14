package com.ly.ideamcp.model.vcs

data class VcsStatusResponse(
    val success: Boolean,
    val changes: List<ChangeInfo>,
    val totalChanges: Int
)

data class ChangeInfo(
    val filePath: String,
    val changeType: String, // ADDED, MODIFIED, DELETED, RENAMED
    val oldRevision: String? = null,
    val newRevision: String? = null
)
