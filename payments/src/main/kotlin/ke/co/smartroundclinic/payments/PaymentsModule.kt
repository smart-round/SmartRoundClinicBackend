package ke.co.smartroundclinic.payments

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.payments.domain.service.IntaSendService
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.presentation.controller.doctorPaymentsController
import ke.co.smartroundclinic.payments.presentation.controller.intaSendController
import ke.co.smartroundclinic.payments.presentation.controller.patientPaymentsController
import ke.co.smartroundclinic.payments.presentation.controller.paymentController
import org.koin.ktor.ext.inject

fun Application.paymentsModule() {
    val paymentService: PaymentService by inject()
    val intaSendService: IntaSendService by inject()
    routing {
        paymentController(paymentService)
        intaSendController(intaSendService, AppConfig.intaSend.webhookChallenge)
        doctorPaymentsController(paymentService)
        patientPaymentsController(paymentService)
    }
}
