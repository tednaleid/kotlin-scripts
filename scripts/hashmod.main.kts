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
    private val input by option(help = "Lines to hash, defaults to using stdin").inputStream().defaultStdin()

    override fun run() {
        input.bufferedReader().lines().forEach {
            val bucket = Hashing.murmur3_32().bucketFor(it, buckets)
            echo("$it $bucket")
        }
    }
}

HashMod().main(args)