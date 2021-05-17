package com.github.richardkabiling.scrum.cli

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

data class Sprint(
    val namespace: String,
    val project: String,
    val name: String,
    val members: List<Member>,
    val schedule: Schedule,
    val effectiveCapacity: EffectiveCapacity
) {
    val membersByRole: Map<String, List<Member>> = members.groupBy { it.role }

    val roles: Set<String> = members.map { it.role }
        .toSet()

    data class Member(
        val name: String,
        val role: String
    ) {
        override fun toString() = name
    }

    data class Schedule(
        val dates: List<LocalDate>,
        val offset: ZoneOffset
    ) {
        val start: LocalDate = dates.first()
        val end: LocalDate = dates.last()
        val duration: Int = dates.size
    }

    data class EffectiveCapacity(
        val base: Map<String, Map<LocalDate, BigDecimal>>,
        val time: Map<String, Map<LocalDate, BigDecimal>>,
        val manpower: Map<String, Map<LocalDate, BigDecimal>>,
        val timeOff: Map<String, Map<LocalDate, BigDecimal>>,
        val overhead: Map<String, Map<LocalDate, BigDecimal>>,
        val training: Map<String, Map<LocalDate, BigDecimal>>,
        val overtime: Map<String, Map<LocalDate, BigDecimal>>,
        val efficiency: Map<String, Map<LocalDate, BigDecimal>>,
        val detailed: Map<String, Map<LocalDate, BigDecimal>>
    ) {
        val daily: Map<LocalDate, BigDecimal> = detailed.values
            .reduce { acc, map -> acc addKeyWise map }

        val member: Map<String, BigDecimal> = detailed.mapValues { (_, v) ->
            v.values.reduce(BigDecimal::plus)
        }
    }

}