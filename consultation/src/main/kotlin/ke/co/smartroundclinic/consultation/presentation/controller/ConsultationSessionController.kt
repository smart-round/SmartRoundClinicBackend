package ke.co.smartroundclinic.consultation.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.presentation.dto.request.StartConsultationReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"
private val PARTICIPANTS = listOf("DOCTOR", "PATIENT")

fun Route.consultationSessionController(service: ConsultationSessionService) {
    authenticate("auth-jwt") {
        route("/consultation") {

            // POST /consultation
            // Start or retrieve an existing consultation for an appointment.
            post {
                val userId = call.getUserId() ?: return@post
                val body = call.receive<StartConsultationReq>()
                val result = service.start(body.appointmentId, userId)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // GET /consultation?id=X  or  GET /consultation?appointmentId=X
            get {
                val id = call.request.queryParameters["id"]
                val appointmentId = call.request.queryParameters["appointmentId"]
                val result = when {
                    id != null -> service.getById(id)
                    appointmentId != null -> service.getByAppointment(appointmentId)
                    else -> throw MissingParametersException("id or appointmentId query parameter is required")
                }
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }

            // PATCH /consultation/{id}/end
            patch("{id}/end") {
                call.requireRole(DOCTOR) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.end(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
