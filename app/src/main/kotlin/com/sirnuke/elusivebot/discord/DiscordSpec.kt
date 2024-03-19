package com.sirnuke.elusivebot.discord

import com.uchuhimo.konf.ConfigSpec

object DiscordSpec : ConfigSpec() {
    val serviceId by optional("Discord")
    val discordToken by required<String>()

    object Kafka : ConfigSpec() {
        val bootstrap by required<String>()
        val producerChannel by optional("discord-input")
        val consumerChannel by optional("discord-output")
    }
}
