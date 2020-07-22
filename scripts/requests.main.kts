#!/bin/sh
//usr/bin/env true; exec kotlinc -script "$0" -- "$@"  # better than #!/usr/bin/env kotlin as that doesn't support switches without "--" prefix

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.8.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class Server : CliktCommand(
    help = """
        A simple http client that makes requests to urls piped via stdin
        
        ex makes 100 requests. Sent to http://localhost/foobar/1 .. http://localhost/foobar/100:
        
        seq 100 | awk '{printf "http://localhost:8000/foobar/%s\n", ${'$'}1}' | scripts/requests.main.kts
"""
) {
    companion object {
        private val APPLICATION_JSON = "application/json".toMediaType()
    }

    private val input by option(help = "input, defaults to stdin").inputStream().defaultStdin()

    private val requestBuilder = Request.Builder()
    private val client = OkHttpClient()

    override fun run() {
        input.bufferedReader().lines().forEach { line ->
            val body = "{\"value\": \"$line\"}".toRequestBody(APPLICATION_JSON)
            val request = requestBuilder.url(line).post(body).build()
            val response = client.newCall(request).execute()
            echo("$line : ${response.code} : ${response.body?.string()}")
        }
    }
}

Server().main(args)