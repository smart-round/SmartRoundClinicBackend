package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.domain.service.IntaSendPaymentService
import ke.co.smartroundclinic.payments.presentation.dto.request.MpesaPaymentBody

private const val PATIENT = "PATIENT"

fun Route.intaSendPaymentController(service: IntaSendPaymentService) {
    authenticate("auth-jwt") {
        route("/payments/intasend/pay") {

            // POST /payments/intasend/pay/mpesa
            // Patient pays for their appointment via MPesa STK push.
            // appointmentId is forwarded as api_ref so the webhook callback
            // can match the transaction back to the appointment.
            post("mpesa") {
                call.requireRole(PATIENT) {
                    val body = call.receive<MpesaPaymentBody>()
                    val result = service.payViaMpesa(body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
