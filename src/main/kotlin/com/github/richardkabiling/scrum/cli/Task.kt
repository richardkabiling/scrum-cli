package com.github.richardkabiling.scrum.cli

import java.math.BigDecimal

data class Task(
    val id: String,
    val refId: String,
    val parentId: String,
    val summary: String,
    val type: String,
    val status: String,
    val actualSpent: BigDecimal,
    val remainingEstimate: BigDecimal,
    val originalEstimate: BigDecimal
)