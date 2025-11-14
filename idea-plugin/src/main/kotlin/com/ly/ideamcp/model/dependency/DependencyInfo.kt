package com.ly.ideamcp.model.dependency

data class DependencyInfo(
    val from: String,
    val to: String,
    val type: String, // compile, runtime, test
    val strength: String // strong, weak
)
