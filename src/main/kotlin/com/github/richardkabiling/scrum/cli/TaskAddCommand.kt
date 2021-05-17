package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import picocli.CommandLine.*
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject

@Command(
    name = "add",
    aliases = ["put", "set"],
    mixinStandardHelpOptions = true
)
class TaskAddCommand : Runnable {

    @Option(
        names = ["-f", "--force"],
        description = ["overrides existing tasks"],
        defaultValue = "false",
    )
    var force: Boolean = false

    @Option(
        names = ["-d", "--date"],
        description = ["date of tasks"],
    )
    var date: LocalDate? = null

    @Parameters(
        index = "0",
        description = ["tasks file in yml format"]
    )
    lateinit var tasksFile: File

    @ParentCommand
    lateinit var parent: TaskCommand

    @Inject
    lateinit var mapper: ObjectMapper

    override fun run() {
        val indexFile = Paths.get(".", parent.parent.namespace, "sprints.yml")
            .toAbsolutePath()
            .toFile()
        val index: LinkedHashSet<String> = mapper.readValue(indexFile)
        if (index.isEmpty()) throw SprintNotFoundException()

        val sprintName = index.last()
            .sanitized()

        val sprint: Sprint = run {
            val sprintFile = Paths.get(".", parent.parent.namespace, "sprint-$sprintName.yml")
                .toAbsolutePath()
                .toFile()
            mapper.readValue(sprintFile)
        }

        if (!tasksFile.isFile)
            throw TasksFileReadException("Tasks file is not file")
        if (!tasksFile.exists())
            throw TasksFileReadException("Tasks file does not exist")

        val tasks: List<Task> = mapper.readValue(tasksFile)

        val date = date ?: OffsetDateTime.now()
            .withOffsetSameInstant(sprint.schedule.offset)
            .toLocalDate()

        val detailedTasksFile = Paths.get(".", parent.parent.namespace, "tasks-${date}.yml")
            .toAbsolutePath()
            .toFile()
        if (detailedTasksFile.exists() && !force)
            throw TasksAlreadyExistsException()

        mapper.writeValue(detailedTasksFile, tasks)
    }
}