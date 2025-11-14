package com.ly.ideamcp.model.navigation

import com.ly.ideamcp.model.CodeLocation

data class FindImplementationsResponse(
    val success: Boolean,
    val elementName: String,
    val implementations: List<ImplementationInfo>,
    val totalImplementations: Int
)

data class ImplementationInfo(
    val name: String,
    val qualifiedName: String,
    val location: CodeLocation
)
