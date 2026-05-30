package ke.co.smartroundclinic.payments.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.payments.domain.service.PaymentService

fun Route.patientPaymentsController(service: PaymentService) {
    authenticate("auth-jwt") {
        route("/patient/payments") {

            // GET /patient/payments?page=1&size=20
            // Patient views their full payment history.
            get {
                call.requireRole("PATIENT") {
                    val patientId = call.getUserId() ?: return@requireRole
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.getByPatient(patientId, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /patient/payments/{id}
            // Patient views a specific payment record.
            get("{id}") {
                call.requireRole("PATIENT") {
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
