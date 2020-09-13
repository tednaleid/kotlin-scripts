#!/usr/bin/env -S kotlinc -script --

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("com.typesafe:config:1.4.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigValueType
import java.io.File
import java.net.URL

class Hocon : CliktCommand(
    help = """
        A quick CLI that can query HOCON config files
"""
) {

    private val file: File? by option(help = "a HOCON file, if missing will look for file names to be piped via stdin").file()
    private val query: String? by option(help = "the paths in the file(s) to query, can be comma separated for multiples")

    override fun run() {
        val theFile: File? = file
        val queries = query?.toString()?.split("[, ]+".toRegex()) ?: emptyList()
        if (theFile != null) {
            queryConfig(theFile.toURI().toURL(), queries)
        } else {
            while (true) when (val line = readLine()) {
                null -> break
                else -> queryConfig(File(line).toURI().toURL(), queries)
            }
        }
    }

    private fun queryConfig(url: URL, queries: List<String>) {
        val resolved = parse(url)
        if (queries.isNotEmpty()) {
            val results: List<String> = queries.map {
                val value = try {
                    resolved.getValue(it)
                } catch (e: Exception) {
                    null
                }
                when (value?.valueType()) {
                    ConfigValueType.STRING -> value.unwrapped().toString()
                    null -> "null"
                    else -> value.render()
                }
            }
            println(results.joinToString(" "))
        } else {
            println(resolved)
        }

    }

    private fun parse(url: URL): Config = ConfigFactory.parseURL(url, ConfigParseOptions.defaults())
}

Hocon().main(args)
