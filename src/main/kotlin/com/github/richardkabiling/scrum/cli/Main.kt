package com.github.richardkabiling.scrum.cli

import io.micronaut.configuration.picocli.PicocliRunner

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.run(ScrumCommand::class.java, *args)
        }
    }
}
