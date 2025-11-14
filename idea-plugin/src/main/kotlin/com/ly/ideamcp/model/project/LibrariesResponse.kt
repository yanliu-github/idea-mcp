package com.ly.ideamcp.model.project

data class LibrariesResponse(
    val success: Boolean,
    val libraries: List<LibraryInfo>,
    val totalLibraries: Int
)
