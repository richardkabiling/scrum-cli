package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import picocli.CommandLine.*
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

@Command(
    name = "baseline",
    aliases = ["base", "snapshot", "snap"],
    mixinStandardHelpOptions = true
)
class TaskBaselineCommand : Runnable {

    @Option(
        names = ["-f", "--force"],
        description = ["overrides existing tasks"],
        defaultValue = "false",
    )
    var force: Boolean = false

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
        val indexFile = Paths.get(".", parent.parent.namespace.sanitized(), "sprints.yml")
            .toAbsolutePath()
            .toFile()
        val index: LinkedHashSet<String> = mapper.readValue(indexFile)
        val sprintName = index.lastOrNull() ?: throw SprintNotFoundException()

        if (!tasksFile.isFile)
            throw TasksFileReadException("Tasks file is not file")
        if (!tasksFile.exists())
            throw TasksFileReadException("Tasks file does not exist")

        val tasks: List<Task> = mapper.readValue(tasksFile)

        val detailedTasksFile =
            Paths.get(".", parent.parent.namespace, "sprint-${sprintName.sanitized()}-tasks-baseline.yml")
                .toAbsolutePath()
                .toFile()
        if (detailedTasksFile.exists() && !force)
            throw TasksAlreadyExistsException()

        mapper.writeValue(detailedTasksFile, tasks)
    }
}