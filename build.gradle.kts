val ktorVersion: String by project
val logbackVersion: String by project
val datetimeVersion: String by project
val kafkaVersion: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

application {
    mainClass.set("org.goal2be.standard.MainKt")
}

group = "goal2be"
version = "0.9.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // openapi
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")

    // misc.
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")

    // serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    // slf4j
    implementation("org.slf4j:slf4j-nop:2.0.3")

    // Authentication
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}