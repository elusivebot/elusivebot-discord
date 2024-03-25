/**
 * Entrypoint for ElusiveBot's Discord interface.
 */

package com.sirnuke.elusivebot.discord

import com.sirnuke.elusivebot.schema.common.Header
import com.sirnuke.elusivebot.schema.messages.ChatMessage
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory

import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.concurrent.thread
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(PrivilegedIntent::class)
@Suppress("TOO_LONG_FUNCTION")
fun main() = runBlocking {
    val log = LoggerFactory.getLogger("com.sirnuke.elusivebot.discord.MainKt")
    log.info("Starting Discord service")

    // spotless:off
    val config = Config { addSpec(DiscordSpec) }
        .from.yaml.file("/config/discord-service.yml")
        .from.env()
    // spotless:on

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

    val kord = Kord(config[DiscordSpec.discordToken])

    consumer.foreach { key, raw ->
        log.info("Got input {} {}", key, raw)
        val chatMessage: ChatMessage = Json.decodeFromString(raw)
        if (running.get()) {
            this.launch {
                val channelId = chatMessage.header.channelId?.toULong()
                channelId ?: run {
                    log.warn("Received response message without a channelId! {}: {}", key, raw)
                    return@launch
                }
                val channel: MessageChannel? = kord.getChannelOf(Snowflake(channelId))
                channel ?: run {
                    log.warn("Unable to find channel with id {}: {}: {}", channelId, key, raw)
                    return@launch
                }
                channel.createMessage(content = chatMessage.message)
            }
        }
    }

    val streams = KafkaStreams(builder.build(), consumerConfig)
    streams.start()

    kord.on<MessageCreateEvent> {
        // No bots, and cancel if in the process of shutting down the service
        if (!running.get() || message.author?.isBot != false) {
            return@on
        }
        // TODO: Fairly certain guildId is null if it's a DM, may want a batter way of handling this
        val guildId = this.guildId?.toString() ?: "direct-message"
        val channelId = message.channelId.toString()
        val chatMessage = ChatMessage(
            header = Header(
                serviceId = config[DiscordSpec.serviceId],
                serverId = guildId,
                channelId = channelId,
            ),
            user = message.author?.id.toString(),
            message = message.content,
        )
        producer.send(
            ProducerRecord(
                config[DiscordSpec.Kafka.producerChannel], channelId, Json.encodeToString(chatMessage)
            )
        ) { _: RecordMetadata?, ex: Exception? ->
            // TODO: Better logging
            ex?.let {
                log.error("Unable to forward Discord response", ex)
            } ?: log.info("Done forwarding Discord message")
        }
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
