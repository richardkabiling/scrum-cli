package com.github.richardkabiling.scrum.cli

import java.time.LocalDate

data class SprintSummary(
    val date: LocalDate,
    val metadata: Sprint,
    val taskStats: TaskStatistics,
    val storyStats: StoryStatistics,
    val capacityStats: _root_ide_package_.com.github.richardkabiling.scrum.cli.CapacityStatistics,
    val storiesById: Map<String, Story>,
    val storyIds: Set<String>
) {
    val daysLeft = metadata.schedule.dates.filter { it >= date }.count()
}