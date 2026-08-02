package ke.co.smartroundclinic.referral

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.referral.domain.service.ReferralService
import ke.co.smartroundclinic.referral.presentation.controller.referralController
import org.koin.ktor.ext.inject

fun Application.referralModule() {
    val service: ReferralService by inject()

    routing {
        referralController(service)
    }
}
