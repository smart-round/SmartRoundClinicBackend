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
    implementation(project(":scheduling"))
    implementation(project(":doctor"))
    implementation(project(":medical-records"))
    testImplementation(libs.kotlin.test.junit)
}
