plugins {
    `java-library`
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
}

group = "org.app"
version = "1.0-SNAPSHOT"
val kotest = "6.0.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
    testImplementation("io.kotest:kotest-property:$kotest")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}