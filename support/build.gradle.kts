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
    implementation(project(":notification"))
    testImplementation(libs.kotlin.test.junit)
}
