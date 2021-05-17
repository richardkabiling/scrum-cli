package com.github.richardkabiling.scrum.cli

import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec

@Command(
    name = "task",
    aliases = ["tasks"],
    mixinStandardHelpOptions = true,
    subcommands = [
        TaskAddCommand::class,
        TaskBaselineCommand::class
    ]
)
class TaskCommand : Runnable {

    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    lateinit var parent: ScrumCommand

    override fun run() {
        throw ParameterException(spec.commandLine(), "too few arguments")
    }

}
