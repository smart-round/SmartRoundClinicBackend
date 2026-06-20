package ke.co.smartroundclinic.patient.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ke.co.smartroundclinic.patient.domain.service.PersonalInformationService
import ke.co.smartroundclinic.patient.presentation.dto.request.CreatePersonalInformationReq
import ke.co.smartroundclinic.patient.presentation.dto.request.UpdatePersonalInformationReq
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole

private const val PATIENT = "PATIENT"
private const val ADMIN = "ADMIN"

fun Route.personalInformationController(service: PersonalInformationService) {
    authenticate("auth-jwt") {
        route("/patient/personal-information") {

            post {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<CreatePersonalInformationReq>()
                    val result = service.create(body.toModel(patientId))
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/all") {
                call.requireRole(ADMIN) {
                    val result = service.getAll()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get {
                call.requireRole(PATIENT, ADMIN, "DOCTOR") {
                    val id = call.parameters["id"]
                    val patientId = (id ?: call.getUserId()) ?: return@requireRole
                    val result = service.get(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            put {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<UpdatePersonalInformationReq>()
                    val result = service.update(patientId, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
