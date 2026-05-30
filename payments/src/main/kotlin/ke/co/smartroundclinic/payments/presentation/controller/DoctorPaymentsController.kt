package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.domain.service.PaymentService

fun Route.doctorPaymentsController(service: PaymentService) {
    authenticate("auth-jwt") {
        route("/doctor/payments") {

            // GET /doctor/payments?page=1&size=20&status=COMPLETED|PENDING
            // Doctor views their own payment history, optionally filtered by status.
            get {
                call.requireRole("DOCTOR") {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByDoctor(doctorId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/payments/summary
            // Returns total earned (COMPLETED), total pending, and transaction counts.
            get("summary") {
                call.requireRole("DOCTOR") {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getDoctorSummary(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
