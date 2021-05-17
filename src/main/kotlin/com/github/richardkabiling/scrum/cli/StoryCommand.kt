package com.github.richardkabiling.scrum.cli

import picocli.CommandLine

@CommandLine.Command(
    name = "story",
    aliases = ["stories"],
    mixinStandardHelpOptions = true,
    subcommands = [
        StoryAddCommand::class,
        StoryBaselineCommand::class
    ]
)
class StoryCommand : Runnable {

    @CommandLine.Spec
    lateinit var spec: CommandLine.Model.CommandSpec

    @CommandLine.ParentCommand
    lateinit var parent: ScrumCommand

    override fun run() {
        throw CommandLine.ParameterException(spec.commandLine(), "too few arguments")
    }
}