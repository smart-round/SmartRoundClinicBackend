package ke.co.smartroundclinic.auth

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.auth.domain.service.UserService
import ke.co.smartroundclinic.auth.presentation.controller.doctorController
import ke.co.smartroundclinic.auth.presentation.controller.patientController
import ke.co.smartroundclinic.auth.presentation.controller.userController
import org.koin.ktor.ext.inject

fun Application.authModule(){
    val userService: UserService by inject()
    routing {
        userController(userService)
        patientController(userService)
        doctorController(userService)
    }
}