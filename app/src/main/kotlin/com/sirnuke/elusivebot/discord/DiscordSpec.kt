package com.sirnuke.elusivebot.discord

import com.uchuhimo.konf.ConfigSpec

object DiscordSpec : ConfigSpec() {
    val serviceId by optional("Discord")
    val discordToken by required<String>()

    object Kafka : ConfigSpec() {
        val bootstrap by required<String>()
        val producerTopic by optional("messages-input")
        val consumerTopic by optional("messages-output")
    }
}
