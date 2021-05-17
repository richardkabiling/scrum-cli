package com.github.richardkabiling.scrum.cli

data class Story(
    val id: String,
    val refId: String,
    val summary: String,
    val type: String,
    val status: String,
    val points: Int?
)