package com.ly.ideamcp.model.navigation

import com.ly.ideamcp.model.CodeLocation

data class CallHierarchyResponse(
    val success: Boolean,
    val methodName: String,
    val callers: List<CallInfo>,
    val callees: List<CallInfo>
)

data class CallInfo(
    val methodName: String,
    val className: String,
    val location: CodeLocation
)
