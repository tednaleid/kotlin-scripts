#!/usr/bin/env kotlin

// requires kotlin on the command line to run:
// see this for installation instructions and other examples:
// https://github.com/tednaleid/kotlin-scripts

@file:DependsOn("com.github.ajalt:clikt:2.7.1")
@file:DependsOn("org.apache.kafka:kafka-clients:2.5.0")
@file:DependsOn("org.apache.kafka:kafka-streams:2.5.0")
@file:DependsOn("org.slf4j:slf4j-nop:1.7.30")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")

// extra datatypes for JSON parsing, Java8 and Date/Time
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.2")
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlin.system.exitProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID


class ConsumeItemChanges : CliktCommand(
    help = """
        A sample app that can read values from the item-changes topic and parse the JSON
        
        ex: 
        consumejson.main.kts --broker kafka-ttc-app.prod.target.com:9092 --topic item-changes --count 15
"""
) {

    private val broker: String by option(help = "The kafka broker <host>:<port> to connect to").default("kafka-ttc-app.prod.target.com:9092")
    private val topic: String by option(help = "The name of the Kafka topic to consume").default("item-changes")
    private val count: Int by option(help = "The number of messages to consume").int().default(10)

    private val jackson = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .findAndRegisterModules() // finds jdk8 and kotlin modules

    override fun run() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "kafka-streams-cli-${UUID.randomUUID()}"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = broker
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.ByteArray().javaClass
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.ByteArray().javaClass

        val buff = System.out.buffered()

        val builder = StreamsBuilder()
        val events: KStream<ByteArray, ByteArray> = builder.stream(topic)

        var counter = 0

        events.foreach { _: ByteArray?, _: ByteArray? ->
            buff.write("${++counter}\n".toByteArray())
        }

//        events.foreach { _: ByteArray?, value: ByteArray? ->
//            if (value != null) {
//                val itemChange: ItemChange = jackson.readValue(value)
//                buff.write("${itemChange.tcin} ${itemChange.type} ${itemChange.timeUuid ?: "null"}\n".toByteArray())
//            }
//        }

        val topology = builder.build()

        val streams = KafkaStreams(topology, props)
        streams.setUncaughtExceptionHandler(ExitProcessExceptionHandler)
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { streams.close() }))
        streams.start()
    }

    object ExitProcessExceptionHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            echo("uncaught exception in thread [${thread.name}], error: ${error.message}", err = true)
            exitProcess(1)
        }
    }
}

data class ItemChange(val tcin: String, val timeUuid: UUID?, val type: String, val createdDateTime: LocalDateTime)

ConsumeItemChanges().main(args)

