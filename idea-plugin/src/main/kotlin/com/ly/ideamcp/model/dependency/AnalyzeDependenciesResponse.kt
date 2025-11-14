package com.ly.ideamcp.model.dependency

data class AnalyzeDependenciesResponse(
    val success: Boolean,
    val dependencies: List<DependencyInfo>,
    val totalDependencies: Int,
    val scope: String
)
