package ke.co.smartroundclinic.doctor

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.doctor.domain.service.PractitionerProfileService
import ke.co.smartroundclinic.doctor.presentation.controller.practitionerProfileController
import org.koin.ktor.ext.inject

fun Application.doctorModule() {
    val profileService: PractitionerProfileService by inject()
    routing {
        practitionerProfileController(profileService)
    }
}