plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "org.app"
version = "1.0-SNAPSHOT"
val kotest = "6.0.0"
val kSerialization = "1.9.0"
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kSerialization")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
    testImplementation("io.kotest:kotest-property:$kotest")
}

tasks.test {
    useJUnitPlatform()
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}