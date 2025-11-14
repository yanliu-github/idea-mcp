package com.ly.ideamcp.model.navigation

import com.ly.ideamcp.model.CodeLocation

data class TypeHierarchyResponse(
    val success: Boolean,
    val className: String,
    val supertypes: List<TypeInfo>,
    val subtypes: List<TypeInfo>
)

data class TypeInfo(
    val name: String,
    val qualifiedName: String,
    val location: CodeLocation
)
