package ke.co.smartroundclinic.auth

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.auth.domain.service.UserService
import ke.co.smartroundclinic.auth.domain.usecase.GetUserStatsUseCase
import ke.co.smartroundclinic.auth.domain.usecase.TrackUserPresenceUseCase
import ke.co.smartroundclinic.auth.presentation.controller.adminStatsController
import ke.co.smartroundclinic.auth.presentation.controller.doctorController
import ke.co.smartroundclinic.auth.presentation.controller.patientController
import ke.co.smartroundclinic.auth.presentation.controller.presenceController
import ke.co.smartroundclinic.auth.presentation.controller.userController
import ke.co.smartroundclinic.common.DoctorOnboardingHandler
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject

fun Application.authModule() {
    val userService: UserService by inject()
    val getUserStatsUseCase: GetUserStatsUseCase by inject()
    val trackUserPresenceUseCase: TrackUserPresenceUseCase by inject()
    val onboardingHandler: DoctorOnboardingHandler? = getKoin().getOrNull()
    routing {
        userController(userService)
        patientController(userService)
        doctorController(userService, onboardingHandler)
        adminStatsController(getUserStatsUseCase)
        presenceController(trackUserPresenceUseCase)
    }
}
