package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

@CommandLine.Command(
    name = "baseline",
    aliases = ["base", "snapshot", "snap"],
    mixinStandardHelpOptions = true
)
class StoryBaselineCommand : Runnable {
    @CommandLine.Option(
        names = ["-f", "--force"],
        description = ["overrides existing stories"],
        defaultValue = "false",
    )
    var force: Boolean = false

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
        val indexFile = Paths.get(".", parent.parent.namespace.sanitized(), "sprints.yml")
            .toAbsolutePath()
            .toFile()
        val index: LinkedHashSet<String> = mapper.readValue(indexFile)
        val sprintName = index.lastOrNull() ?: throw SprintNotFoundException()

        if (!storiesFile.isFile)
            throw StoriesFileReadException("Stories file is not file")
        if (!storiesFile.exists())
            throw StoriesFileReadException("Stories file does not exist")

        val stories: List<Story> = mapper.readValue(storiesFile)

        val detailedStoriesFile =
            Paths.get(".", parent.parent.namespace, "sprint-${sprintName.sanitized()}-stories-baseline.yml")
                .toAbsolutePath()
                .toFile()
        if (detailedStoriesFile.exists() && !force)
            throw StoriesAlreadyExistsException()

        mapper.writeValue(detailedStoriesFile, stories)
    }
}