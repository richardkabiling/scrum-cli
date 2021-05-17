package com.github.richardkabiling.scrum.cli

import java.math.BigDecimal
import java.time.LocalDate

data class CapacityStatistics(
    private val date: LocalDate,
    private val members: List<com.github.richardkabiling.scrum.cli.Sprint.Member>,
    private val schedule: _root_ide_package_.com.github.richardkabiling.scrum.cli.Sprint.Schedule,
    private val capacity: _root_ide_package_.com.github.richardkabiling.scrum.cli.Sprint.EffectiveCapacity,
    private val baselineTasks: List<_root_ide_package_.com.github.richardkabiling.scrum.cli.Task>,
    private val tasksByDate: Map<LocalDate, List<_root_ide_package_.com.github.richardkabiling.scrum.cli.Task>>
) {
    val committedCapacity: Map<LocalDate, BigDecimal> = capacity.daily

    val committedCapacityByRole: Map<String, Map<LocalDate, BigDecimal>> = capacity.detailed.map { (name, capacity) ->
        members.find { it.name == name }!!.role to capacity
    }
        .groupBy({ it.first }) { it.second }
        .mapValues {
            it.value
                .reduce { acc, next -> acc addKeyWise next }
        }

    val renderedWork: Map<LocalDate, BigDecimal> = run {
        val baselineSpent = baselineTasks.map { it.actualSpent }
            .fold(BigDecimal.ZERO, BigDecimal::plus)

        val dateToRendered = tasksByDate.filterKeys { it <= date }
            .map { (date, tasks) ->
                date to (tasks.map { it.actualSpent }
                    .fold(BigDecimal.ZERO, BigDecimal::plus) - baselineSpent)
            }

        dateToRendered.foldRightIndexed(emptyList<Pair<LocalDate, BigDecimal>>()) { index, (date, value), acc ->
            when (index) {
                0 -> listOf(date to value) + acc
                else -> listOf(date to value - dateToRendered[index - 1].second) + acc
            }
        }
            .toMap()
    }

    val renderedWorkByType: Map<String, Map<LocalDate, BigDecimal>> = run {
        val baselineSpentByType = baselineTasks
            .groupBy({ it.type }) { it.actualSpent }
            .mapValues {
                it.value.fold(BigDecimal.ZERO, BigDecimal::plus)
            }

        tasksByDate.filterKeys { it <= date }
            .flatMap { (date, tasks) -> tasks.map { it.type to date to it } }
            .groupBy { (type) -> type }
            .mapValues { (type, value) ->


                val dailySpent = value.groupBy { (_, date) -> date }
                    .map { (date, value) ->
                        val baselineSpent = baselineSpentByType[type] ?: BigDecimal.ZERO
                        val tasks = value.map { it.third }

                        date to tasks.map { it.actualSpent }
                            .fold(BigDecimal.ZERO, BigDecimal::plus) - baselineSpent
                    }

                dailySpent.foldRightIndexed(emptyList<Pair<LocalDate, BigDecimal>>()) { index, (date, value), acc ->
                    when (index) {
                        0 -> listOf(date to value) + acc
                        else -> listOf(date to value - dailySpent[index - 1].second) + acc
                    }
                }
                    .toMap()
            }
    }

    val totalDeficit: BigDecimal = renderedWork.filterKeys { it <= date }
        .map { (date, value) ->
            (committedCapacity[date] ?: BigDecimal.ZERO) - value
        }
        .fold(BigDecimal.ZERO, BigDecimal::plus)

    val deficitByType: Map<String, BigDecimal> = renderedWorkByType.mapValues {
        it.value.filterKeys { it <= date }
            .map { (date, value) ->
                (committedCapacityByRole[it.key]?.get(date) ?: BigDecimal.ZERO) - value
            }
            .fold(BigDecimal.ZERO, BigDecimal::plus)
    }

    val projectedFutureDailyOvertimeByType: Map<String, BigDecimal> = deficitByType.mapValues {
        try {
            it.value / schedule.dates.filter { it >= date }.size.toBigDecimal()
        } catch (e: ArithmeticException) {
            BigDecimal.ZERO
        }
    }

}
