package com.github.richardkabiling.scrum.cli

import mu.KotlinLogging
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec

private val logger = KotlinLogging.logger { }

@Command(
    name = "scrum",
    mixinStandardHelpOptions = true,
    subcommands = [
        PlanCommand::class,
        TaskCommand::class,
        StoryCommand::class,
        StatsCommand::class
    ]
)
class ScrumCommand : Runnable {

    @Option(names = ["-n", "--namespace"], defaultValue = "chad")
    lateinit var namespace: String

    @Spec
    lateinit var spec: CommandSpec

    override fun run() {
        throw ParameterException(spec.commandLine(), "too few arguments")
    }
}
