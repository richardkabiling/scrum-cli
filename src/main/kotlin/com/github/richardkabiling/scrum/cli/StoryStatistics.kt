package com.github.richardkabiling.scrum.cli

import java.math.BigDecimal
import java.time.LocalDate

data class StoryStatistics(
    private val date: LocalDate,
    private val schedule: Sprint.Schedule,
    private val storiesById: Map<String, Story>,
    private val storiesByDate: Map<LocalDate, List<Story>>,
    private val baselineTasks: List<Task>,
    private val baselineTasksById: Map<String, Task>,
    private val tasksByDate: Map<LocalDate, List<Task>>
) {
    val committedStoryPointsPerDay: Map<LocalDate, BigDecimal> = storiesByDate.mapValues { (_, stories) ->
        stories.map { it.points?.toBigDecimal() ?: BigDecimal.ZERO }
            .fold(BigDecimal.ZERO, BigDecimal::plus)
    }

    val earnedStoryPointsPerDay: Map<LocalDate, BigDecimal> = storiesByDate.mapValues { (_, stories) ->
        stories.filter { it.status == STATUS_DONE }
            .map { it.points?.toBigDecimal() ?: BigDecimal.ZERO }
            .fold(BigDecimal.ZERO, BigDecimal::plus)
    }

    val storyCountPerDay: Map<LocalDate, Int> = storiesByDate.mapValues { it.value.count() }

    val totalSpentPerStory: Map<LocalDate, Map<String, Map<String, BigDecimal>>> =
        totalByStory { it.actualSpent - (baselineTasksById[it.id]?.actualSpent ?: BigDecimal.ZERO) }

    val totalRemainingPerStory: Map<LocalDate, Map<String, Map<String, BigDecimal>>> =
        totalByStory { it.remainingEstimate }

    val effectiveEstimateProgressPerStory: Map<String, Map<LocalDate, BigDecimal>> =
        tasksByDate.filterKeys { it <= date }
            .flatMap { (date, tasks) ->
                tasks.map { it.parentId to date to it }
            }
            .groupBy { it.first }
            .mapValues {
                it.value.groupBy { it.second }
                    .mapValues {
                        val tasks = it.value.map { it.third }
                        val totalSpent = tasks.map { it.actualSpent }
                            .fold(BigDecimal.ZERO, BigDecimal::plus)
                        val totalRemaining = tasks.map { it.remainingEstimate }
                            .fold(BigDecimal.ZERO, BigDecimal::plus)
                        val baselineSpent = tasks.map { task -> task.actualSpent }
                            .fold(BigDecimal.ZERO, BigDecimal::plus)

                        totalSpent - baselineSpent + totalRemaining
                    }
            }

    val percentSpentPerStory: Map<String, Map<LocalDate, BigDecimal>> =
        tasksByDate.filterKeys { it <= date }
            .flatMap { (date, tasks) ->
                tasks.map { it.parentId to date to it }
            }
            .groupBy { it.first }
            .mapValues {
                it.value.groupBy { it.second }
                    .mapValues {
                        val tasks = it.value.map { it.third }
                        val totalSpent = tasks.map { it.actualSpent }
                            .fold(BigDecimal.ZERO, BigDecimal::plus)
                        val totalRemaining = tasks.map { it.remainingEstimate }
                            .fold(BigDecimal.ZERO, BigDecimal::plus)
                        val baselineSpent =
                            tasks.map { task -> baselineTasksById[task.id]?.actualSpent ?: BigDecimal.ZERO }
                                .fold(BigDecimal.ZERO, BigDecimal::plus)

                        try {
                            (totalSpent - baselineSpent) / (totalRemaining + totalSpent - baselineSpent) * 100.toBigDecimal()
                        } catch (e: ArithmeticException) {
                            BigDecimal.ZERO
                        }
                    }
            }

    private fun totalByStory(selector: (Task) -> BigDecimal): Map<LocalDate, Map<String, Map<String, BigDecimal>>> {
        return tasksByDate.filterKeys { it <= date }
            .flatMap { (date, tasks) ->
                tasks.map { date to it.type to it.parentId to it }
            }
            .groupBy { it.first }
            .mapValues {
                it.value.groupBy { it.second }
                    .mapValues {
                        it.value.groupBy { it.third }
                            .mapValues {
                                it.value.map { it.fourth }
                                    .map(selector)
                                    .fold(BigDecimal.ZERO, BigDecimal::plus)
                            }
                    }
            }
    }
}

