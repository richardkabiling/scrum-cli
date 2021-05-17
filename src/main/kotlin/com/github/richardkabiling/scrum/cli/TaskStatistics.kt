package com.github.richardkabiling.scrum.cli

import java.math.BigDecimal
import java.time.LocalDate

data class TaskStatistics(
    private val date: LocalDate,
    private val schedule: Sprint.Schedule,
    private val baselineTasks: List<Task>,
    private val baselineTasksById: Map<String, Task>,
    private val tasksByDate: Map<LocalDate, List<Task>>
) {
    val taskCountPerDay: Map<LocalDate, Int> = tasksByDate.mapValues { it.value.count() }

    val taskProgress: Map<String, Progress> = tasksByDate.filterKeys { it <= date }
        .flatMap { (date, tasks) ->
            listOf(
                STATUS_DONE to date to tasks.computeSpent(),
                STATUS_IN_PROGRESS to date to tasks.computeRemaining(STATUS_IN_PROGRESS),
                STATUS_TO_DO to date to tasks.computeRemaining(STATUS_TO_DO)
            )
        }
        .groupBy { (status) -> status }
        .mapValues { it.value.associate { (_, date, value) -> date to value } }

    val idealTaskProgress: Progress = computeIdealTaskProgress { true }

    val taskProgressByType: Map<String, Map<String, Progress>> = tasksByDate.filterKeys { it <= date }
        .flatMap { (date, tasks) ->
            tasks.groupBy { it.type }
                .flatMap { (type, tasks) ->
                    listOf(
                        type to STATUS_DONE to date to tasks.computeSpent(),
                        type to STATUS_IN_PROGRESS to date to tasks.computeRemaining(STATUS_IN_PROGRESS),
                        type to STATUS_TO_DO to date to tasks.computeRemaining(STATUS_TO_DO)
                    )
                }
        }
        .groupBy { (type) -> type }
        .mapValues {
            it.value.groupBy { (_, status) -> status }
                .mapValues { it.value.associate { (_, _, date, value) -> date to value } }
        }

    val idealTaskProgressByType: Map<String, Progress> = run {
        val baselineTypes = baselineTasks.map { it.type }
        val taskTypes = tasksByDate.values.flatMap { ts -> ts.map { it.type } }
        (baselineTypes + taskTypes).toSet()
            .associateWith { type -> computeIdealTaskProgress { task -> task.type == type } }
    }

    private fun computeIdealTaskProgress(
        predicate: (Task) -> Boolean
    ): Map<LocalDate, BigDecimal> {
        val total = listOf(baselineTasks, tasksByDate.values.first())
            .find { it.isNotEmpty() }
            ?.filterSum(predicate) { it.remainingEstimate }
            ?: BigDecimal.ZERO

        val step = try {
            total / (schedule.duration.toBigDecimal() - BigDecimal.ONE)
        } catch (e: ArithmeticException) {
            BigDecimal.ZERO
        }

        return List(schedule.duration) {
            val date = schedule.dates[it]
            val idealValue = total - step * it.toBigDecimal()

            date to idealValue
        }
            .toMap()
    }

    private fun List<Task>.computeSpent(): BigDecimal {
        val hours = filterSum { it.actualSpent }
        val excess = filterSum { baselineTasksById[it.id]?.actualSpent ?: BigDecimal.ZERO }
        return hours - excess
    }

    private fun List<Task>.computeRemaining(status: String): BigDecimal {
        return filterSum({ it.status == status }) { it.remainingEstimate }
    }

}
