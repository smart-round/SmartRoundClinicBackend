package ke.co.smartroundclinic.patient

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.patient.domain.service.PersonalInformationService
import ke.co.smartroundclinic.patient.presentation.controller.personalInformationController
import org.koin.ktor.ext.inject

fun Application.patientModule() {
    val personalInformationService: PersonalInformationService by inject()
    routing {
        personalInformationController(personalInformationService)
    }
}
