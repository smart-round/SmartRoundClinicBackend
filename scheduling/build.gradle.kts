plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":admin"))
    implementation(libs.kotlinx.datetime)
    testImplementation(libs.kotlin.test.junit)
}
