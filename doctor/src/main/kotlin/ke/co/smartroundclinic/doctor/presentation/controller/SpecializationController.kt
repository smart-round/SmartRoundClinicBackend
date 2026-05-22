package ke.co.smartroundclinic.doctor.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.doctor.domain.service.SpecializationService
import ke.co.smartroundclinic.doctor.presentation.dto.request.AddSpecializationReq
import ke.co.smartroundclinic.doctor.presentation.dto.request.UpdateSpecializationReq
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val DOCTOR = "DOCTOR"

fun Route.specializationController(service: SpecializationService) {
    authenticate("auth-jwt") {
        route("/doctor/specializations") {

            // POST /doctor/specializations
            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<AddSpecializationReq>()
                    val result = service.add(body.toModel(doctorId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/specializations
            get {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.getMyWithDetails(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // PUT /doctor/specializations?id={id}
            put {
                call.requireRole(DOCTOR) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpdateSpecializationReq>()
                    val result = service.update(id, doctorId, body.specializationId, body.subSpecializationId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // DELETE /doctor/specializations?id={id}
            delete {
                call.requireRole(DOCTOR) {
                    val id = call.request.queryParameters["id"]
                        ?: throw MissingParametersException("id query parameter is required")
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.remove(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            // GET /doctor/specializations/{specializationId}/doctors?page=1&size=20
            get("doctors") {
                val specializationId = call.parameters["specializationId"]
                    ?: throw MissingParametersException("specializationId is required")
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val result = service.getDoctorsBySpecialization(specializationId, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }
    }
}
