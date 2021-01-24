#!/usr/bin/env -S kotlinc -script --

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("com.google.guava:guava:30.1-jre")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import java.nio.charset.Charset

class BloomFilterLines : CliktCommand(
    help = """try: jot 1000 | scripts/bloomfilter.main.kts
        ```
        jot 2700000 | 
           scripts/bloomfilter.main.kts --expected 25000000 --false-rate 1000 2>/dev/null | 
           awk '
             { found[${'$'}0]++ } 
             NR % 100000 == 0 { printf "%20d - found true %15d false %15d\r", NR, found["true"], found["false"]}
             END { print "" }
           '
        ```
           
        Simple implementation of a bloom filter that will tell you if it has seen a line previously
        with an expected false positive rate of 1/(false rate).   So if false-rate is 1,000, then we expect to get
        a false positive every 1,000 times (as long as we stay under the expected unique values).  If we 
        go above the expected unique values, we will end up with a lot more false positives.
"""
) {

    private val expected: Int by option(help = "The number of expected unique values").int().default(100_000)
    private val falseRate: Int by option(help = "A false positive every N values").int().default(1_000)
    private val input by option(help = "input, defaults to stdin").inputStream().defaultStdin()

    override fun run() {
        val bloomFilter: BloomFilter<String> = BloomFilter.create(
            Funnels.stringFunnel(Charset.forName("UTF-8")),
            expected,
            1.0/falseRate
        )
        input.bufferedReader().lines().forEach { line ->
            println(if(bloomFilter.mightContain(line)) "true" else "false")
            bloomFilter.put(line)
        }
    }
}

BloomFilterLines().main(args)