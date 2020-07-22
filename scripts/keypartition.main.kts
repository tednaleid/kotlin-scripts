#!/bin/sh
//usr/bin/env true; exec kotlinc -script "$0" -- "$@"  # better than #!/usr/bin/env kotlin as that doesn't support switches without "--" prefix

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("com.google.guava:guava:28.0-jre")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import kotlin.math.absoluteValue

fun HashFunction.absoluteValueOf(value: String) = this.hashBytes(value.toByteArray()).asInt().absoluteValue
fun HashFunction.bucketFor(value: String, buckets: Int) =
    this.hashBytes(value.toByteArray()).asInt().absoluteValue % buckets

class KeyPartition : CliktCommand(
    help = """Calculates a murmur3_32 hash for each line it is piped via stdin.

It will emit the original value followed by the partition value, ex:
```
seq 1000 1004 | scripts/keypartition.main.kts --partitions=10
1000 4
1001 5
1002 6
1003 6
1004 1
```

It can also be passed a regular expression to calculate a hash on part of the line, and will emit the line,
the hashed portion, and the partition, ex:
```
seq 1000 1004 | scripts/keypartition.main.kts --partitions=10 --regex '\d{3}${'$'}'
1000 000 0
1001 001 6
1002 002 1
1003 003 0
1004 004 3
``` """
) {

    private val partitions: Int by option(help = "Number of zero-based partitions, default: 24").int().default(24)
    private val regex: String? by option(help = "Regular expression to extract only a portion the line to hash, ex: \"^.*:.*[^:]\"")
    private val input by option(help = "Lines to hash, defaults to using stdin").inputStream().defaultStdin()

    private fun hashByLine() {
        input.bufferedReader().lines().forEach { line ->
            val bucket = Hashing.murmur3_32().bucketFor(line, partitions)
            echo("$line $bucket")
        }
    }

    private fun hashPortionOfLine(keyRegex: Regex) {
        input.bufferedReader().lines().forEach { line ->
            val key = keyRegex.find(line)?.value

            if (key != null) {
                val bucket = Hashing.murmur3_32().bucketFor(key, partitions)
                echo("$line $key $bucket")
            } else {
                echo(line)
            }
        }
    }

    override fun run() {
        val keyRegex = regex?.toRegex()
        if (keyRegex == null) hashByLine() else hashPortionOfLine(keyRegex)
    }
}

KeyPartition().main(args)