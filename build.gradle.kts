import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os

group = "no.nav.syfo"
version = "0.0.1"

object Versions {
    const val confluent = "7.5.1"
    const val flyway = "9.22.3"
    const val hikari = "5.1.0"
    const val jacksonDataType = "2.16.1"
    const val kafka = "3.6.1"
    const val kluent = "1.73"
    const val ktor = "2.3.8"
    const val logback = "1.4.14"
    const val logstashEncoder = "7.4"
    const val micrometerRegistry = "1.12.2"
    const val mockk = "1.13.9"
    const val nimbusJoseJwt = "9.37.3"
    const val postgres = "42.7.2"
    val postgresEmbedded = if (Os.isFamily(Os.FAMILY_MAC)) "1.0.0" else "0.13.4"
    const val scala = "2.13.12"
    const val spek = "2.0.19"
}

plugins {
    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.ktor:ktor-client-apache:${Versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-jackson:${Versions.ktor}")
    implementation("io.ktor:ktor-server-auth-jwt:${Versions.ktor}")
    implementation("io.ktor:ktor-server-call-id:${Versions.ktor}")
    implementation("io.ktor:ktor-server-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-server-status-pages:${Versions.ktor}")

    // Logging
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoder}")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:${Versions.ktor}")
    implementation("io.micrometer:micrometer-registry-prometheus:${Versions.micrometerRegistry}")

    // Database
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    testImplementation("com.opentable.components:otj-pg-embedded:${Versions.postgresEmbedded}")
    constraints {
        implementation("org.apache.commons:commons-compress") {
            because("com.opentable.components:otj-pg-embedded:${Versions.postgresEmbedded} -> https://www.cve.org/CVERecord?id=CVE-2021-36090")
            version {
                require("1.26.0")
            }
        }
    }

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:${Versions.kafka}", excludeLog4j)
    constraints {
        implementation("org.apache.zookeeper:zookeeper") {
            because("org.apache.kafka:kafka_2.13:${Versions.kafka} -> https://www.cve.org/CVERecord?id=CVE-2023-44981")
            version {
                require("3.8.3")
            }
        }
    }

    // (De-)serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jacksonDataType}")

    // Tests
    testImplementation("io.ktor:ktor-server-tests:${Versions.ktor}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("io.ktor:ktor-client-mock:${Versions.ktor}")
    testImplementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusJoseJwt}")
    testImplementation("org.amshove.kluent:kluent:${Versions.kluent}")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spek}")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.spek}")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}
