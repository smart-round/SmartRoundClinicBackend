package ke.co.smartroundclinic.referral.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.referral.domain.service.ReferralService
import ke.co.smartroundclinic.referral.presentation.dto.request.CreateReferralReq

private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.referralController(service: ReferralService) {
    authenticate("auth-jwt") {
        route("/referral") {

            post {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val body = call.receive<CreateReferralReq>()
                    val result = service.create(body, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/eligibility") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val appointmentId = call.request.queryParameters["appointmentId"]
                        ?: throw MissingParametersException("appointmentId is required")
                    val result = service.eligibility(appointmentId, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/mine") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.mine(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/received") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val result = service.received(doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/pending") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val result = service.pending(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/history") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val result = service.history(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("/{id}/accept") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.accept(id, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("/{id}/decline") {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: throw MissingParametersException("id is required")
                    val result = service.decline(id, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/admin") {
                call.requireRole(ADMIN) {
                    val status = call.request.queryParameters["status"]
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                    val result = service.adminList(status, page, size)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/admin/stats") {
                call.requireRole(ADMIN) {
                    val result = service.adminStats()
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
