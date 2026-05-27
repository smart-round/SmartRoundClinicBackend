rootProject.name = "smartroundclinic"

include(":admin")
include(":doctor")
include(":patient")
include(":notification")
include(":auth")
include(":infra")
include(":common")
include(":scheduling")
include(":support")
include(":article")
include(":consultation")
include(":payments")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
