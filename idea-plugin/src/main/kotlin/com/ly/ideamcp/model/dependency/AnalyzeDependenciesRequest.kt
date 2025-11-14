package com.ly.ideamcp.model.dependency

data class AnalyzeDependenciesRequest(
    val scope: String = "project" // project, module, package
)
