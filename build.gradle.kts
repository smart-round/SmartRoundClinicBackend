plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "ke.co"
version = "0.0.1"

application {
    mainClass = "ke.co.ApplicationKt"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":auth"))
    implementation(project(":article"))
    implementation(project(":admin"))
    implementation(project(":doctor"))
    implementation(project(":patient"))
    implementation(project(":notification"))
    implementation(project(":scheduling"))
    implementation(project(":support"))
    implementation(project(":consultation"))
    implementation(project(":payments"))
    implementation(project(":medical-records"))
    implementation(project(":referral"))
    implementation(project(":doctor-chat"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
