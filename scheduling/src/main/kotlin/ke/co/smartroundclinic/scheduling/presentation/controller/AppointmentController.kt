package ke.co.smartroundclinic.scheduling.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ke.co.smartroundclinic.infra.plugins.getRole
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.scheduling.domain.service.AppointmentService
import ke.co.smartroundclinic.scheduling.presentation.dto.request.BookAppointmentReq
import ke.co.smartroundclinic.scheduling.presentation.dto.request.CancelAppointmentReq

private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.appointmentController(appointmentService: AppointmentService) {
    authenticate("auth-jwt") {
        route("/scheduling/appointments") {

            post {
                call.requireRole(PATIENT) {
                    val patientId = call.getUserId() ?: return@requireRole
                    val body = call.receive<BookAppointmentReq>()
                    val result = appointmentService.book(body, patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/patient/{patientId}") {
                call.requireRole(PATIENT, DOCTOR) {
                    val patientId = call.parameters["patientId"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "patientId is required")
                        return@requireRole
                    }
                    val result = appointmentService.getByPatient(patientId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/doctor/{doctorId}") {
                call.requireRole(DOCTOR, PATIENT) {
                    val doctorId = call.parameters["doctorId"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "doctorId is required")
                        return@requireRole
                    }
                    val date = call.request.queryParameters["date"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "date query parameter is required")
                        return@requireRole
                    }
                    val result = appointmentService.getByDoctor(doctorId, date)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            get("/{id}") {
                call.requireRole(DOCTOR, PATIENT) {
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "id is required")
                        return@requireRole
                    }
                    val result = appointmentService.getById(id)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("/{id}/confirm") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "id is required")
                        return@requireRole
                    }
                    val result = appointmentService.confirm(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("/{id}/cancel") {
                call.requireRole(DOCTOR, PATIENT) {
                    val userId = call.getUserId() ?: return@requireRole
                    val role = call.getRole() ?: return@requireRole
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "id is required")
                        return@requireRole
                    }
                    val body = call.receive<CancelAppointmentReq>()
                    val result = appointmentService.cancel(id, userId, role, body)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("/{id}/complete") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "id is required")
                        return@requireRole
                    }
                    val result = appointmentService.complete(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            patch("/{id}/no-show") {
                call.requireRole(DOCTOR) {
                    val doctorId = call.getUserId() ?: return@requireRole
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "id is required")
                        return@requireRole
                    }
                    val result = appointmentService.markNoShow(id, doctorId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }
        }
    }
}
