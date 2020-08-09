#!/usr/bin/env -S kotlinc -script --

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")

// extra datatypes for JSON parsing, Java8 and Date/Time
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.2")
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import java.time.LocalDateTime

class ParseJson : CliktCommand(
    help = """
Simple JSON parsing/pretty printing where it will search for input with a specific ID.

It will also ignore and strip out extra JSON fields.

to see an example, try: 
```
cat <<EOF | scripts/parsejson.main.kts --id 433
  {"id":"169","value": 1, "date": null}
  {"id":"170","value": 2, "other": "ignored"}
  {"id":"433","value": 3,"date":"2009-12-03T10:45:00","foo":"bar"}
EOF
```

output:
```
{
  "id" : "433",
  "value" : 3,
  "date" : "2007-12-03T10:45:00"
}
```

"""
) {

    private val input by option(help = "input, defaults to stdin").inputStream().defaultStdin()
    private val id by option(help = "the id to find, default: 170").default("170")

    private val jackson = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .findAndRegisterModules() // finds jdk8 and kotlin modules

    private val prettyWriter = jackson.writerWithDefaultPrettyPrinter()

    override fun run() {

        input.bufferedReader().lines().forEach { line ->
            val data = jackson.readValue(line, Data::class.java)
            if (data.id == id) {
                echo(prettyWriter.writeValueAsString(data))
            }
        }
    }
}

data class Data(val id: String, val value: Long, val date: LocalDateTime?)

ParseJson().main(args)