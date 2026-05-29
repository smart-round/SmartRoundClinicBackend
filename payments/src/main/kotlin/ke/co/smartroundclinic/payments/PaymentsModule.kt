package ke.co.smartroundclinic.payments

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.payments.domain.service.PaymentService
import ke.co.smartroundclinic.payments.domain.service.SubAccountService
import ke.co.smartroundclinic.payments.domain.service.VerificationService
import ke.co.smartroundclinic.payments.presentation.controller.paymentController
import ke.co.smartroundclinic.payments.presentation.controller.subAccountController
import ke.co.smartroundclinic.payments.presentation.controller.verificationController
import org.koin.ktor.ext.inject

fun Application.paymentsModule() {
    val paymentService: PaymentService by inject()
    val subAccountService: SubAccountService by inject()
    val verificationService: VerificationService by inject()
    routing {
        paymentController(paymentService)
        subAccountController(subAccountService)
        verificationController(verificationService)
    }
}
