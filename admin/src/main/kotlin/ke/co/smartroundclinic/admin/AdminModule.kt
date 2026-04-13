package ke.co.smartroundclinic.admin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.admin.domain.service.KmpdcService
import ke.co.smartroundclinic.admin.domain.service.SpecialityService
import ke.co.smartroundclinic.admin.presentation.controller.kmpdcController
import ke.co.smartroundclinic.admin.presentation.controller.specialityController
import org.koin.ktor.ext.inject

fun Application.adminModule() {
    val specialityService: SpecialityService by inject()
    val kmpdcService: KmpdcService by inject()
    routing {
        specialityController(specialityService)
        kmpdcController(kmpdcService)
    }
}
