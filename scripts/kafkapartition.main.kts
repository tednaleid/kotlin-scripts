#!/usr/bin/env -S kotlinc -script --

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("commons-codec:commons-codec:1.15")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.int
import org.apache.commons.codec.digest.MurmurHash2

class KeyPartition : CliktCommand(
    help = """
Calculates the kafka partition using the Kafka default partitioning strategy for each line it is piped via stdin.  
        
It will emit the original value followed by the partition value, ex:
```
seq 1000 1004 | scripts/kafkapartition.main.kts --partitions=10
1000 6
1001 8
1002 1
1003 3
1004 2
```

It can also be passed a regular expression to calculate a hash on part of the line, and will emit the line,
the hashed portion, and the partition, ex:
```
seq 1000 1004 | scripts/kafkapartition.main.kts --partitions=10 --regex '\d{3}${'$'}'
1000 000 2
1001 001 2
1002 002 8
1003 003 0
1004 004 3
``` """
) {

    private val partitions: Int by option(help = "Number of zero-based partitions, default: 3").int().default(3)
    private val regex: String? by option(help = "Regular expression to extract only a portion the line to hash, ex: \"^.*:.*[^:]\"")
    private val input by option(help = "Lines to hash, defaults to using stdin").inputStream().defaultStdin()

    // used by the kafka default partitioning strategy, does NOT use absoluteValue to get a positive value
    // see: https://github.com/apache/kafka/blob/2.8/clients/src/main/java/org/apache/kafka/clients/producer/internals/DefaultPartitioner.java#L71
    // and: https://github.com/apache/kafka/blob/2.8/clients/src/main/java/org/apache/kafka/common/utils/Utils.java#L1019-L1034
    private fun Int.positiveValue(): Int = this and 0x7fffffff

    private fun bucketFor(value: String, buckets: Int) = MurmurHash2.hash32(value).positiveValue() % buckets

    private fun hashByLine() {
        input.bufferedReader().lines().forEach { line ->
            val bucket = bucketFor(line, partitions)
            echo("$line $bucket")
        }
    }

    private fun hashPortionOfLine(keyRegex: Regex) {
        input.bufferedReader().lines().forEach { line ->
            val key = keyRegex.find(line)?.value

            if (key != null) {
                val bucket = bucketFor(key, partitions)
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