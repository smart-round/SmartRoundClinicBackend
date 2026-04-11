plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":common"))
    
    api(libs.ktor.server.core)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.serialization.gson)
    api(libs.kotlin.client.content.negotiation)
    api(libs.ktor.client.cio)
    api(libs.koin.ktor)
    api(libs.koin.logger.slf4j)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.metrics.micrometer)
    api(libs.micrometer.registry.prometheus)
    api(libs.ktor.server.cors)
    api(libs.ktor.server.csrf)
    api(libs.ktor.server.websockets)
    api(libs.ktor.server.metrics)
    api(libs.cohort.ktor)
    api(libs.kotlin.mongodb.bom)
    api(libs.kotlin.mongodb.driver.coroutine)
    api(libs.kotlin.mongodb.bson.kotlinx)
    implementation(libs.kotlin.aws.s3)
}
