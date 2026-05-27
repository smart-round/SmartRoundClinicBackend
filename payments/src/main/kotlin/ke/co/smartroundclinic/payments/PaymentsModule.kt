package ke.co.smartroundclinic.payments

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.presentation.controller.paymentController
import org.koin.ktor.ext.inject

fun Application.paymentsModule() {
    val paymentService: PaymentService by inject()
    routing {
        paymentController(paymentService)
    }
}
