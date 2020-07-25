#!/usr/bin/env -S kotlinc -script --

@file:DependsOn("com.github.ajalt:clikt:2.7.1")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.int

class Hello : CliktCommand(
    help = """try: echo "Ted\nWorld" | scripts/hello.main.kts --count 3

Help lines that keep their newlines:
```
one
two
``` """
) {

    private val count: Int by option(help = "a number of times to say hello to each line, default 2").int().default(2)
    private val input by option(help = "input, defaults to stdin").inputStream().defaultStdin()

    override fun run() {
        input.bufferedReader().lines().forEach { line ->
            (1..count).forEach { _ ->
                echo("Hello $line")
            }
        }
    }
}

Hello().main(args)