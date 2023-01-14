plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    application
    id("net.researchgate.release") version "3.0.2"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

application {
    mainClass.set("com.sirnuke.elusivebot.discord.MainKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("printVersion") {
  doLast {
    println(project.version)
  }
}

configure<net.researchgate.release.ReleaseExtension> {
  tagTemplate.set("v\${version}")
    with(git) {
      requireBranch.set("master")
    }
}

group = "com.sirnuke"
