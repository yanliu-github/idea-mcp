package com.ly.ideamcp.model.project

data class ProjectStructureResponse(
    val success: Boolean,
    val projectName: String,
    val projectPath: String,
    val modules: List<ModuleInfo>,
    val totalModules: Int
)
