plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    application
    id("net.researchgate.release") version "3.0.2"
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlin {
        diktat()
        toggleOffOn()
    }
    kotlinGradle {
        diktat()
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "com.sirnuke.elusivebot"

val kafkaApiVersion = "3.6.1"

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.12")
    implementation("com.uchuhimo:konf:1.1.2")
    implementation("dev.kord:kord-core:0.13.1")
    implementation("org.apache.kafka:kafka-streams:$kafkaApiVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaApiVersion")

    implementation("com.sirnuke.elusivebot:elusivebot-schema:0.1.0-SNAPSHOT")
    implementation("com.sirnuke.elusivebot:elusivebot-common:0.1.0-SNAPSHOT")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaApiVersion")
}

application {
    mainClass.set("com.sirnuke.elusivebot.discord.MainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("printVersion") {
    doLast {
        @Suppress("DEBUG_PRINT")
        println(project.version)
    }
}

configure<net.researchgate.release.ReleaseExtension> {
    tagTemplate.set("v\${version}")
    with(git) {
        requireBranch.set("main")
    }
}
