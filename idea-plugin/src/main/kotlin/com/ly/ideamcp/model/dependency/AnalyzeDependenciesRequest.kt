package com.ly.ideamcp.model.dependency

data class AnalyzeDependenciesRequest(
    val scope: String = "project", // project, module, package
    val moduleName: String? = null // 模块名称(当 scope = "module" 时使用)
)
