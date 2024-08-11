plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.10"
    application
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
}

run {
    if (project.hasProperty("internalMavenUrl")) {
        val internalMavenUsername: String by project
        val internalMavenPassword: String by project
        val internalMavenUrl: String by project

        repositories {
            maven {
                credentials {
                    username = internalMavenUsername
                    password = internalMavenPassword
                }
                url = uri("$internalMavenUrl/releases")
                name = "Internal-Maven-Releases"
            }
        }

        repositories {
            maven {
                credentials {
                    username = internalMavenUsername
                    password = internalMavenPassword
                }
                url = uri("$internalMavenUrl/snapshots")
                name = "Internal-Maven-Snapshots"
            }
        }
    } else {
        repositories {
            mavenLocal()
        }
    }
}

group = "com.sirnuke.elusivebot"

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("com.uchuhimo:konf:1.1.2")
    implementation("dev.kord:kord-core:0.14.0")

    implementation("com.sirnuke.elusivebot:elusivebot-schema:0.1.0-SNAPSHOT")
    implementation("com.sirnuke.elusivebot:elusivebot-common:0.1.0-SNAPSHOT")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
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
