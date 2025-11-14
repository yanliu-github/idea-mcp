package com.ly.ideamcp.model.tools

data class QuickFixResponse(
    val success: Boolean,
    val filePath: String,
    val fixApplied: String
)
