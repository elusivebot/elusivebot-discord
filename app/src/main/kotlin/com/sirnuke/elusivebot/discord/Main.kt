/**
 * Entrypoint for ElusiveBot's Discord interface.
 */

package com.sirnuke.elusivebot.discord

import com.uchuhimo.konf.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

import kotlin.concurrent.thread
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("TOO_LONG_FUNCTION")
fun main() = runBlocking {
    val log = LoggerFactory.getLogger("com.sirnuke.elusivebot.discord.MainKt")
    log.info("Starting Discord service")

    val config = Config { addSpec(DiscordSpec) }.from.env()

    val running = AtomicBoolean(true)

    val consumerConfig = StreamsConfig(
        mapOf<String, Any>(
            StreamsConfig.APPLICATION_ID_CONFIG to config[DiscordSpec.serviceId],
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to config[DiscordSpec.Kafka.bootstrap],
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name,
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name
        )
    )

    val builder = StreamsBuilder()

    val producerConfig = mapOf(
        "bootstrap.servers" to config[DiscordSpec.Kafka.bootstrap],
        "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer"
    )

    val producer: KafkaProducer<String, String> = KafkaProducer(producerConfig)

    val consumer: KStream<String, String> = builder.stream(config[DiscordSpec.Kafka.consumerChannel])

    consumer.foreach { key, raw ->
        log.info("Got input {} {}", key, raw)
        if (running.get()) {
            // TODO: Pass messages to Kord
        }
    }

    val streams = KafkaStreams(builder.build(), consumerConfig)
    streams.start()

    val kord = Kord(config[DiscordSpec.discordToken])

    kord.on<MessageCreateEvent> {
        // No bots, and cancel if in the process of shutting down the service
        if (!running.get() || message.author?.isBot != false) {
            return@on
        }
        // TODO: Process and pass message to Kafka
    }

    Runtime.getRuntime().addShutdownHook(thread(start = false, name = "shutdown-hook") {
        running.set(false)
        // TODO: Is the order of closing things going to be an issue?
        this.launch { kord.shutdown() }
        streams.close()
        producer.close()
    })

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
