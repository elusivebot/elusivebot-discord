/**
 * Entrypoint for ElusiveBot's Discord interface.
 */

package com.sirnuke.elusivebot.discord

import com.sirnuke.elusivebot.common.Kafka
import com.sirnuke.elusivebot.schema.ChatMessage
import com.sirnuke.elusivebot.schema.Header
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import org.slf4j.LoggerFactory

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

    // spotless:off
    val config = Config { addSpec(DiscordSpec) }
        .from.yaml.file("/config/discord-service.yml")
        .from.env()
    // spotless:on

    log.info(
        "Starting Discord service producer {} & consumer {}", config[DiscordSpec.Kafka.producerTopic],
        config[DiscordSpec.Kafka.consumerTopic]
    )

    val running = AtomicBoolean(true)

    val kord = Kord(config[DiscordSpec.discordToken])

    val kafka = Kafka.Builder(
        applicationId = config[DiscordSpec.serviceId],
        bootstrap = config[DiscordSpec.Kafka.bootstrap],
        scope = this,
    ).registerConsumer(config[DiscordSpec.Kafka.consumerTopic], { stream ->
        stream.filter { key, _ -> key == config[DiscordSpec.serviceId] }
    }) { _, key, msg: ChatMessage ->
        log.info("Got message from the bot: {}", msg.message)
        if (running.get()) {
            this.launch {
                val channelId = msg.header.channelId?.toULong()
                channelId ?: run {
                    log.warn("Received response message without a channelId! {}: {}", key, msg)
                    return@launch
                }
                val channel: MessageChannel? = kord.getChannelOf(Snowflake(channelId))
                channel ?: run {
                    log.warn("Unable to find channel with id {}: {}: {}", channelId, key, msg)
                    return@launch
                }
                channel.createMessage(content = msg.message)
            }
        }
    }.build()

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
        kafka.send(
            topic = config[DiscordSpec.Kafka.producerTopic],
            key = config[DiscordSpec.serviceId],
            message = Json.encodeToString(chatMessage),
        ) { _, ex ->
            ex?.let {
                log.error("Unable to forward Discord response", ex)
            } ?: log.info("Done forwarding Discord message")
        }
    }

    Runtime.getRuntime().addShutdownHook(thread(start = false, name = "shutdown-hook") {
        running.set(false)
        // TODO: Is the order of closing things going to be an issue?
        this.launch { kord.shutdown() }
        kafka.close()
    })

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
