#!/bin/sh
//usr/bin/env true; exec kotlinc -script "$0" -- "$@"

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

class HashMod : CliktCommand() {

    private val buckets: Int by option(help = "Number of zero-based buckets, default: 24").int().default(24)
    private val regex: String? by option(help = "Regular expression to extract only a portion the line to hash, ex: \"^.*:.*[^:]\"")
    private val input by option(help = "Lines to hash, defaults to using stdin").inputStream().defaultStdin()

    private fun hashByLine() {
        input.bufferedReader().lines().forEach { line ->
            val bucket = Hashing.murmur3_32().bucketFor(line, buckets)
            echo("$line $bucket")
        }
    }

    private fun hashPortionOfLine(keyRegex: Regex) {
        input.bufferedReader().lines().forEach { line ->
            val key = keyRegex.find(line)?.value

            if (key != null) {
                val bucket = Hashing.murmur3_32().bucketFor(key, buckets)
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

HashMod().main(args)