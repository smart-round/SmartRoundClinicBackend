package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.SpecializationService
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddSpecializationReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"

fun Route.specializationController(service: SpecializationService) {
    authenticate("auth-jwt") {
        route("/doctor/specializations") {

            // POST /doctor/specializations
            // Add a specialization to the doctor's profile.
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<AddSpecializationReq>()
                    val result = service.add(body.toModel(doctorId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/specializations
            // List all my specializations.
            get {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getMy(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // DELETE /doctor/specializations/{id}
            // Remove a specialization.
            delete("{id}") {
                call.requireRole(DOCTOR) {
                    val id = call.parameters["id"]
                        ?: throw MissingParametersException("id path parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.remove(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
