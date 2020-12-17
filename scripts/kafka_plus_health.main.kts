#!/usr/bin/env -S kotlinc -script --

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("org.apache.kafka:kafka-clients:2.5.0")
@file:DependsOn("org.apache.kafka:kafka-streams:2.5.0")
@file:DependsOn("org.http4k:http4k-core:3.254.0")
@file:DependsOn("org.http4k:http4k-server-jetty:3.254.0")
@file:DependsOn("org.slf4j:slf4j-nop:1.7.30")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import kotlinx.coroutines.runBlocking
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.http
import java.util.Properties
import java.util.UUID


class KafkaStreams : CliktCommand(
    help = """
        A quick CLI that uses Kafka streams to consume a topic from a kafka broker
"""
) {

    private val broker: String by option(help = "The kafka broker <host>:<port> to connect to").default("localhost:9092")
    private val topic: String by option(help = "The name of the Kafka topic to consume").required()

    fun streamKafka() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "kafka-streams-cli-${UUID.randomUUID()}"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = broker
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.ByteArray().javaClass
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.ByteArray().javaClass

        val buff = System.out.buffered()

        val builder = StreamsBuilder()
        val events: KStream<ByteArray, ByteArray> = builder.stream(topic)

        val newlineBytes = System.lineSeparator().toByteArray()

        events.foreach { _: ByteArray?, value: ByteArray? ->
            if (value != null) buff.write(value)
            buff.write(newlineBytes)
        }


        val topology = builder.build()

        val streams = KafkaStreams(topology, props)
        streams.setUncaughtExceptionHandler(ExitProcessExceptionHandler)
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { streams.close() }))
        streams.start()
    }

    private val healthServer = { _: Request ->
        Response(OK).body("Healthy!")
    }.asServer(Jetty(9000))

    override fun run() {
        runBlocking {
            launch { healthServer.start()}
            streamKafka()
        }
    }

    object ExitProcessExceptionHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            echo("uncaught exception in thread [${thread.name}], error: ${error.message}", err = true)
            exitProcess(1)
        }
    }
}


KafkaStreams().main(args)
