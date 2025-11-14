package com.ly.ideamcp.model.project

data class ModulesResponse(
    val success: Boolean,
    val modules: List<ModuleInfo>,
    val totalModules: Int
)
