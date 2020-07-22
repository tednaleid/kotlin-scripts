#!/bin/sh
//usr/bin/env true; exec kotlinc -script "$0" -- "$@"  # better than #!/usr/bin/env kotlin as that doesn't support switches without "--" prefix

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("org.http4k:http4k-core:3.254.0")
@file:DependsOn("org.http4k:http4k-server-jetty:3.254.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Jetty
import org.http4k.server.asServer

class EchoServer : CliktCommand(
    help = """
        A simple server that echos back the requests that it gets with info about the request.
"""
) {

    private val port: Int by option(help = "the port to run the echo server on").int().default(8000)

    override fun run() {
        echo(message = "Running on localhost:${port}", err = true)

        val server = { request: Request ->
            Response(OK).body(request.bodyString())
        }.asServer(Jetty(port))

        server.start()
    }
}

EchoServer().main(args)
