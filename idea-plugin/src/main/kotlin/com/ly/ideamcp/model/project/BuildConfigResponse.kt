package com.ly.ideamcp.model.project

data class BuildConfigResponse(
    val success: Boolean,
    val buildSystem: String, // Gradle, Maven, etc.
    val jdkVersion: String,
    val sourceCompatibility: String,
    val targetCompatibility: String
)
