package com.ly.ideamcp.model.dependency

data class DetectCyclesResponse(
    val success: Boolean,
    val cycles: List<DependencyCycle>,
    val totalCycles: Int,
    val scope: String
)

data class DependencyCycle(
    val nodes: List<String>,
    val description: String
)
