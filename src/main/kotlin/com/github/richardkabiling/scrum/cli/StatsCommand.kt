package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import picocli.CommandLine
import java.nio.file.Paths
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject

@CommandLine.Command(
    name = "stats",
    aliases = ["summary", "statistics"],
    mixinStandardHelpOptions = true,
)
class StatsCommand : Runnable {

    @CommandLine.Option(
        names = ["-d", "--date"],
        description = ["date of tasks"],
    )
    var date: LocalDate? = null

    @CommandLine.ParentCommand
    lateinit var parent: ScrumCommand

    @Inject
    lateinit var mapper: ObjectMapper

    override fun run() {
        val indexFile = Paths.get(".", parent.namespace, "sprints.yml")
            .toAbsolutePath()
            .toFile()
        val index: LinkedHashSet<String> = mapper.readValue(indexFile)
        if (index.isEmpty()) throw SprintNotFoundException()

        val sprintName = index.last()
            .sanitized()

        val sprint: Sprint = run {
            val sprintFile = Paths.get(".", parent.namespace, "sprint-$sprintName.yml")
                .toAbsolutePath()
                .toFile()
            mapper.readValue(sprintFile)
        }

        val baselineStories = run {
            val baselineStoriesFile = Paths.get(".", parent.namespace, "sprint-$sprintName-stories-baseline.yml")
                .toAbsolutePath()
                .toFile()
            when {
                baselineStoriesFile.exists() -> mapper.readValue<List<Story>>(baselineStoriesFile)
                else -> emptyList()
            }
        }

        val baselineStoriesById = baselineStories.associateBy { it.id }

        val baselineTasks = run {
            val baselineTasksFile = Paths.get(".", parent.namespace, "sprint-$sprintName-tasks-baseline.yml")
                .toAbsolutePath()
                .toFile()
            when {
                baselineTasksFile.exists() -> mapper.readValue<List<Task>>(baselineTasksFile)
                else -> emptyList()
            }
        }

        val baselineTasksById = baselineTasks.associateBy { it.id }

        val storiesByDate = sprint.schedule.dates.associateWith {
            val storiesFile = Paths.get(".", parent.namespace, "stories-${it}.yml")
                .toAbsolutePath()
                .toFile()
            when {
                storiesFile.exists() -> mapper.readValue<List<Story>>(storiesFile)
                else -> emptyList()
            }
        }

        val tasksByDate = sprint.schedule.dates.associateWith { date ->
            val tasksFile = Paths.get(".", parent.namespace, "tasks-${date}.yml")
                .toAbsolutePath()
                .toFile()
            when {
                tasksFile.exists() -> mapper.readValue<List<Task>>(tasksFile)
                else -> emptyList()
            }
        }

        val storiesById = (baselineStories + storiesByDate.flatMap { it.value })
            .associateBy { it.id }

        val storyIds = storiesById.keys

        val date = date ?: OffsetDateTime.now()
            .withOffsetSameInstant(sprint.schedule.offset)
            .toLocalDate()

        val summary = SprintSummary(
            date = date,
            metadata = sprint,
            taskStats = TaskStatistics(
                date = date,
                schedule = sprint.schedule,
                baselineTasks = baselineTasks,
                baselineTasksById = baselineTasksById,
                tasksByDate = tasksByDate
            ),
            storyStats = StoryStatistics(
                date = date,
                schedule = sprint.schedule,
                storiesById = storiesById,
                storiesByDate = storiesByDate,
                baselineTasks = baselineTasks,
                baselineTasksById = baselineTasksById,
                tasksByDate = tasksByDate
            ),
            capacityStats = _root_ide_package_.com.github.richardkabiling.scrum.cli.CapacityStatistics(
                date = date,
                members = sprint.members,
                schedule = sprint.schedule,
                capacity = sprint.effectiveCapacity,
                baselineTasks = baselineTasks,
                tasksByDate = tasksByDate
            ),
            storiesById = storiesById,
            storyIds = storyIds
        )

        val statsFile = Paths.get(".", parent.namespace, "sprint-${sprintName}-stats-${date}.yml")
            .toAbsolutePath()
            .toFile()
        mapper.writeValue(statsFile, summary)
    }

}