package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject

@CommandLine.Command(
    name = "add",
    aliases = ["put", "set"],
    mixinStandardHelpOptions = true
)
class StoryAddCommand : Runnable {

    @CommandLine.Option(
        names = ["-f", "--force"],
        description = ["overrides existing stories"],
        defaultValue = "false",
    )
    var force: Boolean = false

    @CommandLine.Option(
        names = ["-d", "--date"],
        description = ["date of stories"],
    )
    var date: LocalDate? = null

    @CommandLine.Parameters(
        index = "0",
        description = ["stories file in yml format"]
    )
    lateinit var storiesFile: File

    @CommandLine.ParentCommand
    lateinit var parent: StoryCommand

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

        if (!storiesFile.isFile)
            throw StoriesFileReadException("Stories file is not file")
        if (!storiesFile.exists())
            throw StoriesFileReadException("Stories file does not exist")

        val stories: List<Story> = mapper.readValue(storiesFile)

        val date = date ?: OffsetDateTime.now()
            .withOffsetSameInstant(sprint.schedule.offset)
            .toLocalDate()

        val detailedStoriesFile = Paths.get(".", parent.parent.namespace.sanitized(), "stories-${date}.yml")
            .toAbsolutePath()
            .toFile()
        if (detailedStoriesFile.exists() && !force)
            throw StoriesAlreadyExistsException()

        mapper.writeValue(detailedStoriesFile, stories)
    }

}

